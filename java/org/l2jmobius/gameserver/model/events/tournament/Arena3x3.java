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

public class Arena3x3 implements Runnable
{
	// list of participants
	public static List<Pair> registered;
	// number of Arenas
	int free = ArenaConfig.ARENA_EVENT_COUNT_3X3;
	// Arenas
	Arena[] arenas = new Arena[ArenaConfig.ARENA_EVENT_COUNT_3X3];
	// list of fights going on
	Map<Integer, String> fights = new ConcurrentHashMap<>(ArenaConfig.ARENA_EVENT_COUNT_3X3);
	
	public Arena3x3()
	{
		registered = new CopyOnWriteArrayList<>();
		int[] coord;
		for (int i = 0; i < ArenaConfig.ARENA_EVENT_COUNT_3X3; i++)
		{
			coord = ArenaConfig.ARENA_EVENT_LOCS_3X3[i];
			arenas[i] = new Arena(i, coord[0], coord[1], coord[2]);
		}
	}
	
	public static Arena3x3 getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean register(Player player, Player assist, Player assist2)
	{
		for (Pair p : registered)
		{
			if ((p.getLeader() == player) || (p.getAssist() == player))
			{
				player.sendMessage("Tournament: You already registered!");
				return false;
			}
			else if ((p.getLeader() == assist) || (p.getAssist() == assist))
			{
				player.sendMessage("Tournament: " + assist.getName() + " already registered!");
				return false;
			}
			else if ((p.getLeader() == assist2) || (p.getAssist2() == assist2))
			{
				player.sendMessage("Tournament: " + assist2.getName() + " already registered!");
				return false;
			}
		}
		return registered.add(new Pair(player, assist, assist2));
	}
	
