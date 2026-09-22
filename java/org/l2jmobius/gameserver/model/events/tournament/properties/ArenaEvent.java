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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;

public class ArenaEvent
{
	private static final ArenaEvent INSTANCE = new ArenaEvent();
	protected static final Logger LOGGER = Logger.getLogger(ArenaEvent.class.getName());
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private volatile ZonedDateTime nextEvent;
	private ScheduledFuture<?> scheduledStart;
	private long scheduleVersion;

	public static ArenaEvent getInstance()
	{
		return INSTANCE;
	}

	private ArenaEvent()
	{
	}

	public String getNextTime()
	{
		final ZonedDateTime next = nextEvent;
		return next == null ? "No programado" : FORMAT.format(next);
	}

	// A missing property defaults to ALL in ArenaConfig. Invalid values fail closed.
	static Set<DayOfWeek> parseDays(String value)
	{
		if ((value == null) || value.trim().isEmpty())
		{
			throw new IllegalArgumentException("TournamentDays esta vacio");
		}
		if ("ALL".equalsIgnoreCase(value.trim()))
		{
			return EnumSet.allOf(DayOfWeek.class);
		}
		final Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		for (String day : value.split(",", -1))
		{
			days.add(DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ROOT)));
		}
		return days;
	}

	static ZonedDateTime calculateNext(ZonedDateTime now, String daysValue, String[] times)
	{
		final Set<DayOfWeek> days = parseDays(daysValue);
		if ((times == null) || (times.length == 0))
		{
			throw new IllegalArgumentException("TournamentStartTime esta vacio");
		}
		ZonedDateTime next = null;
		for (String value : times)
		{
			if ((value == null) || !value.trim().matches("[0-9]{1,2}:[0-9]{2}"))
			{
				throw new IllegalArgumentException("Horario invalido: " + value);
			}
			final String[] parts = value.trim().split(":");
			final LocalTime time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			// Include day 7: today's last start may already have passed.
			for (int offset = 0; offset <= 7; offset++)
			{
				final java.time.LocalDate date = now.toLocalDate().plusDays(offset);
				if (!days.contains(date.getDayOfWeek()))
				{
					continue;
				}
				final ZonedDateTime candidate = date.atTime(time).atZone(now.getZone());
				if (candidate.isAfter(now) && ((next == null) || candidate.isBefore(next)))
				{
					next = candidate;
				}
			}
		}
		return next;
	}

	public synchronized void StartCalculationOfNextEventTime()
	{
		final long version = ++scheduleVersion;
		if (scheduledStart != null)
		{
			scheduledStart.cancel(false);
			scheduledStart = null;
		}
		nextEvent = null;
		if (!ArenaConfig.TOURNAMENT_EVENT_TIME)
		{
			return;
		}
		try
		{
			final ZonedDateTime now = ZonedDateTime.now();
			nextEvent = calculateNext(now, ArenaConfig.TOURNAMENT_EVENT_DAYS, ArenaConfig.TOURNAMENT_EVENT_INTERVAL_BY_TIME_OF_DAY);
			final long delay = Math.max(0L, nextEvent.toInstant().toEpochMilli() - System.currentTimeMillis());
			scheduledStart = ThreadPool.schedule(() -> startScheduledEvent(version), delay);
			LOGGER.info("Tournament: Next Event " + nextEvent);
		}
		catch (Exception e)
		{
			nextEvent = null;
			LOGGER.warning("Tournament: No se pudo programar el evento. Revisar TournamentDays y TournamentStartTime: " + e.getMessage());
		}
	}

	private void startScheduledEvent(long version)
	{
		synchronized (this)
		{
			if ((version != scheduleVersion) || !ArenaConfig.TOURNAMENT_EVENT_TIME)
			{
				return;
			}
			scheduledStart = null;
			nextEvent = null;
			// A manual event already running schedules its successor when it ends.
			if (ArenaTask.is_started())
			{
				return;
			}
		}
		// SpawnEvent holds ArenaTask's monitor for the event duration.
		// Do not hold this scheduler's monitor while entering it.
		ArenaTask.SpawnEvent();
	}
}
