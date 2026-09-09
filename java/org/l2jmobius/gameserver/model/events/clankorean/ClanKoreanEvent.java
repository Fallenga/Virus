package org.l2jmobius.gameserver.model.events.clankorean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.util.Broadcast;

public final class ClanKoreanEvent
{
	private static final Logger LOGGER = Logger.getLogger(ClanKoreanEvent.class.getName());
	public static final int TEAM_SIZE = 5;
	
	public enum State
	{
		INACTIVE,
		REGISTRATION,
		PREPARING,
		FIGHTING
	}
	
	private final Map<Integer, Team> registeredTeams = new ConcurrentHashMap<>();
	private final List<Team> registrationOrder = new ArrayList<>();
	private final List<Team> pendingTeams = new ArrayList<>();
	private final List<Arena> arenas = new ArrayList<>();
	private final Map<Integer, Match> activeMatches = new ConcurrentHashMap<>();
	private final Map<Integer, List<Integer>> selections = new ConcurrentHashMap<>();
	private volatile State state = State.INACTIVE;
	private volatile Spawn npcSpawn;
	
	private ClanKoreanEvent()
	{
	}
	
	public static ClanKoreanEvent getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public static void init()
	{
		ClanKoreanConfig.load();
		if (ClanKoreanConfig.ENABLED)
		{
			getInstance().scheduleNextEvent();
			LOGGER.info("Clan Korean: Sistema cargado.");
		}
	}
	
	public State getState()
	{
		return state;
	}
	
	public int getRegisteredTeamCount()
	{
		return registeredTeams.size();
	}
	
	public boolean isRegistered(Player player)
	{
		return (player != null) && registeredTeams.values().stream().anyMatch(team -> team.memberIds.contains(player.getObjectId()));
	}
	