	public boolean isRegistered(Player player)
	{
		for (Pair p : registered)
		{
			if ((p.getLeader() == player) || (p.getAssist() == player) || (p.getAssist2() == player))
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
			if ((p.getLeader() == player) || (p.getAssist() == player) || (p.getAssist2() == player))
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
	
	private class Pair
	{
		private final Player leader, assist, assist2;
		
		public Pair(Player leader, Player assist, Player assist2)
		{
			this.leader = leader;
			this.assist = assist;
			this.assist2 = assist2;
		}
		
		public Player getAssist()
		{
			return assist;
		}
		
		public Player getAssist2()
		{
			return assist2;
		}
		
		public Player getLeader()
		{
			return leader;
		}
		
		public boolean check()
		{
			if (((leader == null) || !leader.isOnline()))
			{
				if ((assist != null) && assist.isOnline())
				{
					assist.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if ((assist2 != null) && assist2.isOnline())
				{
					assist2.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				return false;
			}
			
			else if ((((assist == null) || !assist.isOnline()) || (((assist2 == null) || !assist2.isOnline()) && ((leader != null) && leader.isOnline()))))
			{
				leader.sendMessage("Tournament: You participation in Event was Canceled.");
				
				if ((assist != null) && assist.isOnline())
				{
					assist.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if ((assist2 != null) && assist2.isOnline())
				{
					assist2.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
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
				if ((assist != null) && assist.isOnline() && assist.isArenaAttack() && !assist.isDead() && !assist.isInsideZone(ZoneId.PVP))
				{
					assist.canLogout();
				}
				if ((assist2 != null) && assist2.isOnline() && assist2.isArenaAttack() && !assist2.isDead() && !assist2.isInsideZone(ZoneId.PVP))
				{
					assist2.canLogout();
				}
			}
			
			if (((leader == null) || leader.isDead() || !leader.isOnline() || !leader.isInsideZone(ZoneId.PVP) || !leader.isArenaAttack()) && ((assist == null) || assist.isDead() || !assist.isOnline() || !assist.isInsideZone(ZoneId.PVP) || !assist.isArenaAttack()) && ((assist2 == null) || assist2.isDead() || !assist2.isOnline() || !assist2.isInsideZone(ZoneId.PVP)))
			{
				return false;
			}
			
			return !(leader.isDead() && assist.isDead() && assist2.isDead());
		}
		
		public boolean isAlive()
		{
			if (((leader == null) || leader.isDead() || !leader.isOnline() || !leader.isInsideZone(ZoneId.PVP) || !leader.isArenaAttack()) && ((assist == null) || assist.isDead() || !assist.isOnline() || !assist.isInsideZone(ZoneId.PVP) || !assist.isArenaAttack()) && ((assist2 == null) || assist2.isDead() || !assist2.isOnline() || !assist2.isInsideZone(ZoneId.PVP)))
			{
				return false;
			}
			
			return !(leader.isDead() && assist.isDead() && assist2.isDead());
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
			if ((assist != null) && assist.isOnline())
			{
				assist.setCurrentCp(assist.getMaxCp());
				assist.setCurrentHp(assist.getMaxHp());
				assist.setCurrentMp(assist.getMaxMp());
				
				if (!assist.isJailed())
				{
					assist.teleToLocation(x, y + 200, z, 0);
				}
				
				assist.broadcastUserInfo();
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setCurrentCp(assist2.getMaxCp());
				assist2.setCurrentHp(assist2.getMaxHp());
				assist2.setCurrentMp(assist2.getMaxMp());
				
				if (!assist2.isJailed())
				{
					assist2.teleToLocation(x, y + 150, z, 0);
				}
				
				assist2.broadcastUserInfo();
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setCurrentCp(assist.getMaxCp());
				assist.setCurrentHp(assist.getMaxHp());
				assist.setCurrentMp(assist.getMaxMp());
				
				if (!assist.isJailed())
				{
					assist.teleToLocation(x, y, z, 0);
				}
				
				assist.broadcastUserInfo();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setCurrentCp(assist2.getMaxCp());
				assist2.setCurrentHp(assist2.getMaxHp());
				assist2.setCurrentMp(assist2.getMaxMp());
				
				if (!assist2.isJailed())
				{
					assist2.teleToLocation(x, y, z, 0);
				}
				
				assist2.broadcastUserInfo();
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setTitle(title);
				assist.getAppearance().setTitleColor(Integer.decode("0x" + color));
				assist.broadcastUserInfo();
				assist.broadcastTitleInfo();
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setTitle(title);
				assist2.getAppearance().setTitleColor(Integer.decode("0x" + color));
				assist2.broadcastUserInfo();
				assist2.broadcastTitleInfo();
			}
		}
		
		public void saveTitle()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader._originalTitleColorTournament = leader.getAppearance().getTitleColor();
				leader._originalTitleTournament = leader.getTitle();
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist._originalTitleColorTournament = assist.getAppearance().getTitleColor();
				assist._originalTitleTournament = assist.getTitle();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2._originalTitleColorTournament = assist2.getAppearance().getTitleColor();
				assist2._originalTitleTournament = assist2.getTitle();
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setTitle(assist._originalTitleTournament);
				assist.getAppearance().setTitleColor(assist._originalTitleColorTournament);
				assist.broadcastUserInfo();
				assist.broadcastTitleInfo();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setTitle(assist2._originalTitleTournament);
				assist2.getAppearance().setTitleColor(assist2._originalTitleColorTournament);
				assist2.broadcastUserInfo();
				assist2.broadcastTitleInfo();
			}
		}
		
		public void rewards()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_3X3, leader, true);
				ArenaRanking.addRank3x3(leader);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_3X3, assist, true);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_3X3, assist2, true);
			}
			
			sendPacket("Congratulations, your team won the event!", 5);
		}
		
