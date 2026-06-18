/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.voicedcommandhandlers;

import java.util.Set;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.quest.Quest;

public class Event_TVT implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"tvtjoin",
		"tvtleave",
		"tvtinfo"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith("tvtjoin"))
		{
			JoinTvT(activeChar);
		}
		else if (command.startsWith("tvtleave"))
		{
			LeaveTvT(activeChar);
		}
		else if (command.startsWith("tvtinfo"))
		{
			TvTinfo(activeChar);
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	// Helper: Ejecuta un método estático de la clase TvT sin importar classloaders
	private Object invokeStaticMethod(Class<?> clazz, String methodName)
	{
		try
		{
			return clazz.getMethod(methodName).invoke(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	// Helper: Obtiene el valor de una variable estática de la clase TvT de forma segura
	private Object getStaticFieldValue(Class<?> clazz, String fieldName)
	{
		try
		{
			return clazz.getField(fieldName).get(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private boolean JoinTvT(Player activeChar)
	{
		Quest tvt = QuestManager.getInstance().getQuest("TvT");
		if (tvt == null)
		{
			activeChar.sendMessage("The TvT Event is not loaded on the server.");
			return false;
		}
		
		Class<?> tvtClass = tvt.getClass();
		Boolean isParticipating = (Boolean) invokeStaticMethod(tvtClass, "IS_PARTICIPATING");
		if ((isParticipating == null) || !isParticipating)
		{
			activeChar.sendMessage("There is no TvT Event in registration progress.");
			return false;
		}
		
		if (activeChar.isRegisteredOnEvent() || activeChar.isOnEvent())
		{
			activeChar.sendMessage("You are already registered.");
			return false;
		}
		
		// Ejecutamos el registro llamando al onEvent "Participate" nativo de Mobius.
		String result = tvt.onEvent("Participate", null, activeChar);
		
		if ((result != null) && result.equals("registration-success.html"))
		{
			activeChar.sendMessage("Your participation in the TvT event has been approved.");
			return true;
		}
		else if ((result != null) && result.equals("registration-ip.html"))
		{
			activeChar.sendMessage("Your IP is already registered in this event (Dualbox Block).");
			return false;
		}
		else
		{
			return false;
		}
	}
	
	private boolean LeaveTvT(Player activeChar)
	{
		Quest tvt = QuestManager.getInstance().getQuest("TvT");
		if (tvt == null)
		{
			return false;
		}
		
		Class<?> tvtClass = tvt.getClass();
		Boolean isParticipating = (Boolean) invokeStaticMethod(tvtClass, "IS_PARTICIPATING");
		if ((isParticipating == null) || !isParticipating)
		{
			activeChar.sendMessage("There is no TvT Event in progress.");
			return false;
		}
		
		Boolean isStarting = (Boolean) invokeStaticMethod(tvtClass, "IS_STARTING");
		Boolean isStarted = (Boolean) invokeStaticMethod(tvtClass, "IS_STARTED");
		if (((isStarting != null) && isStarting) || ((isStarted != null) && isStarted))
		{
			activeChar.sendMessage("You cannot leave now because the TvT event is starting/started.");
			return false;
		}
		
		if (!activeChar.isRegisteredOnEvent())
		{
			activeChar.sendMessage("You aren't registered in the TvT Event.");
			return false;
		}
		
		String result = tvt.onEvent("CancelParticipation", null, activeChar);
		if ((result != null) && result.equals("registration-canceled.html"))
		{
			activeChar.sendMessage("Your registration has been canceled.");
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private boolean TvTinfo(Player activeChar)
	{
		Quest tvt = QuestManager.getInstance().getQuest("TvT");
		if (tvt == null)
		{
			return false;
		}
		
		Class<?> tvtClass = tvt.getClass();
		Boolean isInactive = (Boolean) invokeStaticMethod(tvtClass, "IS_INACTIVE");
		if ((isInactive == null) || isInactive)
		{
			activeChar.sendMessage("There is no TvT Event in progress.");
			return false;
		}
		
		Boolean isStarting = (Boolean) invokeStaticMethod(tvtClass, "IS_STARTING");
		Boolean isStarted = (Boolean) invokeStaticMethod(tvtClass, "IS_STARTED");
		if (((isStarting != null) && isStarting) || ((isStarted != null) && isStarted))
		{
			activeChar.sendMessage("Command available only during the registration period.");
			return false;
		}
		
		// Obtenemos los valores de las variables usando reflexión segura
		Set<Player> playerList = (Set<Player>) getStaticFieldValue(tvtClass, "PLAYER_LIST");
		Integer minLvl = (Integer) getStaticFieldValue(tvtClass, "MINIMUM_PARTICIPANT_LEVEL");
		Integer maxLvl = (Integer) getStaticFieldValue(tvtClass, "MAXIMUM_PARTICIPANT_LEVEL");
		ItemHolder reward = (ItemHolder) getStaticFieldValue(tvtClass, "REWARD");
		
		int registeredCount = (playerList != null) ? playerList.size() : 0;
		int minLevelVal = (minLvl != null) ? minLvl : 0;
		int maxLevelVal = (maxLvl != null) ? maxLvl : 0;
		
		int rewardId = (reward != null) ? reward.getId() : 57;
		long rewardAmount = (reward != null) ? reward.getCount() : 0;
		ItemTemplate rewardItem = ItemData.getInstance().getTemplate(rewardId);
		String rewardName = rewardItem != null ? rewardItem.getName() : "Unknown Item";
		
		if (registeredCount == 1)
		{
			activeChar.sendMessage("There is " + registeredCount + " player participating in this event.");
		}
		else
		{
			activeChar.sendMessage("There are " + registeredCount + " players participating in this event.");
		}
		
		activeChar.sendMessage("Reward: " + rewardAmount + " " + rewardName + "!");
		activeChar.sendMessage("Player Min level: " + minLevelVal + ".");
		activeChar.sendMessage("Player Max level: " + maxLevelVal + ".");
		return true;
	}
}