/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.voicedcommandhandlers;

import java.util.List;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.events.tournament.Arena1x1;
import custom.events.tournament.Arena2x2;
import custom.events.tournament.Arena4x4;
import custom.events.tournament.Arena9x9;
import custom.events.tournament.TournamentConfig;
import custom.events.tournament.TournamentManager;

/**
 * Voice Command Handler para el Evento Tournament Permite usar el comando .tmevent para interactuar con el torneo
 */
public class TmEvent implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"tmevent"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (command.equalsIgnoreCase("tmevent"))
		{
			// Si no hay parámetros (ej: solo escribió .tmevent), abrimos el menú HTML
			if ((params == null) || params.trim().isEmpty())
			{
				showMenu(player);
				return true;
			}
			
			String[] args = params.split(" ");
			String action = args[0].toLowerCase(); // Limpiado de caracteres ocultos
			
			if (action.equals("register"))
			{
				registerPlayer(player);
			}
			else if (action.equals("cancel") || action.equals("unregister"))
			{
				cancelRegistration(player);
			}
			else
			{
				showMenu(player);
			}
		}
		return true;
	}
	
	private void showMenu(Player player)
	{
		final TournamentManager manager = TournamentManager.getInstance();
		if (manager == null)
		{
			player.sendMessage("Tournament manager is not initialized.");
			return;
		}
		
		final NpcHtmlMessage htm = new NpcHtmlMessage(0);
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><title>Tournament panel</title><body>");
		sb.append("<center>");
		sb.append("<br>");
		sb.append("<font color=\"LEVEL\">" + TournamentConfig.TM_NAME + " Panel</font><br>");
		sb.append("<img src=\"L2UI.SquareGray\" width=280 height=1><br><br>");
		
		if (manager.isStarted())
		{
			sb.append("Event Status: <font color=\"00FF00\">ACTIVE</font><br>");
			if (manager.isJoining())
			{
				sb.append("Registration: <font color=\"00FF00\">OPEN</font><br><br>");
			}
			else
			{
				sb.append("Registration: <font color=\"FF0000\">CLOSED (Fights running)</font><br><br>");
			}
		}
		else
		{
			sb.append("Event Status: <font color=\"FF0000\">INACTIVE</font><br>");
			sb.append("Next Event: <font color=\"LEVEL\">" + manager.getNextTime() + "</font><br><br>");
		}
		
		// Cuadro de estado de arenas inscritas
		sb.append("<table width=250>");
		sb.append("<tr><td><font color=\"AAAAAA\">1v1 Arena:</font></td><td align=right>" + Arena1x1.getInstance().getRegisteredCount() + " registered</td></tr>");
		sb.append("<tr><td><font color=\"AAAAAA\">2v2 Arena:</font></td><td align=right>" + Arena2x2.getInstance().getRegisteredCount() + " registered</td></tr>");
		sb.append("<tr><td><font color=\"AAAAAA\">4v4 Arena:</font></td><td align=right>" + Arena4x4.getInstance().getRegisteredCount() + " registered</td></tr>");
		sb.append("<tr><td><font color=\"AAAAAA\">9v9 Arena:</font></td><td align=right>" + Arena9x9.getInstance().getRegisteredCount() + " registered</td></tr>");
		sb.append("</table><br><br>");
		
		final boolean isReg = isAlreadyRegistered(player);
		if (isReg)
		{
			sb.append("<font color=\"00FF00\">You are currently registered!</font><br><br>");
			sb.append("<button value=\"Cancel Registration\" action=\"bypass -h user_tmevent cancel\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
		}
		else
		{
			if (manager.isJoining())
			{
				sb.append("<font color=\"AAAAAA\">Your party size determines your category.</font><br><br>");
				sb.append("<button value=\"Register Team / Solo\" action=\"bypass -h user_tmevent register\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			}
			else
			{
				sb.append("<font color=\"FF0000\">Registration is currently closed.</font><br>");
			}
		}
		
		sb.append("<br><button value=\"Refresh\" action=\"bypass -h user_tmevent\" width=75 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</center>");
		sb.append("</body></html>");
		htm.setHtml(sb.toString());
		player.sendPacket(htm);
	}
	
	private boolean isAlreadyRegistered(Player player)
	{
		return Arena1x1.getInstance().isRegistered(player) || Arena2x2.getInstance().isRegistered(player) || Arena4x4.getInstance().isRegistered(player) || Arena9x9.getInstance().isRegistered(player);
	}
	
	private void registerPlayer(Player player)
	{
		final TournamentManager manager = TournamentManager.getInstance();
		if (manager == null)
		{
			player.sendMessage("Tournament manager is not initialized.");
			return;
		}
		
		if (!manager.isJoining())
		{
			player.sendMessage("Registration is closed or the event hasn't started yet!");
			return;
		}
		
		if (isAlreadyRegistered(player))
		{
			player.sendMessage("You or someone in your party is already registered in the tournament!");
			return;
		}
		
		if (player.isInParty())
		{
			if (player.getParty().getLeader() != player)
			{
				player.sendMessage("Only the party leader can register the team!");
				return;
			}
			
			final int partySize = player.getParty().getMemberCount();
			final List<Player> members = player.getParty().getMembers();
			
			if (partySize == 2)
			{
				if (Arena2x2.getInstance().register(members.get(0), members.get(1)))
				{
					player.getParty().getMembers().forEach(member ->
					{
						if (member != null)
						{
							member.sendMessage("Your party has been successfully registered for 2v2 Tournament!");
						}
					});
				}
			}
			else if (partySize == 4)
			{
				if (Arena4x4.getInstance().register(members.get(0), members.get(1), members.get(2), members.get(3)))
				{
					player.getParty().getMembers().forEach(member ->
					{
						if (member != null)
						{
							member.sendMessage("Your party has been successfully registered for 4v4 Tournament!");
						}
					});
				}
			}
			else if (partySize == 9)
			{
				if (Arena9x9.getInstance().register(members.get(0), members.get(1), members.get(2), members.get(3), members.get(4), members.get(5), members.get(6), members.get(7), members.get(8)))
				{
					player.getParty().getMembers().forEach(member ->
					{
						if (member != null)
						{
							member.sendMessage("Your party has been successfully registered for 9v9 Tournament!");
						}
					});
				}
			}
			else
			{
				player.sendMessage("Invalid party size! Tournament only supports: Solo (1v1), Duo (2v2), Party (4v4) or Full Party (9v9).");
			}
		}
		else
		{
			// Registro Solo (1v1)
			if (Arena1x1.getInstance().register(player))
			{
				player.sendMessage("You have been successfully registered for 1v1 Tournament!");
			}
		}
		showMenu(player);
	}
	
	private void cancelRegistration(Player player)
	{
		if (isAlreadyRegistered(player))
		{
			boolean removed = false;
			if (Arena1x1.getInstance().isRegistered(player))
			{
				removed = Arena1x1.getInstance().remove(player);
			}
			else if (Arena2x2.getInstance().isRegistered(player))
			{
				removed = Arena2x2.getInstance().remove(player);
			}
			else if (Arena4x4.getInstance().isRegistered(player))
			{
				removed = Arena4x4.getInstance().remove(player);
			}
			else if (Arena9x9.getInstance().isRegistered(player))
			{
				removed = Arena9x9.getInstance().remove(player);
			}
			if (removed)
			{
				player.sendMessage("Your registration in the tournament has been cancelled.");
			}
			else
			{
				player.sendMessage("Could not cancel registration or you were already removed.");
			}
		}
		else
		{
			player.sendMessage("You are not registered in any tournament category!");
		}
		showMenu(player);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}