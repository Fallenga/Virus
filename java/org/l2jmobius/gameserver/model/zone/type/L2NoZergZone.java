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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.enums.PartyMessageType;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.ZergManager;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Lucas
 */
public class L2NoZergZone extends ZoneType
{
	private int _maxClanMembers;
	private int _maxAllyMembers;
	private int _minPartyMembers;
	private boolean _showRules;
	private boolean _checkParty;
	private boolean _checkClan;
	private boolean _checkAlly;
	
	public L2NoZergZone(int id)
	{
		super(id);
		
		_maxClanMembers = 0;
		_maxAllyMembers = 0;
		_minPartyMembers = 0;
		_showRules = false;
		_checkParty = false;
		_checkClan = false;
		_checkAlly = false;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("MaxClanMembers"))
		{
			_maxClanMembers = Integer.parseInt(value);
		}
		else if (name.equals("MaxAllyMembers"))
		{
			_maxAllyMembers = Integer.parseInt(value);
		}
		else if (name.equals("MinPartyMembers"))
		{
			_minPartyMembers = Integer.parseInt(value);
		}
		else if (name.equals("showRules"))
		{
			_showRules = Boolean.parseBoolean(value);
		}
		else if (name.equals("checkParty"))
		{
			_checkParty = Boolean.parseBoolean(value);
		}
		else if (name.equals("checkClan"))
		{
			_checkClan = Boolean.parseBoolean(value);
		}
		else if (name.equals("checkAlly"))
		{
			_checkAlly = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.NO_ZERG, true);
		
		if (creature instanceof Player)
		{
			final Player activeChar = (Player) creature;
			
			if (_checkParty)
			{
				if (!activeChar.isInParty() || (activeChar.getParty().getMemberCount() < _minPartyMembers))
				{
					activeChar.sendPacket(new ExShowScreenMessage("Your party does not have " + _minPartyMembers + " members to enter on this zone!", 6 * 1000));
					activeChar.teleToLocation(TeleportWhereType.TOWN); // Adaptado a L2JMobius
				}
			}
			
			if (_showRules)
			{
				ZergManager.showZergHtml(activeChar, 0);
			}
			
			if (_checkClan)
			{
				MaxClanMembersOnArea(activeChar);
			}
			
			if (_checkAlly)
			{
				MaxAllyMembersOnArea(activeChar);
			}
			
			if (_checkParty)
			{
				checkPartyMembers(activeChar);
			}
		}
	}
	
	public boolean MaxClanMembersOnArea(Player activeChar)
	{
		return ZergManager.getInstance().checkClanArea(activeChar, _maxClanMembers, true);
	}
	
	public boolean MaxAllyMembersOnArea(Player activeChar)
	{
		return ZergManager.getInstance().checkAllyArea(activeChar, _maxAllyMembers, World.getInstance().getPlayers(), true);
	}
	
	public void checkPartyMembers(Player player)
	{
		Party party = player.getParty();
		
		if (party == null)
		{
			return;
		}
		
		for (Player member : party.getMembers())
		{
			if (member == null)
			{
				continue;
			}
			
			if (!member.isOnline())
			{
				continue;
			}
			
			if ((member.getClan() != player.getClan()) && (member.getClan().getAllyId() != player.getClan().getAllyId()))
			{
				player.sendMessage("Only clan member party are allowed.");
				player.getParty().removePartyMember(player, PartyMessageType.LEFT);
				break;
			}
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.NO_ZERG, false);
	}
	
	@Override
	public void onDieInside(Creature character)
	{
	}
	
	@Override
	public void onReviveInside(Creature character)
	{
	}
}
