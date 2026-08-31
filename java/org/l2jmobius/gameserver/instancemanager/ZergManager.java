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
package org.l2jmobius.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Lucas
 */
public class ZergManager
{
	private static final Logger _log = Logger.getLogger(ZergManager.class.getName());
	private static final Location GIRAN_TOWN = new Location(83463, 148045, -3400);
	
	public ZergManager()
	{
		_log.log(Level.INFO, "Anti-ZergManager - Loaded.");
	}
	
	private boolean checkClanAreaKickTask(Player activeChar, Integer numberBox)
	{
		Map<String, List<Player>> zergMap = new HashMap<>();
		
		Clan clan = activeChar.getClan();
		
		if (clan != null)
		{
			for (Player player : clan.getOnlineMembers(0)) // Se añadió '0' para no excluir a ningún miembro
			{
				if (!player.isInsideZone(ZoneId.NO_ZERG) || (player.getClan() == null))
				{
					continue;
				}
				
				String zerg1 = activeChar.getClan().getName();
				String zerg2 = player.getClan().getName();
				
				if (zerg1.equals(zerg2))
				{
					if (zergMap.get(zerg1) == null)
					{
						zergMap.put(zerg1, new ArrayList<>());
					}
					
					zergMap.get(zerg1).add(player);
					
					if (zergMap.get(zerg1).size() > numberBox)
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean checkAllyAreaKickTask(Player activeChar, Integer numberBox, Collection<Player> world)
	{
		// Validación para evitar NullPointerException si el emisor no tiene clan o alianza
		if ((activeChar.getClan() == null) || (activeChar.getAllyId() == 0))
		{
			return false;
		}
		
		Map<String, List<Player>> zergMap = new HashMap<>();
		
		for (Player player : world)
		{
			if (!player.isInsideZone(ZoneId.NO_ZERG) || (player.getAllyId() == 0))
			{
				continue;
			}
			
			String zerg1 = activeChar.getClan().getAllyName();
			String zerg2 = player.getClan().getAllyName();
			
			if (zerg1.equals(zerg2))
			{
				if (zergMap.get(zerg1) == null)
				{
					zergMap.put(zerg1, new ArrayList<>());
				}
				
				zergMap.get(zerg1).add(player);
				
				if (zergMap.get(zerg1).size() > numberBox)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean checkClanArea(Player activeChar, Integer numberBox, Boolean forcedTeleport)
	{
		if (checkClanAreaKickTask(activeChar, numberBox))
		{
			if (forcedTeleport)
			{
				activeChar.sendPacket(new ExShowScreenMessage("Allowed only " + numberBox + " clans members on this area!", 6 * 1000));
				activeChar.teleToLocation(GIRAN_TOWN); // Adaptado a L2JMobius
			}
			return true;
		}
		return false;
	}
	
	public boolean checkAllyArea(Player activeChar, Integer numberBox, Collection<Player> world, Boolean forcedTeleport)
	{
		if (checkAllyAreaKickTask(activeChar, numberBox, world))
		{
			if (forcedTeleport)
			{
				activeChar.sendPacket(new ExShowScreenMessage("Allowed only " + numberBox + " ally members on this area!", 6 * 1000));
				activeChar.teleToLocation(GIRAN_TOWN); // Adaptado a L2JMobius
			}
			return true;
		}
		return false;
	}
	
	public static void showZergHtml(Player activeChar, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, "data/html/mods/menu/zerg/Page-" + val + ".htm");
		activeChar.sendPacket(html);
	}
	
	private static class SingletonHolder
	{
		protected static final ZergManager _instance = new ZergManager();
	}
	
	public static final ZergManager getInstance()
	{
		return SingletonHolder._instance;
	}
}