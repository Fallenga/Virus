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
package custom.events.ColiseumTournament;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.instancemanager.AntiFeedManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Event;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

import handlers.admincommandhandlers.AdminTRPanel;

/**
 * ColiseumTournament (Order vs Chaos) - aCis 409 -
 * @author Lucasdesigner
 */
public class ColiseumTournament extends Event
{
	private static final Logger LOGGER = Logger.getLogger(ColiseumTournament.class.getName());
	private static final int MANAGER = 1002002;
	
	private enum State
	{
		INACTIVE,
		REGISTRATION,
		PREPARE,
		FIGHTING,
		FINISH
	}
	
	private enum Side
	{
		ORDER,
		CHAOS
	}
	
	private static final class Loc
	{
		final int x, y, z;
		
		Loc(int x, int y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	// -------------------------
	// Config SQL
	// -------------------------
	private static final class Cfg
	{
		static boolean ENABLED;
		
		static int REG_MIN;
		static int MIN_PLAYERS;
		static int MAX_PLAYERS;
		static boolean IP_PROTECTION;
		static boolean AUTO_SCHEDULE_ENABLED;
		static String SCHEDULE_TIMES;
		
		static int FIGHT_SECONDS;
		static int ROUND_DELAY;
		static int COUNTDOWN;
		
		static int SUDDEN_TICK_MS;
		static int SUDDEN_DRAIN_PCT;
		
		static int REWARD_ITEM;
		static long REWARD_COUNT;
		
		static boolean TEAM_REWARD;
		static int TEAM_ITEM;
		static long TEAM_COUNT;
		
		static boolean STD_BUFFS;
		static final List<int[]> MAGE_BUFFS = new ArrayList<>();
		static final List<int[]> FIGHTER_BUFFS = new ArrayList<>();
		
		static String CHAMPION_TITLE;
		static String CHAMPION_NAME_COLOR_HEX;
		
		static int PTS_WIN;
		static int PTS_LOSS;
		static int PTS_CHAMP;
		
		static Loc ENTRY;
		static Loc SEAT_A;
		static Loc SEAT_B;
		static Loc ARENA_A;
		static Loc ARENA_B;
		static Loc WAIT_WIN;
		static Loc WAIT_LOSE;
		static Loc EXIT;
		static int NPC_ID;
		static Loc NPC_SPAWN;
		static int NPC_HEADING;
		
		private static final String SQL_CREATE_CONFIG = "CREATE TABLE IF NOT EXISTS coliseum_config (name varchar(64) NOT NULL, value varchar(255) NOT NULL, PRIMARY KEY (name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
		private static final String SQL_INSERT_DEFAULT = "INSERT IGNORE INTO coliseum_config (name, value) VALUES (?, ?)";
		private static final String SQL_SET_CONFIG = "REPLACE INTO coliseum_config (name, value) VALUES (?, ?)";
		private static final String SQL_LOAD_CONFIG = "SELECT name, value FROM coliseum_config";
		
		static void load()
		{
			ensureTableAndDefaults();
			final Map<String, String> cfg = new HashMap<>();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(SQL_LOAD_CONFIG);
				ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					cfg.put(rs.getString("name"), rs.getString("value"));
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[Coliseum] Error cargando configuración SQL: " + e.getMessage(), e);
				ENABLED = false;
				return;
			}
			
			ENABLED = bool(cfg, "ColiseumEnabled", true);
			REG_MIN = i(cfg, "ColiseumRegistrationMinutes", 10);
			MIN_PLAYERS = i(cfg, "ColiseumMinPlayers", 2);
			MAX_PLAYERS = i(cfg, "ColiseumMaxPlayers", 100);
			IP_PROTECTION = bool(cfg, "ColiseumIpProtectionEnabled", false);
			AUTO_SCHEDULE_ENABLED = bool(cfg, "ColiseumAutoScheduleEnabled", false);
			SCHEDULE_TIMES = get(cfg, "ColiseumScheduleTimes", "12:00;18:00;22:00");
			FIGHT_SECONDS = i(cfg, "ColiseumFightSeconds", 180);
			ROUND_DELAY = i(cfg, "ColiseumRoundDelaySeconds", 8);
			COUNTDOWN = i(cfg, "ColiseumCountdownSeconds", 10);
			SUDDEN_TICK_MS = i(cfg, "ColiseumSuddenDeathTickMs", 2000);
			SUDDEN_DRAIN_PCT = i(cfg, "ColiseumSuddenDeathDrainPercent", 5);
			REWARD_ITEM = i(cfg, "ColiseumRewardItemId", 57);
			REWARD_COUNT = l(cfg, "ColiseumRewardItemCount", 500000);
			TEAM_REWARD = bool(cfg, "ColiseumTeamRewardEnabled", false);
			TEAM_ITEM = i(cfg, "ColiseumTeamRewardItemId", 57);
			TEAM_COUNT = l(cfg, "ColiseumTeamRewardItemCount", 100000);
			STD_BUFFS = bool(cfg, "ColiseumUseStandardBuffs", true);
			parseBuffs(get(cfg, "ColiseumMageBuffs", ""), MAGE_BUFFS);
			parseBuffs(get(cfg, "ColiseumFighterBuffs", ""), FIGHTER_BUFFS);
			CHAMPION_TITLE = get(cfg, "ColiseumChampionTitle", "Coliseum Champion");
			CHAMPION_NAME_COLOR_HEX = get(cfg, "ColiseumChampionNameColor", "").trim();
			PTS_WIN = i(cfg, "ColiseumPointsWin", 1);
			PTS_LOSS = i(cfg, "ColiseumPointsLoss", 0);
			PTS_CHAMP = i(cfg, "ColiseumPointsChampion", 10);
			ENTRY = loc(get(cfg, "ColiseumEntry", "82698,148638,-3473"));
			SEAT_A = loc(get(cfg, "ColiseumSeatA", "149200,46600,-3413"));
			SEAT_B = loc(get(cfg, "ColiseumSeatB", "149800,46600,-3413"));
			ARENA_A = loc(get(cfg, "ColiseumArenaA", "149506,46728,-3413"));
			ARENA_B = loc(get(cfg, "ColiseumArenaB", "149550,47120,-3413"));
			WAIT_WIN = loc(get(cfg, "ColiseumWaitWinners", "149200,46800,-3413"));
			WAIT_LOSE = loc(get(cfg, "ColiseumWaitLosers", "149800,46800,-3413"));
			EXIT = loc(get(cfg, "ColiseumExit", "82698,148638,-3473"));
			NPC_ID = i(cfg, "ColiseumNpcId", 1002002);
			final String npcSpawn = get(cfg, "ColiseumNpcSpawn", "82698,148638,-3473,0");
			NPC_SPAWN = loc(npcSpawn);
			NPC_HEADING = heading(npcSpawn);
			LOGGER.info("[Coliseum] Configuración cargada desde SQL coliseum_config.");
		}
		
		private static void ensureTableAndDefaults()
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(SQL_CREATE_CONFIG))
			{
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[Coliseum] No se pudo crear coliseum_config: " + e.getMessage(), e);
				return;
			}
			
