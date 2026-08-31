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

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * @author Lucas
 */
public class TournamentCmd implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"tournament"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equalsIgnoreCase("tournament"))
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
			else if (activeChar.isNoble())
			{
				activeChar.sendMessage("Solo es exclusivo para Nobless");
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
			if (activeChar.getInventory().getItemByItemId(57) == null)
			{
				activeChar.sendMessage("Gastaste 300.000 de adena");
				return false;
			}
			int placex;
			int placey;
			int placez;
			
			placex = -185836;
			placey = 242575;
			placez = 1680;
			
			activeChar.teleToLocation(placex, placey, placez);
			activeChar.sendMessage("Usted fue transportado a Tournament Zone!");
			activeChar.getInventory().destroyItemByItemId("Adena", 57, 300000, activeChar, activeChar.getTarget());
			activeChar.sendMessage("Ha desaparecido 100.000 de adena");
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
