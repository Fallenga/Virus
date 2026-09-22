package org.l2jmobius.gameserver.model.events.clankorean;

import java.io.File;
import java.io.FileInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClanKoreanConfig
{
	private static final Logger LOGGER = Logger.getLogger(ClanKoreanConfig.class.getName());
	private static final String CONFIG_FILE = "./config/Events/Clan Korean.ini";
	
	public static boolean ENABLED;
	public static Set<DayOfWeek> EVENT_DAYS;
	public static List<LocalTime> EVENT_START_TIMES;
	public static int EVENT_DURATION_MINUTES;
	public static int PREPARE_SECONDS;
	public static int FIGHTER_TIME_SECONDS;
	public static int MATCH_TIME_MINUTES;
	// Each entry contains the item ID and the amount delivered to each winning player.
	public static long[][] REWARDS;
	public static boolean HEAL_WINNER;
	public static boolean ALLOW_SAME_HWID;
	public static int MINIMUM_CLAN_LEVEL;
	public static int MINIMUM_ARENAS;
	public static int ARENA_REUSE_DELAY_SECONDS;
	public static int NPC_ID;
	public static int NPC_X;
	public static int NPC_Y;
	public static int NPC_Z;
	public static int NPC_HEADING;
	public static int[][] ARENA_LOCATIONS;
	public static int BENCH_OFFSET_Y;
	
	private ClanKoreanConfig()
	{
	}
	
	public static void load()
	{
		final Properties properties = new Properties();
		final File file = new File(CONFIG_FILE);
		try (FileInputStream input = new FileInputStream(file))
		{
			properties.load(input);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Clan Korean: No se pudo cargar " + CONFIG_FILE, e);
			return;
		}
		
		ENABLED = Boolean.parseBoolean(properties.getProperty("Enabled", "false"));
		EVENT_DAYS = parseDays(properties.getProperty("EventDays", "ALL"));
		EVENT_START_TIMES = parseStartTimes(properties.getProperty("EventStartTimes", "20:00"));
		try
		{
			EVENT_DURATION_MINUTES = Integer.parseInt(properties.getProperty("EventDurationMinutes", "20").trim());
			if ((EVENT_DURATION_MINUTES < 1) || (EVENT_DURATION_MINUTES > 1440))
			{
				throw new IllegalArgumentException("La duracion debe estar entre 1 y 1440 minutos.");
			}
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.log(Level.SEVERE, "Clan Korean: EventDurationMinutes invalido. El evento quedara deshabilitado.", e);
			ENABLED = false;
			EVENT_DURATION_MINUTES = 20;
		}
		PREPARE_SECONDS = Integer.parseInt(properties.getProperty("PrepareSeconds", "15"));
		FIGHTER_TIME_SECONDS = Integer.parseInt(properties.getProperty("FighterTimeSeconds", "180"));
		MATCH_TIME_MINUTES = Integer.parseInt(properties.getProperty("MatchTimeMinutes", "30"));
		try
		{
			REWARDS = parseRewards(properties.getProperty("Rewards", "4356,6"));
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.log(Level.SEVERE, "Clan Korean: Rewards invalido. El evento quedara deshabilitado.", e);
			ENABLED = false;
			REWARDS = new long[0][0];
		}
		HEAL_WINNER = Boolean.parseBoolean(properties.getProperty("HealWinnerBetweenRounds", "true"));
		ALLOW_SAME_HWID = Boolean.parseBoolean(properties.getProperty("AllowSameHWID", "false"));
		MINIMUM_CLAN_LEVEL = Integer.parseInt(properties.getProperty("MinimumClanLevel", "5"));
		MINIMUM_ARENAS = Integer.parseInt(properties.getProperty("MinimumArenas", "3"));
		ARENA_REUSE_DELAY_SECONDS = Integer.parseInt(properties.getProperty("ArenaReuseDelaySeconds", "3"));
		NPC_ID = Integer.parseInt(properties.getProperty("NpcId", "9997"));
		
		final int[] npc = parseLocation(properties.getProperty("NpcSpawn", "-186303,244340,1576,32767"), 4);
		NPC_X = npc[0];
		NPC_Y = npc[1];
		NPC_Z = npc[2];
		NPC_HEADING = npc[3];
		final String[] arenas = properties.getProperty("ArenaLocations", "-87500,-153300,-9176,-88050,-153300,-9176").split(";");
		ARENA_LOCATIONS = new int[arenas.length][6];
		for (int i = 0; i < arenas.length; i++)
		{
			ARENA_LOCATIONS[i] = parseLocation(arenas[i].trim(), 6);
		}
		if (ARENA_LOCATIONS.length < MINIMUM_ARENAS)
		{
			LOGGER.severe("Clan Korean: Se configuraron " + ARENA_LOCATIONS.length + " arenas, pero el minimo requerido es " + MINIMUM_ARENAS + ". El evento quedara deshabilitado.");
			ENABLED = false;
		}
		BENCH_OFFSET_Y = Integer.parseInt(properties.getProperty("BenchOffsetY", "120"));
	}
	
	private static Set<DayOfWeek> parseDays(String value)
	{
		final Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
		if (value.trim().equalsIgnoreCase("ALL"))
		{
			result.addAll(EnumSet.allOf(DayOfWeek.class));
			return result;
		}
		for (String day : value.split(","))
		{
			try
			{
				result.add(DayOfWeek.valueOf(day.trim().toUpperCase()));
			}
			catch (IllegalArgumentException e)
			{
				LOGGER.severe("Clan Korean: Dia invalido en EventDays: " + day.trim() + ". El evento quedara deshabilitado.");
				ENABLED = false;
			}
		}
		if (result.isEmpty())
		{
			LOGGER.severe("Clan Korean: EventDays no contiene ningun dia valido. El evento quedara deshabilitado.");
			ENABLED = false;
		}
		return result;
	}
	
	private static List<LocalTime> parseStartTimes(String value)
	{
		final List<LocalTime> result = new ArrayList<>();
		for (String part : value.split(",", -1))
		{
			try
			{
				final LocalTime time = LocalTime.parse(part.trim());
				if (!result.contains(time))
				{
					result.add(time);
				}
			}
			catch (IllegalArgumentException e)
			{
				LOGGER.severe("Clan Korean: Hora invalida en EventStartTimes: " + part.trim() + ". El evento quedara deshabilitado.");
				ENABLED = false;
			}
		}
		if (result.isEmpty())
		{
			LOGGER.severe("Clan Korean: EventStartTimes no contiene horas validas. El evento quedara deshabilitado.");
			ENABLED = false;
		}
		return result;
	}
	
	private static int[] parseLocation(String value, int size)
	{
		final String[] parts = value.split(",");
		if (parts.length != size)
		{
			throw new IllegalArgumentException("Clan Korean: Coordenada invalida: " + value);
		}
		final int[] result = new int[size];
		for (int i = 0; i < size; i++)
		{
			result[i] = Integer.parseInt(parts[i].trim());
		}
		return result;
	}
	
	private static long[][] parseRewards(String value)
	{
		final String[] entries = value.split(";", -1);
		final long[][] rewards = new long[entries.length][2];
		final Set<Integer> itemIds = new java.util.HashSet<>();
		for (int i = 0; i < entries.length; i++)
		{
			final String[] fields = entries[i].trim().split(",", -1);
			if (fields.length != 2)
			{
				throw new IllegalArgumentException("Premio invalido: " + entries[i]);
			}
			final int itemId = Integer.parseInt(fields[0].trim());
			final long amount = Long.parseLong(fields[1].trim());
			if ((itemId <= 0) || (amount <= 0) || !itemIds.add(itemId))
			{
				throw new IllegalArgumentException("ID duplicado o premio no positivo: " + entries[i]);
			}
			rewards[i][0] = itemId;
			rewards[i][1] = amount;
		}
		return rewards;
	}
}
