package org.l2jmobius.gameserver.model.events.fos;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.enums.Team;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.Duel;
import org.l2jmobius.gameserver.taskmanager.PvpFlagTaskManager;

public class FOSEventTeleporter implements Runnable
{
	/** The instance of the player to teleport */
	private Player _activeChar = null;
	/** Coordinates of the spot to teleport to */
	private int[] _coordinates = new int[3];
	/** Admin removed this player from event */
	private boolean _adminRemove = false;
	private final boolean _eventTeleport;
	private final org.l2jmobius.gameserver.model.instancezone.Instance _instance;
	
	/**
	 * Initialize the teleporter and start the delayed task.
	 * @param playerInstance
	 * @param coordinates
	 * @param fastSchedule
	 * @param adminRemove
	 */
	public FOSEventTeleporter(Player playerInstance, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_activeChar = playerInstance;
		_coordinates = coordinates.clone();
		_eventTeleport = !adminRemove && (FOSEvent.isStarting() || FOSEvent.isStarted());
		_instance = playerInstance == null ? null : playerInstance.getInstanceWorld();
		_adminRemove = adminRemove;
		
		long delay = (FOSEvent.isStarted() ? FOSConfig.FOS_EVENT_RESPAWN_TELEPORT_DELAY : FOSConfig.FOS_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;
		
		ThreadPool.schedule(this, fastSchedule ? 0 : delay);
	}
		
	/**
	 * The task method to teleport the player<br>
	 * 1. Unsummon pet if there is one<br>
	 * 2. Remove all effects<br>
	 * 3. Revive and full heal the player<br>
	 * 4. Teleport the player<br>
	 * 5. Broadcast status and user info
	 */
	@Override
	public void run()
	{
		if ((_activeChar == null) || !_activeChar.isOnline())
			return;
		
		// Ignore delayed respawns after removal, event end or transfer to another instance.
		if (_eventTeleport && (!FOSEvent.isStarted()
			|| !FOSEvent.isPlayerParticipant(_activeChar.getObjectId())
			|| (_activeChar.getInstanceWorld() != _instance)))
		{
			return;
		}
		// An old return task must not pull a player out of a newly joined event.
		if (!_eventTeleport && FOSEvent.isPlayerParticipant(_activeChar.getObjectId()))
		{
			return;
		}

		Summon summon = _activeChar.getPet();
		
		if (summon != null)
			summon.unSummon(_activeChar);
		
		if ((FOSConfig.FOS_EVENT_EFFECTS_REMOVAL == 0) || ((FOSConfig.FOS_EVENT_EFFECTS_REMOVAL == 1) && ((_activeChar.getTeam() == Team.NONE) || (_activeChar.isInDuel() && (_activeChar.getDuelState() != Duel.DUELSTATE_INTERRUPTED)))))
			_activeChar.stopAllEffectsExceptThoseThatLastThroughDeath();
		
		if (_activeChar.isInDuel())
			_activeChar.setDuelState(Duel.DUELSTATE_INTERRUPTED);
		
		_activeChar.doRevive();
		
	//	if (_activeChar instanceof FakePlayer && !FOSEvent.isStarted())
	//		_activeChar.teleToLocation(60608, -94016, -1344, 0);
	//	else
			_activeChar.teleToLocation((_coordinates[0] + Rnd.get(101)) - 50, (_coordinates[1] + Rnd.get(101)) - 50, _coordinates[2], 0, _eventTeleport ? _instance : null);
		
		if (_eventTeleport)
		{
			_activeChar.setTeam(FOSEvent.getParticipantTeamId(_activeChar.getObjectId()) == 0 ? Team.BLUE : Team.RED);
			PvpFlagTaskManager.getInstance().remove(_activeChar);
			_activeChar.updatePvPFlag(0);
		}
		else
			_activeChar.setTeam(Team.NONE);
		
		_activeChar.setCurrentCp(_activeChar.getMaxCp());
		_activeChar.setCurrentHp(_activeChar.getMaxHp());
		_activeChar.setCurrentMp(_activeChar.getMaxMp());
		
		_activeChar.broadcastStatusUpdate();
		_activeChar.broadcastUserInfo();
		FOSEvent.onTeleported(_activeChar);
	}
}
