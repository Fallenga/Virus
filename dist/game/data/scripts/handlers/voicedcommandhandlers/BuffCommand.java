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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Lucas
 */
public class BuffCommand implements IVoicedCommandHandler
{
	
	private final String[] _voicedCommands =
	{
		"buff"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		
		if (check(activeChar))
		{ // retorna
			showHtml(activeChar);
		}
		
		return true;
		
	}
	
	public static void getFullBuff(Player p, boolean isClassMage)
	{
		if (check(p))
		{
			if (isClassMage)
			{
				
				{
					p.stopAllEffects();
					SkillData.getInstance().getSkill(1204, 2).applyEffects(p, p); // Wind Walk
					SkillData.getInstance().getSkill(1040, 3).applyEffects(p, p); // Shield
					SkillData.getInstance().getSkill(1036, 2).applyEffects(p, p); // Magic Barrier
					SkillData.getInstance().getSkill(1045, 6).applyEffects(p, p); // Blessed Body
					SkillData.getInstance().getSkill(1048, 6).applyEffects(p, p); // Blessed Soul
					SkillData.getInstance().getSkill(1085, 3).applyEffects(p, p); // Acumen
					SkillData.getInstance().getSkill(1059, 3).applyEffects(p, p); // Empower
					SkillData.getInstance().getSkill(1062, 2).applyEffects(p, p); // Berserker
					SkillData.getInstance().getSkill(1303, 2).applyEffects(p, p); // Wild Magic
					SkillData.getInstance().getSkill(1078, 6).applyEffects(p, p); // Concentration
					SkillData.getInstance().getSkill(273, 1).applyEffects(p, p); // Mystic
					SkillData.getInstance().getSkill(276, 1).applyEffects(p, p); // Concentration
					SkillData.getInstance().getSkill(365, 1).applyEffects(p, p); // Siren
					SkillData.getInstance().getSkill(264, 1).applyEffects(p, p); // Earth
					SkillData.getInstance().getSkill(267, 1).applyEffects(p, p); // Warding
					SkillData.getInstance().getSkill(304, 1).applyEffects(p, p); // Vitality
					SkillData.getInstance().getSkill(268, 1).applyEffects(p, p); // Wind
					SkillData.getInstance().getSkill(349, 1).applyEffects(p, p); // Renewal
					SkillData.getInstance().getSkill(1355, 1).applyEffects(p, p); // Magnu's Chant
					SkillData.getInstance().getSkill(4703, 3).applyEffects(p, p); // Magnu's Chant
				}
			}
			
			else
			{
				
				{
					p.stopAllEffects();
					SkillData.getInstance().getSkill(1204, 2).applyEffects(p, p); // Wind Walk
					SkillData.getInstance().getSkill(1040, 3).applyEffects(p, p); // Shield
					SkillData.getInstance().getSkill(1068, 3).applyEffects(p, p); // Might
					SkillData.getInstance().getSkill(1036, 2).applyEffects(p, p); // Magic Barrier
					SkillData.getInstance().getSkill(1045, 6).applyEffects(p, p); // Blessed Body
					SkillData.getInstance().getSkill(1240, 3).applyEffects(p, p); // Guidance
					SkillData.getInstance().getSkill(1242, 3).applyEffects(p, p); // Death Whisper
					SkillData.getInstance().getSkill(1268, 4).applyEffects(p, p); // Vampiric Rage
					SkillData.getInstance().getSkill(1062, 2).applyEffects(p, p); // Berserker
					SkillData.getInstance().getSkill(1077, 3).applyEffects(p, p); // Focus
					SkillData.getInstance().getSkill(1086, 2).applyEffects(p, p); // Haste
					SkillData.getInstance().getSkill(275, 1).applyEffects(p, p); // Fury
					SkillData.getInstance().getSkill(274, 1).applyEffects(p, p); // Fire
					SkillData.getInstance().getSkill(271, 1).applyEffects(p, p); // Warrior
					SkillData.getInstance().getSkill(310, 1).applyEffects(p, p); // Vampire
					SkillData.getInstance().getSkill(264, 1).applyEffects(p, p); // Earth
					SkillData.getInstance().getSkill(267, 1).applyEffects(p, p); // Warding
					SkillData.getInstance().getSkill(304, 1).applyEffects(p, p); // Vitality
					SkillData.getInstance().getSkill(269, 1).applyEffects(p, p); // Hunter
					SkillData.getInstance().getSkill(268, 1).applyEffects(p, p); // Wind
					SkillData.getInstance().getSkill(1363, 1).applyEffects(p, p); // Chant of Victory
					SkillData.getInstance().getSkill(4699, 3).applyEffects(p, p); // Chant of Victory
				}
			}
			p.sendMessage("[Buff Command]: Voce foi buffado!");
		}
		
	}
	
	public static boolean check(Player p)
	
	{
		
		return p.isOnEvent() && !p.isInCombat() && !p.isInOlympiadMode(); // restrições
		
	}
	
	public static void showHtml(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		
		html.setFile(player, "data/html/mods/buffCommand.htm");
		html.replace("%currentBuffs%", player.getBuffCount());
		html.replace("%getMaxBuffs%", player.getMaxBuffCount());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
		
	}
	
}
