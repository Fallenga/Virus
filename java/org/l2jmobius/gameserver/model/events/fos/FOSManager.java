package org.l2jmobius.gameserver.model.events.fos;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.util.Broadcast;

public class FOSManager
{
	protected static final Logger _log = Logger.getLogger(FOSManager.class.getName());
	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	/** Task for event cycles<br> */
	private CTFStartTask _task;

	private void cancelPendingTask()
	{
		if ((_task != null) && (_task.nextRun != null))
		{
			_task.nextRun.cancel(false);
		}
		_task = null;
	}

	public static void init()
	{
		FOSConfig.init();
		AdminCommandHandler.getInstance().registerHandler(new AdminFOSEvent());
		VoicedCommandHandler.getInstance().registerHandler(new VoicedFOSEvent());
		getInstance();
	}
	
	/**
	 * New instance only by getInstance()<br>
	 */
	protected FOSManager()
	{
		if (FOSConfig.FOS_EVENT_ENABLED)
		{
			// Cannot start if both teams have same name
			if (!FOSConfig.FOS_EVENT_TEAM_1_NAME.equalsIgnoreCase(FOSConfig.FOS_EVENT_TEAM_2_NAME))
			{
				FOSEvent.init();
				
				scheduleEventStart();
				_log.info("Fortress Engine: is Started.");
			}
			else
			{
				_log.info("Fortress Engine: is uninitiated. Cannot start if both teams have same name!");
			}
		}
		else
		{
			_log.info("Fortress Engine: is disabled.");
		}
	}
	
	/**
	 * Initialize new/Returns the one and only instance<br>
	 * <br>
	 * @return CTFManager<br>
	 */
	public static FOSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Starts CTFStartTask
	 */
	public synchronized void scheduleEventStart()
	{
		cancelPendingTask();
		if (!FOSConfig.FOS_EVENT_ENABLED)
		{
			return;
		}
		
		try
		{
			NextEvent = getNextEventTime();
			if (NextEvent == null)
			{
				_log.warning("Fortress: no configured event times or days.");
				return;
			}
			final long flush2 = NextEvent.getTimeInMillis() - System.currentTimeMillis();
			_log.info("[FOSEventEngine Event]: Proximo Evento: " + NextEvent.getTime().toString());
			_task = new CTFStartTask(System.currentTimeMillis() + flush2);
			_task.nextRun = ThreadPool.schedule(_task, flush2);
		}
		catch (Exception e)
		{
			System.out.println("[FOSEventEngine Event]: Algum erro nas config foi encontrado!");
		}
		
		
	}