			defaultValue("ColiseumEnabled", "true");
			defaultValue("ColiseumRegistrationMinutes", "10");
			defaultValue("ColiseumMinPlayers", "2");
			defaultValue("ColiseumMaxPlayers", "100");
			defaultValue("ColiseumIpProtectionEnabled", "false");
			defaultValue("ColiseumAutoScheduleEnabled", "false");
			defaultValue("ColiseumScheduleTimes", "12:00;18:00;22:00");
			defaultValue("ColiseumFightSeconds", "180");
			defaultValue("ColiseumRoundDelaySeconds", "8");
			defaultValue("ColiseumCountdownSeconds", "10");
			defaultValue("ColiseumSuddenDeathTickMs", "2000");
			defaultValue("ColiseumSuddenDeathDrainPercent", "5");
			defaultValue("ColiseumRewardItemId", "57");
			defaultValue("ColiseumRewardItemCount", "500000");
			defaultValue("ColiseumTeamRewardEnabled", "false");
			defaultValue("ColiseumTeamRewardItemId", "57");
			defaultValue("ColiseumTeamRewardItemCount", "100000");
			defaultValue("ColiseumUseStandardBuffs", "true");
			defaultValue("ColiseumMageBuffs", "1204,2;1085,3;1059,3");
			defaultValue("ColiseumFighterBuffs", "1204,2;1068,3;1040,3");
			defaultValue("ColiseumChampionTitle", "Coliseum Champion");
			defaultValue("ColiseumChampionNameColor", "FF0000");
			defaultValue("ColiseumPointsWin", "1");
			defaultValue("ColiseumPointsLoss", "0");
			defaultValue("ColiseumPointsChampion", "10");
			defaultValue("ColiseumEntry", "82698,148638,-3473");
			defaultValue("ColiseumSeatA", "149200,46600,-3413");
			defaultValue("ColiseumSeatB", "149800,46600,-3413");
			defaultValue("ColiseumArenaA", "149506,46728,-3413");
			defaultValue("ColiseumArenaB", "149550,47120,-3413");
			defaultValue("ColiseumWaitWinners", "149200,46800,-3413");
			defaultValue("ColiseumWaitLosers", "149800,46800,-3413");
			defaultValue("ColiseumExit", "82698,148638,-3473");
			defaultValue("ColiseumNpcId", "1002002");
			defaultValue("ColiseumNpcSpawn", "82698,148638,-3473,0");
		}
		
