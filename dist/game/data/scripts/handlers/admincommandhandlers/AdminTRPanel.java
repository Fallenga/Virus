/*
 * Coliseum Tournament SQL Admin Panel.
 * HTML externo: data/html/mods/ColiseumTournament/main.html
 */
package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

import custom.events.ColiseumTournament.ColiseumTournament;

public class AdminTRPanel implements IAdminCommandHandler
{
	private static final String HTML_PATH = "data/html/mods/ColiseumTournament/main.html";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_tr_panel"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		final ColiseumTournament event = ColiseumTournament.getInstance();
		if (event == null)
		{
			activeChar.sendMessage("Coliseum: evento no cargado.");
			return false;
		}
		
		String message = "";
		try
		{
			final String[] args = command.split(" ", 4);
			final String action = args.length > 1 ? args[1].toLowerCase() : "panel";
			
			switch (action)
			{
				case "startreg":
				{
					final int minutes = parseInt(args, 2, parseInt(event.getConfigValue("ColiseumRegistrationMinutes"), 10));
					event.adminStartRegistration(activeChar, Math.max(1, minutes));
					message = "Registro abierto por " + Math.max(1, minutes) + " minuto(s).";
					break;
				}
				case "forcestart":
				{
					event.adminForceStart(activeChar);
					message = "Forzando inicio.";
					break;
				}
				case "cancel":
				{
					event.adminCancel(activeChar, true);
					message = "Evento cancelado y registro reabierto.";
					break;
				}
				case "close":
				{
					event.adminClose(activeChar);
					message = "Evento cerrado.";
					break;
				}
				case "resetweek":
				{
					event.adminResetWeek(activeChar);
					message = "Ranking semanal reiniciado.";
					break;
				}
				case "rewardchamp":
				{
					event.adminRewardChampion(activeChar);
					message = "Reward de campeón ejecutado.";
					break;
				}
				case "reloadcfg":
				{
					event.adminReload(activeChar);
					message = "Config SQL recargada.";
					break;
				}
				case "scheduleon":
				{
					event.adminSetConfig(activeChar, "ColiseumAutoScheduleEnabled", "true");
					message = "Horarios automáticos ACTIVADOS.";
					break;
				}
				case "scheduleoff":
				{
					event.adminSetConfig(activeChar, "ColiseumAutoScheduleEnabled", "false");
					message = "Horarios automáticos DESACTIVADOS.";
					break;
				}
				case "setnpchere":
				{
					event.adminSetNpcHere(activeChar);
					message = "Posición del NPC guardada usando tu ubicación actual.";
					break;
				}
				case "spawnnpc":
				{
					event.adminSpawnNpc(activeChar);
					message = "NPC spawneado desde SQL.";
					break;
				}
				case "despawnnpc":
				{
					event.adminDespawnNpc(activeChar);
					message = "NPC removido.";
					break;
				}
				case "set":
				{
					if (args.length < 4)
					{
						message = "Uso: //tr_panel set ConfigName valor";
						break;
					}
					final String key = args[2];
					final String value = args[3];
					if (event.adminSetConfig(activeChar, key, value))
					{
						message = "Guardado en SQL: " + key + " = " + value;
					}
					else
					{
						message = "No se pudo guardar: " + key;
					}
					break;
				}
				case "savebasic":
				{
					saveBasic(command, activeChar, event);
					message = "Config principal guardada en SQL.";
					break;
				}
				case "panel":
				default:
				{
					break;
				}
			}
		}
		catch (Exception e)
		{
			message = "Error: revisa números/campos. " + e.getMessage();
		}
		
