package custom.events.championInvade;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.util.Broadcast;

import handlers.admincommandhandlers.AdminChampionInvade;

/**
 * @author Sarada
 * @author L2JMobius Grand Crusade / Master Class Adapted
 * @editor Corrección definitiva del Despawn para L2JMobius
 */
public class ChampionInvade
{
	private static final Logger LOGGER = Logger.getLogger(ChampionInvade.class.getName());
	
	public static boolean _started = false;
	public static boolean _aborted = false;
	public static boolean _finish = false;
	
	// CAMBIO CLAVE: Almacenamos los objetos Spawn en lugar de Npc para controlar los respawns.
	private static final List<Spawn> _activeSpawns = new CopyOnWriteArrayList<>();
	
	public static void StartedEvent() // Iniciar Evento
	{
		Broadcast.toAllOnlinePlayers("[Champion Invade Event]: Duracion: " + Config.EVENT_CHAMPION_FARM_TIME + " minuto(s)!");
		_aborted = false;
		_started = true;
		_finish = false;
		
		// Spawnear los monstruos del archivo XML
		spawnMonastery();
		
		// Ejecutamos el waiter de forma asíncrona para no bloquear el ThreadPool principal
		ThreadPool.execute(() -> waiter(Config.EVENT_CHAMPION_FARM_TIME * 60 * 1000));
	}
	
	// Finalizar Evento
	public static void Finish_Event()
	{
		_started = false;
		_aborted = true;
		_finish = true;
		
		// Despawnear/Eliminar todos los monstruos invocados y detener sus respawns
		despawnMonastery();
		
		Broadcast.toAllOnlinePlayers("[Champion Invade Event]: Finalizado!");
		Broadcast.toAllOnlinePlayers("[Champion Invade Event]: Proximo Evento en " + InitialChampionInvade.getInstance().getNextTime() + " horas!");
		
		try
		{
			if (!AdminChampionInvade._bestfarm_manual)
			{
				InitialChampionInvade.getInstance().StartCalculationOfNextEventTime();
			}
			else
			{
				AdminChampionInvade._bestfarm_manual = false;
			}
		}
		catch (NoClassDefFoundError | Exception e)
		{
			InitialChampionInvade.getInstance().StartCalculationOfNextEventTime();
		}
	}
	
	/**
	 * Carga el archivo XML de Monastery y realiza el spawn de cada registro.
	 */
	private static void spawnMonastery()
	{
		final File xmlFile = new File("data/Monastery.xml");
		if (!xmlFile.exists())
		{
			LOGGER.severe("[Champion Invade]: No se pudo encontrar el archivo XML de spawns en: " + xmlFile.getAbsolutePath());
			return;
		}
		
		try
		{
			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			final Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			final NodeList spawnNodes = doc.getElementsByTagName("spawn");
			for (int i = 0; i < spawnNodes.getLength(); i++)
			{
				final Node spawnNode = spawnNodes.item(i);
				if (spawnNode.getNodeType() == Node.ELEMENT_NODE)
				{
					final Element spawnElement = (Element) spawnNode;
					final String npcIdAttr = spawnElement.getAttribute("npcId");
					
					if (!npcIdAttr.isEmpty())
					{
						final int npcId = Integer.parseInt(npcIdAttr);
						final int x = Integer.parseInt(spawnElement.getAttribute("x"));
						final int y = Integer.parseInt(spawnElement.getAttribute("y"));
						final int z = Integer.parseInt(spawnElement.getAttribute("z"));
						final int heading = spawnElement.getAttribute("heading").isEmpty() ? 0 : Integer.parseInt(spawnElement.getAttribute("heading"));
						createSpawn(npcId, x, y, z, heading);
					}
					else
					{
						final NodeList npcNodes = spawnElement.getElementsByTagName("npc");
						for (int n = 0; n < npcNodes.getLength(); n++)
						{
							final Node npcNode = npcNodes.item(n);
							if (npcNode.getNodeType() == Node.ELEMENT_NODE)
							{
								final Element npcElement = (Element) npcNode;
								final int npcId = Integer.parseInt(npcElement.getAttribute("id"));
								
								if (!npcElement.getAttribute("x").isEmpty())
								{
									final int x = Integer.parseInt(npcElement.getAttribute("x"));
									final int y = Integer.parseInt(npcElement.getAttribute("y"));
									final int z = Integer.parseInt(npcElement.getAttribute("z"));
									final int heading = npcElement.getAttribute("heading").isEmpty() ? 0 : Integer.parseInt(npcElement.getAttribute("heading"));
									createSpawn(npcId, x, y, z, heading);
								}
								else
								{
									final NodeList pointNodes = spawnElement.getElementsByTagName("point");
									for (int p = 0; p < pointNodes.getLength(); p++)
									{
										final Node pointNode = pointNodes.item(p);
										if (pointNode.getNodeType() == Node.ELEMENT_NODE)
										{
											final Element pointElement = (Element) pointNode;
											final int x = Integer.parseInt(pointElement.getAttribute("x"));
											final int y = Integer.parseInt(pointElement.getAttribute("y"));
											final int z = Integer.parseInt(pointElement.getAttribute("z"));
											final int heading = pointElement.getAttribute("heading").isEmpty() ? 0 : Integer.parseInt(pointElement.getAttribute("heading"));
											createSpawn(npcId, x, y, z, heading);
										}
									}
								}
							}
						}
					}
				}
			}
			LOGGER.info("[Champion Invade]: Exito. Se han creado " + _activeSpawns.size() + " puntos de spawn de Monastery.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[Champion Invade]: Error al procesar el archivo Monastery.xml: ", e);
		}
	}
	
