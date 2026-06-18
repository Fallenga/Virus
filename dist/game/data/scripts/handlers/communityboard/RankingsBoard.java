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
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.stream.IntStream;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 
 */
public class RankingsBoard implements IParseBoardHandler
{
	private static final StringBuilder PVP = new StringBuilder();
	private static final StringBuilder PKS = new StringBuilder();
	
	private static final int PAGE_LIMIT_15 = 15;
	
	private long _nextUpdate;
	
	public RankingsBoard()
	{
		
	}
	
	private static final String[] COMMANDS =
	{
		"_bbsranking",
	
	};
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		try
		{
			if (command.equals("_bbsranking"))
			{
				showRakingList(player);
				return true;
			}
			
		}
		catch (Exception exc)
		{
			System.out.println(exc);
		}
		return false;
	}
	
	public void showRakingList(Player player)
	{
		if (_nextUpdate < System.currentTimeMillis())
		{
			PVP.setLength(0);
			PKS.setLength(0);
			
			try (Connection con = DatabaseFactory.getConnection();)
			{
				try (PreparedStatement ps = con.prepareStatement("SELECT char_name, pvpkills FROM characters WHERE pvpkills > 0 ORDER BY pvpkills DESC LIMIT " + PAGE_LIMIT_15);
					ResultSet rs = ps.executeQuery())
				{
					int index = 1;
					while (rs.next())
					{
						final String name = rs.getString("char_name");
						final Player databasePlayer = World.getInstance().getPlayer(name);
						final String status = "L2UI_CH3.msnicon" + ((databasePlayer != null) && databasePlayer.isOnline() ? "1" : "4");
						
						StringUtil.append(PVP, "<table width=300 bgcolor=000000><tr><td width=20 align=right>", getColor(index), String.format("%02d", index), "</td>");
						StringUtil.append(PVP, "<td width=20 height=18><img src=", status, " width=16 height=16></td><td width=160 align=left>", name, "</td>");
						StringUtil.append(PVP, "<td width=100 align=right>", StringUtil.formatNumber(rs.getInt("pvpkills")), "</font></td></tr></table><img src=L2UI.SquareGray width=296 height=1>");
						index++;
					}
					IntStream.range(index - 1, PAGE_LIMIT_15).forEach(x -> applyEmpty(PVP));
				}
				
				try (PreparedStatement ps = con.prepareStatement("SELECT char_name, pkkills FROM characters WHERE pkkills > 0 ORDER BY pkkills DESC LIMIT " + PAGE_LIMIT_15);
					ResultSet rs = ps.executeQuery())
				{
					int index = 1;
					while (rs.next())
					{
						final String name = rs.getString("char_name");
						final Player databasePlayer = World.getInstance().getPlayer(name);
						final String status = "L2UI_CH3.msnicon" + ((databasePlayer != null) && databasePlayer.isOnline() ? "1" : "4");
						
						StringUtil.append(PKS, "<table width=300 bgcolor=000000><tr><td width=20 align=right>", getColor(index), String.format("%02d", index), "</td>");
						StringUtil.append(PKS, "<td width=20 height=18><img src=", status, " width=16 height=16></td><td width=160 align=left>", name, "</td>");
						StringUtil.append(PKS, "<td width=100 align=right>", StringUtil.formatNumber(rs.getInt("pkkills")), "</font></td></tr></table><img src=L2UI.SquareGray width=296 height=1>");
						index++;
					}
					IntStream.range(index - 1, PAGE_LIMIT_15).forEach(x -> applyEmpty(PKS));
				}
			}
			catch (Exception e)
			{
			}
			
			_nextUpdate = System.currentTimeMillis() + 60000L;
		}
		String content = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/ranking/rankinglist.html");
		
		if (content != null)
		{
			content = content.replaceAll("%name%", player.getName());
			content = content.replaceAll("%pvp%", PVP.toString());
			content = content.replaceAll("%pks%", PKS.toString());
			content = content.replaceAll("%time%", String.valueOf((_nextUpdate - System.currentTimeMillis()) / 1000));
			CommunityBoardHandler.separateAndSend(content, player);
			
		}
		else
		{
			
		}
	}
	
	public String getColor(int index)
	{
		switch (index)
		{
			case 1:
				return "<font color=FFFF00>";
			case 2:
				return "<font color=FFA500>";
			case 3:
				return "<font color=E9967A>";
		}
		return "";
	}
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	public void applyEmpty(StringBuilder sb)
	{
		sb.append("<table width=300 bgcolor=000000><tr>");
		sb.append("<td width=20 align=right><font color=B09878>--</font></td><td width=20 height=18></td>");
		sb.append("<td width=160 align=left><font color=B09878>----------------</font></td>");
		sb.append("<td width=100 align=right><font color=FF0000>0</font></td>");
		sb.append("</tr></table><img src=L2UI.SquareGray width=296 height=1>");
	}
	
	public static RankingsBoard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RankingsBoard INSTANCE = new RankingsBoard();
	}
	
}