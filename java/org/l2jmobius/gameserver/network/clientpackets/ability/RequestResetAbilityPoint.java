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

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ability.ExAcquireAPSkillList;

/**
 * @author Mobius
 */
public class RequestResetAbilityPoint extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isSubClassActive())
		{
			return;
		}
		
		if (player.isInStoreMode() || (player.getActiveRequester() != null))
		{
			return;
		}
		else if (player.getLevel() < 80)
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
		else if (player.getAbilityPoints() == 0)
		{
			player.sendMessage("You don't have ability points to reset!");
			return;
		}
		else if (player.getAbilityPointsUsed() == 0)
		{
			player.sendMessage("You haven't used your ability points yet!");
			return;
		}
		else if (player.getSp() < Config.ABILITY_POINTS_RESET_SP)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_FOR_THIS);
			return;
		}
		
		player.setSp(player.getSp() - Config.ABILITY_POINTS_RESET_SP);
		for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
		{
			final Skill skill = player.getKnownSkill(sk.getSkillId());
			if (skill != null)
			{
				player.removeSkill(skill);
				player.getEffectList().stopSkillEffects(SkillFinishType.SILENT, skill); // TODO: Check if retail shows system message.
			}
		}
		
		player.setAbilityPointsUsed(0);
		player.sendPacket(new ExAcquireAPSkillList(player));
		player.broadcastUserInfo();
	}
}