	/**
	 * Method to start participation
	 */
	public synchronized void startReg()
	{
		if (!FOSEvent.isInactive())
		{
			return;
		}
		cancelPendingTask();
		_task = new CTFStartTask(System.currentTimeMillis());
		if (!FOSEvent.startParticipation())
		{
			Broadcast.toAllOnlinePlayers("Fortress: Event was cancelled.");
			_log.warning("FOSEventEngine[FOSManager.run()]: Error spawning event npc for participation.");
			
			scheduleEventStart();
		}
		else
		{
			Broadcast.toAllOnlinePlayers("Fortress: Joinable in " + FOSConfig.FOS_NPC_LOC_NAME + "!");
			
			if (FOSConfig.ALLOW_TVTFOS_COMMANDS)
			{
				Broadcast.toAllOnlinePlayers("Fortress: Command: .fosjoin / .fosleave / .fosinfo");
			} 
			
			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + (60000L * FOSConfig.FOS_EVENT_PARTICIPATION_TIME));
			 ThreadPool.execute(_task);
		}
	}
	
	/**
	 * Method to start the fight
	 */
	public synchronized void startEvent()
	{
		if (!FOSEvent.isParticipating())
		{
			return;
		}
		cancelPendingTask();
		_task = new CTFStartTask(System.currentTimeMillis());
		if (!FOSEvent.startFight())
		{
			Broadcast.toAllOnlinePlayers("Fortress: Event cancelled due to lack of Participation.");
			_log.info("FOSEventEngine[FOSManager.run()]: Lack of registration, abort event.");
			
			scheduleEventStart();
		}
		else
		{
			FOSEvent.sysMsgToAllParticipants("Teleporting in " + FOSConfig.FOS_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + (60000L * FOSConfig.FOS_EVENT_RUNNING_TIME));
			 ThreadPool.execute(_task);
		}
	}
	
	/**
	 * Method to end the event and reward
	 */
	public synchronized void endEvent()
	{
		if (!FOSEvent.isStarted())
		{
			return;
		}
		cancelPendingTask();
		_task = new CTFStartTask(System.currentTimeMillis());
		Broadcast.toAllOnlinePlayers(FOSEvent.calculateRewards());
		FOSEvent.sysMsgToAllParticipants("Teleporting back town in " + FOSConfig.FOS_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		FOSEvent.stopFight();
		
		scheduleEventStart();
	}
	
	public synchronized void skipDelay()
	{
		if ((_task != null) && (_task.nextRun != null) && _task.nextRun.cancel(false))
		{
			_task.setStartTime(System.currentTimeMillis());
			 ThreadPool.execute(_task);
		}
	}
	
	/**
	 * Class for CTF cycles
	 */
	class CTFStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;
		
		public CTFStartTask(long startTime)
		{
			_startTime = startTime;
		}
		
		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}
		
		@Override
		public void run()
		{
			synchronized (FOSManager.this)
			{
				if (_task != this)
				{
					return;
				}
			int delay = (int) Math.ceil((_startTime - System.currentTimeMillis()) / 1000.0);
			
			if (delay > 0)
			{
				announce(delay);
			}
			
			int nextMsg = 0;
			if (delay > 3600)
			{
				nextMsg = delay - 3600;
			}
			else if (delay > 1800)
			{
				nextMsg = delay - 1800;
			}
			else if (delay > 900)
			{
				nextMsg = delay - 900;
			}
			else if (delay > 600)
			{
				nextMsg = delay - 600;
			}
			else if (delay > 300)
			{
				nextMsg = delay - 300;
			}
			else if (delay > 60)
			{
				nextMsg = delay - 60;
			}
			else if (delay > 5)
			{
				nextMsg = delay - 5;
			}
			else if (delay > 0)
			{
				nextMsg = delay;
			}
			else
			{
				// start
				if (FOSEvent.isInactive())
				{
					startReg();
				}
				else if (FOSEvent.isParticipating())
				{
					startEvent();
				}
				else
				{
					endEvent();
				}
			}
			
			if (delay > 0)
			{
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
			}
		}
		
		}

		private void announce(long time)
		{
			if ((time >= 3600) && ((time % 3600) == 0))
			{
				if (FOSEvent.isParticipating())
				{
					Broadcast.toAllOnlinePlayers("Fortress: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (FOSEvent.isStarted())
				{
					FOSEvent.sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (FOSEvent.isParticipating())
				{
					Broadcast.toAllOnlinePlayers("Fortress: " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (FOSEvent.isStarted())
				{
					FOSEvent.sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
				}
			}
			else
			{
				if (FOSEvent.isParticipating())
				{
					Broadcast.toAllOnlinePlayers("Fortress: " + time + " second(s) until registration is closed!");
				}
				else if (FOSEvent.isStarted())
				{
					FOSEvent.sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final FOSManager _instance = new FOSManager();
	}

 
	
	public String getNextTime()
	{
		final Calendar next = getNextEventTime();
		if (next != null)
			return format.format(next.getTime());
		return "Erro";
	}
	public Calendar getNextEventTime()
	{
		try
		{
			final Calendar currentTime = Calendar.getInstance();
			Calendar nextEvent = null;
			for (int dayOffset = 0; dayOffset <= 7; dayOffset++)
			{
				final Calendar eventDay = (Calendar) currentTime.clone();
				eventDay.add(Calendar.DAY_OF_MONTH, dayOffset);
				if (!FOSConfig.isEventDay(eventDay))
				{
					continue;
				}

				for (String timeOfDay : FOSConfig.FOS_EVENT_INTERVAL)
				{
					final String[] splitTimeOfDay = timeOfDay.trim().split(":");
					if (splitTimeOfDay.length != 2)
					{
						throw new IllegalArgumentException("Invalid FOSEventInterval value: " + timeOfDay);
					}

					final Calendar testStartTime = (Calendar) eventDay.clone();
					testStartTime.setLenient(false);
					testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
					testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
					testStartTime.set(Calendar.SECOND, 0);
					testStartTime.set(Calendar.MILLISECOND, 0);
					if ((dayOffset == 0) && (testStartTime.getTimeInMillis() <= currentTime.getTimeInMillis()))
					{
						continue;
					}

					if ((nextEvent == null) || (testStartTime.getTimeInMillis() < nextEvent.getTimeInMillis()))
					{
						nextEvent = testStartTime;
					}
				}
			}
			return nextEvent;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("FOSEventEngine Invade]: "+e);
			return null;
		}
		
	}
	
	
}