	/**
	 * Metodo auxiliar que crea el objeto Spawn en el mundo de juego.
	 */
	private static void createSpawn(int npcId, int x, int y, int z, int heading)
	{
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			if (template == null)
			{
				LOGGER.warning("[Champion Invade]: No se encontro la plantilla de datos para el NPC: " + npcId);
				return;
			}
			
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(x, y, z);
			spawn.setHeading(heading);
			spawn.setAmount(1);
			spawn.setRespawnDelay(10); // Tiempo de respawn durante el evento (en segundos)
			spawn.startRespawn(); // Permite que revivan mientras el evento esté activo
			
			spawn.doSpawn(false);
			_activeSpawns.add(spawn); // Guardamos el Spawn entero
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[Champion Invade]: Error spawneando ID " + npcId + ": ", e);
		}
	}
	
	/**
	 * Cancela todos los respawns y remueve del mapa los monstruos activos.
	 */
	private static void despawnMonastery()
	{
		int count = 0;
		for (Spawn spawn : _activeSpawns)
		{
			if (spawn != null)
			{
				try
				{
					// 1. Detiene los timers de respawn en el core de Mobius
					spawn.stopRespawn();
					
					// 2. Obtiene la instancia activa actual de este spawn y la borra
					final Npc npc = spawn.getLastSpawn();
					if (npc != null)
					{
						npc.deleteMe();
						count++;
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "[Champion Invade]: Error removiendo un spawn de Monastery: ", e);
				}
			}
		}
		_activeSpawns.clear(); // Limpiamos la lista para el próximo evento
		LOGGER.info("[Champion Invade]: Se han removido " + count + " monstruos activos de Monastery.");
	}
	
	protected static void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000L);
		
		while (((startWaiterTime + interval) > System.currentTimeMillis()) && (!_aborted))
		{
			seconds--;
			
			if (_started)
			{
				if (seconds == 3600)
				{
					Broadcast.toAllOnlinePlayers("[Champion Invade Event]: " + (seconds / 3600) + " hora(s) para finalizar!");
				}
				else if ((seconds == 60) || (seconds == 120) || (seconds == 180) || (seconds == 240) || (seconds == 300) || (seconds == 600) || (seconds == 900) || (seconds == 1800))
				{
					Broadcast.toAllOnlinePlayers("[Champion Invade Event]: " + (seconds / 60) + " minuto(s) para finalizar!");
				}
				else if ((seconds == 1) || (seconds == 2) || (seconds == 3) || (seconds == 10) || (seconds == 15) || (seconds == 30))
				{
					Broadcast.toAllOnlinePlayers("[Champion Invade Event]: " + seconds + " segundo(s) para finalizar!");
				}
			}
			
			try
			{
				Thread.sleep(1000L);
			}
			catch (InterruptedException ie)
			{
				LOGGER.log(Level.SEVERE, "ChampionInvade: Waiter loop interrumpido: ", ie);
			}
		}
		
		if (!_aborted)
		{
			Finish_Event();
		}
	}
	
	public static boolean is_started()
	{
		return _started;
	}
	
	public static boolean is_finish()
	{
		return _finish;
	}
}