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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;

/**
 * @author Axcel Kuhn (Baseado no Restart System)
 */
public class ArenaEvent
{
	private static final ArenaEvent INSTANCE = new ArenaEvent();
	protected static final Logger LOGGER = Logger.getLogger(ArenaEvent.class.getName());
	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public static ArenaEvent getInstance()
	{
		return INSTANCE;
	}
	
	public String getNextTime()
	{
		if (NextEvent != null)
		{
			return format.format(NextEvent.getTime());
		}
		return "No programado";
	}
	
	private ArenaEvent()
	{
	}
	
	public synchronized void StartCalculationOfNextEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar testStartTime = null;
			long flush2 = 0, timeL = 0;
			int count = 0;
			
			for (String timeOfDay : ArenaConfig.TOURNAMENT_EVENT_INTERVAL_BY_TIME_OF_DAY)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 00);
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
				
				if (count == 0)
				{
					flush2 = timeL;
					NextEvent = testStartTime;
				}
				
				if (timeL < flush2)
				{
					flush2 = timeL;
					NextEvent = testStartTime;
				}
				count++;
			}
			LOGGER.info("Tournament: Next Event " + NextEvent.getTime().toString());
			ThreadPool.schedule(new StartEventTask(), flush2);
		}
		catch (Exception e)
		{
			LOGGER.warning("Tournament: No se pudo calcular el proximo evento: " + e.getMessage());
		}
	}
	
	class StartEventTask implements Runnable
	{
		@Override
		public void run()
		{
			LOGGER.info("----------------------------------------------------------------------------");
			LOGGER.info("Tournament: Event Started.");
			LOGGER.info("----------------------------------------------------------------------------");
			ArenaTask.SpawnEvent();
		}
	}
}