	public List<Player> getOnlineClanMembers(Player leader)
	{
		final List<Player> result = new ArrayList<>();
		if ((leader == null) || (leader.getClan() == null))
		{
			return result;
		}
		final int clanId = leader.getClanId();
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline() && (player.getClanId() == clanId))
			{
				result.add(player);
			}
		}
		result.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
		return result;
	}
	
	public List<Integer> getSelection(Player leader)
	{
		if (!isClanLeader(leader))
		{
			return new ArrayList<>();
		}
		return selections.computeIfAbsent(leader.getObjectId(), key ->
		{
			final List<Integer> selected = new ArrayList<>();
			selected.add(leader.getObjectId());
			return selected;
		});
	}
	
	public synchronized String toggleMember(Player leader, int objectId)
	{
		if (state != State.REGISTRATION)
		{
			return "El registro no esta disponible.";
		}
		if (!isClanLeader(leader))
		{
			return "Solamente el lider del clan puede seleccionar el equipo.";
		}
		if (leader.getClan().getLevel() < ClanKoreanConfig.MINIMUM_CLAN_LEVEL)
		{
			return "El clan debe ser nivel " + ClanKoreanConfig.MINIMUM_CLAN_LEVEL + " o superior.";
		}
		if (objectId == leader.getObjectId())
		{
			return "El lider forma parte obligatoriamente del equipo.";
		}
		final Player member = World.getInstance().getPlayer(objectId);
		if ((member == null) || !member.isOnline() || (member.getClanId() != leader.getClanId()))
		{
			return "El jugador no esta conectado o ya no pertenece al clan.";
		}
		final List<Integer> selected = getSelection(leader);
		if (selected.remove(Integer.valueOf(objectId)))
		{
			return member.getName() + " fue quitado del equipo.";
		}
		if (selected.size() >= TEAM_SIZE)
		{
			return "El equipo ya tiene cinco integrantes.";
		}
		selected.add(objectId);
		return member.getName() + " fue agregado al equipo.";
	}
	
	public synchronized String registerTeam(Player leader)
	{
		if (state != State.REGISTRATION)
		{
			return "El registro no esta disponible.";
		}
		if (!isClanLeader(leader))
		{
			return "Solamente el lider del clan puede registrar el equipo.";
		}
		if (leader.getClan().getLevel() < ClanKoreanConfig.MINIMUM_CLAN_LEVEL)
		{
			return "El clan debe ser nivel " + ClanKoreanConfig.MINIMUM_CLAN_LEVEL + " o superior.";
		}
		if (registeredTeams.containsKey(leader.getClanId()))
		{
			return "Tu clan ya tiene un equipo registrado.";
		}
		final List<Integer> selected = new ArrayList<>(getSelection(leader));
		if (selected.size() != TEAM_SIZE)
		{
			return "Debes seleccionar exactamente cinco integrantes contando al lider.";
		}
		final Set<Integer> unique = new HashSet<>(selected);
		if (unique.size() != TEAM_SIZE)
		{
			return "La seleccion contiene integrantes repetidos.";
		}
		final Set<ClientHardwareInfoHolder> selectedHardware = new HashSet<>();
		for (int objectId : selected)
		{
			final Player member = World.getInstance().getPlayer(objectId);
			final String error = validateMember(member, leader.getClanId());
			if (error != null)
			{
				return error;
			}
			if (isRegistered(member))
			{
				return member.getName() + " ya esta registrado en otro equipo.";
			}
			if (!ClanKoreanConfig.ALLOW_SAME_HWID)
			{
				final ClientHardwareInfoHolder hardwareInfo = getHardwareInfo(member);
				if (hardwareInfo == null)
				{
					return member.getName() + " no posee informacion de hardware valida.";
				}
				if (!selectedHardware.add(hardwareInfo))
				{
					return "No se permite registrar dos personajes desde la misma computadora.";
				}
				if (isHardwareRegistered(hardwareInfo))
				{
					return member.getName() + " ya participa con otro personaje desde la misma computadora.";
				}
			}
		}
		final Team team = new Team(leader.getClanId(), leader.getClan().getName(), leader.getObjectId(), selected);
		registeredTeams.put(team.clanId, team);
		registrationOrder.add(team);
		for (int objectId : selected)
		{
			final Player member = World.getInstance().getPlayer(objectId);
			member.setArenaProtection(true);
			member.sendMessage("Clan Korean: Tu equipo fue registrado. Orden de combate: " + (selected.indexOf(objectId) + 1) + ".");
		}
		return "Equipo registrado correctamente.";
	}
	
	public synchronized String unregisterTeam(Player leader)
	{
		if ((state != State.REGISTRATION) || !isClanLeader(leader))
		{
			return "No puedes cancelar el registro en este momento.";
		}
		final Team team = registeredTeams.remove(leader.getClanId());
		if (team == null)
		{
			return "Tu clan no tiene un equipo registrado.";
		}
		registrationOrder.remove(team);
		clearProtection(team);
		return "El equipo fue retirado del evento.";
	}
	
	private boolean isClanLeader(Player player)
	{
		return (player != null) && (player.getClan() != null) && (player.getClan().getLeaderId() == player.getObjectId());
	}
	
	private String validateMember(Player player, int clanId)
	{
		if ((player == null) || !player.isOnline())
		{
			return "Uno de los integrantes seleccionados se desconecto.";
		}
		if (player.getClanId() != clanId)
		{
			return player.getName() + " ya no pertenece al clan.";
		}
		if (player.isDead() || player.isJailed() || player.isInStoreMode() || player.inObserverMode() || player.isCursedWeaponEquipped())
		{
			return player.getName() + " no cumple los requisitos para participar.";
		}
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			return player.getName() + " esta registrado en Olympiad.";
		}
		if (player.isArenaProtection() || player.isInArenaEvent() || player.isArenaAttack())
		{
			return player.getName() + " ya participa en otro evento.";
		}
		return null;
	}
	
	private ClientHardwareInfoHolder getHardwareInfo(Player player)
	{
		return ((player == null) || (player.getClient() == null)) ? null : player.getClient().getHardwareInfo();
	}
	
	private boolean isHardwareRegistered(ClientHardwareInfoHolder hardwareInfo)
	{
		if (hardwareInfo == null)
		{
			return false;
		}
		for (Team team : registeredTeams.values())
		{
			for (int objectId : team.memberIds)
			{
				final Player registered = World.getInstance().getPlayer(objectId);
				final ClientHardwareInfoHolder registeredHardware = getHardwareInfo(registered);
				if ((registeredHardware != null) && registeredHardware.equals(hardwareInfo))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public synchronized void startRegistration()
	{
		if (state != State.INACTIVE)
		{
			return;
		}
		registeredTeams.clear();
		registrationOrder.clear();
		pendingTeams.clear();
		activeMatches.clear();
		arenas.clear();
		selections.clear();
		state = State.REGISTRATION;
		spawnNpc();
		Broadcast.toAllOnlinePlayers("Clan Korean 5x5: Registro abierto durante " + ClanKoreanConfig.REGISTRATION_MINUTES + " minutos.");
		ThreadPool.schedule(this::closeRegistration, ClanKoreanConfig.REGISTRATION_MINUTES * 60L * 1000L);
	}
	
	private synchronized void closeRegistration()
	{
		if (state != State.REGISTRATION)
		{
			return;
		}
		unspawnNpc();
		for (Team team : new ArrayList<>(registrationOrder))
		{
			if (validateTeam(team) != null)
			{
				registrationOrder.remove(team);
				registeredTeams.remove(team.clanId);
				clearProtection(team);
			}
		}
		if (registrationOrder.size() < 2)
		{
			Broadcast.toAllOnlinePlayers("Clan Korean: Evento cancelado. Se necesitan al menos dos clanes validos.");
			resetEvent();
			return;
		}
		for (int i = 0; i < ClanKoreanConfig.ARENA_LOCATIONS.length; i++)
		{
			arenas.add(new Arena(i, ClanKoreanConfig.ARENA_LOCATIONS[i]));
		}
		pendingTeams.addAll(registrationOrder);
		state = State.FIGHTING;
		Broadcast.toAllOnlinePlayers("Clan Korean: Comienzan " + (pendingTeams.size() / 2) + " enfrentamientos en " + arenas.size() + " arena(s).");
		dispatchMatches();
	}
	
	private synchronized void dispatchMatches()
	{
		if (state != State.FIGHTING)
		{
			return;
		}
		for (Arena arena : arenas)
		{
			if (!arena.free || (pendingTeams.size() < 2))
			{
				continue;
			}
			final Team team1 = pendingTeams.remove(0);
			final Team team2 = pendingTeams.remove(0);
			arena.free = false;
			final Match newMatch = new Match(arena, team1, team2);
			activeMatches.put(arena.id, newMatch);
			newMatch.prepare();
			ThreadPool.schedule(newMatch::start, ClanKoreanConfig.PREPARE_SECONDS * 1000L);
		}
		if (activeMatches.isEmpty() && (pendingTeams.size() < 2))
		{
			finishEventWindow();
		}
	}
	
	private String validateTeam(Team team)
	{
		final Player leader = World.getInstance().getPlayer(team.leaderId);
		if ((leader == null) || !leader.isOnline() || (leader.getClan() == null) || (leader.getClanId() != team.clanId) || (leader.getClan().getLevel() < ClanKoreanConfig.MINIMUM_CLAN_LEVEL))
		{
			return "El lider o el nivel del clan ya no cumple los requisitos.";
		}
		for (int objectId : team.memberIds)
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if ((player == null) || !player.isOnline() || (player.getClanId() != team.clanId))
			{
				return "Equipo invalido";
			}
		}
		return null;
	}
	
	private void spawnNpc()
	{
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(ClanKoreanConfig.NPC_ID);
			if (template == null)
			{
				LOGGER.warning("Clan Korean: No existe el NPC " + ClanKoreanConfig.NPC_ID + ".");
				return;
			}
			npcSpawn = new Spawn(template);
			npcSpawn.setXYZ(ClanKoreanConfig.NPC_X, ClanKoreanConfig.NPC_Y, ClanKoreanConfig.NPC_Z);
			npcSpawn.setHeading(ClanKoreanConfig.NPC_HEADING);
			npcSpawn.setRespawnDelay(1);
			npcSpawn.setAmount(1);
			npcSpawn.init();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Clan Korean: Error al crear el NPC.", e);
		}
	}
	
	private void unspawnNpc()
	{
		final Spawn spawn = npcSpawn;
		if (spawn == null)
		{
			return;
		}
		final Npc npc = spawn.getLastSpawn();
		if (npc != null)
		{
			npc.deleteMe();
		}
		spawn.stopRespawn();
		npcSpawn = null;
	}
	
	private void scheduleNextEvent()
	{
		long shortestDelay = Long.MAX_VALUE;
		final LocalDateTime now = LocalDateTime.now();
		for (String value : ClanKoreanConfig.START_TIMES)
		{
			final String trimmed = value.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			try
			{
				final LocalTime time = LocalTime.parse(trimmed);
				LocalDateTime next = now.toLocalDate().atTime(time);
				if (!next.isAfter(now))
				{
					next = next.plusDays(1);
				}
				shortestDelay = Math.min(shortestDelay, Duration.between(now, next).toMillis());
			}
			catch (Exception e)
			{
				LOGGER.warning("Clan Korean: Horario invalido: " + trimmed);
			}
		}
		if (shortestDelay != Long.MAX_VALUE)
		{
			ThreadPool.schedule(() ->
			{
				startRegistration();
				scheduleNextEvent();
			}, shortestDelay);
		}
	}
	
	private synchronized void finishMatch(Match finishedMatch, Team winner, String reason)
	{
		if ((state == State.INACTIVE) || (finishedMatch == null) || !activeMatches.containsKey(finishedMatch.arena.id))
		{
			return;
		}
		if (winner != null)
		{
			for (int objectId : winner.memberIds)
			{
				final Player player = World.getInstance().getPlayer(objectId);
				if ((player != null) && player.isOnline())
				{
					player.addItem("Clan_Korean", ClanKoreanConfig.REWARD_ID, ClanKoreanConfig.REWARD_AMOUNT, player, true);
				}
			}
			Broadcast.toAllOnlinePlayers("Clan Korean - Arena " + (finishedMatch.arena.id + 1) + ": El clan " + winner.clanName + " gano el combate. " + reason);
		}
		else
		{
			Broadcast.toAllOnlinePlayers("Clan Korean - Arena " + (finishedMatch.arena.id + 1) + ": El combate termino en empate. " + reason);
		}
		finishedMatch.cleanup();
		activeMatches.remove(finishedMatch.arena.id);
		finishedMatch.arena.free = true;
		ThreadPool.schedule(this::dispatchMatches, 3000);
	}
	
	private synchronized void finishEventWindow()
	{
		if (state == State.INACTIVE)
		{
			return;
		}
		if (!pendingTeams.isEmpty())
		{
			final Team withoutOpponent = pendingTeams.remove(0);
			clearProtection(withoutOpponent);
			final Player leader = World.getInstance().getPlayer(withoutOpponent.leaderId);
			if ((leader != null) && leader.isOnline())
			{
				leader.sendMessage("Clan Korean: Tu equipo quedo sin rival y fue retirado de la cola.");
			}
		}
		registeredTeams.clear();
		registrationOrder.clear();
		pendingTeams.clear();
		activeMatches.clear();
		arenas.clear();
		selections.clear();
		state = State.INACTIVE;
		Broadcast.toAllOnlinePlayers("Clan Korean: Todos los enfrentamientos finalizaron.");
	}
	
	private synchronized void resetEvent()
	{
		unspawnNpc();
		for (Team team : registeredTeams.values())
		{
			clearProtection(team);
		}
		registeredTeams.clear();
		registrationOrder.clear();
		pendingTeams.clear();
		activeMatches.clear();
		arenas.clear();
		selections.clear();
		state = State.INACTIVE;
	}
	
	private void clearProtection(Team team)
	{
		for (int objectId : team.memberIds)
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if ((player != null) && player.isOnline())
			{
				player.setArenaProtection(false);
				player.setArenaAttack(false);
				player.setInArenaEvent(false);
				player.setStopArena(false);
				player.setInvul(false);
			}
		}
	}
	
	private final class Match
	{
		private final Arena arena;
		private final Team team1;
		private final Team team2;
		private final Map<Integer, int[]> returnLocations = new HashMap<>();
		private int index1;
		private int index2;
		private int activeObjectId1 = -1;
		private int activeObjectId2 = -1;
		private long fighterDeadline;
		private long matchDeadline;
		
		private Match(Arena arena, Team team1, Team team2)
		{
			this.arena = arena;
			this.team1 = team1;
			this.team2 = team2;
		}
		
		private void prepare()
		{
			prepareTeam(team1, arena.team1X, arena.team1Y, arena.team1Z);
			prepareTeam(team2, arena.team2X, arena.team2Y, arena.team2Z);
			Broadcast.toAllOnlinePlayers("Clan Korean - Arena " + (arena.id + 1) + ": " + team1.clanName + " vs " + team2.clanName + ".");
		}
		
		private void prepareTeam(Team team, int x, int y, int z)
		{
			for (int i = 0; i < team.memberIds.size(); i++)
			{
				final Player player = World.getInstance().getPlayer(team.memberIds.get(i));
				returnLocations.put(player.getObjectId(), new int[]
				{
					player.getX(),
					player.getY(),
					player.getZ()
				});
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setArenaProtection(true);
				player.setInArenaEvent(true);
				player.setArenaAttack(false);
				player.setInvul(true);
				player.setStopArena(true);
				player.teleToLocation(x, y + (i * ClanKoreanConfig.BENCH_OFFSET_Y), z, 0);
			}
		}
		
		private void start()
		{
			if ((state != State.FIGHTING) || !activeMatches.containsKey(arena.id))
			{
				return;
			}
			state = State.FIGHTING;
			matchDeadline = System.currentTimeMillis() + (ClanKoreanConfig.MATCH_TIME_MINUTES * 60L * 1000L);
			activateCurrentFighters();
			ThreadPool.schedule(this::check, 1000);
		}
		
		private void activateCurrentFighters()
		{
			final Player fighter1 = getPlayer(team1, index1);
			final Player fighter2 = getPlayer(team2, index2);
			if (fighter1.getObjectId() != activeObjectId1)
			{
				activate(fighter1, arena.team1X, arena.team1Y, arena.team1Z);
				activeObjectId1 = fighter1.getObjectId();
			}
			if (fighter2.getObjectId() != activeObjectId2)
			{
				activate(fighter2, arena.team2X, arena.team2Y, arena.team2Z);
				activeObjectId2 = fighter2.getObjectId();
			}
			fighterDeadline = System.currentTimeMillis() + (ClanKoreanConfig.FIGHTER_TIME_SECONDS * 1000L);
			final String message = "Clan Korean: " + fighter1.getName() + " vs " + fighter2.getName() + ".";
			forEachParticipant(player -> player.sendMessage(message));
		}
		
		private void activate(Player player, int x, int y, int z)
		{
			if (player.isDead())
			{
				player.doRevive();
			}
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.teleToLocation(x, y, z, 0);
			player.setInvul(false);
			player.setStopArena(false);
			player.setArenaAttack(true);
			player.broadcastUserInfo();
		}
		
		private void eliminate(Player player)
		{
			if ((player != null) && player.isOnline())
			{
				player.setArenaAttack(false);
				player.setInvul(true);
				player.setStopArena(true);
			}
		}
		
		private void check()
		{
			if (state != State.FIGHTING)
			{
				return;
			}
			final Player fighter1 = getPlayer(team1, index1);
			final Player fighter2 = getPlayer(team2, index2);
			final boolean lost1 = (fighter1 == null) || !fighter1.isOnline() || fighter1.isDead();
			final boolean lost2 = (fighter2 == null) || !fighter2.isOnline() || fighter2.isDead();
			
			if (System.currentTimeMillis() >= matchDeadline)
			{
				finishMatch(this, null, "Se alcanzo el tiempo maximo del combate.");
				return;
			}
			if (lost1 && lost2)
			{
				eliminate(fighter1);
				eliminate(fighter2);
				index1++;
				index2++;
			}
			else if (lost1)
			{
				eliminate(fighter1);
				index1++;
				healWinner(fighter2);
			}
			else if (lost2)
			{
				eliminate(fighter2);
				index2++;
				healWinner(fighter1);
			}
			else if (System.currentTimeMillis() >= fighterDeadline)
			{
				final double life1 = lifePercent(fighter1);
				final double life2 = lifePercent(fighter2);
				if (life1 == life2)
				{
					eliminate(fighter1);
					eliminate(fighter2);
					index1++;
					index2++;
				}
				else if (life1 < life2)
				{
					eliminate(fighter1);
					index1++;
					healWinner(fighter2);
				}
				else
				{
					eliminate(fighter2);
					index2++;
					healWinner(fighter1);
				}
			}
			else
			{
				ThreadPool.schedule(this::check, 1000);
				return;
			}
			
			if ((index1 >= TEAM_SIZE) && (index2 >= TEAM_SIZE))
			{
				finishMatch(this, null, "Ambos equipos perdieron a su ultimo luchador.");
			}
			else if (index1 >= TEAM_SIZE)
			{
				finishMatch(this, team2, "");
			}
			else if (index2 >= TEAM_SIZE)
			{
				finishMatch(this, team1, "");
			}
			else
			{
				ThreadPool.schedule(this::activateCurrentFighters, 3000);
				ThreadPool.schedule(this::check, 4000);
			}
		}
		
		private double lifePercent(Player player)
		{
			return ((player.getCurrentHp() + player.getCurrentCp()) * 100.0) / (player.getMaxHp() + player.getMaxCp());
		}
		
		private void healWinner(Player player)
		{
			if (ClanKoreanConfig.HEAL_WINNER && (player != null) && player.isOnline())
			{
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
			}
		}
		
		private Player getPlayer(Team team, int index)
		{
			return (index >= team.memberIds.size()) ? null : World.getInstance().getPlayer(team.memberIds.get(index));
		}
		
		private void cleanup()
		{
			forEachParticipant(player ->
			{
				player.setArenaAttack(false);
				player.setInArenaEvent(false);
				player.setArenaProtection(false);
				player.setStopArena(false);
				player.setInvul(false);
				if (player.isDead())
				{
					player.doRevive();
				}
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				final int[] location = returnLocations.get(player.getObjectId());
				if (location != null)
				{
					player.teleToLocation(location[0], location[1], location[2], 0);
				}
				player.broadcastUserInfo();
			});
		}
		
		private void forEachParticipant(PlayerConsumer consumer)
		{
			for (int objectId : team1.memberIds)
			{
				final Player player = World.getInstance().getPlayer(objectId);
				if ((player != null) && player.isOnline())
				{
					consumer.accept(player);
				}
			}
			for (int objectId : team2.memberIds)
			{
				final Player player = World.getInstance().getPlayer(objectId);
				if ((player != null) && player.isOnline())
				{
					consumer.accept(player);
				}
			}
		}
	}
	
	@FunctionalInterface
	private interface PlayerConsumer
	{
		void accept(Player player);
	}
	
	private static final class Arena
	{
		private final int id;
		private final int team1X;
		private final int team1Y;
		private final int team1Z;
		private final int team2X;
		private final int team2Y;
		private final int team2Z;
		private volatile boolean free = true;
		
		private Arena(int id, int[] location)
		{
			this.id = id;
			team1X = location[0];
			team1Y = location[1];
			team1Z = location[2];
			team2X = location[3];
			team2Y = location[4];
			team2Z = location[5];
		}
	}
	
	private static final class Team
	{
		private final int clanId;
		private final String clanName;
		private final int leaderId;
		private final List<Integer> memberIds;
		
		private Team(int clanId, String clanName, int leaderId, List<Integer> memberIds)
		{
			this.clanId = clanId;
			this.clanName = clanName;
			this.leaderId = leaderId;
			this.memberIds = new ArrayList<>(memberIds);
		}
	}
	
	private static final class SingletonHolder
	{
		private static final ClanKoreanEvent INSTANCE = new ClanKoreanEvent();
	}
}
