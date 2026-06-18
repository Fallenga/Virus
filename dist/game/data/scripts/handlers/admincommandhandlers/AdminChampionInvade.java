package handlers.admincommandhandlers;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

import custom.events.championInvade.ChampionInvade;

/**
 * @author L2JMobius / Adapted for Mobius
 */
public class AdminChampionInvade implements IAdminCommandHandler
{
	private static final Logger _log = Logger.getLogger(AdminChampionInvade.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_championinvade"
	};
	
	public static boolean _bestfarm_manual = false;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equalsIgnoreCase("admin_championinvade"))
		{
			if (ChampionInvade._started)
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Champion Invade Event]: Event Finished.");
				_log.info("----------------------------------------------------------------------------");
				ChampionInvade._aborted = true;
				finishEventSpecialChampion();
				
				activeChar.sendMessage("SYS: Voce Finalizou o Champion Invade Event Manualmente..");
			}
			else
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Champion Invade Event]: Event Started.");
				_log.info("----------------------------------------------------------------------------");
				initEventChampionEvent();
				_bestfarm_manual = true;
				activeChar.sendMessage("SYS: Voce ativou o Champion Invade Event Manualmente..");
			}
		}
		return true;
	}
	
	private static void initEventChampionEvent()
	{
		ThreadPool.execute(() -> ChampionInvade.StartedEvent());
	}
	
	private static void finishEventSpecialChampion()
	{
		ThreadPool.execute(() -> ChampionInvade.Finish_Event());
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}