package org.l2jmobius.gameserver.model.actor.instance;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.BufferManager;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.BuffSkillHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Baggos (Adaptado para L2J Mobius Classic Interlude)
 */
public class PlayerBuffer extends Npc
{
	private static final Logger LOGGER = Logger.getLogger(PlayerBuffer.class.getName());
	private static final int PAGE_LIMIT = 6;
	private static final int BUFF_DURATION = 3600; // 1 hour
	
	public PlayerBuffer(NpcTemplate template)
	{
		super(template);
	}
	
	private void showMainWindow(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, getHtmlPath(getId(), 0, activeChar));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%name%", activeChar.getName());
		html.replace("%buffcount%", "You have " + activeChar.getBuffCount() + "/" + activeChar.getMaxBuffCount() + " buffs.");
		
		activeChar.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		// Validaciones iniciales
		if ((player.getPvpFlag() > 0) && Config.PRESTRICT_USE_BUFFER_ON_PVPFLAG)
		{
			player.sendMessage("You can't use buffer when you are pvp flagged.");
			return;
		}
		
		if (player.isInCombat() && Config.PRESTRICT_USE_BUFFER_IN_COMBAT)
		{
			player.sendMessage("You can't use buffer when you are in combat.");
			return;
		}
		
		if (player.isDead())
		{
			return;
		}
		
		// Log para debugging
		// LOGGER.info("PlayerBuffer Command received: " + command);
		
		// Limpiar el comando. Normalmente Mobius ya entrega el comando sin "npc_<objectId>_",
		// pero soportamos también el bypass completo por compatibilidad.
		String cleanCommand = command.trim();
		if (cleanCommand.startsWith("npc_"))
		{
			final int separator = cleanCommand.indexOf('_', 4);
			if (separator > 0)
			{
				cleanCommand = cleanCommand.substring(separator + 1);
			}
		}
		
		// Mismo hack usado por SchemeBuffer: permite nombres con espacios al crear schemes.
		if (cleanCommand.toLowerCase().startsWith("createscheme "))
		{
			cleanCommand = "createscheme;" + cleanCommand.substring("createscheme ".length());
		}
		
		// Los comandos de schemes usan ';' para no romper nombres con espacios.
		// Los comandos viejos del buffer manual siguen aceptando espacios.
		String[] params = cleanCommand.contains(";") ? cleanCommand.split(";", -1) : cleanCommand.split("\\s+");
		if (params.length == 0)
		{
			return;
		}
		
		String actualCommand = params[0].toLowerCase();
		
