package org.l2jmobius.gameserver.model.events.fos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.RewardHolder;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.PartyMessageType;
import org.l2jmobius.gameserver.enums.StatusUpdateType;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.impl.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.model.events.impl.creature.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class FOSEvent
{
	enum EventState
	{
		INACTIVE,
		INACTIVATING,
		PARTICIPATING,
		STARTING,
		STARTED,
		REWARDING
	}
	
	protected static final Logger _log = Logger.getLogger(FOSEvent.class.getName());
	/** html path **/
	private static final String htmlPath = "data/html/mods/events/fos/";
	/**
	 * The teams of the FOSEvent<br>
	 */
	private static FOSEventTeam[] _teams = new FOSEventTeam[2];
	/**
	 * The state of the FOSEvent<br>
	 */
	private static EventState _state = EventState.INACTIVE;
	/**
	 * The spawn of the participation npc<br>
	 */
	
	private static Spawn _npcSpawn = null;
	/**
	 * the npc instance of the participation npc<br>
	 */
	private static Npc _lastNpcSpawn = null;
	
	/**
	 * The spawn of Team2 flag<br>
	 */
	private static Spawn _ArtifactSpawn = null;
	/**
	 * the npc instance of Team2 flag<br>
	 */
	private static Npc _lastArtifactSpawn = null;
	private static Instance _eventInstance = null;
	/** Team currently controlling the fortress. -1 means that it has not been captured yet. */
	private static volatile byte _fortressOwnerTeamId = -1;
	private static final Map<Integer, Boolean> _hadSealSkill = new HashMap<>();
	private static final Map<Integer, Boolean> _doorAttackableState = new HashMap<>();
	private static final Object LISTENER_OWNER = new Object();
	
	/**
	 * No instance of this class!<br>
	 */
	private FOSEvent()
	{
	}
	
	/**
	 * Teams initializing<br>
	 */
	public static void init()
	{
		_teams[0] = new FOSEventTeam(FOSConfig.FOS_EVENT_TEAM_1_NAME, FOSConfig.FOS_EVENT_TEAM_1_COORDINATES);
		_teams[1] = new FOSEventTeam(FOSConfig.FOS_EVENT_TEAM_2_NAME, FOSConfig.FOS_EVENT_TEAM_2_COORDINATES);
	}
	
	/**
	 * Starts the participation of the FOSEvent<br>
	 * 1. Get L2NpcTemplate by FOSConfig.FOS_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_ID);
		
		if (tmpl == null)
		{
			_log.warning("FOSEventEngine: L2EventManager is a NullPointer -> Invalid npc id in configs?");
			return false;
		}
		
		try
		{
			_npcSpawn = new Spawn(tmpl);
			
			// _npcSpawn.setLocx(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[0]);
			// _npcSpawn.setLocy(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[1]);
			// _npcSpawn.setLocz(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[2]);
			// _npcSpawn.getAmount();
			// _npcSpawn.setHeading(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setXYZ(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[0], FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[1], FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[2]);
			_npcSpawn.setHeading(FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			// _npcSpawn.init();
			// _lastNpcSpawn = _npcSpawn.getLastSpawn();
			_lastNpcSpawn = _npcSpawn.doSpawn(false);
			_lastNpcSpawn.setCurrentHp(_lastNpcSpawn.getMaxHp());
			_lastNpcSpawn.setTitle("FOS Event");
			_lastNpcSpawn.isAggressive();
			_lastNpcSpawn.broadcastPacket(new MagicSkillUse(_lastNpcSpawn, _lastNpcSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "FOSEventEngine: exception: " + e.getMessage(), e);
			return false;
		}
		
		setState(EventState.PARTICIPATING);
		return true;
	}
	
	private static int highestLevelPcInstanceOf(Map<Integer, Player> players)
	{
		int maxLevel = Integer.MIN_VALUE, maxLevelId = -1;
		for (Player player : players.values())
		{
			if (player.getLevel() >= maxLevel)
			{
				maxLevel = player.getLevel();
				maxLevelId = player.getObjectId();
			}
		}
		return maxLevelId;
	}
	
	/**
	 * Starts the FOSEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in FOSConfigs<br>
	 * 3. Abort if not enought participants(return false)<br>
	 * 4. Set state EventState.STARTED<br>
	 * 5. Teleport all participants to team spot<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startFight()
	{
		// Set state to STARTING
		setState(EventState.STARTING);
		
		// Randomize and balance team distribution
		Map<Integer, Player> allParticipants = new HashMap<>();
		allParticipants.putAll(_teams[0].getParticipatedPlayers());
		allParticipants.putAll(_teams[1].getParticipatedPlayers());
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		
		int balance[] =
		{
			0,
			0
		}, priority = 0, highestLevelPlayerId;
		Player highestLevelPlayer;
		// TODO: allParticipants should be sorted by level instead of using highestLevelPcInstanceOf for every fetch
		while (!allParticipants.isEmpty())
		{
			// Priority team gets one player
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getLevel();
			// Exiting if no more players
			if (allParticipants.isEmpty())
			{
				break;
			}
			// The other team gets one player
			// TODO: Code not dry
			priority = 1 - priority;
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getLevel();
			// Recalculating priority
			priority = balance[0] > balance[1] ? 1 : 0;
		}
		
		// Check for enought participants
		if ((_teams[0].getParticipatedPlayerCount() < FOSConfig.FOS_EVENT_MIN_PLAYERS_IN_TEAMS) || (_teams[1].getParticipatedPlayerCount() < FOSConfig.FOS_EVENT_MIN_PLAYERS_IN_TEAMS))
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);
			// Cleanup of teams
			_teams[0].cleanMe();
			_teams[1].cleanMe();
			// Unspawn the event NPC
			unSpawnNpc();
			return false;
		}
		_fortressOwnerTeamId = -1;
		// Setinstance Doors
		
		final Instance instance = InstanceManager.getInstance().createInstance();
		_eventInstance = instance;
		instanceDoors(FOSConfig.FOS_DOORS_ATTACKABLE, instance);
		healDoors(FOSConfig.FOS_DOORS_ATTACKABLE);
		if (_lastArtifactSpawn != null)
		{
			_lastArtifactSpawn.setInstance(instance);
		}
		// Spawn Artifact
		SpawnArtifact(instance);
		// Set state STARTED
		setState(EventState.STARTED);
		
		// Iterate over all teams
		for (FOSEventTeam team : _teams)
		{
			// Iterate over all participated player instances in this team
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				if (playerInstance != null)
				{
					// if (FOSConfig.ENABLE_FOS_INSTANCE)
					// playerInstance.setInstanceId(FOSConfig.FOS_INSTANCE_ID, true);
					playerInstance.setInstance(instance);
					// Teleporter implements Runnable and starts itself
					setSealOfRuler(playerInstance);
					addListeners(playerInstance);
					new FOSEventTeleporter(playerInstance, team.getCoordinates(), false, false);
				}
			}
		}
		
		return true;
	}
	
	public static boolean restartFight()
	{
		// Restore fort doors
		healDoors(FOSConfig.FOS_DOORS_ATTACKABLE);
		
		// Iterate over all teams
		for (FOSEventTeam team : _teams)
		{
			// Iterate over all participated player instances in this team
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				if (playerInstance != null)
				{
					// playerInstance.setTitle("Score: " + team.getPoints());
					// playerInstance.broadcastTitleInfo();
					playerInstance.abortCast();
					playerInstance.abortAttack();
					playerInstance.broadcastCharInfo();
					new FOSEventTeleporter(playerInstance, team.getCoordinates(), true, false);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Finishes the event by time. The team controlling the fortress wins. If nobody captured it, both teams receive only the loser reward.
	 * @return result announcement
	 */
	public static String calculateRewards()
	{
		setState(EventState.REWARDING);
		
		if ((_fortressOwnerTeamId < 0) || (_fortressOwnerTeamId >= _teams.length))
		{
			rewardTeamLos(_teams[0]);
			rewardTeamLos(_teams[1]);
			sysMsgToAllParticipants("El evento finalizo sin propietario. Ambos equipos reciben la recompensa de perdedor.");
			return "Fortress Event: finalizo sin propietario. Ambos equipos reciben la recompensa de perdedor.";
		}
		
		final FOSEventTeam winner = _teams[_fortressOwnerTeamId];
		final FOSEventTeam loser = _teams[1 - _fortressOwnerTeamId];
		rewardTeamWin(winner);
		rewardTeamLos(loser);
		
		return "Fortress Event: Team " + winner.getName() + " gano por controlar la fortaleza al finalizar el tiempo!";
	}
	
	public static FOSEventTeam[] teamsResult()
	{
		FOSEventTeam[] teams = new FOSEventTeam[2];
		if (_teams[0].getPoints() > _teams[1].getPoints())
		{
			teams[0] = _teams[0];
			teams[1] = _teams[1];
		}
		else
		{
			teams[0] = _teams[1];
			teams[1] = _teams[0];
		}
		return teams;
	}
	
	private static void rewardTeamWin(FOSEventTeam team)
	{
		// Iterate over all participated player instances of the winning team
		for (Player playerInstance : team.getParticipatedPlayers().values())
		{
			// Check for nullpointer
			if (playerInstance == null)
			{
				continue;
			}
			
			// for (RewardHolder reward : FOSConfig.FOS_EVENT_REWARDS_WIN)
			// {
			// if (Rnd.get(100) <= reward.getRewardChance())
			// {
			// if (playerInstance.isVip())
			// {
			// if (reward.getRewardId() == 5556)
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), (int) (Rnd.get(reward.getRewardMin(), reward.getRewardMax()) * Config.VIP_DROP_RATE), playerInstance, true);
			// else if (reward.getRewardId() == 5556)
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), (int) (Rnd.get(reward.getRewardMin(), reward.getRewardMax()) * Config.VIP_DROP_RATE), playerInstance, true);
			// else
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), playerInstance, true);
			// }
			// else
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), playerInstance, true);
			// }
			// }
			for (RewardHolder reward : FOSConfig.FOS_EVENT_REWARDS_WIN)
			{
				if (Rnd.get(100) <= reward.getRewardChance())
				{
					playerInstance.addItem("FOS Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), playerInstance, true);
				}
			}
			
			StatusUpdate statusUpdate = new StatusUpdate(playerInstance);
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			
			statusUpdate.addUpdate(StatusUpdateType.CUR_LOAD, playerInstance.getCurrentLoad());
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Reward.htm"));
			playerInstance.sendPacket(statusUpdate);
			playerInstance.sendPacket(npcHtmlMessage);
		}
	}
	
	private static void rewardTeamLos(FOSEventTeam team)
	{
		// Iterate over all participated player instances of the winning team
		for (Player playerInstance : team.getParticipatedPlayers().values())
		{
			// Check for nullpointer
			if (playerInstance == null)
			{
				continue;
			}
			
			// for (RewardHolder reward : FOSConfig.FOS_EVENT_REWARDS_LOS)
			// {
			// if (Rnd.get(100) <= reward.getRewardChance())
			// {
			// if (playerInstance.getInventory().getItemByItemId(9594) != null || playerInstance.getInventory().getItemByItemId(9595) != null || playerInstance.getInventory().getItemByItemId(9596) != null)
			// {
			// if (reward.getRewardId() == 5556)
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), (int) (Rnd.get(reward.getRewardMin(), reward.getRewardMax()) * Config.VIP_DROP_RATE), playerInstance, true);
			// else if (reward.getRewardId() == 5556)
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), (int) (Rnd.get(reward.getRewardMin(), reward.getRewardMax()) * Config.VIP_DROP_RATE), playerInstance, true);
			// else
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), playerInstance, true);
			// }
			// else
			// playerInstance.addItem("FOS Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), playerInstance, true);
			// }
			// }
			
			for (RewardHolder reward : FOSConfig.FOS_EVENT_REWARDS_LOS)
			{
				if (Rnd.get(100) <= reward.getRewardChance())
				{
					playerInstance.addItem("FOS Reward", reward.getRewardId(), Rnd.get(reward.getRewardMin(), reward.getRewardMax()), playerInstance, true);
				}
			}
			StatusUpdate statusUpdate = new StatusUpdate(playerInstance);
			statusUpdate.addUpdate(StatusUpdateType.CUR_LOAD, playerInstance.getCurrentLoad());
			playerInstance.sendPacket(statusUpdate);
		}
	}
	
	/**
	 * Stops the FOSEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove FOS npc from world<br>
	 * 3. Open doors specified in FOSConfigs<br>
	 * 4. Teleport all participants back to participation npc location<br>
	 * 5. Teams cleaning<br>
	 * 6. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);
		// Unspawn event npc
		unSpawnNpc();
		// Move to original instance
		instanceRemoveDoors(FOSConfig.FOS_DOORS_ATTACKABLE);
		// Restore fort doors
		healDoors(FOSConfig.FOS_DOORS_ATTACKABLE);
		
		// Iterate over all teams
		for (FOSEventTeam team : _teams)
		{
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				// Check for nullpointer
				if (playerInstance != null)
				{
					// if (FOSConfig.ENABLE_FOS_INSTANCE)
					// playerInstance.setInstanceId(0, true);
					playerInstance.setInstance(null);
					
					if (_lastArtifactSpawn != null)
					{
						_lastArtifactSpawn.setInstance(null);
					}
					
					removeSealOfRuler(playerInstance);
					removeListeners(playerInstance);
					new FOSEventTeleporter(playerInstance, FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
				}
			}
		}
		
		// Cleanup of teams
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		if (_eventInstance != null)
		{
			final Instance instance = _eventInstance;
			_eventInstance = null;
			org.l2jmobius.commons.threads.ThreadPool.schedule(instance::destroy, (FOSConfig.FOS_EVENT_START_LEAVE_TELEPORT_DELAY + 2) * 1000L);
		}
		_hadSealSkill.clear();
		_fortressOwnerTeamId = -1;
		// Set state INACTIVE
		setState(EventState.INACTIVE);
	}
	
	/**
	 * Adds a player to a FOSEvent team<br>
	 * 1. Calculate the id of the team in which the player should be added<br>
	 * 2. Add the player to the calculated team<br>
	 * <br>
	 * @param playerInstance as L2PcInstance<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static synchronized boolean addParticipant(Player playerInstance)
	{
		// Check for nullpoitner
		if ((playerInstance == null) || isPlayerParticipant(playerInstance.getObjectId()))
		{
			return false;
		}
		
		byte teamId = 0;
		
		// Check to which team the player should be added
		if (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount())
		{
			teamId = (byte) (Rnd.get(2));
		}
		else
		{
			teamId = (byte) (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount() ? 1 : 0);
		}
		
		return _teams[teamId].addPlayer(playerInstance);
	}
	
	/**
	 * Removes a FOSEvent player from it's team<br>
	 * 1. Get team id of the player<br>
	 * 2. Remove player from it's team<br>
	 * <br>
	 * @param playerObjectId
	 * @return boolean: true if success, otherwise false
	 */
	public static boolean removeParticipant(int playerObjectId)
	{
		// Get the teamId of the player
		byte teamId = getParticipantTeamId(playerObjectId);
		
		// Check if the player is participant
		if (teamId != -1)
		{
			// Remove event-owned listeners and the temporary capture skill as well.
			final Player player = _teams[teamId].getParticipatedPlayers().get(playerObjectId);
			if (player != null)
			{
				removeListeners(player);
				if (_hadSealSkill.containsKey(playerObjectId))
				{
					removeSealOfRuler(player);
				}
				player.setTeam(org.l2jmobius.gameserver.enums.Team.NONE);
			}
			_teams[teamId].removePlayer(playerObjectId);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send a SystemMessage to all participated players<br>
	 * 1. Send the message to all players of team number one<br>
	 * 2. Send the message to all players of team number two<br>
	 * <br>
	 * @param message as String<br>
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		CreatureSay cs = new CreatureSay(null, ChatType.PARTY, "FOS Manager", message);
		
		for (Player playerInstance : _teams[0].getParticipatedPlayers().values())
		{
			if (playerInstance != null)
			{
				playerInstance.sendPacket(cs);
			}
		}
		
		for (Player playerInstance : _teams[1].getParticipatedPlayers().values())
		{
			if (playerInstance != null)
			{
				playerInstance.sendPacket(cs);
			}
		}
	}
	
	private static void SpawnArtifact(Instance instance)
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(FOSConfig.FOS_EVENT_ARTIFACT_NPC_ID);
		
		if (tmpl == null)
		{
			_log.warning("FOSEventEngine: Second Head Quater is a NullPointer -> Invalid npc id in configs?");
			return;
		}
		
		try
		{
			_ArtifactSpawn = new Spawn(tmpl);
			
			// _ArtifactSpawn.setLocx(FOSConfig.FOS_EVENT_FLAG_COORDINATES[0]);
			// _ArtifactSpawn.setLocy(FOSConfig.FOS_EVENT_FLAG_COORDINATES[1]);
			// _ArtifactSpawn.setLocz(FOSConfig.FOS_EVENT_FLAG_COORDINATES[2]);
			// _ArtifactSpawn.getAmount();
			// _ArtifactSpawn.setHeading(FOSConfig.FOS_EVENT_FLAG_COORDINATES[3]);
			_ArtifactSpawn.setXYZ(FOSConfig.FOS_EVENT_FLAG_COORDINATES[0], FOSConfig.FOS_EVENT_FLAG_COORDINATES[1], FOSConfig.FOS_EVENT_FLAG_COORDINATES[2]);
			_ArtifactSpawn.setHeading(FOSConfig.FOS_EVENT_FLAG_COORDINATES[3]);
			_ArtifactSpawn.setInstanceId(instance.getId());
			// _ArtifactSpawn.init();
			// _lastArtifactSpawn = _ArtifactSpawn.getLastSpawn();
			_lastArtifactSpawn = _ArtifactSpawn.doSpawn(false);
			_lastArtifactSpawn.setCurrentHp(_lastArtifactSpawn.getMaxHp());
			_lastArtifactSpawn.setTitle("Event Flag");
			_lastArtifactSpawn.isAggressive();
			// _lastArtifactSpawn.setInstanceId(FOSConfig.FOS_INSTANCE_ID, false); // Set instance first
			_lastArtifactSpawn.broadcastPacket(new MagicSkillUse(_lastArtifactSpawn, _lastArtifactSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "SpawnArtifact: exception: " + e.getMessage(), e);
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * UnSpawns the FOSEvent npc
	 */
	private static void unSpawnNpc()
	{
		// Delete the npc
		if (_lastNpcSpawn != null)
		{
			_lastNpcSpawn.deleteMe();
		}
		_npcSpawn = null;
		_lastNpcSpawn = null;
		
		// Remove Artifact
		if (_lastArtifactSpawn != null)
		{
			_lastArtifactSpawn.deleteMe();
			_ArtifactSpawn = null;
			_lastArtifactSpawn = null;
		}
	}
	
	/**
	 * Called when a player logs in<br>
	 * <br>
	 * @param playerInstance as L2PcInstance<br>
	 */
	public static void onLogin(Player playerInstance)
	{
		if ((playerInstance == null) || (!isStarting() && !isStarted()))
		{
			return;
		}
		
		byte teamId = getParticipantTeamId(playerInstance.getObjectId());
		
		if (teamId == -1)
		{
			return;
		}
		
		_teams[teamId].addPlayer(playerInstance);
		
		// if (FOSConfig.ENABLE_FOS_INSTANCE)
		// {
		// playerInstance.noCarrierUnparalyze();
		// playerInstance.setInstanceId(FOSConfig.FOS_INSTANCE_ID, true);
		// }
		
		new FOSEventTeleporter(playerInstance, _teams[teamId].getCoordinates(), true, false);
	}
	
	/**
	 * Called when a player logs out<br>
	 * <br>
	 * @param playerInstance as L2PcInstance<br>
	 */
	public static void onLogout(Player playerInstance)
	{
		if ((playerInstance != null) && (isStarting() || isStarted() || isParticipating()))
		{
			// if (playerInstance.isNoCarrier())
			// return;
			
			/*
			 * if (removeParticipant(playerInstance.getObjectId())) { playerInstance.teleToLocation((FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)) - 50, (FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)) - 50,
			 * FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0); playerInstance.setTitle(playerInstance._originalTitle); playerInstance.broadcastTitleInfo(); }
			 */
		}
	}
	
	/**
	 * Called on every bypass by npc of type L2TvTEventNpc Needs synchronization cause of the max player check
	 * @param command as String
	 * @param playerInstance as L2PcInstance
	 */
	public static synchronized void onBypass(String command, Player playerInstance)
	{
		if ((playerInstance == null) || !isParticipating())
		{
			return;
		}
		
		final String htmContent;
		
		if (command.equals("fos_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = playerInstance.getLevel();
			
			if (playerInstance.isCursedWeaponEquipped())
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (playerInstance.isInArenaEvent())
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Tournament.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (OlympiadManager.getInstance().isRegistered(playerInstance))
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Olympiad.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (playerInstance.getReputation() < 0)
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Karma.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if ((playerLevel < FOSConfig.FOS_EVENT_MIN_LVL) || (playerLevel > FOSConfig.FOS_EVENT_MAX_LVL))
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(FOSConfig.FOS_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(FOSConfig.FOS_EVENT_MAX_LVL));
				}
			}
			else if ((_teams[0].getParticipatedPlayerCount() == FOSConfig.FOS_EVENT_MAX_PLAYERS_IN_TEAMS) && (_teams[1].getParticipatedPlayerCount() == FOSConfig.FOS_EVENT_MAX_PLAYERS_IN_TEAMS))
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "TeamsFull.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(FOSConfig.FOS_EVENT_MAX_PLAYERS_IN_TEAMS));
				}
			}
			else if (FOSConfig.FOS_EVENT_MULTIBOX_PROTECTION_ENABLE && onMultiBoxRestriction(playerInstance))
			{
				htmContent = HtmCache.getInstance().getHtm(playerInstance, htmlPath + "MultiBox.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%maxbox%", String.valueOf(FOSConfig.FOS_EVENT_NUMBER_BOX_REGISTER));
				}
			}
			else if ((FOSConfig.FOS_EVENT_PARTICIPATION_FEE[0] > 0) && (FOSConfig.FOS_EVENT_PARTICIPATION_FEE[1] > 0) && !playerInstance.destroyItemByItemId("FOS participation", FOSConfig.FOS_EVENT_PARTICIPATION_FEE[0], FOSConfig.FOS_EVENT_PARTICIPATION_FEE[1], playerInstance, false))
			{
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(playerInstance, htmlPath + "ParticipationFee.htm"));
				npcHtmlMessage.replace("%fee%", FOSConfig.FOS_EVENT_PARTICIPATION_FEE[1] + " item(s) ID " + FOSConfig.FOS_EVENT_PARTICIPATION_FEE[0]);
			}
			else if (addParticipant(playerInstance))
			{
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Registered.htm"));
			}
			else
			{
				return;
			}
			
			playerInstance.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("fos_event_remove_participation"))
		{
			removeParticipant(playerInstance.getObjectId());
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(playerInstance, htmlPath + "Unregistered.htm"));
			playerInstance.sendPacket(npcHtmlMessage);
		}
	}
	
	/**
	 * Called on every onAction in L2PcIstance<br>
	 * <br>
	 * @param playerInstance
	 * @param targetedPlayerObjectId
	 * @return boolean: true if player is allowed to target, otherwise false
	 */
	public static boolean onAction(Player playerInstance, int targetedPlayerObjectId)
	{
		if ((playerInstance == null) || !isStarted())
		{
			return true;
		}
		
		if (playerInstance.isGM())
		{
			return true;
		}
		
		byte playerTeamId = getParticipantTeamId(playerInstance.getObjectId());
		byte targetedPlayerTeamId = getParticipantTeamId(targetedPlayerObjectId);
		
		if (((playerTeamId != -1) && (targetedPlayerTeamId == -1)) || ((playerTeamId == -1) && (targetedPlayerTeamId != -1)))
		{
			playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if ((playerTeamId != -1) && (targetedPlayerTeamId != -1) && (playerTeamId == targetedPlayerTeamId) && (playerInstance.getObjectId() != targetedPlayerObjectId) && !FOSConfig.FOS_EVENT_TARGET_TEAM_MEMBERS_ALLOWED)
		{
			playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Called on every scroll use<br>
	 * <br>
	 * @param playerObjectId
	 * @return boolean: true if player is allowed to use scroll, otherwise false
	 */
	public static boolean onScrollUse(int playerObjectId)
	{
		if (!isStarted())
		{
			return true;
		}
		
		if (isPlayerParticipant(playerObjectId) && !FOSConfig.FOS_EVENT_SCROLL_ALLOWED)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Called on every potion use
	 * @param playerObjectId
	 * @return boolean: true if player is allowed to use potions, otherwise false
	 */
	public static boolean onPotionUse(int playerObjectId)
	{
		if (!isStarted())
		{
			return true;
		}
		
		if (isPlayerParticipant(playerObjectId) && !FOSConfig.FOS_EVENT_POTIONS_ALLOWED)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Called on every escape use(thanks to nbd)
	 * @param playerObjectId
	 * @return boolean: true if player is not in FOS event, otherwise false
	 */
	public static boolean onEscapeUse(int playerObjectId)
	{
		if (!isStarted())
		{
			return true;
		}
		
		if (isPlayerParticipant(playerObjectId))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Called on every summon item use
	 * @param playerObjectId
	 * @return boolean: true if player is allowed to summon by item, otherwise false
	 */
	public static boolean onItemSummon(int playerObjectId)
	{
		if (!isStarted())
		{
			return true;
		}
		
		if (isPlayerParticipant(playerObjectId) && !FOSConfig.FOS_EVENT_SUMMON_BY_ITEM_ALLOWED)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Is called when a player is killed<br>
	 * <br>
	 * @param killerCharacter as L2Character<br>
	 * @param killedPlayerInstance as L2PcInstance<br>
	 */
	public static void onKill(Creature killerCharacter, Player killedPlayerInstance)
	{
		if ((killedPlayerInstance == null) || !isStarted())
		{
			return;
		}
		
		byte killedTeamId = getParticipantTeamId(killedPlayerInstance.getObjectId());
		
		if (killedTeamId == -1)
		{
			return;
		}
		
		new FOSEventTeleporter(killedPlayerInstance, _teams[killedTeamId].getCoordinates(), false, false);
		
		if (killerCharacter == null)
		{
			return;
		}
		
		Player killerPlayerInstance = null;
		
		if ((killerCharacter instanceof Pet) || (killerCharacter instanceof Servitor))
		{
			killerPlayerInstance = ((Summon) killerCharacter).getOwner();
			if (killerPlayerInstance == null)
			{
				return;
			}
		}
		else if (killerCharacter instanceof Player)
		{
			killerPlayerInstance = (Player) killerCharacter;
		}
		else
		{
			return;
		}
		
		byte killerTeamId = getParticipantTeamId(killerPlayerInstance.getObjectId());
		
		if ((killerTeamId != -1) && (killedTeamId != -1) && (killerTeamId != killedTeamId))
		{
			sysMsgToAllParticipants(killerPlayerInstance.getName() + " Hunted Player " + killedPlayerInstance.getName() + "!");
		}
	}
	
	/**
	 * Called on Appearing packet received (player finished teleporting)
	 * @param playerInstance
	 */
	public static void onTeleported(Player playerInstance)
	{
		if (!isStarted() || (playerInstance == null) || !isPlayerParticipant(playerInstance.getObjectId()))
		{
			return;
		}
		
		if (playerInstance.isMageClass())
		{
			if ((FOSConfig.FOS_EVENT_MAGE_BUFFS != null) && !FOSConfig.FOS_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : FOSConfig.FOS_EVENT_MAGE_BUFFS.keySet())
				{
					Skill skill = SkillData.getInstance().getSkill(i, FOSConfig.FOS_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
					{
						skill.applyEffects(playerInstance, playerInstance);
					}
				}
			}
		}
		else
		{
			if ((FOSConfig.FOS_EVENT_FIGHTER_BUFFS != null) && !FOSConfig.FOS_EVENT_FIGHTER_BUFFS.isEmpty())
			{
				for (int i : FOSConfig.FOS_EVENT_FIGHTER_BUFFS.keySet())
				{
					Skill skill = SkillData.getInstance().getSkill(i, FOSConfig.FOS_EVENT_FIGHTER_BUFFS.get(i));
					if (skill != null)
					{
						skill.applyEffects(playerInstance, playerInstance);
					}
				}
			}
		}
		removeParty(playerInstance);
	}
	
	/**
	 * @param source
	 * @param target
	 * @param skill
	 * @return true if player valid for skill
	 */
	public static final boolean checkForFOSSkill(Player source, Player target, Skill skill)
	{
		if (!isStarted())
		{
			return true;
		}
		
		// FOS is started
		final int sourcePlayerId = source.getObjectId();
		final int targetPlayerId = target.getObjectId();
		final boolean isSourceParticipant = isPlayerParticipant(sourcePlayerId);
		final boolean isTargetParticipant = isPlayerParticipant(targetPlayerId);
		
		// both players not participating
		if (!isSourceParticipant && !isTargetParticipant)
		{
			return true;
		}
		
		// one player not participating
		if (!(isSourceParticipant && isTargetParticipant))
		{
			return false;
		}
		
		// players in the different teams ?
		if (getParticipantTeamId(sourcePlayerId) != getParticipantTeamId(targetPlayerId))
		{
			if (!skill.isBad())
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Sets the FOSEvent state<br>
	 * <br>
	 * @param state as EventState<br>
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}
	
	/**
	 * Is FOSEvent inactive?<br>
	 * <br>
	 * @return boolean: true if event is inactive(waiting for next event cycle), otherwise false<br>
	 */
	public static boolean isInactive()
	{
		boolean isInactive;
		
		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}
		
		return isInactive;
	}
	
	/**
	 * Is FOSEvent in inactivating?<br>
	 * <br>
	 * @return boolean: true if event is in inactivating progress, otherwise false<br>
	 */
	public static boolean isInactivating()
	{
		boolean isInactivating;
		
		synchronized (_state)
		{
			isInactivating = _state == EventState.INACTIVATING;
		}
		
		return isInactivating;
	}
	
	/**
	 * Is FOSEvent in participation?<br>
	 * <br>
	 * @return boolean: true if event is in participation progress, otherwise false<br>
	 */
	public static boolean isParticipating()
	{
		boolean isParticipating;
		
		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}
		
		return isParticipating;
	}
	
	/**
	 * Is FOSEvent starting?<br>
	 * <br>
	 * @return boolean: true if event is starting up(setting up fighting spot, teleport players etc.), otherwise false<br>
	 */
	public static boolean isStarting()
	{
		boolean isStarting;
		
		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}
		
		return isStarting;
	}
	
	/**
	 * Is FOSEvent started?<br>
	 * <br>
	 * @return boolean: true if event is started, otherwise false<br>
	 */
	public static boolean isStarted()
	{
		boolean isStarted;
		
		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}
		
		return isStarted;
	}
	
	/**
	 * Is FOSEvent rewarding?<br>
	 * <br>
	 * @return boolean: true if event is currently rewarding, otherwise false<br>
	 */
	public static boolean isRewarding()
	{
		boolean isRewarding;
		
		synchronized (_state)
		{
			isRewarding = _state == EventState.REWARDING;
		}
		
		return isRewarding;
	}
	
	/**
	 * Returns the team id of a player, if player is not participant it returns -1
	 * @param playerObjectId
	 * @return byte: team name of the given playerName, if not in event -1
	 */
	public static byte getParticipantTeamId(int playerObjectId)
	{
		return (byte) (_teams[0].containsPlayer(playerObjectId) ? 0 : (_teams[1].containsPlayer(playerObjectId) ? 1 : -1));
	}
	
	/**
	 * Returns the team of a player, if player is not participant it returns null
	 * @param playerObjectId
	 * @return FOSEventTeam: team of the given playerObjectId, if not in event null
	 */
	public static FOSEventTeam getParticipantTeam(int playerObjectId)
	{
		return (_teams[0].containsPlayer(playerObjectId) ? _teams[0] : (_teams[1].containsPlayer(playerObjectId) ? _teams[1] : null));
	}
	
	/**
	 * Returns the enemy team of a player, if player is not participant it returns null
	 * @param playerObjectId
	 * @return FOSEventTeam: enemy team of the given playerObjectId, if not in event null
	 */
	public static FOSEventTeam getParticipantEnemyTeam(int playerObjectId)
	{
		return (_teams[0].containsPlayer(playerObjectId) ? _teams[1] : (_teams[1].containsPlayer(playerObjectId) ? _teams[0] : null));
	}
	
	/**
	 * Returns the team coordinates in which the player is in, if player is not in a team return null
	 * @param playerObjectId
	 * @return int[]: coordinates of teams, 2 elements, index 0 for team 1 and index 1 for team 2
	 */
	public static int[] getParticipantTeamCoordinates(int playerObjectId)
	{
		return _teams[0].containsPlayer(playerObjectId) ? _teams[0].getCoordinates() : (_teams[1].containsPlayer(playerObjectId) ? _teams[1].getCoordinates() : null);
	}
	
	/**
	 * Is given player participant of the event?
	 * @param playerObjectId
	 * @return boolean: true if player is participant, ohterwise false
	 */
	public static boolean isPlayerParticipant(int playerObjectId)
	{
		if (!isParticipating() && !isStarting() && !isStarted())
		{
			return false;
		}
		
		return _teams[0].containsPlayer(playerObjectId) || _teams[1].containsPlayer(playerObjectId);
	}
	
	/**
	 * Returns participated player count<br>
	 * <br>
	 * @return int: amount of players registered in the event<br>
	 */
	public static int getParticipatedPlayersCount()
	{
		if (!isParticipating() && !isStarting() && !isStarted())
		{
			return 0;
		}
		
		return _teams[0].getParticipatedPlayerCount() + _teams[1].getParticipatedPlayerCount();
	}
	
	/**
	 * Returns teams names<br>
	 * <br>
	 * @return String[]: names of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static String[] getTeamNames()
	{
		return new String[]
		{
			_teams[0].getName(),
			_teams[1].getName()
		};
	}
	
	/**
	 * Returns player count of both teams<br>
	 * <br>
	 * @return int[]: player count of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static int[] getTeamsPlayerCounts()
	{
		return new int[]
		{
			_teams[0].getParticipatedPlayerCount(),
			_teams[1].getParticipatedPlayerCount()
		};
	}
	
	/**
	 * Returns points count of both teams
	 * @return int[]: points of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static int[] getTeamsPoints()
	{
		return new int[]
		{
			_teams[0].getPoints(),
			_teams[1].getPoints()
		};
	}
	
	public static boolean isDoorAttackable(int id, Creature attacker)
	{
		for (int doorId : FOSConfig.FOS_DOORS_ATTACKABLE)
		{
			if (doorId != id)
			{
				continue;
			}
			
			if ((attacker instanceof Player) && isPlayerParticipant(((Player) attacker).getObjectId()))
			{
				return true;
			}
			else if ((attacker instanceof Summon) && isPlayerParticipant(((Summon) attacker).getOwner().getObjectId()))
			{
				return true;
			}
			else if ((attacker instanceof Pet) && isPlayerParticipant(((Pet) attacker).getOwner().getObjectId()))
			{
				return true;
			}
		}
		return false;
	}
	
	private static void instanceDoors(List<Integer> doors, Instance instance)
	{
		for (int doorId : doors)
		{
			Door door = DoorData.getInstance().getDoor(doorId);
			if (door != null)
			{
				_doorAttackableState.putIfAbsent(doorId, door.isAttackableDoor());
				door.setIsAttackableDoor(true);
				door.setInstance(instance);
			}
		}
	}
	
	private static void instanceRemoveDoors(List<Integer> doors)
	{
		for (int doorId : doors)
		{
			Door door = DoorData.getInstance().getDoor(doorId);
			
			if (door != null)
			{
				door.setInstance(null);
				door.setIsAttackableDoor(_doorAttackableState.getOrDefault(doorId, false));
			}
		}
		_doorAttackableState.clear();
	}
	
	private static void healDoors(List<Integer> doors)
	{
		for (int doorId : doors)
		{
			Door doorInstance = DoorData.getInstance().getDoor(doorId);
			
			if (doorInstance != null)
			{
				if (doorInstance.isDead())
				{
					doorInstance.doRevive();
				}
				
				doorInstance.setMeshIndex(1);
				doorInstance.getStatus().setCurrentHp(doorInstance.getMaxHp());
				doorInstance.broadcastStatusUpdate();
			}
		}
	}
	
	public static boolean checkIfOkToCastSealOfRule(Player player)
	{
		if ((Math.abs(player.getZ() - FOSConfig.FOS_EVENT_FLAG_COORDINATES[2]) > 100) || (_lastArtifactSpawn == null) || !player.isInsideRadius(_lastArtifactSpawn, 180, true, false))
		{
			return false;
		}
		
		if ((player.getTarget() == _lastArtifactSpawn) && isPlayerParticipant(player.getObjectId()))
		{
			return true;
		}
		
		return false;
	}
	
	private static void addListeners(Player player)
	{
		removeListeners(player);
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onKill(event.getAttacker(), player), LISTENER_OWNER));
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onLogout(event.getPlayer()), LISTENER_OWNER));
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onSealCast(event), LISTENER_OWNER));
	}
	
	private static void removeListeners(Player player)
	{
		player.removeListenerIf(EventType.ON_CREATURE_DEATH, listener -> listener.getOwner() == LISTENER_OWNER);
		player.removeListenerIf(EventType.ON_PLAYER_LOGOUT, listener -> listener.getOwner() == LISTENER_OWNER);
		player.removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == LISTENER_OWNER);
	}
	
	private static synchronized void onSealCast(OnCreatureSkillFinishCast event)
	{
		if (!isStarted() || !event.getCaster().isPlayer() || (event.getSkill() == null) || (event.getSkill().getId() != FOSConfig.FOS_EVENT_SUMMON_SKILL_ID) || (event.getTarget() != _lastArtifactSpawn))
		{
			return;
		}
		final Player player = event.getCaster().asPlayer();
		if (!checkIfOkToCastSealOfRule(player))
		{
			return;
		}
		final byte teamId = getParticipantTeamId(player.getObjectId());
		if ((teamId < 0) || (_fortressOwnerTeamId == teamId))
		{
			return;
		}
		_fortressOwnerTeamId = teamId;
		sysMsgToAllParticipants(player.getName() + " captured the fortress for " + _teams[teamId].getName() + "!");
		broadcastScreenMessage("Fortress owner: " + _teams[teamId].getName(), 10);
		// Also on the first capture, return only the opposing team to its spawn.
		// The capturing team stays in place and the event timer continues unchanged.
		final FOSEventTeam displacedTeam = _teams[1 - teamId];
		for (Player participant : displacedTeam.getParticipatedPlayers().values())
		{
			if (participant != null)
			{
				participant.abortCast();
				participant.abortAttack();
				new FOSEventTeleporter(participant, displacedTeam.getCoordinates(), true, false);
			}
		}
	}
	
	public static void setSealOfRuler(Player player)
	{
		try
		{
			Skill sealOfRuler = SkillData.getInstance().getSkill(FOSConfig.FOS_EVENT_SUMMON_SKILL_ID, 1);
			
			final boolean alreadyKnown = player.getAllSkills().stream().anyMatch(skill -> skill.getId() == FOSConfig.FOS_EVENT_SUMMON_SKILL_ID);
			_hadSealSkill.put(player.getObjectId(), alreadyKnown);
			if (!alreadyKnown)
			{
				player.addSkill(sealOfRuler, false);
			}
			
			player.sendSkillList();
			player.sendMessage("You have been given the Seal Of Ruler skill for this event.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void removeSealOfRuler(Player player)
	{
		try
		{
			Skill sealOfRuler = SkillData.getInstance().getSkill(FOSConfig.FOS_EVENT_SUMMON_SKILL_ID, 1);
			if (!_hadSealSkill.getOrDefault(player.getObjectId(), false))
			{
				player.removeSkill(sealOfRuler, false);
				player.sendSkillList();
			}
			_hadSealSkill.remove(player.getObjectId());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Broadcast a message to all participant screens
	 * @param message String
	 * @param duration int (in seconds)
	 */
	public static void broadcastScreenMessage(String message, int duration)
	{
		for (FOSEventTeam team : _teams)
		{
			for (Player playerInstance : team.getParticipatedPlayers().values())
			{
				if (playerInstance != null)
				{
					playerInstance.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				}
			}
		}
	}
	
	public static void removeParty(Player activeChar)
	{
		if (activeChar.getParty() != null)
		{
			Party party = activeChar.getParty();
			party.removePartyMember(activeChar, PartyMessageType.LEFT);
		}
	}
	
	public static List<Player> allParticipants()
	{
		List<Player> players = new ArrayList<>();
		players.addAll(_teams[0].getParticipatedPlayers().values());
		players.addAll(_teams[1].getParticipatedPlayers().values());
		return players;
	}
	
	public static boolean onMultiBoxRestriction(Player activeChar)
	{
		if ((activeChar == null) || (activeChar.getClient() == null))
		{
			return true;
		}
		
		final int maximumBoxes = Math.max(1, FOSConfig.FOS_EVENT_NUMBER_BOX_REGISTER);
		final ClientHardwareInfoHolder activeHardware = activeChar.getClient().getHardwareInfo();
		final String activeIp = activeChar.getClient().getHostAddress();
		int registeredBoxes = 0;
		
		for (Player participant : allParticipants())
		{
			if ((participant == null) || (participant.getClient() == null))
			{
				continue;
			}
			
			final ClientHardwareInfoHolder participantHardware = participant.getClient().getHardwareInfo();
			
			final boolean sameComputer;
			
			if ((activeHardware != null) && (participantHardware != null))
			{
				sameComputer = activeHardware.equals(participantHardware);
			}
			else
			{
				sameComputer = !activeIp.isEmpty() && activeIp.equals(participant.getClient().getHostAddress());
			}
			
			if (sameComputer && (++registeredBoxes >= maximumBoxes))
			{
				return true;
			}
		}
		
		return false;
	}
}
