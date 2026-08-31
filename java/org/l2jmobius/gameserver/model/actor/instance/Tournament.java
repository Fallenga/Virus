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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.instancemanager.AntiFeedManager;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.tournament.Arena1x1;
import org.l2jmobius.gameserver.model.events.tournament.Arena3x3;
import org.l2jmobius.gameserver.model.events.tournament.Arena5x5;
import org.l2jmobius.gameserver.model.events.tournament.Arena9x9;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaConfig;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Lucas
 */
public class Tournament extends Npc
{
	public Tournament(NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (player.isArenaProtection())
		{
			String fileRename = "data/html/mods/tournament/Remove.htm";
			NpcHtmlMessage Rehtml = new NpcHtmlMessage(getObjectId());
			Rehtml.setFile(player, fileRename);
			Rehtml.replace("%objectId%", getObjectId());
			
			if (Arena1x1.registered.size() == 0)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena1x1.registered.size() == 1)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena1x1.registered.size() == 2)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena1x1.registered.size() == 3)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena1x1.registered.size() == 4)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena1x1.registered.size() == 5)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena1x1.registered.size() == 6)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena1x1.registered.size() == 7)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena1x1.registered.size() == 8)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena1x1.registered.size() >= 9)
			{
				Rehtml.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			if (Arena3x3.registered.size() == 0)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena3x3.registered.size() == 1)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena3x3.registered.size() == 2)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena3x3.registered.size() == 3)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena3x3.registered.size() == 4)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena3x3.registered.size() == 5)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena3x3.registered.size() == 6)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena3x3.registered.size() == 7)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena3x3.registered.size() == 8)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena3x3.registered.size() >= 9)
			{
				Rehtml.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			if (Arena5x5.registered.size() == 0)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena5x5.registered.size() == 1)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena5x5.registered.size() == 2)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena5x5.registered.size() == 3)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena5x5.registered.size() == 4)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena5x5.registered.size() == 5)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena5x5.registered.size() == 6)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena5x5.registered.size() == 7)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena5x5.registered.size() == 8)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena5x5.registered.size() >= 9)
			{
				Rehtml.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			if (Arena9x9.registered.size() == 0)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena9x9.registered.size() == 1)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena9x9.registered.size() == 2)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena9x9.registered.size() == 3)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena9x9.registered.size() == 4)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena9x9.registered.size() == 5)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena9x9.registered.size() == 6)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena9x9.registered.size() == 7)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena9x9.registered.size() == 8)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena9x9.registered.size() >= 9)
			{
				Rehtml.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			player.sendPacket(Rehtml);
		}
		else
		{
			String filename = "data/html/mods/tournament/index.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, filename);
			html.replace("%objectId%", getObjectId());
			
			if (Arena1x1.registered.size() == 0)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena1x1.registered.size() == 1)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena1x1.registered.size() == 2)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena1x1.registered.size() == 3)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena1x1.registered.size() == 4)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena1x1.registered.size() == 5)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena1x1.registered.size() == 6)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena1x1.registered.size() == 7)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena1x1.registered.size() == 8)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena1x1.registered.size() >= 9)
			{
				html.replace("%1x1%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			if (Arena3x3.registered.size() == 0)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena3x3.registered.size() == 1)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena3x3.registered.size() == 2)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena3x3.registered.size() == 3)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena3x3.registered.size() == 4)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena3x3.registered.size() == 5)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena3x3.registered.size() == 6)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena3x3.registered.size() == 7)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena3x3.registered.size() == 8)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena3x3.registered.size() >= 9)
			{
				html.replace("%3x3%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			if (Arena5x5.registered.size() == 0)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena5x5.registered.size() == 1)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena5x5.registered.size() == 2)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena5x5.registered.size() == 3)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena5x5.registered.size() == 4)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena5x5.registered.size() == 5)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena5x5.registered.size() == 6)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena5x5.registered.size() == 7)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena5x5.registered.size() == 8)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena5x5.registered.size() >= 9)
			{
				html.replace("%5x5%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			if (Arena9x9.registered.size() == 0)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_0_over\" fore=\"L2UI_CH3.calculate1_0\">");
			}
			else if (Arena9x9.registered.size() == 1)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_1_over\" fore=\"L2UI_CH3.calculate1_1\">");
			}
			else if (Arena9x9.registered.size() == 2)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_2_over\" fore=\"L2UI_CH3.calculate1_2\">");
			}
			else if (Arena9x9.registered.size() == 3)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_3_over\" fore=\"L2UI_CH3.calculate1_3\">");
			}
			else if (Arena9x9.registered.size() == 4)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_4_over\" fore=\"L2UI_CH3.calculate1_4\">");
			}
			else if (Arena9x9.registered.size() == 5)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_5_over\" fore=\"L2UI_CH3.calculate1_5\">");
			}
			else if (Arena9x9.registered.size() == 6)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_6_over\" fore=\"L2UI_CH3.calculate1_6\">");
			}
			else if (Arena9x9.registered.size() == 7)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_7_over\" fore=\"L2UI_CH3.calculate1_7\">");
			}
			else if (Arena9x9.registered.size() == 8)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_8_over\" fore=\"L2UI_CH3.calculate1_8\">");
			}
			else if (Arena9x9.registered.size() >= 9)
			{
				html.replace("%9x9%", "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_9_over\" fore=\"L2UI_CH3.calculate1_9\">");
			}
			
			player.sendPacket(html);
		}
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		
		if (command.startsWith("1x1"))
		{
			if (!ArenaConfig.ALLOW_1X1_REGISTER)
			{
				player.sendMessage("Tournament 1x1 is not enabled.");
				return;
			}
			
			if ((Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP == 0) || AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.L2EVENT_ID, player, Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP))
			{
				final List<String> players_in_boxes = player.active_boxes_characters;
				
				if ((players_in_boxes != null) && (players_in_boxes.size() > 1))
				{
					for (final String character_name : players_in_boxes)
					{
						final Player ppl = World.getInstance().getPlayer(character_name);
						
						if ((ppl != null) && ppl.isArenaProtection())
						{
							player.sendMessage("You are already participating in Tournament with another char!");
							return;
						}
					}
				}
			}
			
			if (player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("You are already registered in another tournament event.");
				return;
			}
			
			if (ArenaConfig.CHECK_NOBLESS)
			{
				if (!player.isNoble())
				{
					player.sendMessage("You does not have the necessary requirements.");
					return;
				}
			}
			
			if (player.isCursedWeaponEquipped() || player.inObserverMode() || player.isInStoreMode() || (player.getReputation() > 0))
			{
				player.sendMessage("You does not have the necessary requirements.");
				return;
			}
			
			// Comentar o eliminar si TvTEvent no existe
			/*
			 * if (TvTEvent.isPlayerParticipant(player.getObjectId())) { player.sendMessage("You already participated in another event!"); return; }
			 */
			
			if (OlympiadManager.getInstance().isRegistered(player))
			{
				player.sendMessage("You is registered in the Olympiad.");
				return;
			}
			
			if ((player.getClassId() == ClassId.SHILLIEN_ELDER) || (player.getClassId() == ClassId.SHILLIEN_SAINT) || (player.getClassId() == ClassId.BISHOP) || (player.getClassId() == ClassId.CARDINAL) || (player.getClassId() == ClassId.ELDER) || (player.getClassId() == ClassId.EVA_SAINT))
			{
				player.sendMessage("Your class is not allowed in 1x1 game event.");
				return;
			}
			
			if (Arena1x1.getInstance().register(player))
			{
				player.setArena1x1(true);
				player.setArenaProtection(true);
			}
		}
		
		if (command.startsWith("3x3"))
		{
			if (!ArenaConfig.ALLOW_3X3_REGISTER)
			{
				player.sendMessage("Tournament 3x3 is not enabled.");
				return;
			}
			
			if ((player._active_boxes > 1) && !ArenaConfig.Allow_Same_HWID_On_Tournament)
			{
				final List<String> players_in_boxes = player.active_boxes_characters;
				
				if ((players_in_boxes != null) && (players_in_boxes.size() > 1))
				{
					for (final String character_name : players_in_boxes)
					{
						final Player ppl = World.getInstance().getPlayer(character_name);
						
						if ((ppl != null) && ppl.isArenaProtection())
						{
							player.sendMessage("You are already participating in Tournament with another char!");
							return;
						}
					}
				}
			}
			
			if (player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("Tournament: You already registered!");
				return;
			}
			
			if (!player.isInParty())
			{
				player.sendMessage("Tournament: You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("Tournament: You are not the party leader!");
				return;
			}
			
			if (player.getParty().getMemberCount() < 3)
			{
				player.sendMessage("Tournament: Your party does not have 3 members.");
				player.sendPacket(new ExShowScreenMessage("Your party does not have 3 members", 6 * 1000));
				return;
			}
			
			if (player.getParty().getMemberCount() > 3)
			{
				player.sendMessage("Tournament: Your Party can not have more than 3 members.");
				player.sendPacket(new ExShowScreenMessage("Your Party can not have more than 3 members", 6 * 1000));
				return;
			}
			
			// CORRECCIÓN: Usar getMembers() en lugar de getMembers().get()
			List<Player> members = player.getParty().getMembers();
			Player assist1 = members.get(1);
			Player assist2 = members.get(2);
			
			if (!player.isInsideZone(ZoneId.TOURNAMENT) || !assist1.isInsideZone(ZoneId.TOURNAMENT) || !assist2.isInsideZone(ZoneId.TOURNAMENT))
			{
				player.sendMessage("Tournament: You or your members are outside the event area.");
				assist1.sendMessage("Tournament: You or your members are outside the event area.");
				assist2.sendMessage("Tournament: You or your members are outside the event area.");
				return;
			}
			
			if (player.isCursedWeaponEquipped() || assist1.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || player.isInStoreMode() || assist1.isInStoreMode() || assist2.isInStoreMode() || (player.getReputation() > 0) || (assist1.getReputation() > 0) || (assist2.getReputation() > 0))
			{
				player.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist1.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist2.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				return;
			}
			
			if ((player.getClassId() == ClassId.SHILLIEN_ELDER) || (player.getClassId() == ClassId.SHILLIEN_SAINT) || (player.getClassId() == ClassId.BISHOP) || (player.getClassId() == ClassId.CARDINAL) || (player.getClassId() == ClassId.ELDER) || (player.getClassId() == ClassId.EVA_SAINT) || (assist1.getClassId() == ClassId.SHILLIEN_ELDER) || (assist1.getClassId() == ClassId.SHILLIEN_SAINT) || (assist1.getClassId() == ClassId.BISHOP) || (assist1.getClassId() == ClassId.CARDINAL) || (assist1.getClassId() == ClassId.ELDER) || (assist1.getClassId() == ClassId.EVA_SAINT) || (assist2.getClassId() == ClassId.SHILLIEN_ELDER) || (assist2.getClassId() == ClassId.SHILLIEN_SAINT) || (assist2.getClassId() == ClassId.BISHOP) || (assist2.getClassId() == ClassId.CARDINAL) || (assist2.getClassId() == ClassId.ELDER) || (assist2.getClassId() == ClassId.EVA_SAINT))
			{
				player.sendMessage("Your class is not allowed in 3x3 game event.");
				assist1.sendMessage("Your class is not allowed in 3x3 game event.");
				assist2.sendMessage("Your class is not allowed in 3x3 game event.");
				return;
			}
			
			if (OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist1) || OlympiadManager.getInstance().isRegistered(assist2))
			{
				player.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist1.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist2.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				return;
			}
			
			// Comentar si TvTEvent no existe
			/*
			 * if (TvTEvent.isPlayerParticipant(player.getObjectId()) || TvTEvent.isPlayerParticipant(assist1.getObjectId()) || TvTEvent.isPlayerParticipant(assist2.getObjectId())) { player.sendMessage("Tournament: You already participated in another event!");
			 * assist1.sendMessage("Tournament: You already participated in another event!"); assist2.sendMessage("Tournament: You already participated in another event!"); return; }
			 */
			
			if (!ArenaConfig.Allow_Same_HWID_On_Tournament)
			{
				// getHWid() puede no existir, usar getIP o similar
				String ip1 = player.getIPAddress();
				String ip2 = assist1.getIPAddress();
				String ip3 = assist2.getIPAddress();
				
				if (ip1.equals(ip2) || ip1.equals(ip3))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip2.equals(ip1) || ip2.equals(ip3))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip3.equals(ip1) || ip3.equals(ip2))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
			}
			
			if (ArenaConfig.ARENA_SKILL_PROTECT)
			{
				removeEffects(player);
				removeEffects(assist1);
				removeEffects(assist2);
			}
			
			ClasseCheck(player);
			
			if (player.dominator_cont > ArenaConfig.dominator_COUNT_3X3)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dominator_COUNT_3X3 + " Dominator's or " + ArenaConfig.dominator_COUNT_3X3 + " Doomcryer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dominator_COUNT_3X3 + " Dominator's or " + ArenaConfig.dominator_COUNT_3X3 + " Doomcryer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			
			if (Arena3x3.getInstance().register(player, assist1, assist2))
			{
				player.sendMessage("Tournament: Your participation has been approved.");
				assist1.sendMessage("Tournament: Your participation has been approved.");
				assist2.sendMessage("Tournament: Your participation has been approved.");
				player.setArenaProtection(true);
				assist1.setArenaProtection(true);
				assist2.setArenaProtection(true);
				player.setArena3x3(true);
				assist1.setArena3x3(true);
				assist2.setArena3x3(true);
				showChatWindow(player);
			}
			else
			{
				player.sendMessage("Tournament: You or your member does not have the necessary requirements.");
			}
		}
		
		if (command.startsWith("5x5"))
		{
			if (!ArenaConfig.ALLOW_5X5_REGISTER)
			{
				player.sendMessage("Tournament 5x5 is not enabled.");
				return;
			}
			
			if ((player._active_boxes > 1) && !ArenaConfig.Allow_Same_HWID_On_Tournament)
			{
				final List<String> players_in_boxes = player.active_boxes_characters;
				
				if ((players_in_boxes != null) && (players_in_boxes.size() > 1))
				{
					for (final String character_name : players_in_boxes)
					{
						final Player ppl = World.getInstance().getPlayer(character_name);
						
						if ((ppl != null) && ppl.isArenaProtection())
						{
							player.sendMessage("You are already participating in Tournament with another char!");
							return;
						}
					}
				}
			}
			
			if (player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("Tournament: You already registered!");
				return;
			}
			
			if (!player.isInParty())
			{
				player.sendMessage("Tournament: You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("Tournament: You are not the party leader!");
				return;
			}
			
			if (player.getParty().getMemberCount() < 5)
			{
				player.sendMessage("Tournament: Your party does not have 5 members.");
				player.sendPacket(new ExShowScreenMessage("Your party does not have 5 members", 6 * 1000));
				return;
			}
			
			if (player.getParty().getMemberCount() > 5)
			{
				player.sendMessage("Tournament: Your Party can not have more than 5 members.");
				player.sendPacket(new ExShowScreenMessage("Your Party can not have more than 5 members", 6 * 1000));
				return;
			}
			
			List<Player> members = player.getParty().getMembers();
			Player assist1 = members.get(1);
			Player assist2 = members.get(2);
			Player assist3 = members.get(3);
			Player assist4 = members.get(4);
			
			if (!player.isInsideZone(ZoneId.TOURNAMENT) || !assist1.isInsideZone(ZoneId.TOURNAMENT) || !assist2.isInsideZone(ZoneId.TOURNAMENT) || !assist3.isInsideZone(ZoneId.TOURNAMENT) || !assist4.isInsideZone(ZoneId.TOURNAMENT))
			{
				player.sendMessage("Tournament: You or your members are outside the event area.");
				assist1.sendMessage("Tournament: You or your members are outside the event area.");
				assist2.sendMessage("Tournament: You or your members are outside the event area.");
				assist3.sendMessage("Tournament: You or your members are outside the event area.");
				assist4.sendMessage("Tournament: You or your members are outside the event area.");
				return;
			}
			
			if (player.isCursedWeaponEquipped() || assist1.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || assist3.isCursedWeaponEquipped() || assist4.isCursedWeaponEquipped() || player.isInStoreMode() || assist1.isInStoreMode() || assist2.isInStoreMode() || assist3.isInStoreMode() || assist4.isInStoreMode() || (player.getReputation() > 0) || (assist1.getReputation() > 0) || (assist2.getReputation() > 0) || (assist3.getReputation() > 0) || (assist4.getReputation() > 0))
			{
				player.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist1.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist2.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist3.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist4.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				return;
			}
			
			if (OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist1) || OlympiadManager.getInstance().isRegistered(assist2) || OlympiadManager.getInstance().isRegistered(assist3) || OlympiadManager.getInstance().isRegistered(assist4))
			{
				player.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist1.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist2.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist3.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist4.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				return;
			}
			
			// Comentar si TvTEvent no existe
			/*
			 * if (TvTEvent.isPlayerParticipant(player.getObjectId()) || TvTEvent.isPlayerParticipant(assist1.getObjectId()) || TvTEvent.isPlayerParticipant(assist2.getObjectId()) || TvTEvent.isPlayerParticipant(assist3.getObjectId()) || TvTEvent.isPlayerParticipant(assist4.getObjectId())) {
			 * player.sendMessage("Tournament: You already participated in TvT event!"); assist1.sendMessage("Tournament: You already participated in TvT event!"); assist2.sendMessage("Tournament: You already participated in TvT event!");
			 * assist3.sendMessage("Tournament: You already participated in TvT event!"); assist4.sendMessage("Tournament: You already participated in TvT event!"); return; }
			 */
			
			if (!ArenaConfig.Allow_Same_HWID_On_Tournament)
			{
				String ip1 = player.getIPAddress();
				String ip2 = assist1.getIPAddress();
				String ip3 = assist2.getIPAddress();
				String ip4 = assist3.getIPAddress();
				String ip5 = assist4.getIPAddress();
				
				if (ip1.equals(ip2) || ip1.equals(ip3) || ip1.equals(ip4) || ip1.equals(ip5))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip2.equals(ip1) || ip2.equals(ip3) || ip2.equals(ip4) || ip2.equals(ip5))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip3.equals(ip1) || ip3.equals(ip2) || ip3.equals(ip4) || ip3.equals(ip5))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip4.equals(ip1) || ip4.equals(ip2) || ip4.equals(ip3) || ip4.equals(ip5))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist1.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
			}
			
			removeEffects(player);
			removeEffects(assist1);
			removeEffects(assist2);
			removeEffects(assist3);
			removeEffects(assist4);
			
			ClasseCheck(player);
			
			if (player.duelist_cont > ArenaConfig.duelist_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.duelist_COUNT_5X5 + " Duelist's or " + ArenaConfig.duelist_COUNT_5X5 + " Grand Khauatari's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.duelist_COUNT_5X5 + " Duelist's or " + ArenaConfig.duelist_COUNT_5X5 + " Grand Khauatari's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.dreadnought_cont > ArenaConfig.dreadnought_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dreadnought_COUNT_5X5 + " Dread Nought's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dreadnought_COUNT_5X5 + " Dread Nought's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.tanker_cont > ArenaConfig.tanker_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.tanker_COUNT_5X5 + " Tanker's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.tanker_COUNT_5X5 + " Tanker's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.dagger_cont > ArenaConfig.dagger_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dagger_COUNT_5X5 + " Dagger's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dagger_COUNT_5X5 + " Dagger's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.archer_cont > ArenaConfig.archer_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.archer_COUNT_5X5 + " Archer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.archer_COUNT_5X5 + " Archer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.bs_cont > ArenaConfig.bs_COUNT_5X5)
			{
				if (ArenaConfig.bs_COUNT_5X5 == 0)
				{
					player.sendMessage("Tournament: Bishop's not allowed in 5x5.");
					player.sendPacket(new ExShowScreenMessage("Tournament: Bishop's not allowed in 5x5.", 6 * 1000));
				}
				else
				{
					player.sendMessage("Tournament: Only " + ArenaConfig.bs_COUNT_5X5 + " Bishop's allowed per party.");
					player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.bs_COUNT_5X5 + " Bishop's allowed per party.", 6 * 1000));
				}
				clean(player);
				return;
			}
			else if (player.archmage_cont > ArenaConfig.archmage_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.archmage_COUNT_5X5 + " Archmage's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.archmage_COUNT_5X5 + " Archmage's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.soultaker_cont > ArenaConfig.soultaker_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.soultaker_COUNT_5X5 + " Soultaker's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.soultaker_COUNT_5X5 + " Soultaker's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.mysticMuse_cont > ArenaConfig.mysticMuse_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.mysticMuse_COUNT_5X5 + " Mystic Muse's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.mysticMuse_COUNT_5X5 + " Mystic Muse's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.stormScreamer_cont > ArenaConfig.stormScreamer_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.stormScreamer_COUNT_5X5 + " Storm Screamer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.stormScreamer_COUNT_5X5 + " Storm Screamer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.titan_cont > ArenaConfig.titan_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.titan_COUNT_5X5 + " Titan's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.titan_COUNT_5X5 + " Titan's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.dominator_cont > ArenaConfig.dominator_COUNT_5X5)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dominator_COUNT_5X5 + " Dominator's or " + ArenaConfig.dominator_COUNT_5X5 + " Doomcryer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dominator_COUNT_5X5 + " Dominator's or " + ArenaConfig.dominator_COUNT_5X5 + " Doomcryer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (Arena5x5.getInstance().register(player, assist1, assist2, assist3, assist4))
			{
				player.sendMessage("Tournament: Your participation has been approved.");
				assist1.sendMessage("Tournament: Your participation has been approved.");
				assist2.sendMessage("Tournament: Your participation has been approved.");
				assist3.sendMessage("Tournament: Your participation has been approved.");
				assist4.sendMessage("Tournament: Your participation has been approved.");
				
				player.setArenaProtection(true);
				assist1.setArenaProtection(true);
				assist2.setArenaProtection(true);
				assist3.setArenaProtection(true);
				assist4.setArenaProtection(true);
				
				player.setArena5x5(true);
				assist1.setArena5x5(true);
				assist2.setArena5x5(true);
				assist3.setArena5x5(true);
				assist4.setArena5x5(true);
				clean(player);
				showChatWindow(player);
			}
			else
			{
				player.sendMessage("Tournament: You or your member does not have the necessary requirements.");
			}
		}
		
		if (command.startsWith("9x9"))
		{
			if (!ArenaConfig.ALLOW_9X9_REGISTER)
			{
				player.sendMessage("Tournament 9x9 is not enabled.");
				return;
			}
			
			if ((player._active_boxes > 1) && !ArenaConfig.Allow_Same_HWID_On_Tournament)
			{
				final List<String> players_in_boxes = player.active_boxes_characters;
				
				if ((players_in_boxes != null) && (players_in_boxes.size() > 1))
				{
					for (final String character_name : players_in_boxes)
					{
						final Player ppl = World.getInstance().getPlayer(character_name);
						
						if ((ppl != null) && ppl.isArenaProtection())
						{
							player.sendMessage("You are already participating in Tournament with another char!");
							return;
						}
					}
				}
			}
			
			if (player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("Tournament: You already registered!");
				return;
			}
			
			if (!player.isInParty())
			{
				player.sendMessage("Tournament: You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("Tournament: You are not the party leader!");
				return;
			}
			
			if (player.getParty().getMemberCount() < 9)
			{
				player.sendMessage("Tournament: Your party does not have 9 members.");
				player.sendPacket(new ExShowScreenMessage("Your party does not have 9 members", 6 * 1000));
				return;
			}
			
			if (player.getParty().getMemberCount() > 9)
			{
				player.sendMessage("Tournament: Your Party can not have more than 9 members.");
				player.sendPacket(new ExShowScreenMessage("Your Party can not have more than 9 members", 6 * 1000));
				return;
			}
			
			List<Player> members = player.getParty().getMembers();
			Player assist = members.get(1);
			Player assist2 = members.get(2);
			Player assist3 = members.get(3);
			Player assist4 = members.get(4);
			Player assist5 = members.get(5);
			Player assist6 = members.get(6);
			Player assist7 = members.get(7);
			Player assist8 = members.get(8);
			
			if (!player.isInsideZone(ZoneId.TOURNAMENT) || !assist.isInsideZone(ZoneId.TOURNAMENT) || !assist2.isInsideZone(ZoneId.TOURNAMENT) || !assist3.isInsideZone(ZoneId.TOURNAMENT) || !assist4.isInsideZone(ZoneId.TOURNAMENT) || !assist5.isInsideZone(ZoneId.TOURNAMENT) || !assist6.isInsideZone(ZoneId.TOURNAMENT) || !assist7.isInsideZone(ZoneId.TOURNAMENT) || !assist8.isInsideZone(ZoneId.TOURNAMENT))
			{
				player.sendMessage("Tournament: You or your members are outside the event area.");
				assist.sendMessage("Tournament: You or your members are outside the event area.");
				assist2.sendMessage("Tournament: You or your members are outside the event area.");
				assist3.sendMessage("Tournament: You or your members are outside the event area.");
				assist4.sendMessage("Tournament: You or your members are outside the event area.");
				assist5.sendMessage("Tournament: You or your members are outside the event area.");
				assist6.sendMessage("Tournament: You or your members are outside the event area.");
				assist7.sendMessage("Tournament: You or your members are outside the event area.");
				assist8.sendMessage("Tournament: You or your members are outside the event area.");
				return;
			}
			
			if (player.isCursedWeaponEquipped() || assist.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || assist3.isCursedWeaponEquipped() || assist4.isCursedWeaponEquipped() || assist5.isCursedWeaponEquipped() || assist6.isCursedWeaponEquipped() || assist7.isCursedWeaponEquipped() || assist8.isCursedWeaponEquipped() || player.isInStoreMode() || assist.isInStoreMode() || assist2.isInStoreMode() || assist3.isInStoreMode() || assist4.isInStoreMode() || assist5.isInStoreMode() || assist6.isInStoreMode() || assist7.isInStoreMode() || assist8.isInStoreMode() || (player.getReputation() > 0) || (assist.getReputation() > 0) || (assist2.getReputation() > 0) || (assist3.getReputation() > 0) || (assist4.getReputation() > 0) || (assist5.getReputation() > 0) || (assist6.getReputation() > 0) || (assist7.getReputation() > 0) || (assist8.getReputation() > 0))
			{
				player.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist2.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist3.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist4.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist5.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist6.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist7.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				assist8.sendMessage("Tournament: You or your member does not have the necessary requirements.");
				return;
			}
			
			if (OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist) || OlympiadManager.getInstance().isRegistered(assist2) || OlympiadManager.getInstance().isRegistered(assist3) || OlympiadManager.getInstance().isRegistered(assist4) || OlympiadManager.getInstance().isRegistered(assist5) || OlympiadManager.getInstance().isRegistered(assist6) || OlympiadManager.getInstance().isRegistered(assist7) || OlympiadManager.getInstance().isRegistered(assist8))
			{
				player.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist2.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist3.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist4.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist5.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist6.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist7.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				assist8.sendMessage("Tournament: You or your member is registered in the Olympiad.");
				return;
			}
			
			// Comentar si TvTEvent no existe
			/*
			 * if (TvTEvent.isPlayerParticipant(player.getObjectId()) || TvTEvent.isPlayerParticipant(assist.getObjectId()) || TvTEvent.isPlayerParticipant(assist2.getObjectId()) || TvTEvent.isPlayerParticipant(assist3.getObjectId()) || TvTEvent.isPlayerParticipant(assist4.getObjectId()) ||
			 * TvTEvent.isPlayerParticipant(assist5.getObjectId()) || TvTEvent.isPlayerParticipant(assist6.getObjectId()) || TvTEvent.isPlayerParticipant(assist7.getObjectId()) || TvTEvent.isPlayerParticipant(assist8.getObjectId())) {
			 * player.sendMessage("Tournament: You already participated in TvT event!"); assist.sendMessage("Tournament: You already participated in TvT event!"); assist2.sendMessage("Tournament: You already participated in TvT event!");
			 * assist3.sendMessage("Tournament: You already participated in TvT event!"); assist4.sendMessage("Tournament: You already participated in TvT event!"); assist5.sendMessage("Tournament: You already participated in TvT event!");
			 * assist6.sendMessage("Tournament: You already participated in TvT event!"); assist7.sendMessage("Tournament: You already participated in TvT event!"); assist8.sendMessage("Tournament: You already participated in TvT event!"); return; }
			 */
			
			if (!ArenaConfig.Allow_Same_HWID_On_Tournament)
			{
				String ip1 = player.getIPAddress();
				String ip2 = assist.getIPAddress();
				String ip3 = assist2.getIPAddress();
				String ip4 = assist3.getIPAddress();
				String ip5 = assist4.getIPAddress();
				String ip6 = assist5.getIPAddress();
				String ip7 = assist6.getIPAddress();
				String ip8 = assist7.getIPAddress();
				String ip9 = assist8.getIPAddress();
				
				if (ip1.equals(ip2) || ip1.equals(ip3) || ip1.equals(ip4) || ip1.equals(ip5) || ip1.equals(ip6) || ip1.equals(ip7) || ip1.equals(ip8) || ip1.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				// Repetir para ip2, ip3, etc...
				else if (ip2.equals(ip1) || ip2.equals(ip3) || ip2.equals(ip4) || ip2.equals(ip5) || ip2.equals(ip6) || ip2.equals(ip7) || ip2.equals(ip8) || ip2.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip3.equals(ip1) || ip3.equals(ip2) || ip3.equals(ip4) || ip3.equals(ip5) || ip3.equals(ip6) || ip3.equals(ip7) || ip3.equals(ip8) || ip3.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip4.equals(ip1) || ip4.equals(ip2) || ip4.equals(ip3) || ip4.equals(ip5) || ip4.equals(ip6) || ip4.equals(ip7) || ip4.equals(ip8) || ip4.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip5.equals(ip1) || ip5.equals(ip2) || ip5.equals(ip3) || ip5.equals(ip4) || ip5.equals(ip6) || ip5.equals(ip7) || ip5.equals(ip8) || ip5.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip6.equals(ip1) || ip6.equals(ip2) || ip6.equals(ip3) || ip6.equals(ip4) || ip6.equals(ip5) || ip6.equals(ip7) || ip6.equals(ip8) || ip6.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip7.equals(ip1) || ip7.equals(ip2) || ip7.equals(ip3) || ip7.equals(ip4) || ip7.equals(ip5) || ip7.equals(ip6) || ip7.equals(ip8) || ip7.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip8.equals(ip1) || ip8.equals(ip2) || ip8.equals(ip3) || ip8.equals(ip4) || ip8.equals(ip5) || ip8.equals(ip6) || ip8.equals(ip7) || ip8.equals(ip9))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
				else if (ip9.equals(ip1) || ip9.equals(ip2) || ip9.equals(ip3) || ip9.equals(ip4) || ip9.equals(ip5) || ip9.equals(ip6) || ip9.equals(ip7) || ip9.equals(ip8))
				{
					player.sendMessage("Tournament: Register only 1 player per Computer");
					assist.sendMessage("Tournament: Register only 1 player per Computer");
					assist2.sendMessage("Tournament: Register only 1 player per Computer");
					assist3.sendMessage("Tournament: Register only 1 player per Computer");
					assist4.sendMessage("Tournament: Register only 1 player per Computer");
					assist5.sendMessage("Tournament: Register only 1 player per Computer");
					assist6.sendMessage("Tournament: Register only 1 player per Computer");
					assist7.sendMessage("Tournament: Register only 1 player per Computer");
					assist8.sendMessage("Tournament: Register only 1 player per Computer");
					return;
				}
			}
			
			removeEffects(player);
			removeEffects(assist);
			removeEffects(assist2);
			removeEffects(assist3);
			removeEffects(assist4);
			removeEffects(assist5);
			removeEffects(assist6);
			removeEffects(assist7);
			removeEffects(assist8);
			
			ClasseCheck(player);
			
			if (player.duelist_cont > ArenaConfig.duelist_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.duelist_COUNT_9X9 + " Duelist's or " + ArenaConfig.duelist_COUNT_9X9 + " Grand Khauatari's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.duelist_COUNT_9X9 + " Duelist's or " + ArenaConfig.duelist_COUNT_9X9 + " Grand Khauatari's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.dreadnought_cont > ArenaConfig.dreadnought_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dreadnought_COUNT_9X9 + " Dread Nought's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dreadnought_COUNT_9X9 + " Dread Nought's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.tanker_cont > ArenaConfig.tanker_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.tanker_COUNT_9X9 + " Tanker's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.tanker_COUNT_9X9 + " Tanker's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.dagger_cont > ArenaConfig.dagger_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dagger_COUNT_9X9 + " Dagger's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dagger_COUNT_9X9 + " Dagger's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.archer_cont > ArenaConfig.archer_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.archer_COUNT_9X9 + " Archer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.archer_COUNT_9X9 + " Archer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.bs_cont > ArenaConfig.bs_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.bs_COUNT_9X9 + " Bishop's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.bs_COUNT_9X9 + " Bishop's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.archmage_cont > ArenaConfig.archmage_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.archmage_COUNT_9X9 + " Archmage's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.archmage_COUNT_9X9 + " Archmage's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.soultaker_cont > ArenaConfig.soultaker_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.soultaker_COUNT_9X9 + " Soultaker's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.soultaker_COUNT_9X9 + " Soultaker's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.mysticMuse_cont > ArenaConfig.mysticMuse_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.mysticMuse_COUNT_9X9 + " Mystic Muse's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.mysticMuse_COUNT_9X9 + " Mystic Muse's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.stormScreamer_cont > ArenaConfig.stormScreamer_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.stormScreamer_COUNT_9X9 + " Storm Screamer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.stormScreamer_COUNT_9X9 + " Storm Screamer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.titan_cont > ArenaConfig.titan_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.titan_COUNT_9X9 + " Titan's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.titan_COUNT_9X9 + " Titan's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (player.dominator_cont > ArenaConfig.dominator_COUNT_9X9)
			{
				player.sendMessage("Tournament: Only " + ArenaConfig.dominator_COUNT_9X9 + " Dominator's or " + ArenaConfig.dominator_COUNT_9X9 + " Doomcryer's allowed per party.");
				player.sendPacket(new ExShowScreenMessage("Only " + ArenaConfig.dominator_COUNT_9X9 + " Dominator's or " + ArenaConfig.dominator_COUNT_9X9 + " Doomcryer's allowed per party.", 6 * 1000));
				clean(player);
				return;
			}
			else if (Arena9x9.getInstance().register(player, assist, assist2, assist3, assist4, assist5, assist6, assist7, assist8))
			{
				player.sendMessage("Tournament: Your participation has been approved.");
				assist.sendMessage("Tournament: Your participation has been approved.");
				assist2.sendMessage("Tournament: Your participation has been approved.");
				assist3.sendMessage("Tournament: Your participation has been approved.");
				assist4.sendMessage("Tournament: Your participation has been approved.");
				assist5.sendMessage("Tournament: Your participation has been approved.");
				assist6.sendMessage("Tournament: Your participation has been approved.");
				assist7.sendMessage("Tournament: Your participation has been approved.");
				assist8.sendMessage("Tournament: Your participation has been approved.");
				
				player.setArenaProtection(true);
				assist.setArenaProtection(true);
				assist2.setArenaProtection(true);
				assist3.setArenaProtection(true);
				assist4.setArenaProtection(true);
				assist5.setArenaProtection(true);
				assist6.setArenaProtection(true);
				assist7.setArenaProtection(true);
				assist8.setArenaProtection(true);
				
				player.setArena9x9(true);
				assist.setArena9x9(true);
				assist2.setArena9x9(true);
				assist3.setArena9x9(true);
				assist4.setArena9x9(true);
				assist5.setArena9x9(true);
				assist6.setArena9x9(true);
				assist7.setArena9x9(true);
				assist8.setArena9x9(true);
				clean(player);
				showChatWindow(player);
			}
			else
			{
				player.sendMessage("Tournament: You or your member does not have the necessary requirements.");
			}
		}
		else if (command.startsWith("remove"))
		{
			if (!player.isInParty() && !player.isArena1x1())
			{
				player.sendMessage("Tournament: You dont have a party.");
				return;
			}
			else if ((player.getParty() != null) && !player.getParty().isLeader(player))
			{
				player.sendMessage("Tournament: You are not the party leader!");
				return;
			}
			
			if (player.isArena1x1())
			{
				Arena1x1.getInstance().remove(player);
			}
			if (player.isArena3x3())
			{
				Arena3x3.getInstance().remove(player);
			}
			if (player.isArena5x5())
			{
				Arena5x5.getInstance().remove(player);
			}
			if (player.isArena9x9())
			{
				Arena9x9.getInstance().remove(player);
			}
		}
		else if (command.startsWith("observeOne"))
		{
			observe1x1List(player);
		}
		else if (command.startsWith("observeTwo"))
		{
			observe3x3List(player);
		}
		else if (command.startsWith("observeTree"))
		{
			observe5x5List(player);
		}
		else if (command.startsWith("observeFour"))
		{
			observe9x9List(player);
		}
		else if (command.startsWith("back"))
		{
			showChatWindow(player);
		}
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		if (currentCommand.equalsIgnoreCase("watchOne"))
		{
			if (player.isArena1x1() || player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("You can't watch when you are registered.");
				return;
			}
			
			int arenaId = 0;
			if (st.countTokens() == 1)
			{
				arenaId = Integer.valueOf(st.nextToken()).intValue();
			}
			
			Arena1x1.getInstance().addSpectator(player, arenaId);
		}
		else if (currentCommand.equalsIgnoreCase("watchTwo"))
		{
			if (player.isArena1x1() || player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("You can't watch when you are registered.");
				return;
			}
			
			int arenaId = 0;
			if (st.countTokens() == 1)
			{
				arenaId = Integer.valueOf(st.nextToken()).intValue();
			}
			
			Arena3x3.getInstance().addSpectator(player, arenaId);
		}
		else if (currentCommand.equalsIgnoreCase("watchTree"))
		{
			if (player.isArena1x1() || player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("You can't watch when you are registered.");
				return;
			}
			
			int arenaId = 0;
			if (st.countTokens() == 1)
			{
				arenaId = Integer.valueOf(st.nextToken()).intValue();
			}
			
			Arena5x5.getInstance().addSpectator(player, arenaId);
		}
		else if (currentCommand.equalsIgnoreCase("watchFour"))
		{
			if (player.isArena1x1() || player.isArena3x3() || player.isArena5x5() || player.isArena9x9() || player.isArenaProtection())
			{
				player.sendMessage("You can't watch when you are registered.");
				return;
			}
			
			int arenaId = 0;
			if (st.countTokens() == 1)
			{
				arenaId = Integer.valueOf(st.nextToken()).intValue();
			}
			
			Arena9x9.getInstance().addSpectator(player, arenaId);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	// Método para remover efectos de un jugador
	private void removeEffects(Player player)
	{
		if ((player == null) || !player.isOnline())
		{
			return;
		}
		
		for (BuffInfo info : player.getEffectList().getBuffs())
		{
			if (ArenaConfig.ARENA_DISABLE_SKILL_LIST_PERM.contains(info.getSkill().getId()))
			{
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
			}
			if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
			{
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
			}
		}
	}
	
	public void observe1x1List(Player player)
	{
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> fights = Arena1x1.getInstance().getFights();
		if ((fights == null) || fights.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">There are no 1x1 fights occurring.</font>");
		}
		else
		{
			for (Iterator<Integer> iterator = fights.keySet().iterator(); iterator.hasNext();)
			{
				int id = iterator.next().intValue();
				sb.append("<font color=\"LEVEL\">" + fights.get(Integer.valueOf(id)) + "</font><br1>");
			}
		}
		String filename = "data/html/mods/tournament/Watch-1.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%fighter%", sb.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	public void observe3x3List(Player player)
	{
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> fights = Arena3x3.getInstance().getFights();
		if ((fights == null) || fights.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">There are no 3x3 fights occurring.</font>");
		}
		else
		{
			for (Iterator<Integer> iterator = fights.keySet().iterator(); iterator.hasNext();)
			{
				int id = iterator.next().intValue();
				sb.append("<font color=\"LEVEL\">" + fights.get(Integer.valueOf(id)) + "</font><br1>");
			}
		}
		String filename = "data/html/mods/tournament/Watch-2.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%fighter%", sb.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	public void observe5x5List(Player player)
	{
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> fights = Arena5x5.getInstance().getFights();
		if ((fights == null) || fights.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">There are no 5x5 fights occurring.</font>");
		}
		else
		{
			for (Iterator<Integer> iterator = fights.keySet().iterator(); iterator.hasNext();)
			{
				int id = iterator.next().intValue();
				sb.append("<font color=\"LEVEL\">" + fights.get(Integer.valueOf(id)) + "</font><br1>");
			}
		}
		String filename = "data/html/mods/tournament/Watch-3.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%fighter%", sb.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	public void observe9x9List(Player player)
	{
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> fights = Arena9x9.getInstance().getFights();
		if ((fights == null) || fights.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">There are no 9x9 fights occurring.</font>");
		}
		else
		{
			for (Iterator<Integer> iterator = fights.keySet().iterator(); iterator.hasNext();)
			{
				int id = iterator.next().intValue();
				sb.append("<font color=\"LEVEL\">" + fights.get(Integer.valueOf(id)) + "</font><br1>");
			}
		}
		String filename = "data/html/mods/tournament/Watch-4.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%fighter%", sb.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	public void ClasseCheck(Player activeChar)
	{
		Party plparty = activeChar.getParty();
		for (Player player : plparty.getMembers())
		{
			if (player != null)
			{
				if (player.getParty() != null)
				{
					if ((player.getClassId() == ClassId.GLADIATOR) || (player.getClassId() == ClassId.DUELIST))
					{
						activeChar.duelist_cont = activeChar.duelist_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.WARLORD) || (player.getClassId() == ClassId.DREADNOUGHT))
					{
						activeChar.dreadnought_cont = activeChar.dreadnought_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.PALADIN) || (player.getClassId() == ClassId.PHOENIX_KNIGHT) || (player.getClassId() == ClassId.DARK_AVENGER) || (player.getClassId() == ClassId.HELL_KNIGHT) || (player.getClassId() == ClassId.EVA_TEMPLAR) || (player.getClassId() == ClassId.TEMPLE_KNIGHT) || (player.getClassId() == ClassId.SHILLIEN_KNIGHT) || (player.getClassId() == ClassId.SHILLIEN_TEMPLAR))
					{
						activeChar.tanker_cont = activeChar.tanker_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.ADVENTURER) || (player.getClassId() == ClassId.TREASURE_HUNTER) || (player.getClassId() == ClassId.WIND_RIDER) || (player.getClassId() == ClassId.PLAINS_WALKER) || (player.getClassId() == ClassId.GHOST_HUNTER) || (player.getClassId() == ClassId.ABYSS_WALKER))
					{
						activeChar.dagger_cont = activeChar.dagger_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.HAWKEYE) || (player.getClassId() == ClassId.SAGITTARIUS) || (player.getClassId() == ClassId.SILVER_RANGER) || (player.getClassId() == ClassId.MOONLIGHT_SENTINEL) || (player.getClassId() == ClassId.PHANTOM_RANGER) || (player.getClassId() == ClassId.GHOST_SENTINEL))
					{
						activeChar.archer_cont = activeChar.archer_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.SHILLIEN_ELDER) || (player.getClassId() == ClassId.SHILLIEN_SAINT) || (player.getClassId() == ClassId.BISHOP) || (player.getClassId() == ClassId.CARDINAL) || (player.getClassId() == ClassId.ELDER) || (player.getClassId() == ClassId.EVA_SAINT))
					{
						activeChar.bs_cont = activeChar.bs_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.ARCHMAGE) || (player.getClassId() == ClassId.SORCERER))
					{
						activeChar.archmage_cont = activeChar.archmage_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.SOULTAKER) || (player.getClassId() == ClassId.NECROMANCER))
					{
						activeChar.soultaker_cont = activeChar.soultaker_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.MYSTIC_MUSE) || (player.getClassId() == ClassId.SPELLSINGER))
					{
						activeChar.mysticMuse_cont = activeChar.mysticMuse_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.STORM_SCREAMER) || (player.getClassId() == ClassId.SPELLHOWLER))
					{
						activeChar.stormScreamer_cont = activeChar.stormScreamer_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.TITAN) || (player.getClassId() == ClassId.DESTROYER))
					{
						activeChar.titan_cont = activeChar.titan_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.TYRANT) || (player.getClassId() == ClassId.GRAND_KHAVATARI))
					{
						activeChar.grandKhauatari_cont = activeChar.grandKhauatari_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.ORC_SHAMAN) || (player.getClassId() == ClassId.OVERLORD))
					{
						activeChar.dominator_cont = activeChar.dominator_cont + 1;
					}
					
					if ((player.getClassId() == ClassId.DOOMCRYER) || (player.getClassId() == ClassId.WARCRYER))
					{
						activeChar.doomcryer_cont = activeChar.doomcryer_cont + 1;
					}
				}
			}
		}
	}
	
	public void clean(Player player)
	{
		player.duelist_cont = 0;
		player.dreadnought_cont = 0;
		player.tanker_cont = 0;
		player.dagger_cont = 0;
		player.archer_cont = 0;
		player.bs_cont = 0;
		player.archmage_cont = 0;
		player.soultaker_cont = 0;
		player.mysticMuse_cont = 0;
		player.stormScreamer_cont = 0;
		player.titan_cont = 0;
		player.grandKhauatari_cont = 0;
		player.dominator_cont = 0;
		player.doomcryer_cont = 0;
	}
}