		// Manejar diferentes comandos
		if (actualCommand.equals("bufflist"))
		{
			if (params.length > 1)
			{
				autoBuffFunction(player, params[1]);
			}
			else
			{
				player.sendMessage("Uso: bufflist [fighter|mage]");
			}
			return;
		}
		else if (actualCommand.equals("restore"))
		{
			// Restaurar HP/MP/CP
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			final Pet summon = player.getPet();
			if (summon != null)
			{
				summon.setCurrentHp(summon.getMaxHp());
				summon.setCurrentMp(summon.getMaxMp());
			}
			
			showMainWindow(player);
			return;
		}
		else if (actualCommand.equals("cancellation"))
		{
			// Cancelar todos los buffs
			SkillData.getInstance().getSkill(1056, 1).applyEffects(this, player);
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			player.broadcastPacket(new MagicSkillUse(this, player, 1056, 1, 850, 0));
			player.stopAllEffects();
			
			final Pet summon = player.getPet();
			if (summon != null)
			{
				summon.stopAllEffects();
			}
			
			showMainWindow(player);
			return;
		}
		else if (actualCommand.equals("openlist"))
		{
			if (params.length > 2)
			{
				String category = params[1];
				String htmfile = params[2];
				showListWindow(player, category, htmfile);
			}
			return;
		}
		else if (actualCommand.equals("support"))
		{
			showGiveBuffsWindow(player);
			return;
		}
		else if (actualCommand.equals("createscheme"))
		{
			if (params.length > 1)
			{
				String schemeName = params[1];
				createScheme(player, schemeName);
			}
			else
			{
				player.sendMessage("Usage: createscheme <schemeName>");
			}
			return;
		}
		else if (actualCommand.equals("deletescheme"))
		{
			if (params.length > 1)
			{
				String schemeName = params[1];
				deleteScheme(player, schemeName);
			}
			else
			{
				player.sendMessage("Usage: deletescheme <schemeName>");
			}
			return;
		}
		else if (actualCommand.equals("main"))
		{
			showMainWindow(player);
			return;
		}
		else if (actualCommand.equals("editschemes"))
		{
			if (params.length > 3)
			{
				String groupType = params[1];
				String schemeName = params[2];
				int page = Integer.parseInt(params[3]);
				showEditSchemeWindow(player, groupType, schemeName, page);
			}
			else
			{
				player.sendMessage("Usage: editschemes <groupType> <schemeName> <page>");
			}
			return;
		}
		else if (actualCommand.equals("skillselect") || actualCommand.equals("skillunselect"))
		{
			if (params.length > 4)
			{
				String groupType = params[1];
				String schemeName = params[2];
				int skillId = Integer.parseInt(params[3]);
				int page = Integer.parseInt(params[4]);
				
				List<Integer> skills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
				
				if (actualCommand.equals("skillselect") && !schemeName.equalsIgnoreCase("none"))
				{
					if (!skills.contains(skillId))
					{
						final Skill skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
						
						if (skill != null)
						{
							// Dances / Songs
							if (skill.isDance())
							{
								if (getCountOf(skills, true) < Config.DANCES_MAX_AMOUNT)
								{
									skills.add(skillId);
									player.sendMessage("Dance/Song added to scheme " + schemeName);
									BufferManager.getInstance().saveSchemes();
								}
								else
								{
									player.sendMessage("This scheme has reached the maximum amount of dances/songs (" + Config.DANCES_MAX_AMOUNT + ").");
								}
							}
							// Buffs normales
							else
							{
								if (getCountOf(skills, false) < player.getStat().getMaxBuffCount())
								{
									skills.add(skillId);
									player.sendMessage("Buff added to scheme " + schemeName);
									BufferManager.getInstance().saveSchemes();
								}
								else
								{
									player.sendMessage("This scheme has reached the maximum amount of buffs (" + player.getStat().getMaxBuffCount() + ").");
								}
							}
						}
						else
						{
							player.sendMessage("Skill " + skillId + " not found.");
						}
					}
					else
					{
						player.sendMessage("Skill already in scheme.");
					}
				}
				else if (actualCommand.equals("skillunselect"))
				{
					if (skills.remove(Integer.valueOf(skillId)))
					{
						player.sendMessage("Skill " + skillId + " removed from scheme " + schemeName);
						BufferManager.getInstance().saveSchemes();
					}
					else
					{
						player.sendMessage("Skill not found in scheme.");
					}
				}
				
				showEditSchemeWindow(player, groupType, schemeName, page);
			}
			else
			{
				player.sendMessage("Usage: skillselect <groupType> <schemeName> <skillId> <page>");
			}
			
			return;
		}
		else if (actualCommand.equals("givebuffs"))
		{
			
			if (params.length > 2)
			{
				String schemeName = params[1];
				int cost = Integer.parseInt(params[2]);
				String targetType = (params.length > 3) ? params[3] : "player";
				
				Creature target = null;
				if (targetType.equalsIgnoreCase("pet"))
				{
					target = player.getPet();
					if (target == null)
					{
						player.sendMessage("You don't have a pet.");
						showGiveBuffsWindow(player);
						return;
					}
				}
				else
				{
					target = player;
				}
				
				List<Integer> skills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
				if (skills.isEmpty())
				{
					player.sendMessage("Scheme '" + schemeName + "' is empty.");
					showGiveBuffsWindow(player);
					return;
				}
				
				// Check cost
				if ((cost > 0) && !player.reduceAdena("NPC Buffer", cost, this, true))
				{
					player.sendMessage("You don't have enough adena.");
					showGiveBuffsWindow(player);
					return;
				}
				
				// Apply buffs
				for (int skillId : skills)
				{
					BuffSkillHolder holder = BufferManager.getInstance().getAvailableBuff(skillId);
					if (holder != null)
					{
						Skill skill = SkillData.getInstance().getSkill(skillId, holder.getLevel());
						if (skill != null)
						{
							skill.applyEffects(this, target, false, BUFF_DURATION);
							broadcastPacket(new MagicSkillUse(this, target, skillId, holder.getLevel(), 450, 0));
						}
					}
				}
				
				player.sendMessage("Buffs from scheme '" + schemeName + "' applied successfully.");
				showGiveBuffsWindow(player);
			}
			else
			{
				player.sendMessage("Usage: givebuffs <schemeName> <cost> [pet]");
			}
			return;
		}
		else if (actualCommand.equals("dobuff"))
		{
			if (params.length > 4)
			{
				int buffid = Integer.parseInt(params[1]);
				int bufflevel = Integer.parseInt(params[2]);
				
				String category = params[3];
				String windowhtml = params[4];
				
				Creature target = player;
				
				if (category.startsWith("pet"))
				{
					if (player.getPet() == null)
					{
						player.sendMessage("You don't have a pet.");
						showMainWindow(player);
						return;
					}
					
					target = player.getPet();
				}
				
				Skill skill = SkillData.getInstance().getSkill(buffid, bufflevel);
				
				if (skill == null)
				{
					player.sendMessage("Skill " + buffid + " level " + bufflevel + " not found.");
					return;
				}
				
				MagicSkillUse mgc = new MagicSkillUse(this, target, buffid, bufflevel, 1150, 0);
				
				player.sendPacket(mgc);
				player.broadcastPacket(mgc);
				
				skill.applyEffects(this, target, false, BUFF_DURATION);
				
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/mods/buffer/" + category + "/" + windowhtml + ".htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%name%", player.getName());
				player.sendPacket(html);
			}
			
			return;
		}
		else if (actualCommand.equals("getbuff"))
		{
			if (params.length > 2)
			{
				int buffid = Integer.parseInt(params[1]);
				int bufflevel = Integer.parseInt(params[2]);
				if (buffid != 0)
				{
					SkillData.getInstance().getSkill(buffid, bufflevel).applyEffects(this, player, false, BUFF_DURATION);
					broadcastPacket(new MagicSkillUse(this, player, buffid, bufflevel, 450, 0));
					showMainWindow(player);
				}
			}
			return;
		}
		else
		{
			LOGGER.warning("PlayerBuffer: Unknown NPC bypass: \"" + cleanCommand + "\" NpcId: " + getId());
			player.sendMessage("Unknown command: " + cleanCommand);
		}
		