		public void rewardsLost()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_3X3, leader, true);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_3X3, assist, true);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_3X3, assist2, true);
			}
		}
		
		public void setInTournamentEvent(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setInArenaEvent(val);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setInArenaEvent(val);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setInArenaEvent(val);
			}
		}
		
		public void removeMessage()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendMessage("Tournament: Your participation has been removed.");
				leader.setArenaProtection(false);
				leader.setArena3x3(false);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.sendMessage("Tournament: Your participation has been removed.");
				assist.setArenaProtection(false);
				assist.setArena3x3(false);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.sendMessage("Tournament: Your participation has been removed.");
				assist2.setArenaProtection(false);
				assist2.setArena3x3(false);
			}
		}
		
		public void setArenaProtection(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setArenaProtection(val);
				leader.setArena3x3(val);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setArenaProtection(val);
				assist.setArena3x3(val);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setArenaProtection(val);
				assist2.setArena3x3(val);
			}
		}
		
		public void revive()
		{
			if ((leader != null) && leader.isOnline() && leader.isDead())
			{
				leader.doRevive();
			}
			
			if ((assist != null) && assist.isOnline() && assist.isDead())
			{
				assist.doRevive();
			}
			
			if ((assist2 != null) && assist2.isOnline() && assist2.isDead())
			{
				assist2.doRevive();
			}
		}
		
		public void setImobilised(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setInvul(val);
				leader.setStopArena(val);
			}
			if ((assist != null) && assist.isOnline())
			{
				assist.setInvul(val);
				assist.setStopArena(val);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setInvul(val);
				assist2.setStopArena(val);
			}
		}
		
		public void setArenaAttack(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setArenaAttack(val);
				leader.broadcastUserInfo();
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setArenaAttack(val);
				assist.broadcastUserInfo();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setArenaAttack(val);
				assist2.broadcastUserInfo();
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
			
			if ((assist != null) && assist.isOnline())
			{
				if (assist.getPet() != null)
				{
					Summon summon = assist.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof Pet)
					{
						summon.unSummon(assist);
					}
				}
				
				if ((assist.getMountType() == MountType.WYVERN) || (assist.getMountType() == MountType.WOLF) || (assist.getMountType() == MountType.STRIDER))
				{
					assist.dismount();
				}
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				if (assist2.getPet() != null)
				{
					Summon summon = assist2.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof Pet)
					{
						summon.unSummon(assist2);
					}
				}
				
				if ((assist2.getMountType() == MountType.WYVERN) || (assist2.getMountType() == MountType.WOLF) || (assist2.getMountType() == MountType.STRIDER))
				{
					assist2.dismount();
				}
			}
		}
		
		public void removeSkills()
		{
			// Verificar si el jugador es un healer/support (no se le remueven habilidades)
			ClassId classId = leader.getClassId();
			if ((classId == ClassId.SHILLIEN_ELDER) || (classId == ClassId.SHILLIEN_SAINT) || (classId == ClassId.BISHOP) || (classId == ClassId.CARDINAL) || (classId == ClassId.ELDER) || (classId == ClassId.EVA_SAINT))
			{
				return;
			}
			
			// Remover efectos de habilidades que están en la lista de STOP
			for (BuffInfo info : leader.getEffectList().getBuffs())
			{
				if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
				{
					leader.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
				}
			}
			
			// Para assist
			if ((assist != null) && assist.isOnline())
			{
				ClassId assistClassId = assist.getClassId();
				if (!((assistClassId == ClassId.SHILLIEN_ELDER) || (assistClassId == ClassId.SHILLIEN_SAINT) || (assistClassId == ClassId.BISHOP) || (assistClassId == ClassId.CARDINAL) || (assistClassId == ClassId.ELDER) || (assistClassId == ClassId.EVA_SAINT)))
				{
					for (BuffInfo info : assist.getEffectList().getBuffs())
					{
						if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
						{
							assist.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
						}
					}
				}
			}
			
			// Para assist2
			if ((assist2 != null) && assist2.isOnline())
			{
				ClassId assist2ClassId = assist2.getClassId();
				if (!((assist2ClassId == ClassId.SHILLIEN_ELDER) || (assist2ClassId == ClassId.SHILLIEN_SAINT) || (assist2ClassId == ClassId.BISHOP) || (assist2ClassId == ClassId.CARDINAL) || (assist2ClassId == ClassId.ELDER) || (assist2ClassId == ClassId.EVA_SAINT)))
				{
					for (BuffInfo info : assist2.getEffectList().getBuffs())
					{
						if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
						{
							assist2.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
						}
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
		}
		
		public void inicarContagem(int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(leader, duration), 0);
			}
			if ((assist != null) && assist.isOnline())
			{
				assist.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(assist, duration), 0);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(assist2, duration), 0);
			}
		}
		
		public void sendPacketinit(String message, int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				leader.getAppearance().setVisible();
			}
			if ((assist != null) && assist.isOnline())
			{
				assist.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				assist.getAppearance().setVisible();
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				assist2.getAppearance().setVisible();
			}
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
			pairOne.inicarContagem(ArenaConfig.ARENA_WAIT_INTERVAL_3X3);
			pairTwo.inicarContagem(ArenaConfig.ARENA_WAIT_INTERVAL_3X3);
			try
			{
				Thread.sleep(ArenaConfig.ARENA_WAIT_INTERVAL_3X3 * 1000);
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
			if (pairOne.isAlive() && !pairTwo.isAlive())
			{
				Player leader1 = pairOne.getLeader();
				Player leader2 = pairTwo.getLeader();
				
				if ((leader1.getClan() != null) && (leader2.getClan() != null) && ArenaConfig.TOURNAMENT_EVENT_ANNOUNCE)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1);
					sm.addString("3X3: (" + leader1.getClan().getName() + " VS " + leader2.getClan().getName() + ") Winner is " + leader1.getClan().getName());
				}
				
				pairOne.rewards();
				pairTwo.rewardsLost();
			}
			else if (pairTwo.isAlive() && !pairOne.isAlive())
			{
				Player leader1 = pairTwo.getLeader();
				Player leader2 = pairOne.getLeader();
				
				if ((leader1.getClan() != null) && (leader2.getClan() != null) && ArenaConfig.TOURNAMENT_EVENT_ANNOUNCE)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1);
					sm.addString("3X3: (" + leader1.getClan().getName() + " VS " + leader2.getClan().getName() + ") Winner is " + leader1.getClan().getName());
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
			try
			{
				spectator.enterObserverMode(new Location(x, y, z));
			}
			catch (Exception e)
			{
				// Si el método no existe, ignora
			}
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
				all.put(dp.getAssist().getObjectId(), dp.getAssist());
				all.put(dp.getAssist2().getObjectId(), dp.getAssist2());
			}
			return all;
		}
		return all;
	}
	
	private static class SingletonHolder
	{
		protected static final Arena3x3 INSTANCE = new Arena3x3();
	}ckage org.l2jmobius.gameserver.model.events.tournament;

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

