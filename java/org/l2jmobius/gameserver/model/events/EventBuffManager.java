package org.l2jmobius.gameserver.model.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.BufferManager;
import org.l2jmobius.gameserver.data.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.BuffSkillHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Stores the buff choice made at an event NPC and applies it when the event starts a fight.
 */
public final class EventBuffManager
{
	public static final String KOREAN = "korean";
	public static final String TOURNAMENT = "tournament";

	private static final Map<Integer, Choice> CHOICES = new ConcurrentHashMap<>();

	private EventBuffManager()
	{
	}

	public static void showWindow(Player player, String event, int npcObjectId)
	{
		final StringBuilder schemes = new StringBuilder();
		final Map<String, List<Integer>> available = BufferManager.getInstance().getPlayerSchemes(player.getObjectId());
		if ((available == null) || available.isEmpty())
		{
			schemes.append("<font color=\"B09878\">No tenes schemes creados.</font>");
		}
		else
		{
			for (Map.Entry<String, List<Integer>> entry : available.entrySet())
			{
				schemes.append("<button value=\"").append(entry.getKey()).append("\" action=\"bypass -h npc_").append(npcObjectId).append("_eventbuff_scheme ").append(event).append(" ").append(entry.getKey()).append("\" width=190 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npcObjectId);
		html.setHtml("<html><body><center><br><font name=hs12 color=LEVEL>BUFFS DEL EVENTO</font><br1>La eleccion se aplicara al comenzar tu combate.<br><br><button value=\"Auto Buff Guerrero\" action=\"bypass -h npc_" + npcObjectId + "_eventbuff " + event + " fighter\" width=190 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1><button value=\"Auto Buff Mago\" action=\"bypass -h npc_" + npcObjectId + "_eventbuff " + event + " mage\" width=190 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1><button value=\"Sin Buffs\" action=\"bypass -h npc_" + npcObjectId + "_eventbuff " + event + " none\" width=190 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br><br><font color=LEVEL>Mis schemes</font><br>" + schemes + "</center></body></html>");
		player.sendPacket(html);
	}

	public static void select(Player player, String event, String choice)
	{
		CHOICES.put(player.getObjectId(), new Choice(event, choice));
		player.sendMessage("Evento: seleccionaste " + choice + ".");
	}

	public static boolean hasSelection(Player player, String event)
	{
		final Choice choice = CHOICES.get(player.getObjectId());
		return (choice != null) && event.equals(choice.event);
	}

	public static void apply(Player player, String event)
	{
		final Choice choice = CHOICES.get(player.getObjectId());
		if ((choice == null) || !event.equals(choice.event) || "none".equalsIgnoreCase(choice.value))
		{
			return;
		}
		if ("fighter".equalsIgnoreCase(choice.value))
		{
			applyList(player, Config.PFIGHTER_SKILL_LIST);
			return;
		}
		if ("mage".equalsIgnoreCase(choice.value))
		{
			applyList(player, Config.PMAGE_SKILL_LIST);
			return;
		}
		for (int skillId : BufferManager.getInstance().getScheme(player.getObjectId(), choice.value))
		{
			final BuffSkillHolder holder = BufferManager.getInstance().getAvailableBuff(skillId);
			if (holder != null)
			{
				final Skill skill = SkillData.getInstance().getSkill(skillId, holder.getLevel());
				if (skill != null)
				{
					skill.applyEffects(player, player);
				}
			}
		}
		player.updateEffectIcons();
	}

	private static void applyList(Player player, List<Integer> skillIds)
	{
		for (int skillId : skillIds)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, SkillData.getInstance().getMaxLevel(skillId));
			if (skill != null)
			{
				skill.applyEffects(player, player);
			}
		}
		player.updateEffectIcons();
	}

	public static void clear(Player player, String event)
	{
		final Choice choice = CHOICES.get(player.getObjectId());
		if ((choice != null) && event.equals(choice.event))
		{
			CHOICES.remove(player.getObjectId());
		}
	}

	private static final class Choice
	{
		private final String event;
		private final String value;

		private Choice(String event, String value)
		{
			this.event = event;
			this.value = value;
		}
	}
}
