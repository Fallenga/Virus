package org.l2jmobius.gameserver.util;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.fos.FOSEvent;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;

/** Keeps manual restart requests separate from event-controlled resurrection. */
public final class EventRespawnProtection
{
	private EventRespawnProtection()
	{
	}
	
	public static boolean isProtected(Player player)
	{
		return keepsBuffsOnDeath(player) || ((player != null) && FOSEvent.isPlayerParticipant(player.getObjectId()) && (FOSEvent.isStarting() || FOSEvent.isStarted() || FOSEvent.isRewarding()));
	}
	
	public static boolean keepsBuffsOnDeath(Player player)
	{
		if (player == null)
		{
			return false;
		}
		// Tournament and Clan Korean set this only while in their arena.
		if (player.isInArenaEvent() || player.isInTournamentEvent())
		{
			return true;
		}
		if (!player.isOnEvent())
		{
			return false;
		}
		if (player.getVariables().getBoolean("CastleDominationActive", false))
		{
			return true;
		}
		// Scripts are loaded separately: do not add a core dependency on TvT.java.
		for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if ((listener.getOwner() != null) && "custom.events.TeamVsTeam.TvT".equals(listener.getOwner().getClass().getName()))
			{
				return true;
			}
		}
		return false;
	}
}
