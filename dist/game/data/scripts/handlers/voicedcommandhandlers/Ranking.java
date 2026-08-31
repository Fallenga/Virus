/*
 * Copyright (C) 2004-2014 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaRanking;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Lucas
 */
public class Ranking implements IVoicedCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(Ranking.class.getName());
	
	private static final String[] VOICED_COMMANDS =
	{
		"pvp",
		"level",
		"pks",
		"clan",
		"1x1_rank",
		"9x9_rank",
		"eloPoint",
		"clanPoint",
		"ranking"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		// Menú principal
		if (command.equals("ranking"))
		{
			showRankingHtml(activeChar);
			return true;
		}
		
		// Ranking PvP
		if (command.equals("pvp"))
		{
			showPvpRanking(activeChar);
			return true;
		}
		
		// Ranking Level
		if (command.equals("level"))
		{
			showLevelRanking(activeChar);
			return true;
		}
		
		// Ranking PK
		if (command.equals("pks"))
		{
			showPkRanking(activeChar);
			return true;
		}
		
		// Ranking Clan
		if (command.equals("clan"))
		{
			showClanRanking(activeChar);
			return true;
		}
		
		// Rankings del torneo
		if (command.equals("1x1_rank"))
		{
			ArenaRanking.getTopRank1x1Html(activeChar);
			return true;
		}
		
		if (command.equals("9x9_rank"))
		{
			ArenaRanking.getTopRank9x9Html(activeChar);
			return true;
		}
		
		// Ranking Elo
		if (command.equals("eloPoint"))
		{
			showEloRanking(activeChar);
			return true;
		}
		
		// Ranking Clan Points
		if (command.equals("clanPoint"))
		{
			showClanPointRanking(activeChar);
			return true;
		}
		
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	// =============================================
	// MÉTODOS PARA MOSTRAR RANKINGS
	// =============================================
	
	private void showRankingHtml(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(activeChar, "data/html/mods/menu/Ranking.htm");
		activeChar.sendPacket(html);
	}
	
	private void showPvpRanking(Player activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		tb.append("<html><head><title>PvP Ranking</title></head><body>");
		tb.append("<center><br><font color=\"LEVEL\">[ PvP Ranking ]</font><br><br>");
		tb.append("<table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>PvP's</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,pvpkills,online FROM characters WHERE pvpkills>0 AND accesslevel=0 ORDER BY pvpkills DESC LIMIT 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String pvps = rs.getString("pvpkills");
				String name = rs.getString("char_name");
				
				if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos++;
				String status = rs.getString("online").equals("1") ? "<font color=00FF00>Online</font>" : "<font color=FF0000>Offline</font>";
				
				tb.append("<tr><td><center>").append(pos).append("</td>");
				tb.append("<td><center><font color=00FFFF>").append(name).append("</font></center></td>");
				tb.append("<td><center>").append(pvps).append("</center></td>");
				tb.append("<td><center>").append(status).append("</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error: could not restore pvp ranking data info: " + e);
		}
		
		tb.append("</table><br>");
		tb.append("<center><table width=290><tr><td align=center>");
		tb.append("<button value=\"Back\" action=\"bypass -h ranking\" width=125 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		tb.append("</td></tr></table></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		activeChar.sendPacket(htm);
	}
	
	private void showLevelRanking(Player activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		tb.append("<html><head><title>Level Ranking</title></head><body>");
		tb.append("<center><br><font color=\"LEVEL\">[ Level Ranking ]</font><br><br>");
		tb.append("<table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>Level</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,level,online FROM characters WHERE level>0 AND accesslevel=0 ORDER BY level DESC LIMIT 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String lvl = rs.getString("level");
				String name = rs.getString("char_name");
				
				if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos++;
				String status = rs.getString("online").equals("1") ? "<font color=00FF00>Online</font>" : "<font color=FF0000>Offline</font>";
				
				tb.append("<tr><td><center>").append(pos).append("</td>");
				tb.append("<td><center><font color=00FFFF>").append(name).append("</font></center></td>");
				tb.append("<td><center>").append(lvl).append("</center></td>");
				tb.append("<td><center>").append(status).append("</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error: could not restore level ranking data info: " + e);
		}
		
		tb.append("</table><br>");
		tb.append("<center><table width=290><tr><td align=center>");
		tb.append("<button value=\"Back\" action=\"bypass -h ranking\" width=125 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		tb.append("</td></tr></table></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		activeChar.sendPacket(htm);
	}
	
	private void showPkRanking(Player activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		tb.append("<html><head><title>PK Ranking</title></head><body>");
		tb.append("<center><br><font color=\"LEVEL\">[ PK Ranking ]</font><br><br>");
		tb.append("<table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>PK's</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,pkkills,online FROM characters WHERE pkkills>0 AND accesslevel=0 ORDER BY pkkills DESC LIMIT 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String pks = rs.getString("pkkills");
				String name = rs.getString("char_name");
				
				if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos++;
				String status = rs.getString("online").equals("1") ? "<font color=00FF00>Online</font>" : "<font color=FF0000>Offline</font>";
				
				tb.append("<tr><td><center>").append(pos).append("</td>");
				tb.append("<td><center><font color=00FFFF>").append(name).append("</font></center></td>");
				tb.append("<td><center>").append(pks).append("</center></td>");
				tb.append("<td><center>").append(status).append("</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error: could not restore pk ranking data info: " + e);
		}
		
		tb.append("</table><br>");
		tb.append("<center><table width=290><tr><td align=center>");
		tb.append("<button value=\"Back\" action=\"bypass -h ranking\" width=125 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		tb.append("</td></tr></table></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		activeChar.sendPacket(htm);
	}
	
	private void showClanRanking(Player activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		tb.append("<html><head><title>Clan Ranking</title></head><body>");
		tb.append("<center><br><font color=\"LEVEL\">[ Clan Ranking ]</font><br><br>");
		tb.append("<table width=290><tr><td><center>Rank</center></td><td><center>Level</center></td><td><center>Clan Name</center></td><td><center>Reputation</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT clan_name,clan_level,reputation_score FROM clan_data WHERE clan_level>0 ORDER BY reputation_score DESC LIMIT 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String clan_name = rs.getString("clan_name");
				String clan_level = rs.getString("clan_level");
				String clan_score = rs.getString("reputation_score");
				
				if (clan_name.length() > 14)
				{
					clan_name = clan_name.substring(0, 14) + "..";
				}
				
				pos++;
				tb.append("<tr><td><center>").append(pos).append("</center></td>");
				tb.append("<td><center>").append(clan_level).append("</center></td>");
				tb.append("<td><center><font color=00FFFF>").append(clan_name).append("</font></center></td>");
				tb.append("<td><center><font color=00FF00>").append(clan_score).append("</font></center></td></tr>");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error: could not restore clan ranking data info: " + e);
		}
		
		tb.append("</table><br>");
		tb.append("<center><table width=290><tr><td align=center>");
		tb.append("<button value=\"Back\" action=\"bypass -h ranking\" width=125 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		tb.append("</td></tr></table></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		activeChar.sendPacket(htm);
	}
	
	private void showEloRanking(Player activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		tb.append("<html><head><title>Elo Points Ranking</title></head><body>");
		tb.append("<center><br><font color=\"LEVEL\">[ Elo Points Ranking ]</font><br><br>");
		tb.append("<table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>Points</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,pc_point,online FROM characters WHERE pc_point>0 AND accesslevel=0 ORDER BY pc_point DESC LIMIT 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String points = rs.getString("pc_point");
				String name = rs.getString("char_name");
				
				if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos++;
				String status = rs.getString("online").equals("1") ? "<font color=00FF00>Online</font>" : "<font color=FF0000>Offline</font>";
				
				tb.append("<tr><td><center>").append(pos).append("</td>");
				tb.append("<td><center><font color=00FFFF>").append(name).append("</font></center></td>");
				tb.append("<td><center>").append(points).append("</center></td>");
				tb.append("<td><center>").append(status).append("</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Error: could not restore elo ranking data info: " + e);
		}
		
		tb.append("</table><br>");
		tb.append("<center><table width=290><tr><td align=center>");
		tb.append("<button value=\"Back\" action=\"bypass -h ranking\" width=125 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		tb.append("</td></tr></table></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		activeChar.sendPacket(htm);
	}
	
	private void showClanPointRanking(Player activeChar)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		tb.append("<html><head><title>Clan Points Ranking</title></head><body>");
		tb.append("<center><br><font color=\"LEVEL\">[ Clan Points Ranking ]</font><br><br>");
		tb.append("<table width=290><tr><td><center>Rank</center></td><td><center>Clan Name</center></td><td><center>Raid Points</center></td><td><center>Castle Points</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement ps = con.prepareStatement("SELECT clan_id, boss_points, siege_points FROM clan_points WHERE ((boss_points + siege_points)>0) ORDER BY (boss_points + siege_points) DESC LIMIT 15");
			ResultSet rs = ps.executeQuery();
			int pos = 0;
			
			while (rs.next())
			{
				String raid = rs.getString("boss_points");
				String castle = rs.getString("siege_points");
				String owner = rs.getString("clan_id");
				pos++;
				
				PreparedStatement clanPs = con.prepareStatement("SELECT clan_name FROM clan_data WHERE clan_id=" + owner);
				ResultSet clanRs = clanPs.executeQuery();
				
				while (clanRs.next())
				{
					String clan_name = clanRs.getString("clan_name");
					if (clan_name.length() > 14)
					{
						clan_name = clan_name.substring(0, 14) + "..";
					}
					tb.append("<tr><td><center>").append(pos).append("</center></td>");
					tb.append("<td><center><font color=00FFFF>").append(clan_name).append("</font></center></td>");
					tb.append("<td><center>").append(raid).append("</center></td>");
					tb.append("<td><center>").append(castle).append("</center></td></tr>");
				}
				clanRs.close();
				clanPs.close();
			}
			rs.close();
			ps.close();
		}
		catch (Exception e)
		{
			LOGGER.warning("Error: could not restore clan_points ranking data info: " + e);
		}
		
		tb.append("</table><br>");
		tb.append("<center><table width=290><tr><td align=center>");
		tb.append("<button value=\"Back\" action=\"bypass -h ranking\" width=125 height=21 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		tb.append("</td></tr></table></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		activeChar.sendPacket(htm);
	}
}