		showPanel(activeChar, event, message);
		return true;
	}
	
	private static void saveBasic(String command, Player activeChar, ColiseumTournament event)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // admin_tr_panel
		st.nextToken(); // savebasic
		
		if (st.countTokens() < 10)
		{
			activeChar.sendMessage("Coliseum: completa todos los campos principales.");
			return;
		}
		
		event.adminSetConfig(activeChar, "ColiseumRegistrationMinutes", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumMinPlayers", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumFightSeconds", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumRoundDelaySeconds", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumCountdownSeconds", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumRewardItemId", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumRewardItemCount", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumTeamRewardEnabled", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumTeamRewardItemId", st.nextToken());
		event.adminSetConfig(activeChar, "ColiseumTeamRewardItemCount", st.nextToken());
	}
	
	public static void showPanel(Player activeChar, ColiseumTournament event, String message)
	{
		String html = HtmCache.getInstance().getHtm(activeChar, HTML_PATH);
		if (html == null)
		{
			activeChar.sendMessage("No existe el HTML: " + HTML_PATH);
			return;
		}
		
		html = html.replace("%message%", message == null ? "" : message);
		html = html.replace("%state%", event.getStateHtml());
		html = html.replace("%time%", event.getRegistrationTimeHtml());
		html = html.replace("%registered%", String.valueOf(event.getRegisteredCount()));
		html = html.replace("%order%", String.valueOf(event.getAliveOrderCount()));
		html = html.replace("%chaos%", String.valueOf(event.getAliveChaosCount()));
		html = html.replace("%matches%", String.valueOf(event.getTotalMatches()));
		html = html.replace("%week_start%", String.valueOf(event.getWeekStart()));
		html = html.replace("%champion%", event.getChampionInfoHtml());
		html = html.replace("%schedule%", event.getScheduleHtml());
		
		replaceConfig(html, event); // no-op marker for readability
		html = applyConfig(html, event);
		html = html.replace("%config_rows_basic%", rows(event, new String[][]
		{
			{"Registro min", "ColiseumRegistrationMinutes", "regmin"},
			{"Min players", "ColiseumMinPlayers", "minplayers"},
			{"Max players", "ColiseumMaxPlayers", "maxplayers"},
			{"Protección IP", "ColiseumIpProtectionEnabled", "ipprotection"},
			{"Horario auto", "ColiseumAutoScheduleEnabled", "autoschedule"},
			{"Horas auto", "ColiseumScheduleTimes", "scheduletimes"},
			{"Fight sec", "ColiseumFightSeconds", "fightsec"},
			{"Round delay", "ColiseumRoundDelaySeconds", "rounddelay"},
			{"Countdown", "ColiseumCountdownSeconds", "countdown"},
			{"Reward ID", "ColiseumRewardItemId", "rewardid"},
			{"Reward count", "ColiseumRewardItemCount", "rewardcount"},
			{"Team reward", "ColiseumTeamRewardEnabled", "teamreward"},
			{"Team item", "ColiseumTeamRewardItemId", "teamitem"},
			{"Team count", "ColiseumTeamRewardItemCount", "teamcount"}
		}));
		html = html.replace("%config_rows_general%", rows(event, new String[][]
		{
			{"Activado", "ColiseumEnabled", "enabled"},
			{"Sudden tick ms", "ColiseumSuddenDeathTickMs", "suddentick"},
			{"Sudden drain %", "ColiseumSuddenDeathDrainPercent", "suddendrain"},
			{"Points win", "ColiseumPointsWin", "ptswin"},
			{"Points loss", "ColiseumPointsLoss", "ptsloss"},
			{"Points champion", "ColiseumPointsChampion", "ptschamp"}
		}));
		html = html.replace("%config_rows_champion%", rows(event, new String[][]
		{
			{"Champion title", "ColiseumChampionTitle", "champtitle"},
			{"Name color", "ColiseumChampionNameColor", "champcolor"}
		}));
		html = html.replace("%config_rows_buffs%", rows(event, new String[][]
		{
			{"Use buffs", "ColiseumUseStandardBuffs", "usebuffs"},
			{"Mage buffs", "ColiseumMageBuffs", "magebuffs"},
			{"Fighter buffs", "ColiseumFighterBuffs", "fighterbuffs"}
		}));
		html = html.replace("%config_rows_locations%", rows(event, new String[][]
		{
			{"Entry", "ColiseumEntry", "entry"},
			{"Asiento 1", "ColiseumSeatA", "seata"},
			{"Asiento 2", "ColiseumSeatB", "seatb"},
			{"Arena A", "ColiseumArenaA", "arenaa"},
			{"Arena B", "ColiseumArenaB", "arenab"},
			{"Wait winners", "ColiseumWaitWinners", "waitwin"},
			{"Wait losers", "ColiseumWaitLosers", "waitlose"},
			{"Exit", "ColiseumExit", "exitloc"},
			{"NPC ID", "ColiseumNpcId", "npcid"},
			{"NPC Spawn x,y,z,h", "ColiseumNpcSpawn", "npcspawn"}
		}));
		
		CommunityBoardHandler.separateAndSend(html, activeChar);
	}
	
	private static void replaceConfig(String html, ColiseumTournament event)
	{
		// helper marker only
	}
	
	private static String applyConfig(String html, ColiseumTournament event)
	{
		final String[][] keys =
		{
			{"%regmin%", "ColiseumRegistrationMinutes"},
			{"%minplayers%", "ColiseumMinPlayers"},
			{"%maxplayers%", "ColiseumMaxPlayers"},
			{"%ipprotection%", "ColiseumIpProtectionEnabled"},
			{"%autoschedule%", "ColiseumAutoScheduleEnabled"},
			{"%scheduletimes%", "ColiseumScheduleTimes"},
			{"%fightsec%", "ColiseumFightSeconds"},
			{"%rounddelay%", "ColiseumRoundDelaySeconds"},
			{"%countdown%", "ColiseumCountdownSeconds"},
			{"%seata%", "ColiseumSeatA"},
			{"%seatb%", "ColiseumSeatB"},
			{"%rewardid%", "ColiseumRewardItemId"},
			{"%rewardcount%", "ColiseumRewardItemCount"},
			{"%teamreward%", "ColiseumTeamRewardEnabled"},
			{"%teamitem%", "ColiseumTeamRewardItemId"},
			{"%teamcount%", "ColiseumTeamRewardItemCount"},
			{"%npcid%", "ColiseumNpcId"},
			{"%npcspawn%", "ColiseumNpcSpawn"}
		};
		for (String[] k : keys)
		{
			html = html.replace(k[0], event.getConfigValue(k[1]));
		}
		return html;
	}
	
	private static String rows(ColiseumTournament event, String[][] rows)
	{
		final StringBuilder sb = new StringBuilder();
		for (String[] r : rows)
		{
			sb.append(row(r[0], r[1], r[2], event));
		}
		return sb.toString();
	}
	
	private static String row(String label, String key, String var, ColiseumTournament event)
	{
		final String value = event.getConfigValue(key);
		return "<table width=730 bgcolor=111111><tr>" +
			"<td width=180><font color=A9C7FF>" + label + "</font></td>" +
			"<td width=230><font color=LEVEL>" + value + "</font></td>" +
			"<td width=180><edit var=\"" + var + "\" width=170 height=18></td>" +
			"<td width=110><button value=\"Guardar\" action=\"bypass -h admin_tr_panel set " + key + " $" + var + "\" width=90 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>" +
			"</tr></table><br1>";
	}
	
	private static int parseInt(String[] args, int index, int def)
	{
		if (args.length <= index)
		{
			return def;
		}
		return parseInt(args[index], def);
	}
	
	private static int parseInt(String value, int def)
	{
		try
		{
			return Integer.parseInt(value.replaceAll("[^0-9-]", "").trim());
		}
		catch (Exception e)
		{
			return def;
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
