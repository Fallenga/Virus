package org.l2jmobius.gameserver.model.events.tournament;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.ClassId;
import org.l2jmobius.gameserver.enums.MountType;
import org.l2jmobius.gameserver.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventBuffManager;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaConfig;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaRanking;
import org.l2jmobius.gameserver.model.events.tournament.properties.ArenaTask;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Arena1x1 implements Runnable
{
	// list of participants
	public static List<Pair> registered;
	// number of Arenas
	int free = ArenaConfig.ARENA_EVENT_COUNT_1X1;
	// Arenas
	Arena[] arenas = new Arena[ArenaConfig.ARENA_EVENT_COUNT_1X1];
	// list of fights going on
	Map<Integer, String> fights = new ConcurrentHashMap<>(ArenaConfig.ARENA_EVENT_COUNT_1X1);
	
	public Arena1x1()
	{
		registered = new CopyOnWriteArrayList<>();
		int[] coord;
		for (int i = 0; i < ArenaConfig.ARENA_EVENT_COUNT_1X1; i++)
		{
			coord = ArenaConfig.ARENA_EVENT_LOCS_1X1[i];
			arenas[i] = new Arena(i, coord[0], coord[1], coord[2]);
		}
	}
	
	public static Arena1x1 getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean register(Player player)
	{
		for (Pair p : registered)
		{
			if (p.getLeader() == player)
			{
				player.sendMessage("Tournament: You already registered!");
				return false;
			}
		}
		return registered.add(new Pair(player));
	}
	
	public boolean isRegistered(Player player)
	{
		for (Pair p : registered)
		{
			if (p.getLeader() == player)
			{
				return true;
			}
		}
		return false;
	}
	
	public void addSpectator(Player spectator, int arenaId)
	{
		Arena arena = getArena(arenaId);
		if (arena != null)
		{
			arena.addSpectator(spectator);
		}
	}
	
	private Arena getArena(int id)
	{
		for (Arena arena : arenas)
		{
			if (arena.id == id)
			{
				return arena;
			}
		}
		return null;
	}
	
	public Map<Integer, String> getFights()
	{
		return fights;
	}
	
	public boolean remove(Player player)
	{
		for (Pair p : registered)
		{
			if (p.getLeader() == player)
			{
				p.removeMessage();
				registered.remove(p);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public synchronized void run()
	{
		boolean load = true;
		
		while (load)
		{
			if (!ArenaTask.is_started())
			{
				load = false;
			}
			
			if ((registered.size() < 2) || (free == 0))
			{
				try
				{
					Thread.sleep(ArenaConfig.ARENA_CALL_INTERVAL * 1000);
				}
				catch (InterruptedException e)
				{
				}
				continue;
			}
			List<Pair> opponents = selectOpponents();
			if ((opponents != null) && (opponents.size() == 2))
			{
				ThreadPool.execute(new EvtArenaTask(opponents));
			}
			try
			{
				Thread.sleep(ArenaConfig.ARENA_CALL_INTERVAL * 1000);
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	private List<Pair> selectOpponents()
	{
		List<Pair> opponents = new CopyOnWriteArrayList<>();
		Pair pairOne = null, pairTwo = null;
		int tries = 3;
		do
		{
			int first = 0, second = 0;
			if (getRegisteredCount() < 2)
			{
				return opponents;
			}
			
			if (pairOne == null)
			{
				first = Rnd.get(getRegisteredCount());
				pairOne = registered.get(first);
				if (pairOne.check())
				{
					opponents.add(0, pairOne);
					registered.remove(first);
				}
				else
				{
					pairOne = null;
					registered.remove(first);
					return null;
				}
				
			}
			if (pairTwo == null)
			{
				second = Rnd.get(getRegisteredCount());
				pairTwo = registered.get(second);
				if (pairTwo.check())
				{
					opponents.add(1, pairTwo);
					registered.remove(second);
				}
				else
				{
					pairTwo = null;
					registered.remove(second);
					return null;
				}
				
			}
		}
		while (((pairOne == null) || (pairTwo == null)) && (--tries > 0));
		return opponents;
	}
	
	public void clear()
	{
		registered.clear();
	}
	
	public static int getRegisteredCount()
	{
		return registered.size();
	}
	
	public class Pair
	{
		private final Player leader;
		
		public Pair(Player leader)
		{
			this.leader = leader;
		}
		
		public Player getLeader()
		{
			return leader;
		}
		
		public boolean check()
		{
			if (((leader == null) || !leader.isOnline()))
			{
				return false;
			}
			
			return true;
		}
		
		public boolean isDead()
		{
			if (ArenaConfig.ARENA_PROTECT)
			{
				if ((leader != null) && leader.isOnline() && leader.isArenaAttack() && !leader.isDead() && !leader.isInsideZone(ZoneId.PVP))
				{
					leader.canLogout();
				}
			}
			
			if (((leader == null) || leader.isDead() || !leader.isOnline() || !leader.isInsideZone(ZoneId.PVP) || !leader.isArenaAttack()))
			{
				return false;
			}
			
			return !(leader.isDead());
		}
		
		public boolean isAlive()
		{
			if (((leader == null) || leader.isDead() || !leader.isOnline() || !leader.isInsideZone(ZoneId.PVP) || !leader.isArenaAttack()))
			{
				return false;
			}
			
			return !(leader.isDead());
		}
		
		public void teleportTo(int x, int y, int z)
		{
			applySelectedBuffs();
			if ((leader != null) && leader.isOnline())
			{
				leader.setCurrentCp(leader.getMaxCp());
				leader.setCurrentHp(leader.getMaxHp());
				leader.setCurrentMp(leader.getMaxMp());
				
				if (!leader.isJailed())
				{
					leader.teleToLocation(x, y, z, 0);
				}
				
				leader.broadcastUserInfo();
			}
		}
		
		public void teleportToOut(int x, int y, int z)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setCurrentCp(leader.getMaxCp());
				leader.setCurrentHp(leader.getMaxHp());
				leader.setCurrentMp(leader.getMaxMp());
				
				if (!leader.isJailed())
				{
					leader.teleToLocation(x, y, z, 0);
				}
				
				leader.broadcastUserInfo();
			}
		}
		
		public void EventTitle(String title, String color)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setTitle(title);
				leader.getAppearance().setTitleColor(Integer.decode("0x" + color));
				leader.broadcastUserInfo();
				leader.broadcastTitleInfo();
			}
		}
		
		public void saveTitle()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader._originalTitleColorTournament = leader.getAppearance().getTitleColor();
				leader._originalTitleTournament = leader.getTitle();
			}
		}
		
		public void backTitle()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setTitle(leader._originalTitleTournament);
				leader.getAppearance().setTitleColor(leader._originalTitleColorTournament);
				leader.broadcastUserInfo();
				leader.broadcastTitleInfo();
			}
		}
		
		public void rewards()
		{
			// Si tienes sistema de misiones, adapta aquí
			leader.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_1X1, leader, true);
			
			if (ArenaTask.is_started())
			{
				ArenaRanking.addRank1x1(leader);
			}
			
			sendPacket("Congratulations, you won the event!", 5);
		}
		
		public void rewardsLost()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_1X1, leader, true);
			}
		}
		
		public void setInTournamentEvent(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setInArenaEvent(val);
			}
		}
		
		public void removeMessage()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendMessage("Tournament: Your participation has been removed.");
				leader.setArenaProtection(false);
				leader.setArena1x1(false);
			}
		}
		
		public void setArenaProtection(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setArenaProtection(val);
				leader.setArena1x1(val);
			}
		}
		
		public void revive()
		{
			if ((leader != null) && leader.isOnline() && leader.isDead())
			{
				leader.doRevive();
			}
		}
		
		public void setImobilised(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setInvul(val);
				leader.setStopArena(val);
			}
		}
		
		public void setArenaAttack(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setArenaAttack(val);
				leader.broadcastUserInfo();
			}
		}
		
		public void removePet()
		{
			if ((leader != null) && leader.isOnline())
			{
				if (leader.getPet() != null)
				{
					Summon summon = leader.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof Pet)
					{
						summon.unSummon(leader);
					}
				}
				
				if ((leader.getMountType() == MountType.WYVERN) || (leader.getMountType() == MountType.WOLF) || (leader.getMountType() == MountType.STRIDER))
				{
					leader.dismount();
				}
			}
		}
		
		public void removeSkills()
		{
			if (!((leader.getClassId() == ClassId.SHILLIEN_ELDER) || (leader.getClassId() == ClassId.SHILLIEN_SAINT) || (leader.getClassId() == ClassId.BISHOP) || (leader.getClassId() == ClassId.CARDINAL) || (leader.getClassId() == ClassId.ELDER) || (leader.getClassId() == ClassId.EVA_SAINT)))
			{
				// En Mobius, los efectos se manejan con BuffInfo
				for (BuffInfo info : leader.getEffectList().getBuffs())
				{
					if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
					{
						leader.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
					}
				}
			}
		}
		
		public void sendPacket(String message, int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
		}
		
		public void inicarContagem(int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(leader, duration), 0);
			}
		}
		
		public void sendPacketinit(String message, int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				leader.getAppearance().setVisible();
			}
		}
		
		private void broadcastAnnounce(String message)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1);
			sm.addString(message);
			// Enviar a todos los jugadores o usar Broadcast
		}
	}
	
	private class EvtArenaTask implements Runnable
	{
		private final Pair pairOne;
		private final Pair pairTwo;
		private Arena arena;
		
		public EvtArenaTask(List<Pair> opponents)
		{
			pairOne = opponents.get(0);
			pairTwo = opponents.get(1);
		}
		
		@Override
		public void run()
		{
			free--;
			pairOne.saveTitle();
			pairTwo.saveTitle();
			portPairsToArena();
			pairOne.inicarContagem(ArenaConfig.ARENA_WAIT_INTERVAL_1X1);
			pairTwo.inicarContagem(ArenaConfig.ARENA_WAIT_INTERVAL_1X1);
			try
			{
				Thread.sleep(ArenaConfig.ARENA_WAIT_INTERVAL_1X1 * 1000);
			}
			catch (InterruptedException e1)
			{
			}
			pairOne.sendPacketinit("Match Started!", 3);
			pairTwo.sendPacketinit("Match Started!", 3);
			pairOne.EventTitle(ArenaConfig.MSG_TEAM1, ArenaConfig.TITLE_COLOR_TEAM1);
			pairTwo.EventTitle(ArenaConfig.MSG_TEAM2, ArenaConfig.TITLE_COLOR_TEAM2);
			pairOne.setImobilised(false);
			pairTwo.setImobilised(false);
			pairOne.setArenaAttack(true);
			pairTwo.setArenaAttack(true);
			
			while (check())
			{
				try
				{
					Thread.sleep(ArenaConfig.ARENA_CHECK_INTERVAL);
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
			finishDuel();
			free++;
		}
		
		private void finishDuel()
		{
			fights.remove(arena.id);
			rewardWinner();
			pairOne.revive();
			pairTwo.revive();
			pairOne.teleportToOut(ArenaConfig.Tournament_locx + Rnd.get(-100, 100), ArenaConfig.Tournament_locy + Rnd.get(-100, 100), ArenaConfig.Tournament_locz);
			pairTwo.teleportToOut(ArenaConfig.Tournament_locx + Rnd.get(-100, 100), ArenaConfig.Tournament_locy + Rnd.get(-100, 100), ArenaConfig.Tournament_locz);
			pairOne.backTitle();
			pairTwo.backTitle();
			pairOne.setInTournamentEvent(false);
			pairTwo.setInTournamentEvent(false);
			pairOne.setArenaProtection(false);
			pairTwo.setArenaProtection(false);
			pairOne.setArenaAttack(false);
			pairTwo.setArenaAttack(false);
			arena.setFree(true);
		}
		
		private void rewardWinner()
		{
			Player leader1 = pairOne.getLeader();
			Player leader2 = pairTwo.getLeader();
			
			if (pairOne.isAlive() && !pairTwo.isAlive())
			{
				if (ArenaConfig.TOURNAMENT_EVENT_ANNOUNCE)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1);
					sm.addString("1X1: (" + leader1.getName() + " VS " + leader2.getName() + ") Winner is " + leader1.getName());
					// Broadcast a todos los jugadores
				}
				
				pairOne.rewards();
				pairTwo.rewardsLost();
			}
			
			if (pairTwo.isAlive() && !pairOne.isAlive())
			{
				if (ArenaConfig.TOURNAMENT_EVENT_ANNOUNCE)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1);
					sm.addString("1X1: (" + leader1.getName() + " VS " + leader2.getName() + ") Winner is " + leader2.getName());
				}
				
				pairTwo.rewards();
				pairOne.rewardsLost();
			}
		}
		
		private boolean check()
		{
			return ArenaTask.is_started() && pairOne.isDead() && pairTwo.isDead();
		}
		
		private void portPairsToArena()
		{
			for (Arena arena : arenas)
			{
				if (arena.isFree)
				{
					this.arena = arena;
					arena.setFree(false);
					pairOne.removePet();
					pairTwo.removePet();
					pairOne.teleportTo(arena.x - 850, arena.y, arena.z);
					pairTwo.teleportTo(arena.x + 850, arena.y, arena.z);
					pairOne.setImobilised(true);
					pairTwo.setImobilised(true);
					pairOne.setInTournamentEvent(true);
					pairTwo.setInTournamentEvent(true);
					pairOne.removeSkills();
					pairTwo.removeSkills();
					fights.put(this.arena.id, pairOne.getLeader().getName() + " vs " + pairTwo.getLeader().getName());
					break;
				}
			}
		}
	}
	
	private class Arena
	{
		protected int x, y, z;
		protected boolean isFree = true;
		int id;
		
		public Arena(int id, int x, int y, int z)
		{
			this.id = id;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void setFree(boolean val)
		{
			isFree = val;
		}
		
		public void addSpectator(Player spectator)
		{
			// En Mobius, el manejo de instancias puede ser diferente
			// spectator.setInstanceId(1, true);
			spectator.enterObserverMode(new Location(x, y, z));
		}
	}
	
	protected class countdown implements Runnable
	{
		private final Player _player;
		private final int _time;
		
		public countdown(Player player, int time)
		{
			_time = time;
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player.isOnline())
			{
				switch (_time)
				{
					case 300:
					case 240:
					case 180:
					case 120:
					case 57:
						if (_player.isOnline())
						{
							_player.sendPacket(new ExShowScreenMessage("The battle starts in 60 second(s)..", 4000));
							_player.sendMessage("60 second(s) to start the battle.");
						}
						break;
					case 45:
						if (_player.isOnline())
						{
							_player.sendPacket(new ExShowScreenMessage("The battle starts in " + _time + " second(s)..", 3000));
							_player.sendMessage(_time + " second(s) to start the battle!");
						}
						break;
					case 27:
						if (_player.isOnline())
						{
							_player.sendPacket(new ExShowScreenMessage("The battle starts in 30 second(s)..", 4000));
							_player.sendMessage("30 second(s) to start the battle.");
						}
						break;
					case 20:
					case 15:
					case 10:
					case 5:
					case 4:
					case 3:
					case 2:
					case 1:
						if (_player.isOnline())
						{
							_player.sendPacket(new ExShowScreenMessage("The battle starts in " + _time + " second(s)..", 3000));
							_player.sendMessage(_time + " second(s) to start the battle!");
						}
						break;
				}
				if (_time > 1)
				{
					ThreadPool.schedule(new countdown(_player, _time - 1), 1000);
				}
			}
		}
	}
	
	public static Map<Integer, Player> allParticipants()
	{
		Map<Integer, Player> all = new ConcurrentHashMap<>();
		if (getRegisteredCount() > 0)
		{
			for (Pair dp : registered)
			{
				all.put(dp.getLeader().getObjectId(), dp.getLeader());
			}
			return all;
		}
		return all;
	}
	
	private static class SingletonHolder
	{
		protected static final Arena1x1 INSTANCE = new Arena1x1();
	}
		private void applySelectedBuffs()
		{
			if ((leader != null) && leader.isOnline())
			{
				EventBuffManager.apply(leader, EventBuffManager.TOURNAMENT);
			}
		}

}