public class Arena3x3 implements Runnable
{
	// list of participants
	public static List<Pair> registered;
	// number of Arenas
	int free = ArenaConfig.ARENA_EVENT_COUNT_3X3;
	// Arenas
	Arena[] arenas = new Arena[ArenaConfig.ARENA_EVENT_COUNT_3X3];
	// list of fights going on
	Map<Integer, String> fights = new ConcurrentHashMap<>(ArenaConfig.ARENA_EVENT_COUNT_3X3);
	
	public Arena3x3()
	{
		registered = new CopyOnWriteArrayList<>();
		int[] coord;
		for (int i = 0; i < ArenaConfig.ARENA_EVENT_COUNT_3X3; i++)
		{
			coord = ArenaConfig.ARENA_EVENT_LOCS_3X3[i];
			arenas[i] = new Arena(i, coord[0], coord[1], coord[2]);
		}
	}
	
	public static Arena3x3 getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean register(Player player, Player assist, Player assist2)
	{
		for (Pair p : registered)
		{
			if ((p.getLeader() == player) || (p.getAssist() == player))
			{
				player.sendMessage("Tournament: You already registered!");
				return false;
			}
			else if ((p.getLeader() == assist) || (p.getAssist() == assist))
			{
				player.sendMessage("Tournament: " + assist.getName() + " already registered!");
				return false;
			}
			else if ((p.getLeader() == assist2) || (p.getAssist2() == assist2))
			{
				player.sendMessage("Tournament: " + assist2.getName() + " already registered!");
				return false;
			}
		}
		return registered.add(new Pair(player, assist, assist2));
	}
	
