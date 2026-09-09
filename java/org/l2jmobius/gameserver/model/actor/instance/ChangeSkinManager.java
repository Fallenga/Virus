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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

/**
 * NPC manager for custom race skins.
 * @author Lucas
 */
public class ChangeSkinManager extends Npc
{
	private static final int TEMPLAR_COIN_ID = 4358;
	private static final long TEMPLAR_COIN_COST = 300;
	private static final String ORIGINAL_SEX_VARIABLE = "CHANGE_SKIN_ORIGINAL_SEX";
	
	private enum Skin
	{
		HUMAN_FIGHTER(Race.HUMAN, ClassId.FIGHTER),
		HUMAN_MAGE(Race.HUMAN, ClassId.MAGE),
		ELF_FIGHTER(Race.ELF, ClassId.ELVEN_FIGHTER),
		ELF_MAGE(Race.ELF, ClassId.ELVEN_MAGE),
		DARK_ELF_FIGHTER(Race.DARK_ELF, ClassId.DARK_FIGHTER),
		DARK_ELF_MAGE(Race.DARK_ELF, ClassId.DARK_MAGE),
		ORC_FIGHTER(Race.ORC, ClassId.ORC_FIGHTER),
		ORC_MAGE(Race.ORC, ClassId.ORC_MAGE),
		DWARF(Race.DWARF, ClassId.DWARVEN_FIGHTER),
		KAMAEL(Race.KAMAEL, ClassId.MALE_SOLDIER),
		ERTHEIA(Race.ERTHEIA, ClassId.ERTHEIA_WIZARD);
		
		private final Race _race;
		private final ClassId _classId;
		
		Skin(Race race, ClassId classId)
		{
			_race = race;
			_classId = classId;
		}
		
		private int getRaceId()
		{
			return _race.ordinal();
		}
		
		private int getClassId()
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
		if ((player == null) || (command == null))
		{
			return;
		}
		
		// Restoring the original appearance is always free.
		if (command.equals("BackMainSkin"))
		{
			restoreOriginalSkin(player);
			return;
		}
		
		final Skin skin = getSkin(command);
		if (skin == null)
		{
			super.onBypassFeedback(player, command);
			return;
		}
		
		if ((player.getCustomRaceSkin() == skin.getRaceId()) && (player.getCustomClassSkin() == skin.getClassId()))
		{
			player.sendMessage("Ya tienes seleccionada esta apariencia.");
			return;
		}
		
		if (!hasEnoughTemplarCoins(player))
		{
			player.sendMessage("Necesitas " + TEMPLAR_COIN_COST + " Templar Coins para cambiar de skin.");
			return;
		}
		
		// Player#destroyItemByItemId validates the amount and sends InventoryUpdate.
		if (!player.destroyItemByItemId("ChangeSkin", TEMPLAR_COIN_ID, TEMPLAR_COIN_COST, this, true))
		{
			player.sendMessage("No se pudo procesar el pago. Intenta nuevamente.");
			return;
		}
		
		rememberOriginalSex(player);
		applySkin(player, skin);
		refreshPlayer(player);
		player.sendMessage("Skin cambiada correctamente. Se descontaron " + TEMPLAR_COIN_COST + " Templar Coins.");
	}
	
	private static Skin getSkin(String command)
	{
		switch (command)
		{
			case "HumanFighter":
			{
				return Skin.HUMAN_FIGHTER;
			}
			case "HumanMage":
			{
				return Skin.HUMAN_MAGE;
			}
			case "ElfFighter":
			{
				return Skin.ELF_FIGHTER;
			}
			case "ElfMage":
			{
				return Skin.ELF_MAGE;
			}
			case "DarkElfFighter":
			{
				return Skin.DARK_ELF_FIGHTER;
			}
			case "DarkElfMage":
			{
				return Skin.DARK_ELF_MAGE;
			}
			case "OrcFighter":
			{
				return Skin.ORC_FIGHTER;
			}
			case "OrcMage":
			{
				return Skin.ORC_MAGE;
			}
			case "Dwarven":
			{
				return Skin.DWARF;
			}
			case "Kamael":
			{
				return Skin.KAMAEL;
			}
			case "Ertheia":
			{
				return Skin.ERTHEIA;
			}
		}
		return null;
	}
	
	private static boolean hasEnoughTemplarCoins(Player player)
	{
		return player.getInventory().getInventoryItemCount(TEMPLAR_COIN_ID, -1) >= TEMPLAR_COIN_COST;
	}
	
	private static void rememberOriginalSex(Player player)
	{
		if (!player.getVariables().hasVariable(ORIGINAL_SEX_VARIABLE))
		{
			player.getVariables().set(ORIGINAL_SEX_VARIABLE, player.getAppearance().isFemale());
		}
	}
	
	private static void applySkin(Player player, Skin skin)
	{
		player.setCustomRaceSkin(skin.getRaceId());
		player.setCustomClassSkin(skin.getClassId());
		
		// Ertheia has no male model. Other skins keep the player's original sex.
		if (skin == Skin.ERTHEIA)
		{
			player.getAppearance().setFemale();
		}
		else
		{
			player.getAppearance().setSex(player.getVariables().getBoolean(ORIGINAL_SEX_VARIABLE, player.getAppearance().isFemale()));
		}
	}
	
	private static void restoreOriginalSkin(Player player)
	{
		if ((player.getCustomRaceSkin() == -1) && (player.getCustomClassSkin() == -1))
		{
			player.sendMessage("Ya tienes tu apariencia original.");
			return;
		}
		
		player.setCustomClassSkin(-1);
		player.setCustomRaceSkin(-1);
		if (player.getVariables().hasVariable(ORIGINAL_SEX_VARIABLE))
		{
			player.getAppearance().setSex(player.getVariables().getBoolean(ORIGINAL_SEX_VARIABLE));
			player.getVariables().remove(ORIGINAL_SEX_VARIABLE);
		}
		
		refreshPlayer(player);
		player.sendMessage("Tu apariencia original fue restaurada sin costo.");
	}
	
	private static void refreshPlayer(Player player)
	{
		// broadcastUserInfo also broadcasts CharInfo to known players.
		player.broadcastUserInfo();
	}
	
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		final String filename = value == 0 ? Integer.toString(npcId) : npcId + "-" + value;
		return "data/html/mods/ChangeSkin/" + filename + ".htm";
	}
}
