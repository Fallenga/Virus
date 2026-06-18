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
package org.l2jmobius.gameserver.network.serverpackets.ability;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExAcquireAPSkillList extends ServerPacket
{
	private final int _abilityPoints;
	private final int _usedAbilityPoints;
	// private final long _price; Removed on Grand Crusade
	private final boolean _enable;
	private final List<Skill> _skills = new ArrayList<>();
	
	public ExAcquireAPSkillList(Player player)
	{
		_abilityPoints = player.getAbilityPoints();
		_usedAbilityPoints = player.getAbilityPointsUsed();
		// _price = AbilityPointsData.getInstance().getPrice(_abilityPoints); Removed on Grand Crusade
		for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
		{
			final Skill knownSkill = player.getKnownSkill(sk.getSkillId());
			if ((knownSkill != null) && (knownSkill.getLevel() == sk.getSkillLevel()))
			{
				_skills.add(knownSkill);
			}
		}
		
		_enable = (!player.isSubClassActive() || (player.getLevel() >= 80));
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ACQUIRE_AP_SKILL_LIST.writeId(this, buffer);
		buffer.writeInt(_enable);
		buffer.writeLong(Config.ABILITY_POINTS_RESET_SP); // Changed to from Adena to SP on Grand Crusade
		// buffer.writeLong(_price); Removed on Grand Crusade
		buffer.writeInt(Config.ABILITY_MAX_POINTS);
		buffer.writeInt(_abilityPoints);
		buffer.writeInt(_usedAbilityPoints);
		buffer.writeInt(_skills.size());
		for (Skill skill : _skills)
		{
			buffer.writeInt(skill.getId());
			buffer.writeInt(skill.getLevel());
		}
	}
}
