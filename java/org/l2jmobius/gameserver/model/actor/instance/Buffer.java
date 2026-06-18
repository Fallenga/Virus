package org.l2jmobius.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Trance, Bluur
 * @adapted for L2jmobius Reanimation
 */
public final class Buffer extends Npc
{
	public Buffer(NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		
		int buffid = 0, bufflevel = 1;
		if (st.countTokens() == 2)
		{
			buffid = Integer.valueOf(st.nextToken());
			bufflevel = Integer.valueOf(st.nextToken());
		}
		else if (st.countTokens() == 1)
		{
			buffid = Integer.valueOf(st.nextToken());
		}
		
		if (actualCommand.equalsIgnoreCase("getbuff"))
		{
			SkillData.getInstance().getSkill(buffid, bufflevel).applyEffects(this, player);
			broadcastPacket(new MagicSkillUse(this, player, buffid, bufflevel, 500, 0));
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 0, player));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("restore"))
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			broadcastPacket(new MagicSkillUse(this, player, 1258, 4, 500, 0));
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 0, player));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("fbuff"))
		{
			player.stopAllEffects();
			
			broadcastPacket(new MagicSkillUse(this, player, 1363, 1, 100, 0)); // Chant of Victory
			
			player.stopAllEffects();
			SkillData.getInstance().getSkill(1204, 2).applyEffects(player, player); // Wind Walk
			SkillData.getInstance().getSkill(1040, 3).applyEffects(player, player); // Shield
			SkillData.getInstance().getSkill(1068, 3).applyEffects(player, player); // Might
			SkillData.getInstance().getSkill(1036, 2).applyEffects(player, player); // Magic Barrier
			SkillData.getInstance().getSkill(1045, 6).applyEffects(player, player); // Blessed Body
			SkillData.getInstance().getSkill(1240, 3).applyEffects(player, player); // Guidance
			SkillData.getInstance().getSkill(1242, 3).applyEffects(player, player); // Death Whisper
			SkillData.getInstance().getSkill(1268, 4).applyEffects(player, player); // Vampiric Rage
			SkillData.getInstance().getSkill(1062, 2).applyEffects(player, player); // Berserker
			SkillData.getInstance().getSkill(1077, 3).applyEffects(player, player); // Focus
			SkillData.getInstance().getSkill(1086, 2).applyEffects(player, player); // Haste
			SkillData.getInstance().getSkill(275, 1).applyEffects(player, player); // Fury
			SkillData.getInstance().getSkill(274, 1).applyEffects(player, player); // Fire
			SkillData.getInstance().getSkill(271, 1).applyEffects(player, player); // Warrior
			SkillData.getInstance().getSkill(310, 1).applyEffects(player, player); // Vampire
			SkillData.getInstance().getSkill(264, 1).applyEffects(player, player); // Earth
			SkillData.getInstance().getSkill(267, 1).applyEffects(player, player); // Warding
			SkillData.getInstance().getSkill(304, 1).applyEffects(player, player); // Vitality
			SkillData.getInstance().getSkill(269, 1).applyEffects(player, player); // Hunter
			SkillData.getInstance().getSkill(268, 1).applyEffects(player, player); // Wind
			SkillData.getInstance().getSkill(1363, 1).applyEffects(player, player); // Chant of Victory
			SkillData.getInstance().getSkill(4699, 3).applyEffects(player, player); // Chant of Victory
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(player, "data/html/mods/Buffer/33299.htm");
			sendHtmlMessage(player, html);
			player.sendPacket(html);
			
		}
		else if (actualCommand.equalsIgnoreCase("mbuff"))
		{
			player.stopAllEffects();
			
			broadcastPacket(new MagicSkillUse(this, player, 1413, 1, 100, 0)); // Magnu's Chant
			
			player.stopAllEffects();
			SkillData.getInstance().getSkill(1204, 2).applyEffects(player, player); // Wind Walk
			SkillData.getInstance().getSkill(1040, 3).applyEffects(player, player); // Shield
			SkillData.getInstance().getSkill(1036, 2).applyEffects(player, player); // Magic Barrier
			SkillData.getInstance().getSkill(1045, 6).applyEffects(player, player); // Blessed Body
			SkillData.getInstance().getSkill(1048, 6).applyEffects(player, player); // Blessed Soul
			SkillData.getInstance().getSkill(1085, 3).applyEffects(player, player); // Acumen
			SkillData.getInstance().getSkill(1059, 3).applyEffects(player, player); // Empower
			SkillData.getInstance().getSkill(1062, 2).applyEffects(player, player); // Berserker
			SkillData.getInstance().getSkill(1303, 2).applyEffects(player, player); // Wild Magic
			SkillData.getInstance().getSkill(1078, 6).applyEffects(player, player); // Concentration
			SkillData.getInstance().getSkill(273, 1).applyEffects(player, player); // Mystic
			SkillData.getInstance().getSkill(276, 1).applyEffects(player, player); // Concentration
			SkillData.getInstance().getSkill(365, 1).applyEffects(player, player); // Siren
			SkillData.getInstance().getSkill(264, 1).applyEffects(player, player); // Earth
			SkillData.getInstance().getSkill(267, 1).applyEffects(player, player); // Warding
			SkillData.getInstance().getSkill(304, 1).applyEffects(player, player); // Vitality
			SkillData.getInstance().getSkill(268, 1).applyEffects(player, player); // Wind
			SkillData.getInstance().getSkill(349, 1).applyEffects(player, player); // Renewal
			SkillData.getInstance().getSkill(1355, 1).applyEffects(player, player); // Magnu's Chant
			SkillData.getInstance().getSkill(4703, 3).applyEffects(player, player); // Magnu's Chant
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(player, "data/html/mods/Buffer/33299.htm");
			sendHtmlMessage(player, html);
			player.sendPacket(html);
		}
		
		else if (actualCommand.equalsIgnoreCase("cancel"))
		{
			player.stopAllEffects();
			broadcastPacket(new MagicSkillUse(this, player, 1056, 12, 500, 0));
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 0, player));
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
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
		return "data/html/mods/Buffer/" + filename + ".htm";
	}
	
	private void sendHtmlMessage(Player player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		player.sendPacket(html);
	}
	
}