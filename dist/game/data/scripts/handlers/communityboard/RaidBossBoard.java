package handlers.communityboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.instancemanager.DBSpawnManager;
import org.l2jmobius.gameserver.instancemanager.GrandBossManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

public class RaidBossBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS = { "_bbsraidinfo" };
	private static final String HTML_PATH = "data/html/CommunityBoard/Custom/raidboss/main.html";
	private static final int PAGE_SIZE = 11;
	private static final int[] GRAND_BOSSES = { 29001, 29006, 29014, 29020, 29022, 29068, 29028 };

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		final String[] parts = command.split(";");
		final String type = (parts.length > 1) && "grand".equalsIgnoreCase(parts[1]) ? "grand" : "raid";
		int page = 1;
		if (parts.length > 2)
		{
			try { page = Math.max(1, Integer.parseInt(parts[2])); }
			catch (NumberFormatException ignored) { page = 1; }
		}

		final List<BossInfo> bosses = "grand".equals(type) ? getGrandBosses() : getRaidBosses();
		final int pages = Math.max(1, (bosses.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		page = Math.min(page, pages);
		final int from = (page - 1) * PAGE_SIZE;
		final int to = Math.min(from + PAGE_SIZE, bosses.size());

		final StringBuilder rows = new StringBuilder();
		for (int i = from; i < to; i++)
		{
			final BossInfo boss = bosses.get(i);
			final boolean alive = boss.respawnTime <= System.currentTimeMillis();
			final String background = (i % 2 == 0) ? "0E0E0E" : "181818";
			rows.append("<table width=500 height=25 bgcolor=").append(background).append("><tr>");
			rows.append("<td width=45 align=center><font color=FFB83D>").append(boss.level).append("</font></td>");
			rows.append("<td width=225><font color=D8C7A1>").append(escape(boss.name)).append("</font><br1><font color=555555>ID ").append(boss.id).append("</font></td>");
			rows.append("<td width=75 align=center><font color=").append(alive ? "5FD067>ALIVE" : "E45B5B>DEAD").append("</font></td>");
			rows.append("<td width=155 align=center><font color=A9A8A4>").append(alive ? "Disponible" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(boss.respawnTime))).append("</font></td>");
			rows.append("</tr></table>");
		}
		if (bosses.isEmpty()) rows.append("<table width=500 height=80><tr><td align=center><font color=777777>No hay Raid Boss registrados.</font></td></tr></table>");

		String html = HtmCache.getInstance().getHtm(player, HTML_PATH);
		if (html == null) return false;
		html = html.replace("%title%", "grand".equals(type) ? "GRAND BOSS" : "RAID BOSS");
		html = html.replace("%rows%", rows.toString());
		html = html.replace("%page%", Integer.toString(page));
		html = html.replace("%pages%", Integer.toString(pages));
		html = html.replace("%previous%", page > 1 ? "<button value=\"ANTERIOR\" action=\"bypass _bbsraidinfo;" + type + ";" + (page - 1) + "\" width=85 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
		html = html.replace("%next%", page < pages ? "<button value=\"SIGUIENTE\" action=\"bypass _bbsraidinfo;" + type + ";" + (page + 1) + "\" width=85 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}

	private List<BossInfo> getRaidBosses()
	{
		final List<BossInfo> result = new ArrayList<>();
		for (Entry<Integer, StatSet> entry : DBSpawnManager.getInstance().getStoredInfo().entrySet())
		{
			final NpcTemplate npc = NpcData.getInstance().getTemplate(entry.getKey());
			if ((npc != null) && npc.isType("RaidBoss"))
			{
				result.add(new BossInfo(npc.getId(), npc.getName(), npc.getLevel(), entry.getValue().getLong("respawnTime", 0L)));
			}
		}
		result.sort(Comparator.comparingInt((BossInfo b) -> b.level).reversed().thenComparing(b -> b.name));
		return result;
	}

	private List<BossInfo> getGrandBosses()
	{
		final List<BossInfo> result = new ArrayList<>();
		for (int id : GRAND_BOSSES)
		{
			final NpcTemplate npc = NpcData.getInstance().getTemplate(id);
			final StatSet info = GrandBossManager.getInstance().getStatSet(id);
			if ((npc != null) && (info != null)) result.add(new BossInfo(id, npc.getName(), npc.getLevel(), info.getLong("respawn_time", 0L)));
		}
		result.sort(Comparator.comparingInt((BossInfo b) -> b.level).reversed());
		return result;
	}

	private static String escape(String text) { return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
	@Override public String[] getCommunityBoardCommands() { return COMMANDS; }

	private static class BossInfo
	{
		final int id;
		final String name;
		final int level;
		final long respawnTime;
		BossInfo(int id, String name, int level, long respawnTime) { this.id = id; this.name = name; this.level = level; this.respawnTime = respawnTime; }
	}
}
