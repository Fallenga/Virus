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
package handlers.bypasshandlers;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAll;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import org.l2jmobius.gameserver.util.Util;

/**
 * @author Mobius
 */
public class ChangePlayerName implements IBypassHandler
{
	private static final int NAME_CHANGE_TICKET = 23622;
	
	private static final String[] COMMANDS =
	{
		"ChangePlayerName"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		// Need to have at least one Name Change Ticket in order to proceed.
		final PlayerInventory inventory = player.getInventory();
		if (inventory.getAllItemsByItemId(NAME_CHANGE_TICKET).isEmpty())
		{
			return false;
		}
		
		final String newName = command.split(" ")[1].trim();
		if (!Util.isAlphaNumeric(newName))
		{
			player.sendMessage("Name must only contain alphanumeric characters.");
			return false;
		}
		if (CharInfoTable.getInstance().doesCharNameExist(newName))
		{
			player.sendMessage("Name " + newName + " already exists.");
			return false;
		}
		
		// Destroy item.
		player.destroyItemByItemId("ChangePlayerName to " + newName, NAME_CHANGE_TICKET, 1, player, true);
		
		// Set name and proceed.
		player.setName(newName);
		CharInfoTable.getInstance().addName(player);
		player.storeMe();
		
		player.sendMessage("Your name has been changed.");
		player.broadcastUserInfo();
		
		if (player.isInParty())
		{
			// Delete party window for other party members
			final Party party = player.getParty();
			party.broadcastToPartyMembers(player, PartySmallWindowDeleteAll.STATIC_PACKET);
			for (Player member : party.getMembers())
			{
				// And re-add
				if (member != player)
				{
					member.sendPacket(new PartySmallWindowAll(member, party));
				}
			}
		}
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.broadcastClanStatus();
		}
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