	public boolean isRegistered(Player player)
	{
		for (Pair p : registered)
		{
			if ((p.getLeader() == player) || (p.getAssist() == player) || (p.getAssist2() == player))
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
			if ((p.getLeader() == player) || (p.getAssist() == player) || (p.getAssist2() == player))
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
	
	private class Pair
	{
		private final Player leader, assist, assist2;
		
		public Pair(Player leader, Player assist, Player assist2)
		{
			this.leader = leader;
			this.assist = assist;
			this.assist2 = assist2;
		}
		
		public Player getAssist()
		{
			return assist;
		}
		
		public Player getAssist2()
		{
			return assist2;
		}
		
		public Player getLeader()
		{
			return leader;
		}
		
		public boolean check()
		{
			if (((leader == null) || !leader.isOnline()))
			{
				if ((assist != null) && assist.isOnline())
				{
					assist.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if ((assist2 != null) && assist2.isOnline())
				{
					assist2.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				return false;
			}
			
			else if ((((assist == null) || !assist.isOnline()) || (((assist2 == null) || !assist2.isOnline()) && ((leader != null) && leader.isOnline()))))
			{
				leader.sendMessage("Tournament: You participation in Event was Canceled.");
				
				if ((assist != null) && assist.isOnline())
				{
					assist.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if ((assist2 != null) && assist2.isOnline())
				{
					assist2.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
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
				if ((assist != null) && assist.isOnline() && assist.isArenaAttack() && !assist.isDead() && !assist.isInsideZone(ZoneId.PVP))
				{
					assist.canLogout();
				}
				if ((assist2 != null) && assist2.isOnline() && assist2.isArenaAttack() && !assist2.isDead() && !assist2.isInsideZone(ZoneId.PVP))
				{
					assist2.canLogout();
				}
			}
			
			if (((leader == null) || leader.isDead() || !leader.isOnline() || !leader.isInsideZone(ZoneId.PVP) || !leader.isArenaAttack()) && ((assist == null) || assist.isDead() || !assist.isOnline() || !assist.isInsideZone(ZoneId.PVP) || !assist.isArenaAttack()) && ((assist2 == null) || assist2.isDead() || !assist2.isOnline() || !assist2.isInsideZone(ZoneId.PVP)))
			{
				return false;
			}
			
			return !(leader.isDead() && assist.isDead() && assist2.isDead());
		}
		
		public boolean isAlive()
		{
			if (((leader == null) || leader.isDead() || !leader.isOnline() || !leader.isInsideZone(ZoneId.PVP) || !leader.isArenaAttack()) && ((assist == null) || assist.isDead() || !assist.isOnline() || !assist.isInsideZone(ZoneId.PVP) || !assist.isArenaAttack()) && ((assist2 == null) || assist2.isDead() || !assist2.isOnline() || !assist2.isInsideZone(ZoneId.PVP)))
			{
				return false;
			}
			
			return !(leader.isDead() && assist.isDead() && assist2.isDead());
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
			if ((assist != null) && assist.isOnline())
			{
				assist.setCurrentCp(assist.getMaxCp());
				assist.setCurrentHp(assist.getMaxHp());
				assist.setCurrentMp(assist.getMaxMp());
				
				if (!assist.isJailed())
				{
					assist.teleToLocation(x, y + 200, z, 0);
				}
				
				assist.broadcastUserInfo();
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setCurrentCp(assist2.getMaxCp());
				assist2.setCurrentHp(assist2.getMaxHp());
				assist2.setCurrentMp(assist2.getMaxMp());
				
				if (!assist2.isJailed())
				{
					assist2.teleToLocation(x, y + 150, z, 0);
				}
				
				assist2.broadcastUserInfo();
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setCurrentCp(assist.getMaxCp());
				assist.setCurrentHp(assist.getMaxHp());
				assist.setCurrentMp(assist.getMaxMp());
				
				if (!assist.isJailed())
				{
					assist.teleToLocation(x, y, z, 0);
				}
				
				assist.broadcastUserInfo();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setCurrentCp(assist2.getMaxCp());
				assist2.setCurrentHp(assist2.getMaxHp());
				assist2.setCurrentMp(assist2.getMaxMp());
				
				if (!assist2.isJailed())
				{
					assist2.teleToLocation(x, y, z, 0);
				}
				
				assist2.broadcastUserInfo();
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setTitle(title);
				assist.getAppearance().setTitleColor(Integer.decode("0x" + color));
				assist.broadcastUserInfo();
				assist.broadcastTitleInfo();
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setTitle(title);
				assist2.getAppearance().setTitleColor(Integer.decode("0x" + color));
				assist2.broadcastUserInfo();
				assist2.broadcastTitleInfo();
			}
		}
		
		public void saveTitle()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader._originalTitleColorTournament = leader.getAppearance().getTitleColor();
				leader._originalTitleTournament = leader.getTitle();
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist._originalTitleColorTournament = assist.getAppearance().getTitleColor();
				assist._originalTitleTournament = assist.getTitle();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2._originalTitleColorTournament = assist2.getAppearance().getTitleColor();
				assist2._originalTitleTournament = assist2.getTitle();
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setTitle(assist._originalTitleTournament);
				assist.getAppearance().setTitleColor(assist._originalTitleColorTournament);
				assist.broadcastUserInfo();
				assist.broadcastTitleInfo();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setTitle(assist2._originalTitleTournament);
				assist2.getAppearance().setTitleColor(assist2._originalTitleColorTournament);
				assist2.broadcastUserInfo();
				assist2.broadcastTitleInfo();
			}
		}
		
		public void rewards()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_3X3, leader, true);
				ArenaRanking.addRank3x3(leader);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_3X3, assist, true);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_WIN_REWARD_COUNT_3X3, assist2, true);
			}
			
			sendPacket("Congratulations, your team won the event!", 5);
		}
		
