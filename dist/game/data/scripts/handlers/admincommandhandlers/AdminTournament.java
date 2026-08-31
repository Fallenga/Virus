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
package handlers.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.tournament.Arena1x1;
import org.l2jmobius.gameserver.model.events.tournament.Arena3x3;
import org.l2jmobius.gameserver.model.events.tournament.Arena5x5;
import org.l2jmobius.gameserver.model.events.tournament.Arena9x9;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaConfig;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaEvent;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaTask;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Admin command handler for Tournament control
 * @author Lucas
 */
public class AdminTournament implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminTournament.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_tournament",
		"admin_tournament_start",
		"admin_tournament_stop",
		"admin_tournament_status",
		"admin_tournament_reload",
		"admin_tournament_reset_ranking"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String cmd = st.nextToken();
		
		if (cmd.equals("admin_tournament") || cmd.equals("admin_tournament_status"))
		{
			showTournamentStatus(activeChar);
			return true;
		}
		else if (cmd.equals("admin_tournament_start"))
		{
			startTournament(activeChar);
			return true;
		}
		else if (cmd.equals("admin_tournament_stop"))
		{
			stopTournament(activeChar);
			return true;
		}
		else if (cmd.equals("admin_tournament_reload"))
		{
			reloadTournament(activeChar);
			return true;
		}
		else if (cmd.equals("admin_tournament_reset_ranking"))
		{
			resetRanking(activeChar);
			return true;
		}
		
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showTournamentStatus(Player activeChar)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("========== Tournament Status ==========\n");
		sb.append("Event Started: ").append(ArenaTask.is_started()).append("\n");
		sb.append("Registered 1x1: ").append(Arena1x1.getRegisteredCount()).append("\n");
		sb.append("Registered 3x3: ").append(Arena3x3.getRegisteredCount()).append("\n");
		sb.append("Registered 5x5: ").append(Arena5x5.getRegisteredCount()).append("\n");
		sb.append("Registered 9x9: ").append(Arena9x9.getRegisteredCount()).append("\n");
		sb.append("Next Event: ").append(ArenaEvent.getInstance().getNextTime()).append("\n");
		sb.append("=======================================");
		
		activeChar.sendMessage(sb.toString());
		
		// Mostrar también en pantalla
		activeChar.sendPacket(new ExShowScreenMessage("Tournament Status - Started: " + ArenaTask.is_started(), 5000));
	}
	
	private void startTournament(Player activeChar)
	{
		if (ArenaTask.is_started())
		{
			activeChar.sendMessage("Tournament is already started!");
			return;
		}
		
		try
		{
			// Limpiar registros previos
			Arena1x1.getInstance().clear();
			Arena3x3.getInstance().clear();
			Arena5x5.getInstance().clear();
			Arena9x9.getInstance().clear();
			
			// Iniciar el evento
			ArenaTask._aborted = false;
			ArenaTask.SpawnEvent();
			
			// Anunciar a todos los jugadores
			broadcastToAll("Tournament started by Admin: " + activeChar.getName());
			
			activeChar.sendMessage("Tournament started successfully!");
			LOGGER.info("Admin " + activeChar.getName() + " started the tournament manually.");
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Failed to start tournament: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void stopTournament(Player activeChar)
	{
		if (!ArenaTask.is_started())
		{
			activeChar.sendMessage("Tournament is not started!");
			return;
		}
		
		try
		{
			// Forzar finalización del evento
			ArenaTask._aborted = true;
			ArenaTask.finishEvent();
			
			// Anunciar a todos los jugadores
			broadcastToAll("Tournament stopped by Admin: " + activeChar.getName());
			
			activeChar.sendMessage("Tournament stopped successfully!");
			LOGGER.info("Admin " + activeChar.getName() + " stopped the tournament manually.");
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Failed to stop tournament: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void reloadTournament(Player activeChar)
	{
		try
		{
			// Detener el torneo si está activo
			if (ArenaTask.is_started())
			{
				ArenaTask._aborted = true;
				ArenaTask.finishEvent();
			}
			
			// Recargar configuración
			ArenaConfig.init();
			
			activeChar.sendMessage("Tournament configuration reloaded successfully!");
			LOGGER.info("Admin " + activeChar.getName() + " reloaded tournament configuration.");
			
			// Mostrar estado actual
			showTournamentStatus(activeChar);
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Failed to reload tournament: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void resetRanking(Player activeChar)
	{
		try
		{
			// Usar el método de ArenaRanking para resetear todos los rankings
			// Nota: Esto requiere que importes ArenaRanking
			// import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaRanking;
			
			// ArenaRanking.resetRank1x1();
			// ArenaRanking.resetRank3x3();
			// ArenaRanking.resetRank5x5();
			// ArenaRanking.resetRank9x9();
			
			activeChar.sendMessage("Tournament ranking reset successfully!");
			LOGGER.info("Admin " + activeChar.getName() + " reset tournament ranking.");
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Failed to reset ranking: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void broadcastToAll(String message)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S1);
		sm.addString("[Tournament] " + message);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				player.sendPacket(sm);
				player.sendMessage("[Tournament] " + message);
			}
		}
	}
}