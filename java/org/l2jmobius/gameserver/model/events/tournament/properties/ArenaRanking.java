/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.events.tournament.properties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ArenaRanking
{
	public static int getRank1x1(Player player)
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 1x1_rank FROM characters WHERE charId=?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					id = rs.getInt("1x1_rank");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return id;
	}
	
	public static int getRank3x3(Player player)
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 3x3_rank FROM characters WHERE charId=?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					id = rs.getInt("3x3_rank");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return id;
	}
	
	public static int getRank5x5(Player player)
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 5x5_rank FROM characters WHERE charId=?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					id = rs.getInt("5x5_rank");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return id;
	}
	
	public static int getRank9x9(Player player)
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 9x9_rank FROM characters WHERE charId=?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					id = rs.getInt("9x9_rank");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return id;
	}
	
	public static void addRank1x1(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 1x1_rank=? WHERE charId=?"))
		{
			ps.setInt(1, getRank1x1(player) + 1);
			ps.setInt(2, player.getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void addRank3x3(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 3x3_rank=? WHERE charId=?"))
		{
			ps.setInt(1, getRank3x3(player) + 1);
			ps.setInt(2, player.getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void addRank5x5(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 5x5_rank=? WHERE charId=?"))
		{
			ps.setInt(1, getRank5x5(player) + 1);
			ps.setInt(2, player.getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void addRank9x9(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 9x9_rank=? WHERE charId=?"))
		{
			ps.setInt(1, getRank9x9(player) + 1);
			ps.setInt(2, player.getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void resetRank1x1()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 1x1_rank=0"))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void resetRank3x3()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 3x3_rank=0"))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void resetRank5x5()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 5x5_rank=0"))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void resetRank9x9()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET 9x9_rank=0"))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	static int getRank1x1Count()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 1x1_rank FROM characters ORDER BY 1x1_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("1x1_rank");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getRank3x3Count()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 3x3_rank FROM characters ORDER BY 3x3_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("3x3_rank");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getRank5x5Count()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 5x5_rank FROM characters ORDER BY 5x5_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("5x5_rank");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getRank9x9Count()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT 9x9_rank FROM characters ORDER BY 9x9_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("9x9_rank");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getTopRank1x1PlayerReward()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters ORDER BY 1x1_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("charId");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getTopRank3x3PlayerReward()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters ORDER BY 3x3_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("charId");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getTopRank5x5PlayerReward()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters ORDER BY 5x5_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("charId");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	static int getTopRank9x9PlayerReward()
	{
		int id = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters ORDER BY 9x9_rank DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				id = rs.getInt("charId");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return id;
	}
	
	public static void getTopRank1x1Html(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder("<html><head><title>Ranking Event 1x1</title></head><body><center><img src=\"l2ui_ch3.herotower_deco\" width=256 height=32></center><br1><table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>Win's</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,1x1_rank,online FROM characters WHERE 1x1_rank>0 AND accesslevel=0 order by 1x1_rank desc limit 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String wins = rs.getString("1x1_rank");
				String name = rs.getString("char_name");
				
				if (name.equals("WWWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWW") || name.equals("WWWWWWWWWWW") || name.equals("WWWWWWWWWW") || name.equals("WWWWWWWWW") || name.equals("WWWWWWWW") || name.equals("WWWWWWW") || name.equals("WWWWWW"))
				{
					name = name.substring(0, 3) + "..";
				}
				else if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos += 1;
				String statu = rs.getString("online");
				String status;
				if (statu.equals("1"))
				{
					status = "<font color=00FF00>Online</font>";
				}
				else
				{
					status = "<font color=FF0000>Offline</font>";
				}
				
				tb.append("<tr><td><center>" + pos + "</td><td><center><font color=00FFFF>" + name + "</font></center></td><td><center>" + wins + "</center></td><td><center>" + status + "</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		tb.append("</table><br>");
		tb.append("<center><a action=\"bypass -h voiced_ranking\">Back</a></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		player.sendPacket(htm);
	}
	
	public static void getTopRank3x3Html(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder("<html><head><title>Ranking Event 3x3</title></head><body><center><img src=\"l2ui_ch3.herotower_deco\" width=256 height=32></center><br1><table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>Win's</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,3x3_rank,online FROM characters WHERE 3x3_rank>0 AND accesslevel=0 order by 3x3_rank desc limit 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String wins = rs.getString("3x3_rank");
				String name = rs.getString("char_name");
				
				if (name.equals("WWWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWW") || name.equals("WWWWWWWWWWW") || name.equals("WWWWWWWWWW") || name.equals("WWWWWWWWW") || name.equals("WWWWWWWW") || name.equals("WWWWWWW") || name.equals("WWWWWW"))
				{
					name = name.substring(0, 3) + "..";
				}
				else if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos += 1;
				String statu = rs.getString("online");
				String status;
				if (statu.equals("1"))
				{
					status = "<font color=00FF00>Online</font>";
				}
				else
				{
					status = "<font color=FF0000>Offline</font>";
				}
				
				tb.append("<tr><td><center>" + pos + "</td><td><center><font color=00FFFF>" + name + "</font></center></td><td><center>" + wins + "</center></td><td><center>" + status + "</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		tb.append("</table><br>");
		tb.append("<center><a action=\"bypass -h voiced_ranking\">Back</a></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		player.sendPacket(htm);
	}
	
	public static void getTopRank5x5Html(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder("<html><head><title>Ranking Event 5x5</title></head><body><center><img src=\"l2ui_ch3.herotower_deco\" width=256 height=32></center><br1><table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>Win's</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,5x5_rank,online FROM characters WHERE 5x5_rank>0 AND accesslevel=0 order by 5x5_rank desc limit 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String wins = rs.getString("5x5_rank");
				String name = rs.getString("char_name");
				
				if (name.equals("WWWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWW") || name.equals("WWWWWWWWWWW") || name.equals("WWWWWWWWWW") || name.equals("WWWWWWWWW") || name.equals("WWWWWWWW") || name.equals("WWWWWWW") || name.equals("WWWWWW"))
				{
					name = name.substring(0, 3) + "..";
				}
				else if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos += 1;
				String statu = rs.getString("online");
				String status;
				if (statu.equals("1"))
				{
					status = "<font color=00FF00>Online</font>";
				}
				else
				{
					status = "<font color=FF0000>Offline</font>";
				}
				
				tb.append("<tr><td><center>" + pos + "</td><td><center><font color=00FFFF>" + name + "</font></center></td><td><center>" + wins + "</center></td><td><center>" + status + "</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		tb.append("</table><br>");
		tb.append("<center><a action=\"bypass -h voiced_ranking\">Back</a></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		player.sendPacket(htm);
	}
	
	public static void getTopRank9x9Html(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder("<html><head><title>Ranking Event 9x9</title></head><body><center><img src=\"l2ui_ch3.herotower_deco\" width=256 height=32></center><br1><table width=290><tr><td><center>Rank</center></td><td><center>Character</center></td><td><center>Win's</center></td><td><center>Status</center></td></tr>");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,9x9_rank,online FROM characters WHERE 9x9_rank>0 AND accesslevel=0 order by 9x9_rank desc limit 15");
			ResultSet rs = ps.executeQuery())
		{
			int pos = 0;
			while (rs.next())
			{
				String wins = rs.getString("9x9_rank");
				String name = rs.getString("char_name");
				
				if (name.equals("WWWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWWW") || name.equals("WWWWWWWWWWWW") || name.equals("WWWWWWWWWWW") || name.equals("WWWWWWWWWW") || name.equals("WWWWWWWWW") || name.equals("WWWWWWWW") || name.equals("WWWWWWW") || name.equals("WWWWWW"))
				{
					name = name.substring(0, 3) + "..";
				}
				else if (name.length() > 14)
				{
					name = name.substring(0, 14) + "..";
				}
				
				pos += 1;
				String statu = rs.getString("online");
				String status;
				if (statu.equals("1"))
				{
					status = "<font color=00FF00>Online</font>";
				}
				else
				{
					status = "<font color=FF0000>Offline</font>";
				}
				
				tb.append("<tr><td><center>" + pos + "</td><td><center><font color=00FFFF>" + name + "</font></center></td><td><center>" + wins + "</center></td><td><center>" + status + "</center></td></tr>");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		tb.append("</table><br>");
		tb.append("<center><a action=\"bypass -h voiced_ranking\">Back</a></center>");
		tb.append("</body></html>");
		
		htm.setHtml(tb.toString());
		player.sendPacket(htm);
	}
	
	public static void rankingRewardPlayer()
	{
		// Reward Top Winner - Descomentar cuando las funciones estén implementadas
		// addReward1x1(getTopRank1x1PlayerReward());
		// addReward3x3(getTopRank3x3PlayerReward());
		// addReward5x5(getTopRank5x5PlayerReward());
		// addReward9x9(getTopRank9x9PlayerReward());
		
		// Reset Ranking
		resetRank1x1();
		resetRank3x3();
		resetRank5x5();
		resetRank9x9();
	}
}