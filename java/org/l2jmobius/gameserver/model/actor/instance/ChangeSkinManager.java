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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

/**
 * @author Lucas
 */
public class ChangeSkinManager extends Npc
{
	public enum Races
	{
		FIGHTER(Race.HUMAN, ClassId.FIGHTER),
		MAGE(Race.HUMAN, ClassId.MAGE),
		ELVENFIGHTER(Race.ELF, ClassId.ELVEN_FIGHTER),
		ELVENMAGE(Race.ELF, ClassId.ELVEN_MAGE),
		DARKFIGHTER(Race.DARK_ELF, ClassId.DARK_FIGHTER),
		DARKMAGE(Race.DARK_ELF, ClassId.DARK_MAGE),
		ORCFIGHTER(Race.ORC, ClassId.ORC_FIGHTER),
		ORCMAGE(Race.ORC, ClassId.ORC_MAGE),
		DWARVEN(Race.DWARF, ClassId.DWARVEN_FIGHTER),
		KAMAEL(Race.KAMAEL, ClassId.MALE_SOLDIER),
		ERTHEIA(Race.ERTHEIA, ClassId.ERTHEIA_WIZARD);
		
		private final Race _race;
		
		private final ClassId _classId;
		
		private Races(Race race, ClassId classId)
		{
			_race = race;
			_classId = classId;
		}
		
		public int getRaceId()
		{
			return _race.ordinal();
		}
		
		public int getClassId()
		{
			return _classId.getId();
		}
	}
	
	public ChangeSkinManager(NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player == null)
		{
			return;
		}
		
		if (!player.hasPremiumStatus())
		{
			player.sendMessage("Necesitas tener la cuenta Premium para este beneficio");
			return;
			
		}
		
		Races race = null;
		
		if (command.startsWith("HumanFighter"))
		{
			race = Races.FIGHTER;
		}
		else if (command.startsWith("HumanMage"))
		{
			race = Races.MAGE;
		}
		else if (command.startsWith("ElfFighter"))
		{
			race = Races.ELVENFIGHTER;
		}
		else if (command.startsWith("ElfMage"))
		{
			race = Races.ELVENMAGE;
		}
		else if (command.startsWith("DarkElfFighter"))
		{
			race = Races.DARKFIGHTER;
		}
		else if (command.startsWith("DarkElfMage"))
		{
			race = Races.DARKMAGE;
		}
		else if (command.startsWith("OrcFighter"))
		{
			race = Races.ORCFIGHTER;
		}
		else if (command.startsWith("OrcMage"))
		{
			race = Races.ORCMAGE;
		}
		else if (command.startsWith("Dwarven"))
		{
			race = Races.DWARVEN;
		}
		else if (command.startsWith("Kamael"))
		{
			race = Races.KAMAEL;
		}
		else if (command.startsWith("Ertheia"))
		{
			player.getAppearance().setFemale();
			race = Races.ERTHEIA;
			
		}
		else if (command.startsWith("BackMainSkin"))
		{
			player.getAppearance().setMale();
			player.setCustomClassSkin(-1);
			player.setCustomRaceSkin(-1);
			
			refreshPlayer(player);
			return;
		}
		
		setRaceCustomSkin(player, race);
		refreshPlayer(player);
	}
	
	private static void setRaceCustomSkin(Player player, Races race)
	{
		if ((player == null) || (race == null))
		{
			return;
		}
		
		player.setCustomRaceSkin(race.getRaceId());
		player.setCustomClassSkin(race.getClassId());
	}
	
	private static void refreshPlayer(Player player)
	{
		player.decayMe();
		player.spawnMe();
		
		player.broadcastUserInfo();
		player.broadcastCharInfo();
	}
	
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String filename = "";
		if (value == 0)
		{
			filename = Integer.toString(npcId);
		}
		else
		{
			filename = npcId + "-" + value;
		}
		return "data/html/mods/ChangeSkin/" + filename + ".htm";
	}
}