		// IMPORTANTE: NO llamar a super.onBypassFeedback para evitar mensajes duplicados
		// super.onBypassFeedback(player, command);
	}
	
	// Métodos auxiliares
	private void createScheme(Player player, String schemeName)
	{
		if (schemeName.length() > 14)
		{
			player.sendMessage("Scheme's name must contain up to 14 chars. Spaces are trimmed.");
			return;
		}
		
		schemeName = schemeName.trim();
		Map<String, List<Integer>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes != null)
		{
			if (schemes.size() >= Config.PBUFFER_MAX_SCHEMES)
			{
				player.sendMessage("Maximum schemes amount (" + Config.PBUFFER_MAX_SCHEMES + ") is already reached.");
				return;
			}
			
			if (schemes.containsKey(schemeName))
			{
				player.sendMessage("The scheme name already exists.");
				return;
			}
		}
		
		BufferManager.getInstance().setScheme(player.getObjectId(), schemeName, new ArrayList<>());
		BufferManager.getInstance().saveSchemes();
		player.sendMessage("Scheme '" + schemeName + "' created successfully.");
		showGiveBuffsWindow(player);
	}
	
	private void deleteScheme(Player player, String schemeName)
	{
		Map<String, List<Integer>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
		if ((schemes != null) && schemes.containsKey(schemeName))
		{
			schemes.remove(schemeName);
			BufferManager.getInstance().saveSchemes();
			player.sendMessage("Scheme '" + schemeName + "' deleted.");
		}
		else
		{
			player.sendMessage("Scheme '" + schemeName + "' not found.");
		}
		showGiveBuffsWindow(player);
	}
	
	private void showListWindow(Player player, String category, String htmfile)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (category.equals("null"))
		{
			html.setFile(player, "data/html/mods/buffer/" + htmfile + ".htm");
			if (htmfile.equals("index"))
			{
				html.replace("%name%", player.getName());
				html.replace("%buffcount%", "You have " + player.getBuffCount() + "/" + player.getMaxBuffCount() + " buffs.");
			}
		}
		else
		{
			html.setFile(player, "data/html/mods/buffer/" + category + "/" + htmfile + ".htm");
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String filename = "";
		if (value == 0)
		{
			filename = Integer.toString(npcId);
		}
		else
		{
			filename = npcId + "-" + value;
		}
		return "data/html/mods/buffer/" + filename + ".htm";
	}
	
	/**
	 * Send an html packet to the {@link Player} set a parameter with Give Buffs menu info for player and pet, depending on targetType parameter {player, pet}.
	 * @param player : The {@link Player} to make checks on.
	 */
	private void showGiveBuffsWindow(Player player)
	{
		final StringBuilder sb = new StringBuilder(200);
		
		final Map<String, List<Integer>> schemes = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
		if ((schemes == null) || schemes.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">You haven't defined any scheme.</font>");
		}
		else
		{
			for (Entry<String, List<Integer>> scheme : schemes.entrySet())
			{
				final int cost = getFee(scheme.getValue());
				sb.append("<font color=\"LEVEL\">" + scheme.getKey() + " [" + scheme.getValue().size() + " skill(s)]" + ((cost > 0) ? " - cost: " + NumberFormat.getInstance(Locale.ENGLISH).format(cost) : "") + "</font><br1>");
				sb.append("<a action=\"bypass npc_%objectId%_givebuffs;" + scheme.getKey() + ";" + cost + "\">Use on Me</a>&nbsp;|&nbsp;");
				sb.append("<a action=\"bypass npc_%objectId%_givebuffs;" + scheme.getKey() + ";" + cost + ";pet\">Use on Pet</a>&nbsp;|&nbsp;");
				sb.append("<a action=\"bypass npc_%objectId%_editschemes;Buffs;" + scheme.getKey() + ";1\">Edit</a>&nbsp;|&nbsp;");
				sb.append("<a action=\"bypass npc_%objectId%_deletescheme;" + scheme.getKey() + "\">Delete</a><br>");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, getHtmlPath(getId(), 1, player));
		html.replace("%schemes%", sb.toString());
		html.replace("%max_schemes%", String.valueOf(Config.PBUFFER_MAX_SCHEMES));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	/**
	 * Send an html packet to the {@link Player} set as parameter with Edit Scheme Menu info. This allows the {@link Player} to edit each created scheme (add/delete skills)
	 * @param player : The {@link Player} to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @param page : The current checked page.
	 */
	private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		final List<Integer> schemeSkills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
		
		html.setFile(player, getHtmlPath(getId(), 2, player));
		html.replace("%schemename%", schemeName);
		html.replace("%count%", schemeSkills.size() + " / " + player.getMaxBuffCount());
		html.replace("%typesframe%", getTypesFrame(groupType, schemeName));
		html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName, page));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	/**
	 * @param player : The {@link Player} to make checks on.
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @param page : The current checked page.
	 * @return A {@link String} representing skills available for selection for a given groupType.
	 */
	private String getGroupSkillList(Player player, String groupType, String schemeName, int page)
	{
		// Retrieve the entire skills list based on group type.
		List<Integer> skills = BufferManager.getInstance().getSkillsIdsByType(groupType);
		if (skills.isEmpty())
		{
			return "That group doesn't contain any skills.";
		}
		
		// Calculate page number.
		final int max = (int) Math.ceil((double) skills.size() / PAGE_LIMIT);
		if (page > max)
		{
			page = max;
		}
		
		// Cut skills list up to page number.
		skills = skills.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skills.size()));
		
		final List<Integer> schemeSkills = BufferManager.getInstance().getScheme(player.getObjectId(), schemeName);
		final StringBuilder sb = new StringBuilder(skills.size() * 150);
		
		int row = 0;
		for (int skillId : skills)
		{
			sb.append(((row % 2) == 0 ? "<table width=\"280\" bgcolor=\"000000\"><tr>" : "<table width=\"280\"><tr>"));
			
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if (schemeSkills.contains(skillId))
			{
				sb.append("<td height=40 width=40><img src=\"" + skill.getIcon() + "\" width=32 height=32></td><td width=190>" + skill.getName() + "<br1><font color=\"B09878\">" + BufferManager.getInstance().getAvailableBuff(skillId).getDescription() + "</font></td><td><button value=\" \" action=\"bypass npc_%objectId%_skillunselect;" + groupType + ";" + schemeName + ";" + skillId + ";" + page + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
			}
			else
			{
				sb.append("<td height=40 width=40><img src=\"" + skill.getIcon() + "\" width=32 height=32></td><td width=190>" + skill.getName() + "<br1><font color=\"B09878\">" + BufferManager.getInstance().getAvailableBuff(skillId).getDescription() + "</font></td><td><button value=\" \" action=\"bypass npc_%objectId%_skillselect;" + groupType + ";" + schemeName + ";" + skillId + ";" + page + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			}
			
			sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=277 height=1>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
		{
			sb.append("<img height=41>");
		}
		
		// Build page footer.
		sb.append("<br><img src=\"L2UI.SquareGray\" width=277 height=1><table width=\"100%\" bgcolor=000000><tr>");
		
		if (page > 1)
		{
			sb.append("<td align=left width=70><a action=\"bypass npc_").append(getObjectId()).append("_editschemes;").append(groupType).append(";").append(schemeName).append(";").append(page - 1).append("\">Previous</a></td>");
		}
		else
		{
			sb.append("<td align=left width=70>Previous</td>");
		}
		
		sb.append("<td align=center width=100>Page ").append(page).append("</td>");
		
		if (page < max)
		{
			sb.append("<td align=right width=70><a action=\"bypass npc_").append(getObjectId()).append("_editschemes;").append(groupType).append(";").append(schemeName).append(";").append(page + 1).append("\">Next</a></td>");
		}
		else
		{
			sb.append("<td align=right width=70>Next</td>");
		}
		
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=277 height=1>");
		
		return sb.toString();
	}
	
	/**
	 * @param groupType : The group of skills to select.
	 * @param schemeName : The scheme to make check.
	 * @return A {@link String} representing all groupTypes available. The group currently on selection isn't linkable.
	 */
	private static String getTypesFrame(String groupType, String schemeName)
	{
		final StringBuilder sb = new StringBuilder(500);
		sb.append("<table>");
		
		int count = 0;
		for (String type : BufferManager.getInstance().getSkillTypes())
		{
			if (count == 0)
			{
				sb.append("<tr>");
			}
			
			if (groupType.equalsIgnoreCase(type))
			{
				sb.append("<td width=65>").append(type).append("</td>");
			}
			else
			{
				sb.append("<td width=65><a action=\"bypass npc_%objectId%_editschemes;").append(type).append(";").append(schemeName).append(";1\">").append(type).append("</a></td>");
			}
			
			count++;
			if (count == 4)
			{
				sb.append("</tr>");
				count = 0;
			}
		}
		
		if (!sb.toString().endsWith("</tr>"))
		{
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * @param list : A list of skill ids.
	 * @return a global fee for all skills contained in list.
	 */
	private static int getFee(List<Integer> list)
	{
		if (Config.PBUFFER_STATIC_BUFF_COST > 0)
		{
			return list.size() * Config.PBUFFER_STATIC_BUFF_COST;
		}
		
		int fee = 0;
		for (int sk : list)
		{
			fee += BufferManager.getInstance().getAvailableBuff(sk).getPrice();
		}
		
		return fee;
	}
	
	private static int getCountOf(List<Integer> skills, boolean dances)
	{
		int count = 0;
		
		for (int skillId : skills)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
			
			if ((skill != null) && (skill.isDance() == dances))
			{
				count++;
			}
		}
		
		return count;
	}
	
	private void autoBuffFunction(Player player, String bufflist)
	{
		ArrayList<Skill> skills_to_buff = new ArrayList<>();
		List<Integer> list = null;
		
		if (bufflist.equalsIgnoreCase("fighter"))
		{
			list = Config.PFIGHTER_SKILL_LIST;
		}
		else if (bufflist.equalsIgnoreCase("mage"))
		{
			list = Config.PMAGE_SKILL_LIST;
		}
		
		if (list != null)
		{
			for (int skillId : list)
			{
				Skill skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
				if (skill != null)
				{
					skills_to_buff.add(skill);
				}
			}
			
			for (Skill sk : skills_to_buff)
			{
				sk.applyEffects(player, player, false, BUFF_DURATION);
			}
			
			player.updateEffectIcons();
			
			list = null;
		}
		
		skills_to_buff.clear();
		
		showMainWindow(player);
	}
}