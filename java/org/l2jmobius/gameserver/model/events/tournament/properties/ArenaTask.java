/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.events.tournament.properties;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.tournament.Arena1x1;
import org.l2jmobius.gameserver.model.events.tournament.Arena3x3;
import org.l2jmobius.gameserver.model.events.tournament.Arena5x5;
import org.l2jmobius.gameserver.model.events.tournament.Arena9x9;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

public abstract class ArenaTask
{
	protected static final Logger LOGGER = Logger.getLogger(ArenaTask.class.getName());
	
	public static Spawn _npcSpawn1;
	public static Spawn _npcSpawn2;
	
	/** The _in progress. */
	public static volatile boolean _started = false;
	public static volatile boolean _aborted = false;
	
	public static synchronized void SpawnEvent()
	{
		if (_started)
		{
			LOGGER.warning("Tournament: Se intento iniciar un evento que ya estaba activo.");
			return;
		}
		
		_aborted = false;
		_started = true;
		
		Arena1x1.getInstance().clear();
		Arena3x3.getInstance().clear();
		Arena5x5.getInstance().clear();
		Arena9x9.getInstance().clear();
		
		spawnNpc1();
		spawnNpc2();
		
		Broadcast.toAllOnlinePlayers("Tournament: El Evento ha Comenzado!");
		Broadcast.toAllOnlinePlayers("Tournament: Duracion: " + ArenaConfig.TOURNAMENT_TIME + " minutos!");
		
		ThreadPool.schedule(Arena1x1.getInstance(), 5000);
		ThreadPool.schedule(Arena3x3.getInstance(), 5000);
		ThreadPool.schedule(Arena5x5.getInstance(), 5000);
		ThreadPool.schedule(Arena9x9.getInstance(), 5000);
		
		waiter(ArenaConfig.TOURNAMENT_TIME * 60 * 1000);
		
		if (!_aborted)
		{
			finishEvent();
		}
	}
	
	public static synchronized void finishEvent()
	{
		if (!_started)
		{
			return;
		}
		_started = false;
		
		Broadcast.toAllOnlinePlayers("Tournament: El Evento ha finalizado");
		
		unspawnNpc1();
		unspawnNpc2();
		
		if (ArenaConfig.TOURNAMENT_EVENT_TIME)
		{
			ArenaEvent.getInstance().StartCalculationOfNextEventTime();
		}
		
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				if (player.isArenaProtection())
				{
					ThreadPool.schedule(() ->
					{
						if (player.isOnline() && !player.isInArenaEvent() && !player.isArenaAttack())
						{
							if (player.isArena1x1())
							{
								Arena1x1.getInstance().remove(player);
							}
							if (player.isArena3x3())
							{
								Arena3x3.getInstance().remove(player);
							}
							if (player.isArena5x5())
							{
								Arena5x5.getInstance().remove(player);
							}
							if (player.isArena9x9())
							{
								Arena9x9.getInstance().remove(player);
							}
							
							player.setArenaProtection(false);
						}
					}, 25000);
				}
				
				player.sendMessage("Next Tournament: " + ArenaEvent.getInstance().getNextTime() + " (GMT-3).");
			}
		}
	}
	
	private static void broadcastAnnounce(String message)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S1);
		sm.addString(message);
		World.getInstance().getPlayers().forEach(p -> p.sendPacket(sm));
	}
	
	public static void spawnNpc1()
	{
		try
		{
			NpcTemplate tmpl = NpcData.getInstance().getTemplate(ArenaConfig.ARENA_NPC);
			if (tmpl == null)
			{
				LOGGER.warning("Tournament1: NPC template " + ArenaConfig.ARENA_NPC + " not found!");
				return;
			}
			
			_npcSpawn1 = new Spawn(tmpl);
			_npcSpawn1.setXYZ(loc1x(), loc1y(), loc1z());
			_npcSpawn1.setHeading(ArenaConfig.NPC_Heading);
			_npcSpawn1.setRespawnDelay(1);
			_npcSpawn1.setAmount(1);
			
			_npcSpawn1.init();
			
			Npc npc = _npcSpawn1.getLastSpawn();
			if (npc != null)
			{
				npc.getStatus().setCurrentHp(999999999);
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 1034, 1, 1, 1));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void spawnNpc2()
	{
		try
		{
			NpcTemplate tmpl = NpcData.getInstance().getTemplate(ArenaConfig.ARENA_NPC);
			if (tmpl == null)
			{
				LOGGER.warning("Tournament2: NPC template " + ArenaConfig.ARENA_NPC + " not found!");
				return;
			}
			
			_npcSpawn2 = new Spawn(tmpl);
			_npcSpawn2.setXYZ(loc2x(), loc2y(), loc2z());
			_npcSpawn2.setHeading(ArenaConfig.NPC_Heading2);
			_npcSpawn2.setRespawnDelay(1);
			_npcSpawn2.setAmount(1);
			
			_npcSpawn2.init();
			
			Npc npc = _npcSpawn2.getLastSpawn();
			if (npc != null)
			{
				
				npc.getStatus().setCurrentHp(999999999);
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 1034, 1, 1, 1));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean is_started()
	{
		return _started;
	}
	
	public static void unspawnNpc1()
	{
		if (_npcSpawn1 == null)
		{
			return;
		}
		
		Npc npc = _npcSpawn1.getLastSpawn();
		if (npc != null)
		{
			npc.deleteMe();
		}
		_npcSpawn1.stopRespawn();
		_npcSpawn1 = null;
	}
	
	public static void unspawnNpc2()
	{
		if (_npcSpawn2 == null)
		{
			return;
		}
		
		Npc npc = _npcSpawn2.getLastSpawn();
		if (npc != null)
		{
			npc.deleteMe();
		}
		_npcSpawn2.stopRespawn();
		_npcSpawn2 = null;
	}
	
	public static int loc1x()
	{
		return ArenaConfig.NPC_locx;
	}
	
	public static int loc1y()
	{
		return ArenaConfig.NPC_locy;
	}
	
	public static int loc1z()
	{
		return ArenaConfig.NPC_locz;
	}
	
	public static int loc2x()
	{
		return ArenaConfig.NPC_locx2;
	}
	
	public static int loc2y()
	{
		return ArenaConfig.NPC_locy2;
	}
	
	public static int loc2z()
	{
		return ArenaConfig.NPC_locz2;
	}
	
	protected static void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		
		while (((startWaiterTime + interval) > System.currentTimeMillis()) && !_aborted)
		{
			seconds--;
			
			switch (seconds)
			{
				case 3600:
					if (_started)
					{
						Broadcast.toAllOnlinePlayers("Tournament: El Evento Comenzo");
						Broadcast.toAllOnlinePlayers("Tournament: Comando .tournament para ir a la zona");
						Broadcast.toAllOnlinePlayers("Tournament: Reward: " + ItemData.getInstance().getTemplate(ArenaConfig.ARENA_REWARD_ID).getName());
						Broadcast.toAllOnlinePlayers("Tournament: " + (seconds / 60 / 60) + " hour(s) till event finish!");
					}
					break;
				case 1800:
				case 900:
				case 600:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
					if (_started)
					{
						Broadcast.toAllOnlinePlayers("Tournament: " + (seconds / 60) + " minute(s) till event finish!");
					}
					break;
				case 30:
				case 15:
				case 10:
				case 3:
				case 2:
				case 1:
					if (_started)
					{
						Broadcast.toAllOnlinePlayers("Tournament: " + seconds + " second(s) till event finish!");
					}
					break;
			}
			
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
