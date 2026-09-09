package org.l2jmobius.gameserver.model.events.clankorean;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClanKoreanConfig
{
	private static final Logger LOGGER = Logger.getLogger(ClanKoreanConfig.class.getName());
	private static final String CONFIG_FILE = "./config/custom/clan_korean.ini";
	
	public static boolean ENABLED;
	public static String[] START_TIMES;
	public static int REGISTRATION_MINUTES;
	public static int PREPARE_SECONDS;
	public static int FIGHTER_TIME_SECONDS;
	public static int MATCH_TIME_MINUTES;
	public static int REWARD_ID;
	public static long REWARD_AMOUNT;
	public static boolean HEAL_WINNER;
	public static boolean ALLOW_SAME_HWID;
	public static int MINIMUM_CLAN_LEVEL;
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
		START_TIMES = properties.getProperty("StartTimes", "20:00").split(",");
		REGISTRATION_MINUTES = Integer.parseInt(properties.getProperty("RegistrationMinutes", "10"));
		PREPARE_SECONDS = Integer.parseInt(properties.getProperty("PrepareSeconds", "15"));
		FIGHTER_TIME_SECONDS = Integer.parseInt(properties.getProperty("FighterTimeSeconds", "180"));
		MATCH_TIME_MINUTES = Integer.parseInt(properties.getProperty("MatchTimeMinutes", "30"));
		REWARD_ID = Integer.parseInt(properties.getProperty("RewardId", "57"));
		REWARD_AMOUNT = Long.parseLong(properties.getProperty("RewardAmount", "1000000"));
		HEAL_WINNER = Boolean.parseBoolean(properties.getProperty("HealWinnerBetweenRounds", "true"));
		ALLOW_SAME_HWID = Boolean.parseBoolean(properties.getProperty("AllowSameHWID", "false"));
		MINIMUM_CLAN_LEVEL = Integer.parseInt(properties.getProperty("MinimumClanLevel", "5"));
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
		BENCH_OFFSET_Y = Integer.parseInt(properties.getProperty("BenchOffsetY", "120"));
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
}
