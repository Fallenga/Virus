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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.NicknameChanged;

/**
 * @author Lucas
 */
public class TemplarZone extends ZoneType
{
	
	private final Map<Integer, String> _originalTitles = new ConcurrentHashMap<>();
	private String _title;
	
	public TemplarZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("title"))
		{
			_title = value;
		}
		else
		{
			super.setParameter(name, value);
		}
		
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature instanceof Player player)
		{
			if ((_title != null) && !_title.isEmpty())
			{
				_originalTitles.put(player.getObjectId(), player.getTitle());
				player.setTitle(_title);
				player.sendPacket(new NicknameChanged(player));
				player.broadcastUserInfo();
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (creature instanceof Player player)
		{
			final String original = _originalTitles.remove(player.getObjectId());
			if (original != null)
			{
				player.setTitle(original);
				player.sendPacket(new NicknameChanged(player));
				player.broadcastUserInfo();
			}
			
		}
		
	}
	
}
