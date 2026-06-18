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
package org.l2jmobius.gameserver.network.clientpackets.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ability.ExAcquireAPSkillList;

/**
 * @author Mobius
 */
public class RequestAcquireAbilityList extends ClientPacket
{
	private static final int TREE_SIZE = 3;
	
	private Map<Integer, SkillHolder> _skills = new LinkedHashMap<>();
	
	@Override
	protected void readImpl()
	{
		readInt(); // Total size
		for (int i = 0; i < TREE_SIZE; i++)
		{
			final int size = readInt();
			for (int j = 0; j < size; j++)
			{
				final SkillHolder holder = new SkillHolder(readInt(), readInt());
				if (holder.getSkillLevel() < 1)
				{
					_skills = null;
					PacketLogger.warning("Player is trying to learn skill " + holder + " by sending packet with level 0!");
					return;
				}
				
				if (_skills.putIfAbsent(holder.getSkillId(), holder) != null)
				{
					_skills = null;
					PacketLogger.warning("Player is trying to send two times one skill " + holder + " to learn!");
					return;
				}
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_skills == null)
		{
			PacketLogger.warning("Player " + player + " tried to exploit RequestAcquireAbilityList!");
			return;
		}
		
		// Se remueve la restricción de subclase para permitir aprender habilidades de AP
		/*
		 * if (player.isSubClassActive()) { return; }
		 */
		
		if ((player.getAbilityPoints() <= 0) || (player.getAbilityPoints() == player.getAbilityPointsUsed()))
		{
			PacketLogger.warning(player + " is trying to learn ability without ability points!");
			return;
		}
		
		if (player.getLevel() < 80)
		{
			player.sendPacket(SystemMessageId.REACH_LEVEL_85_TO_USE_THE_ABILITY);
			return;
		}
		else if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_OR_RESET_ABILITY_POINTS_WHILE_PARTICIPATING_IN_THE_OLYMPIAD_OR_CEREMONY_OF_CHAOS);
			return;
		}
		else if (player.isOnEvent())
		{
			player.sendMessage("You cannot use or reset Ability Points while participating in an event.");
			return;
		}
		
		final int[] pointsSpent = new int[TREE_SIZE];
		Arrays.fill(pointsSpent, 0);
		
		final List<SkillLearn> skillsToLearn = new ArrayList<>(_skills.size());
		for (SkillHolder holder : _skills.values())
		{
			final SkillLearn learn = SkillTreeData.getInstance().getAbilitySkill(holder.getSkillId(), holder.getSkillLevel());
			if (learn == null)
			{
				PacketLogger.warning("SkillLearn " + holder.getSkillId() + " (" + holder.getSkillLevel() + ") not found!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				break;
			}
			
			final Skill skill = holder.getSkill();
			if (skill == null)
			{
				PacketLogger.warning("Skill " + holder.getSkillId() + " (" + holder.getSkillLevel() + ") not found!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				break;
			}
			
			if (player.getSkillLevel(skill.getId()) > 0)
			{
				pointsSpent[learn.getTreeId() - 1] += skill.getLevel();
			}
			
			skillsToLearn.add(learn);
		}
		
		// Sort the skills by their tree id -> row -> column
		skillsToLearn.sort(Comparator.comparingInt(SkillLearn::getTreeId).thenComparing(SkillLearn::getRow).thenComparing(SkillLearn::getColumn));
		
		for (SkillLearn learn : skillsToLearn)
		{
			final Skill skill = SkillData.getInstance().getSkill(learn.getSkillId(), learn.getSkillLevel());
			final int points;
			final int knownLevel = player.getSkillLevel(skill.getId());
			if (knownLevel == 0) // player didn't knew it at all!
			{
				points = learn.getSkillLevel();
			}
			else
			{
				points = learn.getSkillLevel() - knownLevel;
			}
			
			// Case 1: Learning skill without having X points spent on the specific tree
			if (learn.getPointsRequired() > pointsSpent[learn.getTreeId() - 1])
			{
				PacketLogger.warning(player + " is trying to learn " + skill + " without enough ability points spent!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Case 2: Learning skill without having its parent
			for (SkillHolder required : learn.getPreReqSkills())
			{
				if (player.getSkillLevel(required.getSkillId()) < required.getSkillLevel())
				{
					PacketLogger.warning(player + " is trying to learn " + skill + " without having prerequsite skill: " + required.getSkill() + "!");
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			// Case 3 Learning a skill without having enough points
			if ((player.getAbilityPoints() - player.getAbilityPointsUsed()) < points)
			{
				PacketLogger.warning(player + " is trying to learn ability without ability points!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			pointsSpent[learn.getTreeId() - 1] += points;
			player.addSkill(skill, true);
			player.setAbilityPointsUsed(player.getAbilityPointsUsed() + points);
		}
		
		player.sendPacket(new ExAcquireAPSkillList(player));
		
		ThreadPool.schedule(() ->
		{
			player.sendSkillList();
			player.getStat().recalculateStats(false);
			player.broadcastUserInfo();
		}, 100);
	}
}