		private static void defaultValue(String name, String value)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(SQL_INSERT_DEFAULT))
			{
				ps.setString(1, name);
				ps.setString(2, value);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[Coliseum] Default config failed " + name + ": " + e.getMessage(), e);
			}
		}
		
		static void set(String name, String value)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(SQL_SET_CONFIG))
			{
				ps.setString(1, name);
				ps.setString(2, value == null ? "" : value.trim());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[Coliseum] No se pudo guardar config " + name + ": " + e.getMessage(), e);
			}
		}
		
		static String getValue(String name)
		{
			ensureTableAndDefaults();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT value FROM coliseum_config WHERE name=?"))
			{
				ps.setString(1, name);
				try (ResultSet rs = ps.executeQuery())
				{
					if (rs.next())
					{
						return rs.getString("value");
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[Coliseum] No se pudo leer config " + name + ": " + e.getMessage(), e);
			}
			return "";
		}
		
		private static String get(Map<String, String> cfg, String k, String d)
		{
			final String v = cfg.get(k);
			return (v == null) ? d : v.trim();
		}
		
		private static boolean bool(Map<String, String> cfg, String k, boolean d)
		{
			final String v = cfg.get(k);
			return (v == null) ? d : Boolean.parseBoolean(v.trim());
		}
		
		private static int i(Map<String, String> cfg, String k, int d)
		{
			final String v = cfg.get(k);
			if (v == null)
			{
				return d;
			}
			try
			{
				return Integer.parseInt(v.trim());
			}
			catch (Exception e)
			{
				return d;
			}
		}
		
		private static long l(Map<String, String> cfg, String k, long d)
		{
			final String v = cfg.get(k);
			if (v == null)
			{
				return d;
			}
			try
			{
				return Long.parseLong(v.trim());
			}
			catch (Exception e)
			{
				return d;
			}
		}
		
		private static Loc loc(String v)
		{
			final String[] s = v.split(",");
			try
			{
				return new Loc(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()), Integer.parseInt(s[2].trim()));
			}
			catch (Exception e)
			{
				return new Loc(0, 0, 0);
			}
		}
		
		private static int heading(String v)
		{
			final String[] s = v.split(",");
			if (s.length < 4)
			{
				return 0;
			}
			try
			{
				return Integer.parseInt(s[3].trim());
			}
			catch (Exception e)
			{
				return 0;
			}
		}
		
		private static void parseBuffs(String raw, List<int[]> out)
		{
			out.clear();
			if ((raw == null) || raw.trim().isEmpty())
			{
				return;
			}
			
			for (String part : raw.split(";"))
			{
				final String t = part.trim();
				if (t.isEmpty())
				{
					continue;
				}
				
				final String[] s = t.split(",");
				if (s.length != 2)
				{
					continue;
				}
				
				try
				{
					out.add(new int[]
					{
						Integer.parseInt(s[0].trim()),
						Integer.parseInt(s[1].trim())
					});
				}
				catch (Exception ignored)
				{
				}
			}
		}
	}
	
	// -------------------------
	// DB (rank semanal + campeão)
	// -------------------------
	private static final String SQL_WEEK_KEY = "SELECT week_start FROM coliseum_week_meta LIMIT 1";
	private static final String SQL_WEEK_SET = "REPLACE INTO coliseum_week_meta (id, week_start) VALUES (1, ?)";
	private static final String SQL_CLEAR_WEEK = "DELETE FROM coliseum_weekly";
	
	private static final String SQL_UPSERT_WEEKLY = "INSERT INTO coliseum_weekly (week_start, char_id, wins, losses, points, side) VALUES (?,?,?,?,?,?) " + "ON DUPLICATE KEY UPDATE wins=wins+VALUES(wins), losses=losses+VALUES(losses), points=points+VALUES(points), side=VALUES(side)";
	
	private static final String SQL_SET_CHAMP = "REPLACE INTO coliseum_champion (id, week_start, char_id, side) VALUES (1, ?, ?, ?)";
	
	private static final String SQL_GET_CHAMP = "SELECT week_start, char_id, side FROM coliseum_champion WHERE id=1";
	
	private static final String SQL_HISTORY = "INSERT INTO coliseum_history (week_start, champion_id, champion_side, total_players, total_matches, created_at) VALUES (?,?,?,?,?,?)";
	private static final String SQL_ADMIN_LOG = "INSERT INTO coliseum_admin_log (admin_id, admin_name, action, created_at) VALUES (?,?,?,?)";
	
	// -------------------------
	// Runtime
	// -------------------------
	private static ColiseumTournament INSTANCE;
	
	private volatile State _state = State.INACTIVE;
	private volatile long _regEndsAt = 0L;
	private volatile long _weekStart = 0L;
	
	private final Set<Integer> _registered = new HashSet<>();
	private final Map<Integer, Side> _side = new HashMap<>();
	private final Map<Integer, String> _oldTitle = new HashMap<>();
	
	private final List<Integer> _aliveOrder = new ArrayList<>();
	private final List<Integer> _aliveChaos = new ArrayList<>();
	
	private volatile Match _currentMatch;
	private volatile ScheduledFuture<?> _fightWatcher;
	private volatile ScheduledFuture<?> _suddenTask;
	private volatile ScheduledFuture<?> _registrationTask;
	private volatile ScheduledFuture<?> _autoScheduleTask;
	private volatile String _lastAutoScheduleKey = "";
	private volatile Npc _managerNpc;
	private volatile int _lastHtmlNpcObjectId = 0;
	private final Map<Integer, Integer> _lastPlayerHtmlNpcObjectId = new ConcurrentHashMap<>();
	
	private int _totalMatches = 0;
	private int _totalPlayers = 0;
	
	private static final class Match
	{
		final int aObjId;
		final int bObjId;
		final long startedAt;
		boolean finished = false;
		
		Match(int a, int b)
		{
			aObjId = a;
			bObjId = b;
			startedAt = System.currentTimeMillis();
		}
	}
	
	private void ensureDatabaseTables()
	{
		final String[] sqls =
		{
			"CREATE TABLE IF NOT EXISTS coliseum_week_meta (id int(11) NOT NULL, week_start bigint(20) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
			"CREATE TABLE IF NOT EXISTS coliseum_weekly (week_start bigint(20) NOT NULL, char_id int(11) NOT NULL, wins int(11) NOT NULL DEFAULT 0, losses int(11) NOT NULL DEFAULT 0, points int(11) NOT NULL DEFAULT 0, side varchar(10) NOT NULL, PRIMARY KEY (week_start,char_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
			"CREATE TABLE IF NOT EXISTS coliseum_champion (id int(11) NOT NULL, week_start bigint(20) NOT NULL, char_id int(11) NOT NULL, side varchar(10) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
			"CREATE TABLE IF NOT EXISTS coliseum_history (id int(11) NOT NULL AUTO_INCREMENT, week_start bigint(20) NOT NULL, champion_id int(11) NOT NULL, champion_side varchar(10) NOT NULL, total_players int(11) NOT NULL, total_matches int(11) NOT NULL, created_at bigint(20) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
			"CREATE TABLE IF NOT EXISTS coliseum_admin_log (id int(11) NOT NULL AUTO_INCREMENT, admin_id int(11) NOT NULL, admin_name varchar(45) NOT NULL, action varchar(255) NOT NULL, created_at bigint(20) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
		};
		
		for (String sql : sqls)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(sql))
			{
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[Coliseum] No se pudo crear tabla SQL: " + e.getMessage(), e);
			}
		}
	}
	
	// -------------------------
	// Script init
	// -------------------------
	public ColiseumTournament()
	{
		
		super();
		INSTANCE = this;
		
		ensureDatabaseTables();
		Cfg.load();
		AdminCommandHandler.getInstance().registerHandler(new AdminTRPanel());
		// Registrar el talk siempre que exista NPC_ID. Aunque el evento esté apagado
		// o sin inscripción abierta, el NPC debe responder con un HTML informativo.
		if (Cfg.NPC_ID > 0)
		{
			addTalkId(Cfg.NPC_ID);
			addFirstTalkId(Cfg.NPC_ID);
		}
		if (!Cfg.ENABLED)
		{
			LOGGER.info("[Coliseum] Loaded disabled by SQL config. Admin panel available.");
			return;
		}
		loadWeekStart();
		scheduleWeeklyReset();
		scheduleChampionOnlineApply();
		startAutoScheduleChecker();
		
		// No abrimos registro automático al cargar el script.
		// El GM lo abre desde el panel; si existe un NPC estático en XML, responderá aunque el evento esté cerrado.
		LOGGER.info("[Coliseum] Loaded. Event inactive. Open registration from admin panel.");
	}
	
	public static ColiseumTournament getInstance()
	{
		return INSTANCE;
	}
	
	// -------------------------
	// Auto schedule by SQL hours
	// -------------------------
	private void startAutoScheduleChecker()
	{
		if (_autoScheduleTask != null)
		{
			_autoScheduleTask.cancel(false);
			_autoScheduleTask = null;
		}
		try
		{
			Cfg.load();
			if (!Cfg.ENABLED || !Cfg.AUTO_SCHEDULE_ENABLED)
			{
				LOGGER.info("[Coliseum] Horario automático desactivado.");
				return;
			}
			final long delay = getDelayToNextScheduleMillis();
			if (delay < 0)
			{
				LOGGER.warning("[Coliseum] No hay horarios válidos en ColiseumScheduleTimes: " + Cfg.SCHEDULE_TIMES);
				return;
			}
			_autoScheduleTask = ThreadPool.schedule(this::runAutoScheduleTrigger, Math.max(1000L, delay));
			LOGGER.info("[Coliseum] Próximo inicio automático en " + (delay / 1000L) + " segundo(s). Horarios: " + Cfg.SCHEDULE_TIMES);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] Error programando horario automático: " + e.getMessage(), e);
		}
	}
	
	private long getDelayToNextScheduleMillis()
	{
		final String raw = Cfg.SCHEDULE_TIMES;
		if ((raw == null) || raw.trim().isEmpty())
		{
			return -1L;
		}
		final ZoneId zone = ZoneId.systemDefault();
		final java.time.LocalDateTime now = java.time.LocalDateTime.now(zone);
		java.time.LocalDateTime next = null;
		for (String part : raw.split("[;,]"))
		{
			final String t = part.trim();
			if (t.isEmpty())
			{
				continue;
			}
			try
			{
				final LocalTime scheduled = LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm")).withSecond(0).withNano(0);
				java.time.LocalDateTime candidate = now.toLocalDate().atTime(scheduled);
				if (!candidate.isAfter(now))
				{
					candidate = candidate.plusDays(1);
				}
				if ((next == null) || candidate.isBefore(next))
				{
					next = candidate;
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("[Coliseum] Hora inválida en ColiseumScheduleTimes: " + t + ". Usa formato HH:mm;HH:mm");
			}
		}
		return next == null ? -1L : java.time.Duration.between(now, next).toMillis();
	}
	
	private void runAutoScheduleTrigger()
	{
		try
		{
			Cfg.load();
			if (Cfg.ENABLED && Cfg.AUTO_SCHEDULE_ENABLED && (_state == State.INACTIVE))
			{
				adminStartRegistration(null, Math.max(1, Cfg.REG_MIN));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] Error iniciando horario automático: " + e.getMessage(), e);
		}
		finally
		{
			// Reprograma el siguiente horario del día o del día siguiente.
			startAutoScheduleChecker();
		}
	}
	
	public String getScheduleHtml()
	{
		return (Cfg.AUTO_SCHEDULE_ENABLED ? "<font color=00FF00>ON</font>" : "<font color=FF4040>OFF</font>") + " - " + escapeHtml(Cfg.SCHEDULE_TIMES);
	}
	
	// -------------------------
	// Admin panel
	// -------------------------
	public int getRegisteredCount()
	{
		return _registered.size();
	}
	
	public int getAliveOrderCount()
	{
		return _aliveOrder.size();
	}
	
	public int getAliveChaosCount()
	{
		return _aliveChaos.size();
	}
	
	public int getTotalMatches()
	{
		return _totalMatches;
	}
	
	public long getWeekStart()
	{
		return _weekStart;
	}
	
	public void showAdminPanel(Player player)
	{
		AdminTRPanel.showPanel(player, this, "");
	}
	
	public String getStateHtml()
	{
		switch (_state)
		{
			case REGISTRATION:
				return "<font color=00FF00>REGISTRATION</font>";
			case PREPARE:
				return "<font color=FFFF00>PREPARE</font>";
			case FIGHTING:
				return "<font color=LEVEL>FIGHTING</font>";
			case FINISH:
				return "<font color=FF9900>FINISH</font>";
			default:
				return "<font color=FF4040>INACTIVE</font>";
		}
	}
	
	public String getRegistrationTimeHtml()
	{
		if (_state != State.REGISTRATION)
		{
			return "-";
		}
		final long left = Math.max(0, _regEndsAt - System.currentTimeMillis()) / 1000L;
		return (left / 60) + "m " + (left % 60) + "s";
	}
	
	public String getChampionInfoHtml()
	{
		final Champion c = loadChampion();
		if (c == null)
		{
			return "Sin campeón";
		}
		final String name = CharInfoTable.getInstance().getNameById(c.charId);
		return ((name != null) ? name : ("CharId " + c.charId)) + " (" + c.side + ")";
	}
	
	private void openRegistration(int minutes)
	{
		cancelRegistrationTask();
		// El registro temporal queda en memoria (_registered).
		// Si el servidor tiene activado el control dualbox por IP, hay que registrar/limpiar
		// el evento antes de aceptar jugadores, igual que hace TvT.
		if (Cfg.IP_PROTECTION && (Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0))
		{
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.L2EVENT_ID);
			AntiFeedManager.getInstance().clear(AntiFeedManager.L2EVENT_ID);
		}
		_state = State.REGISTRATION;
		_regEndsAt = System.currentTimeMillis() + (minutes * 60_000L);
		scheduleRegistrationClose(minutes * 60_000L);
	}
	
	private void scheduleRegistrationClose(long delay)
	{
		cancelRegistrationTask();
		_registrationTask = ThreadPool.schedule(this::closeRegistrationAndStart, delay);
	}
	
	private void cancelRegistrationTask()
	{
		if (_registrationTask != null)
		{
			_registrationTask.cancel(false);
			_registrationTask = null;
		}
	}
	
	public void adminStartRegistration(Player admin, int minutes)
	{
		if ((_state == State.PREPARE) || (_state == State.FIGHTING))
		{
			if (admin != null)
			{
				admin.sendMessage("Coliseum: cancela el evento actual antes de abrir registro.");
			}
			return;
		}
		clearAntiFeedRegistrations();
		_registered.clear();
		_side.clear();
		_oldTitle.clear();
		_aliveOrder.clear();
		_aliveChaos.clear();
		_currentMatch = null;
		_totalPlayers = 0;
		_totalMatches = 0;
		spawnManagerNpc();
		openRegistration(minutes);
		Broadcast.toAllOnlinePlayers((admin == null ? "[Coliseo] Inscripciones abiertas por horario. Tiempo: " : "[Coliseo] Inscripciones abiertas por admin. Tiempo: ") + minutes + " minuto(s).");
		logAdmin(admin, admin == null ? ("auto_startreg " + minutes) : ("startreg " + minutes));
	}
	
	public void adminForceStart(Player admin)
	{
		if (_state != State.REGISTRATION)
		{
			admin.sendMessage("Coliseum: solo puedes forzar inicio durante registro.");
			return;
		}
		cancelRegistrationTask();
		logAdmin(admin, "forcestart");
		closeRegistrationAndStart();
	}
	
	public void adminCancel(Player admin, boolean reopen)
	{
		cancelRegistrationTask();
		_state = State.FINISH;
		stopFightWatcher();
		for (int objId : new ArrayList<>(_registered))
		{
			final Player p = World.getInstance().getPlayer(objId);
			if (p == null)
			{
				continue;
			}
			final String t = _oldTitle.get(objId);
			if (t != null)
			{
				p.setTitle(t);
			}
			p.broadcastUserInfo();
			p.abortAttack();
			p.abortCast();
			lock(p, false);
			p.standUp();
			if (Cfg.EXIT != null)
			{
				p.teleToLocation(Cfg.EXIT.x, Cfg.EXIT.y, Cfg.EXIT.z, 0);
			}
		}
		clearAntiFeedRegistrations();
		_registered.clear();
		_side.clear();
		_oldTitle.clear();
		_aliveOrder.clear();
		_aliveChaos.clear();
		_currentMatch = null;
		_totalPlayers = 0;
		_totalMatches = 0;
		if (reopen)
		{
			spawnManagerNpc();
			openRegistration(Cfg.REG_MIN);
			Broadcast.toAllOnlinePlayers("[Coliseo] Evento cancelado por admin. Inscripciones reabiertas.");
		}
		else
		{
			despawnManagerNpc();
		}
		logAdmin(admin, reopen ? "cancel_reopen" : "cancel");
	}
	
	public void adminClose(Player admin)
	{
		adminCancel(admin, false);
		_state = State.INACTIVE;
		Broadcast.toAllOnlinePlayers("[Coliseo] Evento cerrado por admin.");
		logAdmin(admin, "close");
	}
	
	public void adminResetWeek(Player admin)
	{
		_weekStart = System.currentTimeMillis();
		clearWeekly();
		saveWeekStart(_weekStart);
		logAdmin(admin, "resetweek");
		admin.sendMessage("Coliseum: ranking semanal reiniciado.");
	}
	
	public void adminRewardChampion(Player admin)
	{
		final Champion c = loadChampion();
		if (c == null)
		{
			admin.sendMessage("Coliseum: no hay campeón guardado en SQL.");
			return;
		}
		final Player champ = World.getInstance().getPlayer(c.charId);
		if (champ == null)
		{
			admin.sendMessage("Coliseum: el campeón no está online. No se entregó reward.");
			return;
		}
		champ.addItem("ColiseumAdminReward", Cfg.REWARD_ITEM, (int) Cfg.REWARD_COUNT, champ, true);
		applyChampionAura(champ);
		logAdmin(admin, "rewardchamp " + c.charId);
		admin.sendMessage("Coliseum: reward entregado a " + champ.getName() + ".");
	}
	
	public void adminReload(Player admin)
	{
		Cfg.load();
		startAutoScheduleChecker();
		if (_state == State.REGISTRATION)
		{
			despawnManagerNpc();
			spawnManagerNpc();
		}
		logAdmin(admin, "reloadcfg");
		admin.sendMessage("Coliseum: configuración recargada desde SQL.");
	}
	
	public boolean adminSetConfig(Player admin, String key, String value)
	{
		if ((value == null) || value.trim().isEmpty() || value.trim().startsWith("$"))
		{
			admin.sendMessage("Coliseum: no se guardó " + key + " porque el campo está vacío.");
			return false;
		}
		if (!isAllowedConfigKey(key))
		{
			admin.sendMessage("Coliseum: config inválida: " + key);
			return false;
		}
		Cfg.set(key, value);
		Cfg.load();
		if ("ColiseumEnabled".equals(key) || "ColiseumAutoScheduleEnabled".equals(key) || "ColiseumScheduleTimes".equals(key))
		{
			startAutoScheduleChecker();
		}
		logAdmin(admin, "setcfg " + key + "=" + value);
		admin.sendMessage("Coliseum: guardado en SQL " + key + " = " + value);
		return true;
	}
	
	public String getConfigValue(String key)
	{
		return escapeHtml(Cfg.getValue(key));
	}
	
	private boolean isAllowedConfigKey(String key)
	{
		return "ColiseumEnabled".equals(key) || "ColiseumRegistrationMinutes".equals(key) || "ColiseumMinPlayers".equals(key) || "ColiseumMaxPlayers".equals(key) || "ColiseumIpProtectionEnabled".equals(key) || "ColiseumAutoScheduleEnabled".equals(key) || "ColiseumScheduleTimes".equals(key) || "ColiseumFightSeconds".equals(key) || "ColiseumRoundDelaySeconds".equals(key) || "ColiseumCountdownSeconds".equals(key) || "ColiseumSuddenDeathTickMs".equals(key) || "ColiseumSuddenDeathDrainPercent".equals(key) || "ColiseumRewardItemId".equals(key) || "ColiseumRewardItemCount".equals(key) || "ColiseumTeamRewardEnabled".equals(key) || "ColiseumTeamRewardItemId".equals(key) || "ColiseumTeamRewardItemCount".equals(key) || "ColiseumUseStandardBuffs".equals(key) || "ColiseumMageBuffs".equals(key) || "ColiseumFighterBuffs".equals(key) || "ColiseumChampionTitle".equals(key) || "ColiseumChampionNameColor".equals(key) || "ColiseumPointsWin".equals(key) || "ColiseumPointsLoss".equals(key) || "ColiseumPointsChampion".equals(key) || "ColiseumEntry".equals(key) || "ColiseumSeatA".equals(key) || "ColiseumSeatB".equals(key) || "ColiseumArenaA".equals(key) || "ColiseumArenaB".equals(key) || "ColiseumWaitWinners".equals(key) || "ColiseumWaitLosers".equals(key) || "ColiseumExit".equals(key) || "ColiseumNpcId".equals(key) || "ColiseumNpcSpawn".equals(key);
	}
	
	private String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
	
	public void adminSetNpcHere(Player admin)
	{
		final String value = admin.getX() + "," + admin.getY() + "," + admin.getZ() + "," + admin.getHeading();
		Cfg.set("ColiseumNpcSpawn", value);
		Cfg.load();
		spawnManagerNpc();
		logAdmin(admin, "set_npc_spawn " + value);
		admin.sendMessage("Coliseum: posición del NPC guardada en SQL: " + value);
	}
	
	public void adminSpawnNpc(Player admin)
	{
		spawnManagerNpc();
		logAdmin(admin, "spawn_npc");
		admin.sendMessage("Coliseum: NPC spawneado desde la posición SQL.");
	}
	
	public void adminDespawnNpc(Player admin)
	{
		despawnManagerNpc();
		logAdmin(admin, "despawn_npc");
		admin.sendMessage("Coliseum: NPC removido.");
	}
	
	private void spawnManagerNpc()
	{
		if (_managerNpc != null)
		{
			return;
		}
		if ((Cfg.NPC_ID <= 0) || (Cfg.NPC_SPAWN == null))
		{
			return;
		}
		try
		{
			_managerNpc = addSpawn(Cfg.NPC_ID, Cfg.NPC_SPAWN.x, Cfg.NPC_SPAWN.y, Cfg.NPC_SPAWN.z, Cfg.NPC_HEADING, false, 0);
			_lastHtmlNpcObjectId = _managerNpc.getObjectId();
			LOGGER.info("[Coliseum] NPC manager spawned id " + Cfg.NPC_ID + " at " + Cfg.NPC_SPAWN.x + "," + Cfg.NPC_SPAWN.y + "," + Cfg.NPC_SPAWN.z + "," + Cfg.NPC_HEADING);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] No se pudo spawn NPC manager: " + e.getMessage(), e);
		}
	}
	
	private void despawnManagerNpc()
	{
		if (_managerNpc != null)
		{
			try
			{
				_managerNpc.deleteMe();
			}
			catch (Exception ignored)
			{
			}
			_managerNpc = null;
			_lastHtmlNpcObjectId = 0;
		}
	}
	
	private void clearAntiFeedRegistrations()
	{
		if (!Cfg.IP_PROTECTION || (Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP <= 0))
		{
			return;
		}
		for (int objId : new ArrayList<>(_registered))
		{
			final Player p = World.getInstance().getPlayer(objId);
			if (p != null)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, p);
			}
		}
	}
	
	public void logAdmin(Player admin, String action)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement create = con.prepareStatement("CREATE TABLE IF NOT EXISTS coliseum_admin_log (id int(11) NOT NULL AUTO_INCREMENT, admin_id int(11) NOT NULL, admin_name varchar(45) NOT NULL, action varchar(255) NOT NULL, created_at bigint(20) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"))
		{
			create.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] admin log table failed: " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_ADMIN_LOG))
		{
			ps.setInt(1, admin == null ? 0 : admin.getObjectId());
			ps.setString(2, admin == null ? "AUTO" : admin.getName());
			ps.setString(3, action);
			ps.setLong(4, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] admin log failed: " + e.getMessage(), e);
		}
	}
	
	// -------------------------
	// UI (HTML)
	// -------------------------
	public void showUi(Npc npc, Player player)
	{
		if (player == null)
		{
			return;
		}
		int objectId = 0;
		if (npc != null)
		{
			objectId = npc.getObjectId();
			_lastHtmlNpcObjectId = objectId;
			_lastPlayerHtmlNpcObjectId.put(player.getObjectId(), objectId);
		}
		else
		{
			final Integer lastForPlayer = _lastPlayerHtmlNpcObjectId.get(player.getObjectId());
			if ((lastForPlayer != null) && (lastForPlayer > 0))
			{
				objectId = lastForPlayer;
			}
			else if (_managerNpc != null)
			{
				objectId = _managerNpc.getObjectId();
			}
			else
			{
				objectId = _lastHtmlNpcObjectId;
			}
		}
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setHtml(buildHtml(npc));
		player.sendPacket(html);
	}
	
	public void showUi(Player player)
	{
		showUi(null, player);
	}
	
	private String buildHtml(Npc npc)
	{
		String html = loadHtml("data/html/mods/ColiseumTournament/player.html");
		if (html == null)
		{
			return "<html><body>Missing HTML: data/html/mods/ColiseumTournament/player.html</body></html>";
		}
		
		final long now = System.currentTimeMillis();
		String status;
		String time = "";
		String countText = "";
		String buttons = "";
		
		if (!Cfg.ENABLED)
		{
			status = "<font color=FF4040>El evento está apagado.</font><br>Vuelve cuando esté ON.<br>";
		}
		else if (_state == State.REGISTRATION)
		{
			status = "<font color=00FF00>INSCRIPCIÓN ABIERTA</font><br>";
			final long left = Math.max(0, _regEndsAt - now);
			final long sec = left / 1000;
			time = "Tiempo: <font color=LEVEL>" + (sec / 60) + "m " + (sec % 60) + "s</font><br>";
			buttons = makePlayerButtons(npc);
		}
		else if ((_state == State.PREPARE) || (_state == State.FIGHTING) || (_state == State.FINISH))
		{
			status = "<font color=FFFF00>El evento ya está en curso.</font><br>Espera a la próxima inscripción.<br>";
		}
		else
		{
			status = "<font color=FF4040>El evento aún no inició.</font><br>Vuelve cuando esté ON o cuando un GM abra registro.<br>";
		}
		
		final int count = _registered.size();
		countText = "Inscritos: <font color=LEVEL>" + count + "</font> / <font color=LEVEL>" + Cfg.MAX_PLAYERS + "</font><br>";
		if ((_state == State.REGISTRATION) && ((count % 2) == 1))
		{
			countText += "<font color=FF4040>(Necesita ser PAR para iniciar)</font><br>";
		}
		
		html = html.replace("%status%", status);
		html = html.replace("%time%", time);
		html = html.replace("%count%", countText);
		html = html.replace("%buttons%", buttons);
		return html;
	}
	
	private String makePlayerButtons(Npc npc)
	{
		final String joinBypass;
		final String leaveBypass;
		// Usar Quest directo: en esta base el bypass npc_%objectId%_Quest puede cerrar
		// la ventana sin ejecutar el evento. Quest directo sí entra a onEvent().
		joinBypass = "bypass -h Quest ColiseumTournament join";
		leaveBypass = "bypass -h Quest ColiseumTournament leave";
		return "<button value=\"Registrarme\" action=\"" + joinBypass + "\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>" + "<button value=\"Salir\" action=\"" + leaveBypass + "\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>";
	}
	
	private static String loadHtml(String path)
	{
		try
		{
			final java.nio.file.Path p = java.nio.file.Paths.get(path);
			if (!java.nio.file.Files.exists(p))
			{
				return null;
			}
			return new String(java.nio.file.Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showUi(npc, player);
		return null;
	}
	
	// -------------------------
	// Quest bypass Join/Leave
	// -------------------------
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player == null)
		{
			return null;
		}
		
		// IMPORTANTE:
		// En esta base el bypass del botón funciona como: bypass -h Quest ColiseumTournament join
		// Ese bypass NO siempre trae el npc, entonces NO hay que devolver el HTML como String.
		// Si se devuelve String, el cliente puede cerrar la ventana.
		// Lo correcto es enviar el NpcHtmlMessage manualmente y retornar null.
		if (!Cfg.ENABLED)
		{
			showUi(npc, player);
			return null;
		}
		
		if ("join".equalsIgnoreCase(event))
		{
			tryJoin(player);
			showUi(npc, player);
			return null;
		}
		else if ("leave".equalsIgnoreCase(event))
		{
			tryLeave(player);
			showUi(npc, player);
			return null;
		}
		showUi(npc, player);
		return null;
	}
	
	private void tryJoin(Player p)
	{
		if (_state != State.REGISTRATION)
		{
			p.sendMessage("Coliseo: inscripción cerrada.");
			return;
		}
		
		if (_registered.contains(p.getObjectId()))
		{
			p.sendMessage("Coliseo: ya estás registrado.");
			return;
		}
		
		if ((Cfg.MAX_PLAYERS > 0) && (_registered.size() >= Cfg.MAX_PLAYERS))
		{
			p.sendMessage("Coliseo: límite máximo de inscritos alcanzado (" + Cfg.MAX_PLAYERS + ").");
			return;
		}
		
		if (Cfg.IP_PROTECTION && (Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.L2EVENT_ID, p, Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP))
		{
			p.sendMessage("Coliseo: límite de jugadores por IP alcanzado.");
			return;
		}
		
		_registered.add(p.getObjectId());
		p.sendMessage("Coliseo: registrado correctamente.");
		screen(p, "¡Registrado en el Coliseo 1x1!", 2500);
	}
	
	private void tryLeave(Player p)
	{
		if (_state != State.REGISTRATION)
		{
			p.sendMessage("Coliseo: no es posible salir ahora.");
			return;
		}
		
		if (_registered.remove(p.getObjectId()))
		{
			if (Cfg.IP_PROTECTION && (Config.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0))
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, p);
			}
			p.sendMessage("Coliseo: saliste del registro.");
		}
	}
	
	// -------------------------
	// Registration close -> start
	// -------------------------
	private void closeRegistrationAndStart()
	{
		if (!Cfg.ENABLED)
		{
			return;
		}
		
		if (_state != State.REGISTRATION)
		{
			return;
		}
		
		final int count = _registered.size();
		if (count < Cfg.MIN_PLAYERS)
		{
			for (int objId : new ArrayList<>(_registered))
			{
				final Player p = World.getInstance().getPlayer(objId);
				if (p != null)
				{
					p.sendMessage("[Coliseo] Evento cerrado: no hubo suficientes jugadores registrados.");
				}
			}
			clearAntiFeedRegistrations();
			_registered.clear();
			_side.clear();
			_oldTitle.clear();
			_aliveOrder.clear();
			_aliveChaos.clear();
			_currentMatch = null;
			_totalPlayers = 0;
			_totalMatches = 0;
			_state = State.INACTIVE;
			despawnManagerNpc();
			Broadcast.toAllOnlinePlayers("[Coliseo] Evento cerrado: no hubo suficientes jugadores registrados.");
			return;
		}
		if ((count % 2) == 1)
		{
			for (int objId : _registered)
			{
				final Player p = World.getInstance().getPlayer(objId);
				if (p != null)
				{
					p.sendMessage("[Coliseo] No inició: el número de jugadores debe ser par. La inscripción sigue abierta.");
				}
			}
			_regEndsAt = System.currentTimeMillis() + (Cfg.REG_MIN * 60_000L);
			scheduleRegistrationClose(Cfg.REG_MIN * 60_000L);
			return;
		}
		
		_state = State.PREPARE;
		_totalPlayers = count;
		_totalMatches = 0;
		
		Broadcast.toAllOnlinePlayers("[Coliseo] Inscripciones cerradas. Preparando evento (" + count + " jugadores).");
		startOpeningCountdown();
	}
	
	private void startOpeningCountdown()
	{
		long delay = 500;
		for (int i = 10; i >= 1; i--)
		{
			final int number = i;
			ThreadPool.schedule(() -> screenRegistered(String.valueOf(number), 1000), delay);
			delay += 1000;
		}
		ThreadPool.schedule(() -> screenRegistered("0", 1000), delay);
		delay += 1000;
		ThreadPool.schedule(() ->
		{
			preparePlayers();
			startTournament();
		}, delay);
	}
	
	private void screenRegistered(String text, int ms)
	{
		for (int objId : _registered)
		{
			final Player player = World.getInstance().getPlayer(objId);
			if (player != null)
			{
				screen(player, text, ms);
			}
		}
	}
	
	private void preparePlayers()
	{
		_side.clear();
		_oldTitle.clear();
		_aliveOrder.clear();
		_aliveChaos.clear();
		
		final List<Integer> list = new ArrayList<>(_registered);
		Collections.shuffle(list);
		
		final int half = list.size() / 2;
		for (int i = 0; i < list.size(); i++)
		{
			final int objId = list.get(i);
			final Side s = (i < half) ? Side.ORDER : Side.CHAOS;
			_side.put(objId, s);
			
			final Player p = World.getInstance().getPlayer(objId);
			if (p == null)
			{
				continue;
			}
			
			_oldTitle.put(objId, p.getTitle());
			p.setTitle(s == Side.ORDER ? "Order" : "Chaos");
			p.broadcastUserInfo();
			
			p.abortAttack();
			p.abortCast();
			p.sitDown();
			teleportToSeat(p, s);
		}
	}
	
	private void startTournament()
	{
		_state = State.FIGHTING;
		
		_aliveOrder.clear();
		_aliveChaos.clear();
		
		for (int objId : _registered)
		{
			final Player p = World.getInstance().getPlayer(objId);
			if (p == null)
			{
				continue;
			}
			
			if (p.isDead())
			{
				p.doRevive();
			}
			
			final Side s = _side.get(objId);
			if (s == Side.ORDER)
			{
				_aliveOrder.add(objId);
			}
			else
			{
				_aliveChaos.add(objId);
			}
			
			// Ya fueron enviados a Asiento 1 / Asiento 2 en preparePlayers().
			// No volvemos a teletransportar acá para evitar el efecto de 2-3 teleports seguidos.
			p.sitDown();
		}
		
		Broadcast.toAllOnlinePlayers("[Coliseo] Evento iniciado. Order vs Chaos.");
		ThreadPool.schedule(this::startNextMatch, 2500);
	}
	
	// -------------------------
	// Match loop (1x1 cross)
	// -------------------------
	private void startNextMatch()
	{
		if (_state != State.FIGHTING)
		{
			return;
		}
		
		cleanupOffline(_aliveOrder);
		cleanupOffline(_aliveChaos);
		
		final int aliveTotal = _aliveOrder.size() + _aliveChaos.size();
		
		if (aliveTotal <= 0)
		{
			finishNoWinner();
			return;
		}
		
		if (aliveTotal == 1)
		{
			final int champId = !_aliveOrder.isEmpty() ? _aliveOrder.get(0) : _aliveChaos.get(0);
			finishChampion(champId);
			return;
		}
		
		Collections.shuffle(_aliveOrder);
		Collections.shuffle(_aliveChaos);
		
		Integer a = !_aliveOrder.isEmpty() ? _aliveOrder.remove(0) : null;
		Integer b = !_aliveChaos.isEmpty() ? _aliveChaos.remove(0) : null;
		
		if ((a == null) && (b != null))
		{
			advanceBye(b);
			return;
		}
		if ((b == null) && (a != null))
		{
			advanceBye(a);
			return;
		}
		
		if ((a == null) || (b == null))
		{
			finishNoWinner();
			return;
		}
		
		_totalMatches++;
		_currentMatch = new Match(a, b);
		
		final Player pa = World.getInstance().getPlayer(a);
		final Player pb = World.getInstance().getPlayer(b);
		
		if ((pa == null) || (pb == null))
		{
			// forfeit
			handleForfeit(a, b);
			return;
		}
		
		teleportToSeats(pa, pb);
		countdownAndFight(pa, pb);
	}
	
	private void advanceBye(int objId)
	{
		final Player p = World.getInstance().getPlayer(objId);
		if (p != null)
		{
			lock(p, true);
			p.abortAttack();
			p.abortCast();
			p.teleToLocation(Cfg.WAIT_WIN.x, Cfg.WAIT_WIN.y, Cfg.WAIT_WIN.z, 1);
			p.sitDown();
			screen(p, "BYE: avanzaste automáticamente!", 2500);
		}
		
		final Side s = _side.get(objId);
		if (s == Side.ORDER)
		{
			_aliveOrder.add(objId);
		}
		else
		{
			_aliveChaos.add(objId);
		}
		
		ThreadPool.schedule(this::startNextMatch, Cfg.ROUND_DELAY * 1000L);
	}
	
	private void teleportToSeat(Player p, Side s)
	{
		final Loc loc = (s == Side.ORDER) ? Cfg.SEAT_A : Cfg.SEAT_B;
		if (loc != null)
		{
			// Evita re-teleportar si ya está en el asiento correcto.
			// Esto elimina el efecto visual de que el PJ "salta" 2 o 3 veces.
			final int dx = p.getX() - loc.x;
			final int dy = p.getY() - loc.y;
			final int dz = p.getZ() - loc.z;
			if (((dx * dx) + (dy * dy) + (dz * dz)) > 250000) // 500 de distancia.
			{
				p.teleToLocation(loc.x, loc.y, loc.z, 2);
			}
		}
		p.sitDown();
	}
	
	private void teleportToSeats(Player a, Player b)
	{
		final Side sa = _side.get(a.getObjectId());
		final Side sb = _side.get(b.getObjectId());
		lock(a, true);
		lock(b, true);
		a.abortAttack();
		a.abortCast();
		b.abortAttack();
		b.abortCast();
		teleportToSeat(a, sa);
		teleportToSeat(b, sb);
	}
	
	private void teleportToArena(Player a, Player b)
	{
		// Standardize + BUFF AUTO mage/fighter
		if (Cfg.STD_BUFFS)
		{
			standardizeAndBuff(a);
			standardizeAndBuff(b);
		}
		else
		{
			// mesmo se sem buffs, ao menos limpa e enche
			standardizeBase(a);
			standardizeBase(b);
		}
		
		a.abortAttack();
		a.abortCast();
		b.abortAttack();
		b.abortCast();
		
		a.teleToLocation(Cfg.ARENA_A.x, Cfg.ARENA_A.y, Cfg.ARENA_A.z, 3);
		b.teleToLocation(Cfg.ARENA_B.x, Cfg.ARENA_B.y, Cfg.ARENA_B.z, 3);
		
		a.standUp();
		b.standUp();
	}
	
	private void countdownAndFight(Player a, Player b)
	{
		// trava
		lock(a, true);
		lock(b, true);
		
		// Tela VS
		final String sa = sideName(_side.get(a.getObjectId()));
		final String sb = sideName(_side.get(b.getObjectId()));
		
		screen(a, sa + " - " + a.getName(), 2000);
		screen(b, sa + " - " + a.getName(), 2000);
		
		ThreadPool.schedule(() ->
		{
			screen(a, "VS", 1400);
			screen(b, "VS", 1400);
		}, 900);
		
		ThreadPool.schedule(() ->
		{
			screen(a, sb + " - " + b.getName(), 2000);
			screen(b, sb + " - " + b.getName(), 2000);
		}, 1600);
		
		long delay = 2500;
		for (int i = Cfg.COUNTDOWN; i >= 1; i--)
		{
			final int n = i;
			ThreadPool.schedule(() ->
			{
				screen(a, String.valueOf(n), 1000);
				screen(b, String.valueOf(n), 1000);
			}, delay);
			delay += 1000;
		}
		
		ThreadPool.schedule(() ->
		{
			screen(a, "0", 800);
			screen(b, "0", 800);
		}, delay);
		delay += 800;
		
		ThreadPool.schedule(() ->
		{
			teleportToArena(a, b);
			screen(a, "¡PELEEN!", 1200);
			screen(b, "¡PELEEN!", 1200);
			
			lock(a, false);
			lock(b, false);
			
			startFightWatcher(_currentMatch);
		}, delay);
	}
	
	private void startFightWatcher(Match m)
	{
		stopFightWatcher();
		_fightWatcher = ThreadPool.scheduleAtFixedRate(() -> pollFight(m), 500, 500);
	}
	
	private void stopFightWatcher()
	{
		if (_fightWatcher != null)
		{
			_fightWatcher.cancel(false);
			_fightWatcher = null;
		}
		stopSudden();
	}
	
	private void pollFight(Match m)
	{
		if ((_state != State.FIGHTING) || (m == null) || m.finished)
		{
			stopFightWatcher();
			return;
		}
		
		final Player a = World.getInstance().getPlayer(m.aObjId);
		final Player b = World.getInstance().getPlayer(m.bObjId);
		
		if ((a == null) || (b == null))
		{
			handleForfeit(m.aObjId, m.bObjId);
			return;
		}
		
		if (a.isDead() || b.isDead())
		{
			final Player loser = a.isDead() ? a : b;
			final Player winner = a.isDead() ? b : a;
			finishMatch(m, winner, loser, false);
			return;
		}
		
		final long elapsed = (System.currentTimeMillis() - m.startedAt) / 1000L;
		if (elapsed >= Cfg.FIGHT_SECONDS)
		{
			startSudden(a, b);
		}
	}
	
	private void startSudden(Player a, Player b)
	{
		if (_suddenTask != null)
		{
			return;
		}
		
		screen(a, "SUDDEN DEATH!", 2000);
		screen(b, "SUDDEN DEATH!", 2000);
		
		_suddenTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			if (a.isDead() || b.isDead())
			{
				return;
			}
			drainPct(a, Cfg.SUDDEN_DRAIN_PCT);
			drainPct(b, Cfg.SUDDEN_DRAIN_PCT);
		}, 0, Cfg.SUDDEN_TICK_MS);
	}
	
	private void stopSudden()
	{
		if (_suddenTask != null)
		{
			_suddenTask.cancel(false);
			_suddenTask = null;
		}
	}
	
	private void drainPct(Player p, int pct)
	{
		try
		{
			final double max = p.getMaxHp();
			final double dmg = Math.max(1, (max * pct) / 100.0);
			p.reduceCurrentHp(dmg, p, null);
		}
		catch (Exception ignored)
		{
		}
	}
	
	private void finishMatch(Match m, Player winner, Player loser, boolean forfeit)
	{
		if ((m == null) || m.finished)
		{
			return;
		}
		
		m.finished = true;
		stopFightWatcher();
		
		try
		{
			if ((loser != null) && loser.isDead())
			{
				loser.doRevive();
			}
		}
		catch (Exception ignored)
		{
		}
		
		// senta e manda para espera
		if (winner != null)
		{
			lock(winner, true);
			winner.abortAttack();
			winner.abortCast();
			lock(winner, true);
			winner.teleToLocation(Cfg.WAIT_WIN.x, Cfg.WAIT_WIN.y, Cfg.WAIT_WIN.z, 0);
			winner.sitDown();
		}
		
		if (loser != null)
		{
			lock(loser, true);
			loser.abortAttack();
			loser.abortCast();
			lock(loser, true);
			loser.teleToLocation(Cfg.WAIT_LOSE.x, Cfg.WAIT_LOSE.y, Cfg.WAIT_LOSE.z, 0);
			loser.sitDown();
		}
		
		// Ranking semanal
		if (winner != null)
		{
			addWeekly(winner.getObjectId(), _side.get(winner.getObjectId()), 1, 0, Cfg.PTS_WIN);
		}
		if (loser != null)
		{
			addWeekly(loser.getObjectId(), _side.get(loser.getObjectId()), 0, 1, Cfg.PTS_LOSS);
		}
		
		// volta vencedor pro pool
		if (winner != null)
		{
			final Side sw = _side.get(winner.getObjectId());
			if (sw == Side.ORDER)
			{
				_aliveOrder.add(winner.getObjectId());
			}
			else
			{
				_aliveChaos.add(winner.getObjectId());
			}
			
			final String loserName = (loser != null) ? loser.getName() : "OFFLINE";
			Broadcast.toAllOnlinePlayers("[Coliseo] " + winner.getName() + " venció a " + loserName + (forfeit ? " (W.O.)" : "") + ".");
		}
		
		ThreadPool.schedule(this::startNextMatch, Cfg.ROUND_DELAY * 1000L);
	}
	
	private void handleForfeit(int objA, int objB)
	{
		final Match m = _currentMatch;
		if ((m == null) || m.finished)
		{
			return;
		}
		
		final Player a = World.getInstance().getPlayer(objA);
		final Player b = World.getInstance().getPlayer(objB);
		
		if ((a == null) && (b == null))
		{
			m.finished = true;
			stopFightWatcher();
			ThreadPool.schedule(this::startNextMatch, 1000);
			return;
		}
		
		if ((a == null) && (b != null))
		{
			// b vence
			finishMatch(m, b, null, true);
			return;
		}
		if ((b == null) && (a != null))
		{
			finishMatch(m, a, null, true);
			return;
		}
	}
	
	private void cleanupOffline(List<Integer> list)
	{
		list.removeIf(objId -> World.getInstance().getPlayer(objId) == null);
	}
	
	// -------------------------
	// Buff AUTO mage/fighter
	// -------------------------
	private static void standardizeBase(Player p)
	{
		p.stopAllEffects();
		p.getMaxCp();
	}
	
	private void standardizeAndBuff(Player p)
	{
		standardizeBase(p);
		
		final boolean mage = isMage(p);
		final List<int[]> buffs = mage ? Cfg.MAGE_BUFFS : Cfg.FIGHTER_BUFFS;
		
		if ((buffs == null) || buffs.isEmpty())
		{
			return;
		}
		
		for (int[] b : buffs)
		{
			applyEffectSafe(p, b[0], b[1]);
		}
	}
	
	private boolean isMage(Player p)
	{
		return p.getClassId() == ClassId.FIGHTER;
	}
	
	/**
	 * Aplica buff por reflection pra N├âO depender se sua base usa SkillTable ou SkillData. Tenta: - net.sf.l2j.gameserver.data.SkillTable - net.sf.l2j.gameserver.datatables.SkillTable - net.sf.l2j.gameserver.data.SkillData E chama: - getInstance().getInfo(id, level) ou getSkill(id, level) -
	 * skill.getEffects(player, player)
	 */
	private static void applyEffectSafe(Player player, int skillId, int level)
	{
		final Skill skill = SkillData.getInstance().getSkill(skillId, level);
		if (skill != null)
		{
			skill.applyEffects(player, player);
		}
	}
	
	// -------------------------
	// Lock / visuals
	// -------------------------
	private void lock(Player p, boolean on)
	{
		p.setImmobilized(on);
	}
	
	private void screen(Player p, String text, int ms)
	{
		try
		{
			// Constructor compatible con esta base L2JMobius.
			// El anterior no mostraba el mensaje en pantalla en algunos clientes.
			p.sendPacket(new ExShowScreenMessage(text, ExShowScreenMessage.MIDDLE_CENTER, ms, 0, true, false));
		}
		catch (Exception ignored)
		{
		}
	}
	
	private void announceAll(String msg)
	{
		for (Player p : World.getInstance().getPlayers())
		{
			p.sendMessage(msg);
		}
	}
	
	private String sideName(Side s)
	{
		return (s == Side.ORDER) ? "ORDER" : "CHAOS";
	}
	
	// -------------------------
	// Finish
	// -------------------------
	private void finishNoWinner()
	{
		announceAll("[Coliseo] Evento finalizado sin campeón (sin jugadores).");
		cleanupAndReopen();
	}
	
	private void finishChampion(int champId)
	{
		final Player champ = World.getInstance().getPlayer(champId);
		final Side s = _side.get(champId);
		
		String champName = (champ != null) ? champ.getName() : CharInfoTable.getInstance().getNameById(champId);
		if (champName == null)
		{
			champName = "CharId " + champId;
		}
		Broadcast.toAllOnlinePlayers("[Coliseo] CAMPEÓN:  " + champName + " (" + sideName(s) + ")!");
		
		if (champ != null)
		{
			champ.addItem(champName, Cfg.REWARD_ITEM, (int) Cfg.REWARD_COUNT, champId, champ, true);
		}
		
		if (Cfg.TEAM_REWARD)
		{
			for (int objId : _registered)
			{
				if (_side.get(objId) != s)
				{
					continue;
				}
				final Player p = World.getInstance().getPlayer(objId);
				if (p == null)
				{
					continue;
				}
				
				p.addItem(champName, Cfg.TEAM_ITEM, (int) Cfg.TEAM_COUNT, objId, p, true);
			}
		}
		
		addWeekly(champId, s, 0, 0, Cfg.PTS_CHAMP);
		saveChampion(champId, s);
		saveHistory(champId, s);
		
		// No aplicar título visual al campeón al finalizar; solo queda guardado en SQL/ranking.
		cleanupAndReopen();
	}
	
	private void cleanupAndReopen()
	{
		_state = State.FINISH;
		stopFightWatcher();
		
		for (int objId : new ArrayList<>(_registered))
		{
			final Player p = World.getInstance().getPlayer(objId);
			if (p == null)
			{
				continue;
			}
			
			final String t = _oldTitle.get(objId);
			if (t != null)
			{
				p.setTitle(t);
			}
			p.broadcastUserInfo();
			
			p.abortAttack();
			p.abortCast();
			lock(p, false);
			p.standUp();
			p.teleToLocation(Cfg.EXIT.x, Cfg.EXIT.y, Cfg.EXIT.z, 0);
		}
		
		clearAntiFeedRegistrations();
		_registered.clear();
		_side.clear();
		_oldTitle.clear();
		_aliveOrder.clear();
		_aliveChaos.clear();
		_currentMatch = null;
		_state = State.INACTIVE;
		despawnManagerNpc();
		Broadcast.toAllOnlinePlayers(Cfg.AUTO_SCHEDULE_ENABLED ? "[Coliseo] Evento finalizado. La próxima inscripción abrirá por horario automático." : "[Coliseo] Evento finalizado. Un GM debe abrir la próxima inscripción desde el panel.");
	}
	
	// -------------------------
	// Weekly rank + champion
	// -------------------------
	private void loadWeekStart()
	{
		long wk = 0L;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_WEEK_KEY);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				wk = rs.getLong("week_start");
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] loadWeekStart failed: " + e.getMessage(), e);
		}
		
		if (wk <= 0)
		{
			wk = startOfWeekNow();
			saveWeekStart(wk);
		}
		_weekStart = wk;
	}
	
	private void saveWeekStart(long wk)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_WEEK_SET))
		{
			ps.setLong(1, wk);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] saveWeekStart failed: " + e.getMessage(), e);
		}
	}
	
	private long startOfWeekNow()
	{
		// week_start simples (epoch seconds) ÔÇö pode evoluir depois pra segunda-feira 00:00
		return Instant.now().getEpochSecond();
	}
	
	private void scheduleWeeklyReset()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			try
			{
				final long now = System.currentTimeMillis();
				final long wkMs = _weekStart * 1000L;
				final long seven = 7L * 24L * 60L * 60L * 1000L;
				
				if ((now - wkMs) >= seven)
				{
					_weekStart = startOfWeekNow();
					saveWeekStart(_weekStart);
					clearWeekly();
					Broadcast.toAllOnlinePlayers("[Coliseo] Ranking semanal reiniciado!");
				}
			}
			catch (Exception ignored)
			{
			}
		}, 60_000, 600_000);
	}
	
	private void addWeekly(int charId, Side side, int wins, int losses, int points)
	{
		if ((charId <= 0) || (side == null))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_UPSERT_WEEKLY))
		{
			ps.setLong(1, _weekStart);
			ps.setInt(2, charId);
			ps.setInt(3, wins);
			ps.setInt(4, losses);
			ps.setInt(5, points);
			ps.setString(6, side.name());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] addWeekly failed: " + e.getMessage(), e);
		}
	}
	
	private void clearWeekly()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_CLEAR_WEEK))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] clearWeekly failed: " + e.getMessage(), e);
		}
	}
	
	private void saveChampion(int champId, Side s)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_SET_CHAMP))
		{
			ps.setLong(1, _weekStart);
			ps.setInt(2, champId);
			ps.setString(3, s.name());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] saveChampion failed: " + e.getMessage(), e);
		}
	}
	
	private void saveHistory(int champId, Side s)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_HISTORY))
		{
			ps.setLong(1, _weekStart);
			ps.setInt(2, champId);
			ps.setString(3, s.name());
			ps.setInt(4, _totalPlayers);
			ps.setInt(5, _totalMatches);
			ps.setLong(6, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] saveHistory failed: " + e.getMessage(), e);
		}
	}
	
	private void scheduleChampionOnlineApply()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			try
			{
				Champion c = loadChampion();
				if (c == null)
				{
					return;
				}
				final Player p = World.getInstance().getPlayer(c.charId);
				if (p == null)
				{
					return;
				}
				applyChampionAura(p);
			}
			catch (Exception ignored)
			{
			}
		}, 60_000, 60_000);
	}
	
	private static final class Champion
	{
		long weekStart;
		int charId;
		String side;
	}
	
	private Champion loadChampion()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_GET_CHAMP);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				Champion c = new Champion();
				c.weekStart = rs.getLong("week_start");
				c.charId = rs.getInt("char_id");
				c.side = rs.getString("side");
				return c;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[Coliseum] loadChampion failed: " + e.getMessage(), e);
		}
		return null;
	}
	
	private void applyChampionAura(Player p)
	{
		// Sin título flotante permanente. El campeón queda guardado en SQL/ranking,
		// pero no se le fuerza el title arriba del personaje.
	}
	
	public static void main(String[] args)
	{
		new ColiseumTournament();
	}
	
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		if ((player == null) || (bypass == null))
		{
			return false;
		}
		if (bypass.equalsIgnoreCase("join") || bypass.endsWith(" join"))
		{
			tryJoin(player);
			showUi(player);
			return true;
		}
		if (bypass.equalsIgnoreCase("leave") || bypass.endsWith(" leave"))
		{
			tryLeave(player);
			showUi(player);
			return true;
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.l2jmobius.gameserver.model.quest.Event#eventStart(org.l2jmobius.gameserver.model.actor.Player)
	 */
	@Override
	public boolean eventStart(Player eventMaker)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.l2jmobius.gameserver.model.quest.Event#eventStop()
	 */
	@Override
	public boolean eventStop()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
