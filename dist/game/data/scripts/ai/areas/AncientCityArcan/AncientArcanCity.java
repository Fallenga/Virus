/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.areas.AncientCityArcan;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.enums.Movie;
import org.l2jmobius.gameserver.instancemanager.QuestManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.ScriptZone;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

import ai.AbstractNpcAI;
import instances.TaintedDimension.TaintedDimension;
import quests.Q10301_ShadowOfTerrorBlackishRedFog.Q10301_ShadowOfTerrorBlackishRedFog;

/**
 * Ancient Arcan City AI.
 * @author St3eT, Mobius
 */
public class AncientArcanCity extends AbstractNpcAI
{
	// NPC
	private static final int CEREMONIAL_CAT = 33093;
	// Location
	private static final Location ANCIENT_ARCAN_CITY = new Location(207559, 86429, -1000);
	private static final Location EARTHQUAKE = new Location(207088, 88720, -1128);
	// Zones
	private static final ScriptZone BROADCAST_ZONE = ZoneManager.getInstance().getZoneById(23600, ScriptZone.class); // Ancient Arcan City zone
	private static final ScriptZone TELEPORT_ZONE = ZoneManager.getInstance().getZoneById(12015, ScriptZone.class); // Anghel Waterfall teleport zone
	// Misc
	private static final SpawnGroup CEREMONY_SPAWNS = SpawnData.getInstance().getSpawnGroupByName("ArcanCeremony");
	private static final Set<Npc> CEREMONIAL_CATS = ConcurrentHashMap.newKeySet();
	private static final int CHANGE_STATE_TIME = 1800000; // 30min
	private static boolean _isCeremonyRunning = false;
	
	private AncientArcanCity()
	{
		addEnterZoneId(TELEPORT_ZONE.getId());
		if (CEREMONY_SPAWNS != null)
		{
			addSpawnId(CEREMONIAL_CAT);
			addEnterZoneId(BROADCAST_ZONE.getId());
			startQuestTimer("CHANGE_STATE", CHANGE_STATE_TIME, null, null, true);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("CHANGE_STATE"))
		{
			_isCeremonyRunning = !_isCeremonyRunning;
			for (Player plr : BROADCAST_ZONE.getPlayersInside())
			{
				plr.sendPacket(new OnEventTrigger(262001, !_isCeremonyRunning));
				plr.sendPacket(new OnEventTrigger(262003, _isCeremonyRunning));
				if (_isCeremonyRunning)
				{
					showOnScreenMsg(plr, NpcStringId.THE_INCREASED_GRASP_OF_DARK_ENERGY_CAUSES_THE_GROUND_TO_SHAKE, ExShowScreenMessage.TOP_CENTER, 5000, true);
					plr.sendPacket(new Earthquake(EARTHQUAKE, 10, 5));
				}
			}
			
			if (_isCeremonyRunning)
			{
				CEREMONY_SPAWNS.spawnAll();
			}
			else
			{
				cancelQuestTimers("SOCIAL_ACTION");
				CEREMONY_SPAWNS.despawnAll();
				CEREMONIAL_CATS.clear();
			}
		}
		else if (event.equals("SOCIAL_ACTION"))
		{
			for (Npc cat : CEREMONIAL_CATS)
			{
				cat.broadcastSocialAction(2);
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (zone.getId() == TELEPORT_ZONE.getId())
			{
				final QuestState qs = player.getQuestState(Q10301_ShadowOfTerrorBlackishRedFog.class.getSimpleName());
				if ((qs != null) && qs.isCond(3))
				{
					final Quest instance = QuestManager.getInstance().getQuest(TaintedDimension.class.getSimpleName());
					if (instance != null)
					{
						instance.notifyEvent("enterInstance", null, player);
					}
				}
				else
				{
					player.teleToLocation(ANCIENT_ARCAN_CITY);
				}
			}
			else
			{
				player.sendPacket(new OnEventTrigger(262001, !_isCeremonyRunning));
				player.sendPacket(new OnEventTrigger(262003, _isCeremonyRunning));
				if (player.getVariables().getBoolean("ANCIENT_ARCAN_CITY_SCENE", true))
				{
					player.getVariables().set("ANCIENT_ARCAN_CITY_SCENE", false);
					playMovie(player, Movie.SI_ARKAN_ENTER);
				}
			}
		}
		return super.onEnterZone(creature, zone);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		CEREMONIAL_CATS.add(npc);
		npc.setRandomAnimation(false);
		startQuestTimer("SOCIAL_ACTION", 4500, null, null, true);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new AncientArcanCity();
	}
}
