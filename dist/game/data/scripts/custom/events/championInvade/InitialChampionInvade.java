package custom.events.championInvade;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.quest.Quest; // Importación de la clase Quest

/**
 * @author Sarada
 * @author L2JMobius Grand Crusade / Master Class Adapted
 * @editor Adaptación para inicio automático en Server Boot
 */
public class InitialChampionInvade extends Quest // Heredamos de Quest para carga automática en arranque
{
	private static final Logger LOGGER = Logger.getLogger(InitialChampionInvade.class.getName());
	private static InitialChampionInvade _instance = null;
	
	private Calendar _nextEvent;
	private final SimpleDateFormat _format = new SimpleDateFormat("HH:mm");
	
	public static InitialChampionInvade getInstance()
	{
		return _instance;
	}
	
	// El constructor DEBE ser público para que Mobius lo instancie en el boot
	public InitialChampionInvade()
	{
		super(-1); // -1 indica que es un script custom/evento y no una quest del juego
		_instance = this;
		
		// Iniciamos el cálculo de la hora del primer evento automáticamente en el arranque
		StartCalculationOfNextEventTime();
	}
	
	public String getRestartNextTime()
	{
		if ((_nextEvent != null) && (_nextEvent.getTime() != null))
		{
			return _format.format(_nextEvent.getTime());
		}
		return "Error";
	}
	
	public String getNextTime()
	{
		Calendar next = getNextEventTime();
		if ((next != null) && (next.getTime() != null))
		{
			return _format.format(next.getTime());
		}
		return "Error";
	}
	
	public Calendar getNextEventTime()
	{
		try
		{
			if ((Config.EVENT_CHAMPION_FARM_INTERVAL_BY_TIME_OF_DAY == null) || (Config.EVENT_CHAMPION_FARM_INTERVAL_BY_TIME_OF_DAY.length == 0))
			{
				return null;
			}
			
			Calendar currentTime = Calendar.getInstance();
			long flush2 = 0;
			long timeL = 0;
			int count = 0;
			Calendar nextEvent = null;
			
			for (String timeOfDay : Config.EVENT_CHAMPION_FARM_INTERVAL_BY_TIME_OF_DAY)
			{
				if ((timeOfDay == null) || timeOfDay.trim().isEmpty())
				{
					continue;
				}
				
				Calendar testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.trim().split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 0);
				testStartTime.set(Calendar.MILLISECOND, 0);
				
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
				
				if (count == 0)
				{
					flush2 = timeL;
					nextEvent = testStartTime;
				}
				
				if (timeL < flush2)
				{
					flush2 = timeL;
					nextEvent = testStartTime;
				}
				count++;
			}
			return nextEvent;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[Champion Invade]: Error al calcular proximo tiempo de evento: ", e);
			return null;
		}
	}
	
	public void StartCalculationOfNextEventTime()
	{
		try
		{
			if ((Config.EVENT_CHAMPION_FARM_INTERVAL_BY_TIME_OF_DAY == null) || (Config.EVENT_CHAMPION_FARM_INTERVAL_BY_TIME_OF_DAY.length == 0))
			{
				LOGGER.warning("[Champion Invade Event]: No se han definido horarios en la configuracion!");
				return;
			}
			
			Calendar currentTime = Calendar.getInstance();
			long flush2 = 0L;
			long timeL = 0L;
			int count = 0;
			
			for (String timeOfDay : Config.EVENT_CHAMPION_FARM_INTERVAL_BY_TIME_OF_DAY)
			{
				if ((timeOfDay == null) || timeOfDay.trim().isEmpty())
				{
					continue;
				}
				
				Calendar testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.trim().split(":");
				
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 0);
				testStartTime.set(Calendar.MILLISECOND, 0);
				
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
				
				if (count == 0)
				{
					flush2 = timeL;
					_nextEvent = testStartTime;
				}
				
				if (timeL < flush2)
				{
					flush2 = timeL;
					_nextEvent = testStartTime;
				}
				count++;
			}
			
			if (_nextEvent != null)
			{
				LOGGER.info("[Champion Invade Event]: Proximo Evento programado para: " + _nextEvent.getTime().toString());
				ThreadPool.schedule(new StartEventTask(), flush2);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[Champion Invade Event]: ¡Error en la configuracion o formato de horas del evento!", e);
		}
	}
	
	private class StartEventTask implements Runnable
	{
		@Override
		public void run()
		{
			LOGGER.info("[Champion Invade Event]: Iniciando Tarea del Evento.");
			ChampionInvade.StartedEvent();
		}
	}
	
	// ESTE METODO ES INDISPENSABLE para que el cargador de scripts de Mobius inicialice la clase al arrancar
	public static void main(String[] args)
	{
		new InitialChampionInvade();
	}
}