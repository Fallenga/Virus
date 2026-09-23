package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.model.events.fos.FOSConfig;
import org.l2jmobius.gameserver.model.events.fos.FOSEvent;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/** Registration NPC for the Fortress event. */
public class FOSRegistration extends Npc
{
	private static final String HTML_PATH = "data/html/mods/events/fos/";

	public FOSRegistration(NpcTemplate template)
	{
		super(template);
	}

	@Override
	public void showChatWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (FOSEvent.isStarted() || FOSEvent.isStarting())
		{
			html.setFile(player, HTML_PATH + "Status.htm");
			html.replace("%team1points%", String.valueOf(FOSEvent.getTeamsPoints()[0]));
			html.replace("%team2points%", String.valueOf(FOSEvent.getTeamsPoints()[1]));
		}
		else if (FOSEvent.isPlayerParticipant(player.getObjectId()))
		{
			html.setFile(player, HTML_PATH + "RemoveParticipation.htm");
		}
		else
		{
			html.setFile(player, HTML_PATH + "Participation.htm");
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%team1name%", FOSConfig.FOS_EVENT_TEAM_1_NAME);
		html.replace("%team2name%", FOSConfig.FOS_EVENT_TEAM_2_NAME);
		html.replace("%team1playercount%", String.valueOf(FOSEvent.getTeamsPlayerCounts()[0]));
		html.replace("%team2playercount%", String.valueOf(FOSEvent.getTeamsPlayerCounts()[1]));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("fos_event_"))
		{
			FOSEvent.onBypass(command, player);
			return;
		}
		super.onBypassFeedback(player, command);
	}
}
