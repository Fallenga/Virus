/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * This class trades Gold Bars for Adena and vice versa.
 * @author Ahmed
 */
public class Potas implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"potas"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equalsIgnoreCase("potas"))
		{
			if (activeChar == null)
			{
				return false;
			}
			else if (activeChar.isInDuel())
			{
				activeChar.sendMessage("You can't use the command in Duel Mode.");
				return false;
			}
			else if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("You can't use the command in Olympiad Mode.");
				return false;
			}
			else if (activeChar.isInCombat())
			{
				activeChar.sendMessage("You can't use the command in Combat Mode.");
				return false;
			}
			else if (activeChar.isFestivalParticipant())
			{
				activeChar.sendMessage("You can't use the command in Festival.");
				return false;
			}
			
			if (!activeChar.isInsideZone(ZoneId.PEACE))
			{
				activeChar.sendMessage("You can use the command in Peace Zone.");
				return false;
			}
			else if (activeChar.inObserverMode())
			{
				activeChar.sendMessage("You can't use the command in Observ Mode.");
				return false;
			}
			else if (activeChar.isDead())
			{
				activeChar.sendMessage("You is Dead. Can't fly.");
				return false;
			}
			else if (activeChar.isFakeDeath())
			{
				activeChar.sendMessage("You are Dead? week up ");
				return false;
			}
			else if (!activeChar.hasPremiumStatus())
			{
				activeChar.sendMessage("You Need Account Premium To Use This.");
				return false;
			}
			else
			{
				// Control de Cooldown (1 hora = 3600000 milisegundos)
				final long currentTime = System.currentTimeMillis();
				final long cooldown = activeChar.getVariables().getLong("potas_cooldown", 0);
				
				if (currentTime < cooldown)
				{
					final long timeLeft = (cooldown - currentTime) / 1000; // Tiempo restante en segundos
					if (timeLeft >= 60)
					{
						final long minutes = timeLeft / 60;
						final long seconds = timeLeft % 60;
						activeChar.sendMessage("Debes esperar " + minutes + " minutos y " + seconds + " segundos para usar este comando de nuevo.");
					}
					else
					{
						activeChar.sendMessage("Debes esperar " + timeLeft + " segundos para usar este comando de nuevo.");
					}
					return false;
				}
				
				if (activeChar.hasPremiumStatus())
				{
					activeChar.getInventory().addItem("Mana Potion", 22038, 500, activeChar, null);
					activeChar.getInventory().addItem("CP Potion", 22037, 500, activeChar, null);
					activeChar.getInventory().addItem("Quick Healing Potion", 22024, 500, activeChar, null);
					activeChar.sendMessage("Has recibido un pack de pociones VIP");
					
					// Guardamos el nuevo cooldown (Tiempo actual + 1 hora)
					activeChar.getVariables().set("potas_cooldown", currentTime + 3600000);
				}
				else
				{
					activeChar.sendMessage("Failed to use this command.");
					return false;
				}
				
				return true;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}