package org.l2jmobius.gameserver.model.events.fos;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.PropertiesParser;
import org.l2jmobius.commons.util.RewardHolder;
import org.l2jmobius.commons.util.StringUtil;

public class FOSConfig
{
	protected static final Logger _log = Logger.getLogger(FOSConfig.class.getName());
	
	private static final String FOS_FILE = "./config/Events/TvTFortress.ini";
	
	public static boolean FOS_EVENT_ENABLED;
	public static String[] FOS_EVENT_INTERVAL;
	/** Days on which automatic Fortress events may start. Empty means ALL. */
	public static Set<Integer> FOS_EVENT_DAYS = new HashSet<>();
	public static int FOS_EVENT_PARTICIPATION_TIME;
	public static int FOS_EVENT_RUNNING_TIME;
	public static String FOS_NPC_LOC_NAME;
	
	public static int FOS_EVENT_PARTICIPATION_NPC_ID;
	public static int FOS_EVENT_ARTIFACT_NPC_ID;
	public static int FOS_EVENT_SUMMON_SKILL_ID;
	// public static int FOS_EVENT_TEAM_1_FLAG;
	// public static int FOS_EVENT_TEAM_2_FLAG;
	// public static int FOS_EVENT_CAPTURE_SKILL;
	public static int[] FOS_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] FOS_EVENT_PARTICIPATION_FEE = new int[2];
	public static int FOS_EVENT_MIN_PLAYERS_IN_TEAMS;
	public static int FOS_EVENT_MAX_PLAYERS_IN_TEAMS;
	public static int FOS_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int FOS_EVENT_START_LEAVE_TELEPORT_DELAY;
	
	public static String FOS_EVENT_TEAM_1_NAME;
	public static int[] FOS_EVENT_TEAM_1_COORDINATES = new int[3];
	public static String FOS_EVENT_TEAM_2_NAME;
	public static int[] FOS_EVENT_TEAM_2_COORDINATES = new int[3];
	public static int[] FOS_EVENT_FLAG_COORDINATES = new int[4];
	
	public static List<RewardHolder> FOS_EVENT_REWARDS_WIN = new ArrayList<>();
	public static List<RewardHolder> FOS_EVENT_REWARDS_LOS = new ArrayList<>();
	public static boolean BLOCK_SKILLS_HEAL_FOS;
	public static boolean FOS_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
	public static boolean FOS_EVENT_SCROLL_ALLOWED;
	public static boolean FOS_EVENT_POTIONS_ALLOWED;
	public static boolean FOS_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> FOS_DOORS_ATTACKABLE;
	public static boolean FOS_REWARD_TEAM_TIE;
	// public static boolean FOS_REWARD_NO_CARRIER_PLAYER;
	public static byte FOS_EVENT_MIN_LVL;
	public static byte FOS_EVENT_MAX_LVL;
	public static int FOS_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> FOS_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> FOS_EVENT_MAGE_BUFFS;
	public static boolean FOS_EVENT_MULTIBOX_PROTECTION_ENABLE;
	public static int FOS_EVENT_NUMBER_BOX_REGISTER;
	public static boolean ALLOW_TVTFOS_COMMANDS;
	public static boolean ENABLE_AUTO_SKILL_FOS_TVT;
	
	public static boolean FOS_PLAYER_CAN_BE_KILLED_IN_PZ;
	// public static boolean ENABLE_FOS_INSTANCE;
	// public static int FOS_INSTANCE_ID;
	
	public static void init()
	{
		PropertiesParser events = new PropertiesParser(FOS_FILE);
		BLOCK_SKILLS_HEAL_FOS = events.getBoolean("BlockSkillsHealInFOS", false);
		FOS_PLAYER_CAN_BE_KILLED_IN_PZ = events.getBoolean("EnableFOSPeaceZoneAttack", false);
		// ENABLE_FOS_INSTANCE = events.getBoolean("EnableFOSInstance", false);
		// FOS_INSTANCE_ID = events.getInt("FOSInstanceId", 1);
		ENABLE_AUTO_SKILL_FOS_TVT = events.getBoolean("EnableAutoClickSummonCrest", false);
		// Novo
		ALLOW_TVTFOS_COMMANDS = events.getBoolean("EnableCommandTVTFortress", false);
		FOS_EVENT_ENABLED = events.getBoolean("FOSEventEnabled", false);
		FOS_EVENT_INTERVAL = events.getString("FOSEventInterval", "20:00").split(",");
		loadEventDays(events.getString("FOSEventDays", "ALL"));
		FOS_EVENT_PARTICIPATION_TIME = events.getInt("FOSEventParticipationTime", 3600);
		FOS_EVENT_RUNNING_TIME = events.getInt("FOSEventRunningTime", 1800);
		FOS_NPC_LOC_NAME = events.getString("FOSNpcLocName", "Giran Town");
		
		FOS_EVENT_PARTICIPATION_NPC_ID = events.getInt("FOSEventParticipationNpcId", 0);
		FOS_EVENT_ARTIFACT_NPC_ID = events.getInt("FOSEventArtifactNpcId", 0);
		FOS_EVENT_SUMMON_SKILL_ID = events.getInt("FOSEventSummonSkillId", 0);
		// FOS_EVENT_TEAM_1_FLAG = events.getInt("FOSEventFirstTeamFlag", 0);
		// FOS_EVENT_TEAM_2_FLAG = events.getInt("FOSEventSecondTeamFlag", 0);
		// FOS_EVENT_CAPTURE_SKILL = events.getInt("FOSEventCaptureSkillId", 0);
		
		if (FOS_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			FOS_EVENT_ENABLED = false;
			_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventParticipationNpcId");
		}
		else
		{
			String[] ctfNpcCoords = events.getString("FOSEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (ctfNpcCoords.length < 3)
			{
				FOS_EVENT_ENABLED = false;
				_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventParticipationNpcCoordinates");
			}
			else
			{
				FOS_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
				FOS_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
				FOS_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
				FOS_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
				if (ctfNpcCoords.length == 4)
				{
					FOS_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(ctfNpcCoords[3]);
				}
				
				// FOS_EVENT_REWARDS = new ArrayList<>();
				FOS_DOORS_ATTACKABLE = new ArrayList<>();
				
				FOS_EVENT_TEAM_1_COORDINATES = new int[3];
				FOS_EVENT_TEAM_2_COORDINATES = new int[3];
				
				FOS_EVENT_MIN_PLAYERS_IN_TEAMS = events.getInt("FOSEventMinPlayersInTeams", 1);
				FOS_EVENT_MAX_PLAYERS_IN_TEAMS = events.getInt("FOSEventMaxPlayersInTeams", 20);
				FOS_EVENT_MIN_LVL = Byte.parseByte(events.getString("FOSEventMinPlayerLevel", "1"));
				FOS_EVENT_MAX_LVL = Byte.parseByte(events.getString("FOSEventMaxPlayerLevel", "80"));
				FOS_EVENT_RESPAWN_TELEPORT_DELAY = events.getInt("FOSEventRespawnTeleportDelay", 20);
				FOS_EVENT_START_LEAVE_TELEPORT_DELAY = events.getInt("FOSEventStartLeaveTeleportDelay", 20);
				FOS_EVENT_EFFECTS_REMOVAL = events.getInt("FOSEventEffectsRemoval", 0);
				FOS_EVENT_TEAM_1_NAME = events.getString("FOSEventTeam1Name", "Team1");
				FOS_EVENT_TEAM_2_NAME = events.getString("FOSEventTeam2Name", "Team2");
				ctfNpcCoords = events.getString("FOSEventTeam1Coordinates", "0,0,0").split(",");
				if (ctfNpcCoords.length < 3)
				{
					FOS_EVENT_ENABLED = false;
					_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventTeam1Coordinates");
				}
				else
				{
					FOS_EVENT_TEAM_1_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
					FOS_EVENT_TEAM_1_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
					FOS_EVENT_TEAM_1_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
					ctfNpcCoords = events.getString("FOSEventTeam2Coordinates", "0,0,0").split(",");
					if (ctfNpcCoords.length < 3)
					{
						FOS_EVENT_ENABLED = false;
						_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventTeam2Coordinates");
					}
					else
					{
						FOS_EVENT_TEAM_2_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
						FOS_EVENT_TEAM_2_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
						FOS_EVENT_TEAM_2_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
						
						// QUARTEL 1
						if (FOS_EVENT_ARTIFACT_NPC_ID == 0)
						{
							FOS_EVENT_ENABLED = false;
							_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventSecondTeamHeadquartersId");
						}
						else
						{
							ctfNpcCoords = events.getString("FOSEventArtifactCoordinates", "0,0,0").split(",");
							if (ctfNpcCoords.length < 3)
							{
								FOS_EVENT_ENABLED = false;
								_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventTeam2FlagCoordinates");
							}
							else
							{
								FOS_EVENT_FLAG_COORDINATES = new int[4];
								FOS_EVENT_FLAG_COORDINATES[0] = Integer.parseInt(ctfNpcCoords[0]);
								FOS_EVENT_FLAG_COORDINATES[1] = Integer.parseInt(ctfNpcCoords[1]);
								FOS_EVENT_FLAG_COORDINATES[2] = Integer.parseInt(ctfNpcCoords[2]);
								if (ctfNpcCoords.length == 4)
								{
									FOS_EVENT_FLAG_COORDINATES[3] = Integer.parseInt(ctfNpcCoords[3]);
								}
							}
							
							ctfNpcCoords = events.getString("FOSEventParticipationFee", "0,0").split(",");
							try
							{
								FOS_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(ctfNpcCoords[0]);
								FOS_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(ctfNpcCoords[1]);
							}
							catch (NumberFormatException nfe)
							{
								if (ctfNpcCoords.length > 0)
								{
									_log.warning("FOSEventEngine[Config.load()]: invalid config property -> FOSEventParticipationFee");
								}
							}
							
							/*
							 * ctfNpcCoords = events.getString("FOSEventReward", "57,100000").split(";"); for (String reward : ctfNpcCoords) { String[] rewardSplit = reward.split(","); if (rewardSplit.length != 2) {
							 * _log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSEventReward \"", reward, "\"")); } else { try { FOS_EVENT_REWARDS.add(new int[] { Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1]) }); } catch
							 * (NumberFormatException nfe) { if (!reward.isEmpty()) { _log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSEventReward \"", reward, "\"")); } } } }
							 */
						}
						
						FOS_EVENT_REWARDS_WIN = parseRewards(events.getString("FOSEventRewardWinners", ""));
						FOS_EVENT_REWARDS_LOS = parseRewards(events.getString("FOSEventRewardLosers", ""));
						
						FOS_EVENT_MULTIBOX_PROTECTION_ENABLE = events.getBoolean("FOSEventMultiBoxEnable", false);
						FOS_EVENT_NUMBER_BOX_REGISTER = events.getInt("FOSEventNumberBoxRegister", 1);
						FOS_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = events.getBoolean("FOSEventTargetTeamMembersAllowed", true);
						FOS_EVENT_SCROLL_ALLOWED = events.getBoolean("FOSEventScrollsAllowed", false);
						FOS_EVENT_POTIONS_ALLOWED = events.getBoolean("FOSEventPotionsAllowed", false);
						FOS_EVENT_SUMMON_BY_ITEM_ALLOWED = events.getBoolean("FOSEventSummonByItemAllowed", false);
						FOS_REWARD_TEAM_TIE = events.getBoolean("FOSRewardTeamTie", false);
						// FOS_REWARD_NO_CARRIER_PLAYER = events.getBoolean("FOSRewardNoCarrierPlayers", false);
						ctfNpcCoords = events.getString("FOSDoorsAttackable", "").split(";");
						for (String door : ctfNpcCoords)
						{
							try
							{
								FOS_DOORS_ATTACKABLE.add(Integer.parseInt(door));
							}
							catch (NumberFormatException nfe)
							{
								if (!door.isEmpty())
								{
									_log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSDoorsAttackable \"", door, "\""));
								}
							}
						}
						
						ctfNpcCoords = events.getString("FOSEventFighterBuffs", "").split(";");
						if (!ctfNpcCoords[0].isEmpty())
						{
							FOS_EVENT_FIGHTER_BUFFS = new HashMap<>(ctfNpcCoords.length);
							for (String skill : ctfNpcCoords)
							{
								String[] skillSplit = skill.split(",");
								if (skillSplit.length != 2)
								{
									_log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSEventFighterBuffs \"", skill, "\""));
								}
								else
								{
									try
									{
										FOS_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
									}
									catch (NumberFormatException nfe)
									{
										if (!skill.isEmpty())
										{
											_log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSEventFighterBuffs \"", skill, "\""));
										}
									}
								}
							}
						}
						
						ctfNpcCoords = events.getString("FOSEventMageBuffs", "").split(";");
						if (!ctfNpcCoords[0].isEmpty())
						{
							FOS_EVENT_MAGE_BUFFS = new HashMap<>(ctfNpcCoords.length);
							for (String skill : ctfNpcCoords)
							{
								String[] skillSplit = skill.split(",");
								if (skillSplit.length != 2)
								{
									_log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSEventMageBuffs \"", skill, "\""));
								}
								else
								{
									try
									{
										FOS_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
									}
									catch (NumberFormatException nfe)
									{
										if (!skill.isEmpty())
										{
											_log.warning(StringUtil.concat("FOSEventEngine[Config.load()]: invalid config property -> FOSEventMageBuffs \"", skill, "\""));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private static void loadEventDays(String configuredDays)
	{
		FOS_EVENT_DAYS.clear();
		final String days = (configuredDays == null) ? "ALL" : configuredDays.trim();
		if (days.equalsIgnoreCase("ALL"))
		{
			return;
		}

		for (String day : days.split(","))
		{
			switch (day.trim().toUpperCase())
			{
				case "MONDAY": FOS_EVENT_DAYS.add(Calendar.MONDAY); break;
				case "TUESDAY": FOS_EVENT_DAYS.add(Calendar.TUESDAY); break;
				case "WEDNESDAY": FOS_EVENT_DAYS.add(Calendar.WEDNESDAY); break;
				case "THURSDAY": FOS_EVENT_DAYS.add(Calendar.THURSDAY); break;
				case "FRIDAY": FOS_EVENT_DAYS.add(Calendar.FRIDAY); break;
				case "SATURDAY": FOS_EVENT_DAYS.add(Calendar.SATURDAY); break;
				case "SUNDAY": FOS_EVENT_DAYS.add(Calendar.SUNDAY); break;
				case "": break;
				default: _log.warning("FOSEventEngine[Config.load()]: invalid day in FOSEventDays -> " + day.trim());
			}
		}
	}

	public static boolean isEventDay(Calendar date)
	{
		return FOS_EVENT_DAYS.isEmpty() || FOS_EVENT_DAYS.contains(date.get(Calendar.DAY_OF_WEEK));
	}
	
	private static List<RewardHolder> parseRewards(String value)
	{
		final List<RewardHolder> rewards = new ArrayList<>();
		for (String entry : value.split(";"))
		{
			if (entry.isBlank())
			{
				continue;
			}
			final String[] data = entry.split(",");
			try
			{
				final int itemId = Integer.parseInt(data[0].trim());
				final int min = Integer.parseInt(data[1].trim());
				final int max = data.length > 2 ? Integer.parseInt(data[2].trim()) : min;
				final int chance = data.length > 3 ? Integer.parseInt(data[3].trim()) : 100;
				rewards.add(new RewardHolder(itemId, min, max, chance));
			}
			catch (RuntimeException e)
			{
				_log.warning("Invalid FOS reward entry: " + entry);
			}
		}
		return rewards;
	}
}
