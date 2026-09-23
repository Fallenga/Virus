package org.l2jmobius.gameserver.model.events.fos;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedFOSEvent implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"fosinfo",
		"fosjoin",
		"fosleave",
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equals("fosinfo"))
		{
			if (FOSEvent.isStarting() || FOSEvent.isStarted())
			{
				showFOSStatuPage(activeChar);
				return true;
			}
			activeChar.sendMessage("Fortress event is not in progress.");
			return false;
		}
		else if (command.equals("fosjoin"))
		{
			if (!FOSEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				FOSEvent.onBypass("fos_event_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are already registered.");
				return false;
			}
		}
		else if (command.equals("fosleave"))
		{
			if (FOSEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				FOSEvent.onBypass("fos_event_remove_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are not registered.");
				return false;
			}
		}
		return true;
	}

	private static void showFOSStatuPage(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		html.setFile(activeChar, "data/html/mods/events/fos/Status.htm");
		html.replace("%team1name%", FOSConfig.FOS_EVENT_TEAM_1_NAME);
		html.replace("%team1playercount%", String.valueOf(FOSEvent.getTeamsPlayerCounts()[0]));
		html.replace("%team1points%", String.valueOf(FOSEvent.getTeamsPoints()[0]));
		html.replace("%team2name%", FOSConfig.FOS_EVENT_TEAM_2_NAME);
		html.replace("%team2playercount%", String.valueOf(FOSEvent.getTeamsPlayerCounts()[1]));
		html.replace("%team2points%", String.valueOf(FOSEvent.getTeamsPoints()[1]));
		activeChar.sendPacket(html);	
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}