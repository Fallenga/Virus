package org.l2jmobius.gameserver.model.actor.instance;

import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventBuffManager;
import org.l2jmobius.gameserver.model.events.clankorean.ClanKoreanConfig;
import org.l2jmobius.gameserver.model.events.clankorean.ClanKoreanEvent;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ClanKorean extends Npc
{
	private static final int PAGE_SIZE = 10;
	
	public ClanKorean(NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		showMain(player);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String action = st.hasMoreTokens() ? st.nextToken() : "";
		if (action.equals("ck_list"))
		{
			showSelection(player, st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0);
		}
		else if (action.equals("ck_toggle"))
		{
			final int objectId = Integer.parseInt(st.nextToken());
			final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			player.sendMessage("Clan Korean: " + ClanKoreanEvent.getInstance().toggleMember(player, objectId));
			showSelection(player, page);
		}
		else if (action.equals("ck_buffs"))
		{
			EventBuffManager.showWindow(player, EventBuffManager.KOREAN, getObjectId());
		}
		else if (action.equals("eventbuff"))
		{
			if (st.hasMoreTokens() && EventBuffManager.KOREAN.equalsIgnoreCase(st.nextToken()) && st.hasMoreTokens())
			{
				EventBuffManager.select(player, EventBuffManager.KOREAN, st.nextToken());
			}
			showMain(player);
		}
		else if (action.equals("eventbuff_scheme"))
		{
			final String event = st.hasMoreTokens() ? st.nextToken() : "";
			final int index = command.indexOf(event) + event.length();
			final String scheme = command.substring(index).trim();
			if (EventBuffManager.KOREAN.equalsIgnoreCase(event) && !scheme.isEmpty())
			{
				EventBuffManager.select(player, EventBuffManager.KOREAN, scheme);
			}
			showMain(player);
		}
		else if (action.equals("ck_register"))
		{
			player.sendMessage("Clan Korean: " + ClanKoreanEvent.getInstance().registerTeam(player));
			showMain(player);
		}
		else if (action.equals("ck_unregister"))
		{
			player.sendMessage("Clan Korean: " + ClanKoreanEvent.getInstance().unregisterTeam(player));
			showMain(player);
		}
		else if (action.equals("ck_main"))
		{
			showMain(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void showMain(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		final ClanKoreanEvent event = ClanKoreanEvent.getInstance();
		final StringBuilder html = new StringBuilder(1200);
		html.append("<html><body><center>");
		html.append("<table width=292 height=358 background=\"L2UI_CT1.SlideShow_DF_Credit_05\"><tr><td valign=top align=center>");
		html.append("<br><font name=hs12 color=LEVEL>CLAN KOREAN 5 VS 5</font><br1>");
		html.append("<img src=L2UI.SquareGray width=260 height=1><br>");
		html.append("Combates 1 vs 1 por turnos.<br1>El ganador permanece en la arena.<br><br>");
		html.append("Clan minimo: <font color=LEVEL>nivel ").append(ClanKoreanConfig.MINIMUM_CLAN_LEVEL).append("</font><br1>");
		html.append("Estado: <font color=00FF00>").append(event.getState()).append("</font><br1>");
		html.append("Arenas activas: <font color=LEVEL>").append(event.getActiveMatchCount()).append("/").append(ClanKoreanConfig.ARENA_LOCATIONS.length).append("</font><br1>");
		html.append("Equipos esperando: <font color=LEVEL>").append(event.getWaitingTeamCount()).append("</font><br1>");
		html.append("Clanes registrados: <font color=LEVEL>").append(event.getRegisteredTeamCount()).append("</font><br><br>");
		if (event.isRegistrationOpen())
		{
			html.append("<button value=\"Configurar buffs\" action=\"bypass -h npc_%objectId%_ck_buffs\" width=180 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
			html.append("<button value=\"Seleccionar equipo\" action=\"bypass -h npc_%objectId%_ck_list 0\" width=180 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
			html.append("<button value=\"Cancelar registro\" action=\"bypass -h npc_%objectId%_ck_unregister\" width=180 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			html.append("El registro se encuentra cerrado.");
		}
		html.append("</td></tr></table></center></body></html>");
		sendHtml(player, html.toString());
	}
	
	private void showSelection(Player player, int requestedPage)
	{
		final ClanKoreanEvent event = ClanKoreanEvent.getInstance();
		final List<Player> members = event.getOnlineClanMembers(player);
		final List<Integer> selected = event.getSelection(player);
		final int maxPage = Math.max(0, (members.size() - 1) / PAGE_SIZE);
		final int page = Math.max(0, Math.min(requestedPage, maxPage));
		final int from = page * PAGE_SIZE;
		final int to = Math.min(from + PAGE_SIZE, members.size());
		final StringBuilder html = new StringBuilder(3500);
		html.append("<html><body><center><font name=hs12 color=LEVEL>FORMAR EQUIPO</font><br>");
		html.append("Seleccionados: <font color=00FF00>").append(selected.size()).append("/5</font><br1>");
		html.append("El lider siempre ocupa el primer lugar.<br><table width=290>");
		for (int i = from; i < to; i++)
		{
			final Player member = members.get(i);
			final boolean included = selected.contains(member.getObjectId());
			html.append("<tr><td width=145>").append(i + 1).append(". ").append(member.getName()).append("</td><td width=90>");
			if (member.getObjectId() == player.getObjectId())
			{
				html.append("<font color=LEVEL>Lider</font>");
			}
			else
			{
				html.append("<button value=\"").append(included ? "Quitar" : "Seleccionar").append("\" action=\"bypass -h npc_%objectId%_ck_toggle ").append(member.getObjectId()).append(" ").append(page).append("\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
			}
			html.append("</td></tr>");
		}
		html.append("</table><br><table><tr>");
		if (page > 0)
		{
			html.append("<td><button value=\"Anterior\" action=\"bypass -h npc_%objectId%_ck_list ").append(page - 1).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		}
		if (page < maxPage)
		{
			html.append("<td><button value=\"Siguiente\" action=\"bypass -h npc_%objectId%_ck_list ").append(page + 1).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		}
		html.append("</tr></table><br>");
		if (selected.size() == ClanKoreanEvent.TEAM_SIZE)
		{
			html.append("<button value=\"Registrar equipo\" action=\"bypass -h npc_%objectId%_ck_register\" width=160 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
		}
		html.append("<button value=\"Volver\" action=\"bypass -h npc_%objectId%_ck_main\" width=100 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		html.append("</center></body></html>");
		sendHtml(player, html.toString());
	}
	
	private void sendHtml(Player player, String content)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(content);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
}
