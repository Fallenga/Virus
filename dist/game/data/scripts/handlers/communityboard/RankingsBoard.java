/*
 * This file is part of the L2J Mobius project.
 */
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/** Modern Community Board rankings for L2 Templar. */
public class RankingsBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(RankingsBoard.class.getName());
	private static final String HTML_PATH = "data/html/CommunityBoard/Custom/ranking/ranking.html";
	private static final int LIMIT = 15;
	private static final long CACHE_TIME = 60000L;
	private static final String[] COMMANDS =
	{
		"_bbsranking"
	};

	private final Object _cacheLock = new Object();
	private volatile long _nextUpdate;
	private String _pvpRanking = "";
	private String _pkRanking = "";
	private String _levelRanking = "";
	private String _onlineTimeRanking = "";

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!command.startsWith("_bbsranking"))
		{
			return false;
		}

		updateCache();
		String type = "pvp";
		final String[] parts = command.split(";");
		if (parts.length > 1)
		{
			type = parts[1].toLowerCase();
		}

		String title;
		String column;
		String ranking;
		switch (type)
		{
			case "pk":
			{
				title = "RANKING PK";
				column = "PK";
				ranking = _pkRanking;
				break;
			}
			case "level":
			{
				title = "RANKING DE NIVEL";
				column = "NIVEL";
				ranking = _levelRanking;
				break;
			}
			case "onlinetime":
			{
				title = "TIEMPO CONECTADO";
				column = "HORAS";
				ranking = _onlineTimeRanking;
				break;
			}
			default:
			{
				title = "RANKING PVP";
				column = "PVP";
				ranking = _pvpRanking;
				break;
			}
		}

		String html = HtmCache.getInstance().getHtm(player, HTML_PATH);
		if (html == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Missing HTML: " + HTML_PATH);
			return false;
		}

		html = html.replace("%title%", title);
		html = html.replace("%column%", column);
		html = html.replace("%ranking%", ranking);
		html = html.replace("%time%", Long.toString(Math.max(0L, (_nextUpdate - System.currentTimeMillis()) / 1000L)));
		CommunityBoardHandler.getInstance().addBypass(player, title, command);
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}

	private void updateCache()
	{
		if (_nextUpdate > System.currentTimeMillis())
		{
			return;
		}

		synchronized (_cacheLock)
		{
			if (_nextUpdate > System.currentTimeMillis())
			{
				return;
			}

			_pvpRanking = loadRanking("pvpkills", "pvpkills", false);
			_pkRanking = loadRanking("pkkills", "pkkills", false);
			_levelRanking = loadRanking("level", "level", false);
			_onlineTimeRanking = loadRanking("onlinetime", "onlinetime", true);
			_nextUpdate = System.currentTimeMillis() + CACHE_TIME;
		}
	}

	private String loadRanking(String valueColumn, String orderColumn, boolean secondsToHours)
	{
		final StringBuilder rows = new StringBuilder();
		final String query = "SELECT char_name, " + valueColumn + ", online FROM characters WHERE accesslevel = 0 AND " + valueColumn + " > 0 ORDER BY " + orderColumn + " DESC, char_name ASC LIMIT " + LIMIT;
		int position = 1;

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final String name = escapeHtml(shorten(rs.getString("char_name"), 18));
				long value = rs.getLong(valueColumn);
				if (secondsToHours)
				{
					value /= 3600L;
				}
				final boolean online = rs.getInt("online") > 0;
				final String color = getPositionColor(position);
				final String rowColor = (position % 2 == 0) ? "161616" : "0E0E0E";
				rows.append("<table width=520 height=24 bgcolor=").append(rowColor).append("><tr>");
				rows.append("<td width=45 align=center><font color=").append(color).append(">").append(position).append("</font></td>");
				rows.append("<td width=28 align=center><img src=\"L2UI_CH3.msnicon").append(online ? "1" : "4").append("\" width=16 height=16></td>");
				rows.append("<td width=327><font color=D8C7A1>").append(name).append("</font></td>");
				rows.append("<td width=120 align=center><font color=FFB83D>").append(value).append("</font></td>");
				rows.append("</tr></table>");
				position++;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load ranking " + valueColumn, e);
		}

		if (position == 1)
		{
			rows.append("<table width=520 height=80><tr><td align=center><font color=777777>No hay datos disponibles.</font></td></tr></table>");
		}
		return rows.toString();
	}

	private static String getPositionColor(int position)
	{
		switch (position)
		{
			case 1:
				return "FFD700";
			case 2:
				return "C0C0C0";
			case 3:
				return "CD7F32";
			default:
				return "A9A8A4";
		}
	}

	private static String shorten(String text, int maxLength)
	{
		return (text != null) && (text.length() > maxLength) ? text.substring(0, maxLength - 2) + ".." : text;
	}

	private static String escapeHtml(String text)
	{
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
}