		public void rewardsLost()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_3X3, leader, true);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_3X3, assist, true);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.addItem("Arena_Event", ArenaConfig.ARENA_REWARD_ID, ArenaConfig.ARENA_LOST_REWARD_COUNT_3X3, assist2, true);
			}
		}
		
		public void setInTournamentEvent(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setInArenaEvent(val);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setInArenaEvent(val);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setInArenaEvent(val);
			}
		}
		
		public void removeMessage()
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendMessage("Tournament: Your participation has been removed.");
				leader.setArenaProtection(false);
				leader.setArena3x3(false);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.sendMessage("Tournament: Your participation has been removed.");
				assist.setArenaProtection(false);
				assist.setArena3x3(false);
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.sendMessage("Tournament: Your participation has been removed.");
				assist2.setArenaProtection(false);
				assist2.setArena3x3(false);
			}
		}
		
		public void setArenaProtection(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setArenaProtection(val);
				leader.setArena3x3(val);
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setArenaProtection(val);
				assist.setArena3x3(val);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setArenaProtection(val);
				assist2.setArena3x3(val);
			}
		}
		
		public void revive()
		{
			if ((leader != null) && leader.isOnline() && leader.isDead())
			{
				leader.doRevive();
			}
			
			if ((assist != null) && assist.isOnline() && assist.isDead())
			{
				assist.doRevive();
			}
			
			if ((assist2 != null) && assist2.isOnline() && assist2.isDead())
			{
				assist2.doRevive();
			}
		}
		
		public void setImobilised(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setInvul(val);
				leader.setStopArena(val);
			}
			if ((assist != null) && assist.isOnline())
			{
				assist.setInvul(val);
				assist.setStopArena(val);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setInvul(val);
				assist2.setStopArena(val);
			}
		}
		
		public void setArenaAttack(boolean val)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.setArenaAttack(val);
				leader.broadcastUserInfo();
			}
			
			if ((assist != null) && assist.isOnline())
			{
				assist.setArenaAttack(val);
				assist.broadcastUserInfo();
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.setArenaAttack(val);
				assist2.broadcastUserInfo();
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
			
			if ((assist != null) && assist.isOnline())
			{
				if (assist.getPet() != null)
				{
					Summon summon = assist.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof Pet)
					{
						summon.unSummon(assist);
					}
				}
				
				if ((assist.getMountType() == MountType.WYVERN) || (assist.getMountType() == MountType.WOLF) || (assist.getMountType() == MountType.STRIDER))
				{
					assist.dismount();
				}
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				if (assist2.getPet() != null)
				{
					Summon summon = assist2.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof Pet)
					{
						summon.unSummon(assist2);
					}
				}
				
				if ((assist2.getMountType() == MountType.WYVERN) || (assist2.getMountType() == MountType.WOLF) || (assist2.getMountType() == MountType.STRIDER))
				{
					assist2.dismount();
				}
			}
		}
		
		public void removeSkills()
		{
			// Verificar si el jugador es un healer/support (no se le remueven habilidades)
			ClassId classId = leader.getClassId();
			if ((classId == ClassId.SHILLIEN_ELDER) || (classId == ClassId.SHILLIEN_SAINT) || (classId == ClassId.BISHOP) || (classId == ClassId.CARDINAL) || (classId == ClassId.ELDER) || (classId == ClassId.EVA_SAINT))
			{
				return;
			}
			
			// Remover efectos de habilidades que están en la lista de STOP
			for (BuffInfo info : leader.getEffectList().getBuffs())
			{
				if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
				{
					leader.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
				}
			}
			
			// Para assist
			if ((assist != null) && assist.isOnline())
			{
				ClassId assistClassId = assist.getClassId();
				if (!((assistClassId == ClassId.SHILLIEN_ELDER) || (assistClassId == ClassId.SHILLIEN_SAINT) || (assistClassId == ClassId.BISHOP) || (assistClassId == ClassId.CARDINAL) || (assistClassId == ClassId.ELDER) || (assistClassId == ClassId.EVA_SAINT)))
				{
					for (BuffInfo info : assist.getEffectList().getBuffs())
					{
						if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
						{
							assist.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
						}
					}
				}
			}
			
			// Para assist2
			if ((assist2 != null) && assist2.isOnline())
			{
				ClassId assist2ClassId = assist2.getClassId();
				if (!((assist2ClassId == ClassId.SHILLIEN_ELDER) || (assist2ClassId == ClassId.SHILLIEN_SAINT) || (assist2ClassId == ClassId.BISHOP) || (assist2ClassId == ClassId.CARDINAL) || (assist2ClassId == ClassId.ELDER) || (assist2ClassId == ClassId.EVA_SAINT)))
				{
					for (BuffInfo info : assist2.getEffectList().getBuffs())
					{
						if (ArenaConfig.ARENA_STOP_SKILL_LIST.contains(info.getSkill().getId()))
						{
							assist2.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill().getId());
						}
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
			
			if ((assist != null) && assist.isOnline())
			{
				assist.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
			
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
		}
		
		public void inicarContagem(int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(leader, duration), 0);
			}
			if ((assist != null) && assist.isOnline())
			{
				assist.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(assist, duration), 0);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.getAppearance().setVisible();
				ThreadPool.schedule(new countdown(assist2, duration), 0);
			}
		}
		
		public void sendPacketinit(String message, int duration)
		{
			if ((leader != null) && leader.isOnline())
			{
				leader.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				leader.getAppearance().setVisible();
			}
			if ((assist != null) && assist.isOnline())
			{
				assist.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				assist.getAppearance().setVisible();
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				assist2.sendPacket(new ExShowScreenMessage(message, duration * 1000));
				assist2.getAppearance().setVisible();
			}
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
			pairOne.inicarContagem(ArenaConfig.ARENA_WAIT_INTERVAL_3X3);
			pairTwo.inicarContagem(ArenaConfig.ARENA_WAIT_INTERVAL_3X3);
			try
			{
				Thread.sleep(ArenaConfig.ARENA_WAIT_INTERVAL_3X3 * 1000);
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
			if (pairOne.isAlive() && !pairTwo.isAlive())
			{
				Player leader1 = pairOne.getLeader();
				Player leader2 = pairTwo.getLeader();
				
				if ((leader1.getClan() != null) && (leader2.getClan() != null) && ArenaConfig.TOURNAMENT_EVENT_ANNOUNCE)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1);
					sm.addString("3X3: (" + leader1.getClan().getName() + " VS " + leader2.getClan().getName() + ") Winner is " + leader1.getClan().getName());
				}
				
				pairOne.rewards();
				pairTwo.rewardsLost();
			}
			else if (pairTwo.isAlive() && !pairOne.isAlive())
			{
				Player leader1 = pairTwo.getLeader();
				Player leader2 = pairOne.getLeader();
				
				if ((leader1.getClan() != null) && (leader2.getClan() != null) && ArenaConfig.TOURNAMENT_EVENT_ANNOUNCE)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1);
					sm.addString("3X3: (" + leader1.getClan().getName() + " VS " + leader2.getClan().getName() + ") Winner is " + leader1.getClan().getName());
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
			try
			{
				spectator.enterObserverMode(new Location(x, y, z));
			}
			catch (Exception e)
			{
				// Si el método no existe, ignora
			}
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
				all.put(dp.getAssist().getObjectId(), dp.getAssist());
				all.put(dp.getAssist2().getObjectId(), dp.getAssist2());
			}
			return all;
		}
		return all;
	}
	
	private static class SingletonHolder
	{
		protected static final Arena3x3 INSTANCE = new Arena3x3();
	}
		private void applySelectedBuffs()
		{
			if ((leader != null) && leader.isOnline())
			{
				EventBuffManager.apply(leader, EventBuffManager.TOURNAMENT);
			}
			if ((assist != null) && assist.isOnline())
			{
				EventBuffManager.apply(assist, EventBuffManager.TOURNAMENT);
			}
			if ((assist2 != null) && assist2.isOnline())
			{
				EventBuffManager.apply(assist2, EventBuffManager.TOURNAMENT);
			}
		}

}
