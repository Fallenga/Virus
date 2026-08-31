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
package org.l2jmobius.gameserver.geoengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.geodata.GeoData;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.Region;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.util.GeoUtils;
import org.l2jmobius.gameserver.util.LinePointIterator;
import org.l2jmobius.gameserver.util.LinePointIterator3D;

/**
 * GeoEngine.
 * @author -Nemesiss-, HorridoJoho, Optimized by Community, Rusacis 3D physics & L2jAcis adaptation.
 */
public class GeoEngine
{
	private static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());
	
	public static final String FILE_NAME_FORMAT = "%d_%d.l2j";
	
	private static final int ELEVATED_SEE_OVER_DISTANCE = 2;
	private static final int MAX_SEE_OVER_HEIGHT = 48;
	private static final int SPAWN_Z_DELTA_LIMIT = 100;
	
	// CONFIGURACIONES DE PRECISIÓN
	private static final int EYE_HEIGHT = 45; // Altura por defecto para objetos estáticos.
	private static final int MAX_Z_DIFF = 64; // Máxima diferencia de altura permitida por celda.
	private static final int MOVEMENT_LAYER_TOLERANCE = 48; // Igual al CELL_IGNORE_HEIGHT de la GeoEngine de referencia (6 * 8).
	private static final int MAX_MOVEMENT_LAYER_STEP = 192; // Protección ante saltos accidentales entre capas/pisos.
	
	private final GeoData _geodata = new GeoData();
	private PrintWriter _geoBugReports = null; // MEJORA ACIS: Impresor de reportes de bugs de geodata
	
	protected GeoEngine()
	{
		int loadedRegions = 0;
		try
		{
			for (int regionX = World.TILE_X_MIN; regionX <= World.TILE_X_MAX; regionX++)
			{
				for (int regionY = World.TILE_Y_MIN; regionY <= World.TILE_Y_MAX; regionY++)
				{
					final Path geoFilePath = Config.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
					if (Files.exists(geoFilePath))
					{
						try
						{
							_geodata.loadRegion(geoFilePath, regionX, regionY);
							loadedRegions++;
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load " + geoFilePath.getFileName() + "!", e);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed to load geodata!", e);
			System.exit(1);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + loadedRegions + " regions.");
		
		// Avoid wrong configuration when no files are loaded.
		if ((loadedRegions == 0) && (Config.PATHFINDING > 0))
		{
			Config.PATHFINDING = 0;
			LOGGER.info(getClass().getSimpleName() + ": Pathfinding is disabled.");
		}
	}
	
	/**
	 * Returns the lower floor height under the given world Z for the provided world X/Y.
	 */
	public int getLowerHeight(int x, int y, int z)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		if (!hasGeoPos(geoX, geoY))
		{
			return z;
		}
		return getNextLowerZ(geoX, geoY, z + 20);
	}
	
	public boolean hasGeoPos(int geoX, int geoY)
	{
		return _geodata.hasGeoPos(geoX, geoY);
	}
	
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return _geodata.checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	/**
	 * MEJORA: Lógica Anti-CornerCut corregida y mejorada. Evita que monstruos y jugadores caminen o ataquen a través de las esquinas diagonales de las paredes.
	 */
	public boolean checkNearestNsweAntiCornerCut(int geoX, int geoY, int worldZ, int nswe)
	{
		boolean can = true;
		
		if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
		{
			boolean pathNorth = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_NORTH) && checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_EAST);
			boolean pathEast = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_EAST) && checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH);
			can = pathNorth || pathEast;
		}
		
		if (can && ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST))
		{
			boolean pathNorth = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_NORTH) && checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_WEST);
			boolean pathWest = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_WEST) && checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_NORTH);
			can = pathNorth || pathWest;
		}
		
		if (can && ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST))
		{
			boolean pathSouth = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_SOUTH) && checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_EAST);
			boolean pathEast = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_EAST) && checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH); // Corregido el North por South del L2OFF original
			can = pathSouth || pathEast;
		}
		
		if (can && ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST))
		{
			boolean pathSouth = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_SOUTH) && checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_WEST);
			boolean pathWest = checkNearestNswe(geoX, geoY, worldZ, Cell.NSWE_WEST) && checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH);
			can = pathSouth || pathWest;
		}
		
		return can && checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		_geodata.setNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		_geodata.unsetNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _geodata.getNearestZ(geoX, geoY, worldZ);
	}
	
	/**
	 * MEJORA: Obtiene la altura en pendientes de forma suavizada, evitando saltos bruscos. Útil para movimiento de mobs en terrenos inclinados.
	 */
	public int getSmoothZ(int x, int y, int z, int prevZ)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		
		if (!hasGeoPos(geoX, geoY))
		{
			return z;
		}
		
		final int nearestZ = getNearestZ(geoX, geoY, z);
		final int nextLowerZ = getNextLowerZ(geoX, geoY, z + 20);
		
		// Si la diferencia con la altura anterior es muy grande, intentar suavizar
		if (Math.abs(nearestZ - prevZ) > (MAX_Z_DIFF * 2))
		{
			// Usar la altura más cercana a la anterior
			final int diffToNearest = Math.abs(nearestZ - prevZ);
			final int diffToLower = Math.abs(nextLowerZ - prevZ);
			return diffToNearest < diffToLower ? nearestZ : nextLowerZ;
		}
		
		// Si estamos en una pendiente, usar la altura que está entre la actual y la anterior
		if (Math.abs(nearestZ - nextLowerZ) > MAX_Z_DIFF)
		{
			// Interpolación para pendientes suaves
			final int diff = nearestZ - nextLowerZ;
			if ((diff > 0) && (diff < 200))
			{
				final int interpZ = nextLowerZ + (diff / 2);
				if (Math.abs(interpZ - prevZ) < Math.abs(nearestZ - prevZ))
				{
					return interpZ;
				}
			}
		}
		
		return nearestZ;
	}
	
	/**
	 * Obtiene la capa de suelo para el siguiente paso de movimiento manteniendo como referencia la capa que la criatura venía pisando. La lógica está adaptada del GeoEngine de referencia: en vez de elegir libremente el Z más cercano, busca la capa inmediatamente inferior a previousZ + 48. Esto
	 * evita saltos entre pisos en bloques multilayer, puentes, curvas y pendientes.
	 */
	private int getMovementLayerZ(int geoX, int geoY, int previousZ)
	{
		if (!hasGeoPos(geoX, geoY))
		{
			return previousZ;
		}
		
		final int lowerZ = getNextLowerZ(geoX, geoY, previousZ + MOVEMENT_LAYER_TOLERANCE);
		if (Math.abs(lowerZ - previousZ) <= MAX_MOVEMENT_LAYER_STEP)
		{
			return lowerZ;
		}
		
		// Fallback conservador: sólo aceptar nearest si permanece cerca de la capa actual.
		final int nearestZ = getNearestZ(geoX, geoY, previousZ);
		return Math.abs(nearestZ - previousZ) <= MAX_MOVEMENT_LAYER_STEP ? nearestZ : previousZ;
	}
	
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return _geodata.getNextLowerZ(geoX, geoY, worldZ);
	}
	
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return _geodata.getNextHigherZ(geoX, geoY, worldZ);
	}
	
	public int getGeoX(int worldX)
	{
		return _geodata.getGeoX(worldX);
	}
	
	public int getGeoY(int worldY)
	{
		return _geodata.getGeoY(worldY);
	}
	
	public int getWorldX(int geoX)
	{
		return _geodata.getWorldX(geoX);
	}
	
	public int getWorldY(int geoY)
	{
		return _geodata.getWorldY(geoY);
	}
	
	public IRegion getRegion(int geoX, int geoY)
	{
		return _geodata.getRegion(geoX, geoY);
	}
	
	public void setRegion(int regionX, int regionY, Region region)
	{
		_geodata.setRegion(regionX, regionY, region);
	}
	
	public int getHeight(int x, int y, int z)
	{
		return getNearestZ(getGeoX(x), getGeoY(y), z);
	}
	
	public int getSpawnHeight(int x, int y, int z)
	{
		final int geoX = getGeoX(x);
		final int geoY = getGeoY(y);
		
		if (!hasGeoPos(geoX, geoY))
		{
			return z;
		}
		
		final int nextLowerZ = getNextLowerZ(geoX, geoY, z + 20);
		return Math.abs(nextLowerZ - z) <= SPAWN_Z_DELTA_LIMIT ? nextLowerZ : z;
	}
	
	public int getSpawnHeight(Location location)
	{
		return getSpawnHeight(location.getX(), location.getY(), location.getZ());
	}
	
	/**
	 * MEJORA ACIS: Se calcula la línea de visión (LOS) con alturas dinámicas según la escala real de colisión del modelo.
	 */
	public boolean canSeeTarget(WorldObject cha, WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		if (target.isDoor())
		{
			return true;
		}
		
		// Altura de visión origen dinámica basada en el tamaño real del personaje
		int eyeHeight = EYE_HEIGHT;
		if (cha instanceof Creature)
		{
			eyeHeight = (int) (((Creature) cha).getCollisionHeight() * 2 * 0.8);
		}
		
		// Altura de visión destino dinámica basada en el tamaño real del objetivo
		int targetEyeHeight = EYE_HEIGHT;
		if (target instanceof Creature)
		{
			targetEyeHeight = (int) (((Creature) target).getCollisionHeight() * 2 * 0.8);
		}
		
		return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), eyeHeight, target.getX(), target.getY(), target.getZ(), targetEyeHeight, cha.getInstanceWorld());
	}
	
	public boolean canSeeTarget(WorldObject cha, ILocational worldPosition)
	{
		return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), cha.getInstanceWorld());
	}
	
	public boolean canSeeTarget(int x, int y, int z, Instance instance, int tx, int ty, int tz, Instance tInstance)
	{
		return (instance == tInstance) && canSeeTarget(x, y, z, tx, ty, tz, instance);
	}
	
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		return canSeeTarget(x, y, z, EYE_HEIGHT, tx, ty, tz, EYE_HEIGHT, instance);
	}
	
	/**
	 * Sobrecarga interna para permitir cálculo con alturas de ojos/pecho personalizadas dinámicas.
	 */
	public boolean canSeeTarget(int x, int y, int z, int eyeOffset, int tx, int ty, int tz, int targetEyeOffset, Instance instance)
	{
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, tz, instance, true))
		{
			return false;
		}
		
		if (FenceData.getInstance().checkIfFenceBetween(x, y, z, tx, ty, tz, instance))
		{
			return false;
		}
		
		return canSeeTargetInternal(x, y, z, eyeOffset, tx, ty, tz, targetEyeOffset, true);
	}
	
	private int getLosGeoZ(int prevX, int prevY, int prevGeoZ, int curX, int curY, int nswe)
	{
		if ((((nswe & Cell.NSWE_NORTH) != 0) && ((nswe & Cell.NSWE_SOUTH) != 0)) || (((nswe & Cell.NSWE_WEST) != 0) && ((nswe & Cell.NSWE_EAST) != 0)))
		{
			throw new RuntimeException("Multiple directions!");
		}
		return checkNearestNsweAntiCornerCut(prevX, prevY, prevGeoZ, nswe) ? getNearestZ(curX, curY, prevGeoZ) : getNextHigherZ(curX, curY, prevGeoZ);
	}
	
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return canSeeTargetInternal(x, y, z, EYE_HEIGHT, tx, ty, tz, EYE_HEIGHT, true);
	}
	
	/**
	 * MEJORA: Utiliza los desplazamientos dinámicos de los ojos/pecho calculados para simular de forma perfecta la visión.
	 */
	private boolean canSeeTargetInternal(int x, int y, int z, int eyeOffset, int tx, int ty, int tz, int targetEyeOffset, boolean performReverse)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		
		z = getNearestZ(geoX, geoY, z);
		tz = getNearestZ(tGeoX, tGeoY, tz);
		
		// Fastpath
		if ((geoX == tGeoX) && (geoY == tGeoY))
		{
			if (hasGeoPos(tGeoX, tGeoY))
			{
				return z == tz;
			}
			return true;
		}
		
		// Aplicar desplazamiento dinámico de altura para la línea de visión realista
		int eyeZ = z + eyeOffset;
		int eyeTz = tz + targetEyeOffset;
		
		if (eyeTz > eyeZ)
		{
			int tmp = tx;
			tx = x;
			x = tmp;
			
			tmp = ty;
			ty = y;
			y = tmp;
			
			tmp = eyeTz;
			eyeTz = eyeZ;
			eyeZ = tmp;
			
			tmp = tGeoX;
			tGeoX = geoX;
			geoX = tmp;
			
			tmp = tGeoY;
			tGeoY = geoY;
			geoY = tmp;
		}
		
		LinePointIterator3D pointIter = new LinePointIterator3D(geoX, geoY, eyeZ, tGeoX, tGeoY, eyeTz);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = pointIter.z();
		int prevGeoZ = prevZ;
		int ptIndex = 0;
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			
			if ((curX == prevX) && (curY == prevY))
			{
				continue;
			}
			
			int beeCurZ = pointIter.z();
			int curGeoZ = prevGeoZ;
			
			if (hasGeoPos(curX, curY))
			{
				int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				curGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, curX, curY, nswe);
				int maxHeight;
				if (ptIndex < ELEVATED_SEE_OVER_DISTANCE)
				{
					maxHeight = eyeZ + MAX_SEE_OVER_HEIGHT;
				}
				else
				{
					maxHeight = beeCurZ + MAX_SEE_OVER_HEIGHT;
				}
				
				boolean canSeeThrough = false;
				if (curGeoZ <= maxHeight)
				{
					if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
					{
						int northGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY - 1, Cell.NSWE_EAST);
						int eastGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX + 1, prevY, Cell.NSWE_NORTH);
						canSeeThrough = (northGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(prevX, prevY - 1, beeCurZ)) && (eastGeoZ <= getNearestZ(prevX + 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST)
					{
						int northGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY - 1, Cell.NSWE_WEST);
						int westGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX - 1, prevY, Cell.NSWE_NORTH);
						canSeeThrough = (northGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(prevX, prevY - 1, beeCurZ)) && (westGeoZ <= getNearestZ(prevX - 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST)
					{
						int southGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY + 1, Cell.NSWE_EAST);
						int eastGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX + 1, prevY, Cell.NSWE_SOUTH);
						canSeeThrough = (southGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(prevX, prevY + 1, beeCurZ)) && (eastGeoZ <= getNearestZ(prevX + 1, prevY, beeCurZ));
					}
					else if ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST)
					{
						int southGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX, prevY + 1, Cell.NSWE_WEST);
						int westGeoZ = getLosGeoZ(prevX, prevY, prevGeoZ, prevX - 1, prevY, Cell.NSWE_SOUTH);
						canSeeThrough = (southGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(prevX, prevY + 1, beeCurZ)) && (westGeoZ <= getNearestZ(prevX - 1, prevY, beeCurZ));
					}
					else
					{
						canSeeThrough = true;
					}
				}
				
				if (!canSeeThrough)
				{
					return false;
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevGeoZ = curGeoZ;
			++ptIndex;
		}
		
		return true;
	}
	
	public Location getValidLocation(ILocational origin, ILocational destination)
	{
		return getValidLocation(origin.getX(), origin.getY(), origin.getZ(), destination.getX(), destination.getY(), destination.getZ(), null);
	}
	
	/**
	 * MEJORA: Agregada validación MAX_Z_DIFF para prevenir que los mobs suban acantilados de forma instantánea o que caigan al vacío inexplicablemente.
	 */
	public Location getValidLocation(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		z = getNearestZ(geoX, geoY, z);
		
		final int tGeoX = getGeoX(tx);
		final int tGeoY = getGeoY(ty);
		final int targetGeoZ = getNearestZ(tGeoX, tGeoY, tz);
		
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, targetGeoZ, instance, false))
		{
			return new Location(x, y, z);
		}
		
		if (FenceData.getInstance().checkIfFenceBetween(x, y, z, tx, ty, targetGeoZ, instance))
		{
			return new Location(x, y, z);
		}
		
		final LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = z;
		
		while (pointIter.next())
		{
			final int curX = pointIter.x();
			final int curY = pointIter.y();
			
			if (hasGeoPos(prevX, prevY))
			{
				final int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
			}
			
			final int curZ = getMovementLayerZ(curX, curY, prevZ);
			if (Math.abs(curZ - prevZ) > MAX_MOVEMENT_LAYER_STEP)
			{
				return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
			}
			
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		
		// Igual que la GeoEngine de referencia: si llegamos al mismo X/Y pero el piso
		// objetivo no pertenece a la capa recorrida, no saltar al otro piso.
		if (Math.abs(prevZ - targetGeoZ) > MOVEMENT_LAYER_TOLERANCE)
		{
			return new Location(x, y, z);
		}
		
		return new Location(tx, ty, prevZ);
	}
	
	public boolean canMoveToTarget(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, Instance instance)
	{
		int geoX = getGeoX(fromX);
		int geoY = getGeoY(fromY);
		fromZ = getNearestZ(geoX, geoY, fromZ);
		int tGeoX = getGeoX(toX);
		int tGeoY = getGeoY(toY);
		toZ = getNearestZ(tGeoX, tGeoY, toZ);
		
		// Door checks.
		if (DoorData.getInstance().checkIfDoorsBetween(fromX, fromY, fromZ, toX, toY, toZ, instance, false))
		{
			return false;
		}
		
		// Fence checks.
		if (FenceData.getInstance().checkIfFenceBetween(fromX, fromY, fromZ, toX, toY, toZ, instance))
		{
			return false;
		}
		
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		
		// First point is guaranteed to be available
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = fromZ;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int curZ = getMovementLayerZ(curX, curY, prevZ);
			
			if (hasGeoPos(prevX, prevY))
			{
				int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return false;
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		
		if (hasGeoPos(prevX, prevY) && (prevZ != toZ))
		{
			// Different floors
			return false;
		}
		
		return true;
	}
	
	public int traceTerrainZ(int x, int y, int z, int tx, int ty)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		z = getNearestZ(geoX, geoY, z);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevZ = z;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int curZ = getMovementLayerZ(curX, curY, prevZ);
			
			prevZ = curZ;
		}
		
		return prevZ;
	}
	
	public boolean canMoveToTarget(ILocational from, int toX, int toY, int toZ)
	{
		return canMoveToTarget(from.getX(), from.getY(), from.getZ(), toX, toY, toZ, null);
	}
	
	public boolean canMoveToTarget(ILocational from, ILocational to)
	{
		return canMoveToTarget(from, to.getX(), to.getY(), to.getZ());
	}
	
	public boolean hasGeo(int x, int y)
	{
		return hasGeoPos(getGeoX(x), getGeoY(y));
	}
	
	/**
	 * MEJORA EXTRAÍDA DE ORION: Verifica si hay una línea de visión limpia a nivel de suelo entre dos puntos. Útil para habilidades de área dirigidas al suelo y validación de físicas de caída/dropeo de items.
	 */
	public boolean canSeeGround(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, Instance instance)
	{
		int geoX = getGeoX(fromX);
		int geoY = getGeoY(fromY);
		fromZ = getNearestZ(geoX, geoY, fromZ);
		int tGeoX = getGeoX(toX);
		int tGeoY = getGeoY(toY);
		toZ = getNearestZ(tGeoX, tGeoY, toZ);
		
		if (DoorData.getInstance().checkIfDoorsBetween(fromX, fromY, fromZ, toX, toY, toZ, instance, false))
		{
			return false;
		}
		
		if (FenceData.getInstance().checkIfFenceBetween(fromX, fromY, fromZ, toX, toY, toZ, instance))
		{
			return false;
		}
		
		final LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = fromZ;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int curZ = getNearestZ(curX, curY, prevZ);
			
			if (hasGeoPos(prevX, prevY))
			{
				int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return false;
				}
				
				if (Math.abs(curZ - prevZ) > MAX_Z_DIFF)
				{
					return false;
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		
		return true;
	}
	
	/**
	 * MEJORA EXTRAÍDA DE ACIS: Suavizador de rutas (Post-Filter). Recorre el camino generado por el A* y elimina nodos innecesarios (zig-zags) si hay un camino directo transitable entre el origen y el destino. Esto hace que el movimiento de los mobs sea súper fluido y real. Puedes llamar a este
	 * método directamente en tu gestor de Pathfinding sobre la lista de Location resultante.
	 * @param path Lista de ubicaciones generada por el A*
	 * @param instance Instancia del mapa (o null)
	 * @return Lista optimizada con las rutas directas despejadas
	 */
	public List<Location> filterPath(List<Location> path, Instance instance)
	{
		if ((path == null) || (path.size() < 3))
		{
			return path;
		}
		
		final ListIterator<Location> iter = path.listIterator();
		Location nodeA = iter.next(); // Nodo de inicio actual
		Location nodeB = iter.next(); // Siguiente paso intermedio
		
		while (iter.hasNext())
		{
			final Location nodeC = path.get(iter.nextIndex());
			
			// Si podemos caminar directamente de A a C sin chocar con nada...
			if (canMoveToTarget(nodeA.getX(), nodeA.getY(), nodeA.getZ(), nodeC.getX(), nodeC.getY(), nodeC.getZ(), instance))
			{
				// Atajamos: eliminamos el nodo intermedio B
				iter.remove();
			}
			else
			{
				// Si hay un obstáculo directo, el nodo B pasa a ser el nuevo punto de inicio 'A'
				nodeA = nodeB;
			}
			nodeB = iter.next();
		}
		
		return path;
	}
	
	/**
	 * MEJORA EXTRAÍDA DE ACIS: Sistema de registro de bugs de geodata. Traduce coordenadas del juego (X,Y,Z) en la Región, Bloque y Celda exacta de la Geodata. Útil para vincular a un comando in-game de GMs (.geobug [comentario]) que exporte los fallos de los mapas a un archivo listo para ser
	 * abierto en programas de edición de geodata.
	 * @param loc Ubicación del fallo
	 * @param comment Comentario breve del error
	 * @return true si se guardó con éxito
	 */
	public synchronized boolean addGeoBug(Location loc, String comment)
	{
		if (_geoBugReports == null)
		{
			try
			{
				final File file = new File("log/geo_bugs.txt");
				if (!file.getParentFile().exists())
				{
					file.getParentFile().mkdirs();
				}
				_geoBugReports = new PrintWriter(new FileOutputStream(file, true), true);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not initialize log/geo_bugs.txt reporting system", e);
				return false;
			}
		}
		
		final int gox = getGeoX(loc.getX());
		final int goy = getGeoY(loc.getY());
		final int goz = loc.getZ();
		
		// Fórmulas matemáticas estándar de L2J para posicionamiento espacial
		final int rx = (gox / 2048) + World.TILE_X_MIN;
		final int ry = (goy / 2048) + World.TILE_Y_MIN;
		final int bx = (gox / 8) % 256;
		final int by = (goy / 8) % 256;
		final int cx = gox % 8;
		final int cy = goy % 8;
		
		try
		{
			_geoBugReports.printf("%d;%d;%d;%d;%d;%d;%d;%s\r\n", rx, ry, bx, by, cx, cy, goz, comment.replace(";", ":"));
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to write geo bug to file", e);
			return false;
		}
	}
	
	/**
	 * MEJORA DE RUSACIS INTEGRADA: Retorna el punto de colisión tridimensional exacto antes de un obstáculo físico. Ideal para saltos, teletransportaciones rápidas o habilidades de aproximación rápida ("Rush Impact", "Warp", "Shadow Step"). Evita completamente que el jugador atraviese puertas
	 * dinámicas o paredes finas en asedios y catacumbas.
	 * @param x Coordenada X origen
	 * @param y Coordenada Y origen
	 * @param z Coordenada Z origen
	 * @param tx Coordenada X destino teórica
	 * @param ty Coordenada Y destino teórica
	 * @param tz Coordenada Z destino teórica
	 * @param instance Instancia actual del mapa
	 * @return Location exacta inmediatamente anterior a la colisión física detectada
	 */
	public Location raycast(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		z = getNearestZ(geoX, geoY, z);
		
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		tz = getNearestZ(tGeoX, tGeoY, tz);
		
		if (DoorData.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, tz, instance, false))
		{
			return new Location(x, y, z);
		}
		
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = z;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int curZ = getNearestZ(curX, curY, prevZ);
			
			if (hasGeoPos(prevX, prevY))
			{
				int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
				
				if (Math.abs(curZ - prevZ) > MAX_Z_DIFF)
				{
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
			}
			
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		
		return new Location(tx, ty, tz);
	}
	
	/**
	 * MEJORA DE RUSACIS INTEGRADA: Comprueba la transitabilidad de vuelo tridimensional (corredor aéreo). Asegura que criaturas voladoras (Wyverns, monturas, transformaciones) no traspasen techos físicos, puentes o plataformas altas.
	 * @param x Coordenada X origen
	 * @param y Coordenada Y origen
	 * @param z Coordenada Z origen
	 * @param height Altura de colisión física (altura vertical del modelo)
	 * @param tx Coordenada X destino teórica
	 * @param ty Coordenada Y destino teórica
	 * @param tz Coordenada Z destino teórica
	 * @param instance Instancia actual del mapa
	 * @return true si la ruta aérea está completamente despejada en todo su volumen físico vertical
	 */
	public boolean canFlyToTarget(int x, int y, int z, double height, int tx, int ty, int tz, Instance instance)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		
		double distance = Math.hypot(tGeoX - geoX, tGeoY - geoY);
		double stepZ = distance > 0 ? (tz - z) / distance : 0;
		int stepCount = 0;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int flyZ = (int) (z + (stepZ * stepCount));
			
			if (hasGeoPos(curX, curY))
			{
				int groundZ = getNearestZ(curX, curY, flyZ);
				if (groundZ > flyZ)
				{
					return false;
				}
				
				int ceilingZ = getNextHigherZ(curX, curY, flyZ);
				if (ceilingZ < (flyZ + height))
				{
					return false;
				}
			}
			stepCount++;
		}
		return true;
	}
	
	/**
	 * MEJORA DE RUSACIS INTEGRADA: Retorna la última ubicación de vuelo libre válida. Detiene el movimiento aéreo del Wyvern o transformación si choca contra un techo, montaña o pared aérea.
	 * @param x Coordenada X origen
	 * @param y Coordenada Y origen
	 * @param z Coordenada Z origen
	 * @param height Altura de colisión física (altura vertical del modelo)
	 * @param tx Coordenada X destino teórica
	 * @param ty Coordenada Y destino teórica
	 * @param tz Coordenada Z destino teórica
	 * @param instance Instancia actual del mapa
	 * @return Location máxima de avance en vuelo 3D antes de la colisión aérea
	 */
	public Location getValidFlyLocation(int x, int y, int z, double height, int tx, int ty, int tz, Instance instance)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = z;
		
		double distance = Math.hypot(tGeoX - geoX, tGeoY - geoY);
		double stepZ = distance > 0 ? (tz - z) / distance : 0;
		int stepCount = 0;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int flyZ = (int) (z + (stepZ * stepCount));
			
			if (hasGeoPos(curX, curY))
			{
				int groundZ = getNearestZ(curX, curY, flyZ);
				if (groundZ > flyZ)
				{
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
				
				int ceilingZ = getNextHigherZ(curX, curY, flyZ);
				if (ceilingZ < (flyZ + height))
				{
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
				
				prevZ = flyZ;
			}
			prevX = curX;
			prevY = curY;
			stepCount++;
		}
		return new Location(tx, ty, tz);
	}
	
	/**
	 * MEJORA DE RUSACIS INTEGRADA: Calcula el movimiento exacto y natación submarina tridimensional. Evita atravesar obstáculos en las profundidades de ríos o mares de geodata y gestiona salidas naturales a la playa/costa.
	 * @param x Coordenada X origen
	 * @param y Coordenada Y origen
	 * @param z Coordenada Z origen
	 * @param tx Coordenada X destino teórica
	 * @param ty Coordenada Y destino teórica
	 * @param tz Coordenada Z destino teórica
	 * @param instance Instancia actual del mapa
	 * @return Location de natación/tránsito de agua controlada
	 */
	public Location getValidSwimLocation(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		int geoX = getGeoX(x);
		int geoY = getGeoY(y);
		int tGeoX = getGeoX(tx);
		int tGeoY = getGeoY(ty);
		
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = z;
		
		double distance = Math.hypot(tGeoX - geoX, tGeoY - geoY);
		double stepZ = distance > 0 ? (tz - z) / distance : 0;
		int stepCount = 0;
		
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int swimZ = (int) (z + (stepZ * stepCount));
			
			if (hasGeoPos(curX, curY))
			{
				int groundZ = getNearestZ(curX, curY, swimZ);
				int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				
				// Validación de muro submarino
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return new Location(getWorldX(prevX), getWorldY(prevY), prevZ);
				}
				
				// Transición costa/playa (si el suelo físico se eleva sobre el nivel del agua)
				if (groundZ >= swimZ)
				{
					prevZ = groundZ;
				}
				else
				{
					prevZ = swimZ;
				}
			}
			prevX = curX;
			prevY = curY;
			stepCount++;
		}
		
		return new Location(tx, ty, tz);
	}
	
	public static GeoEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final GeoEngine _instance = new GeoEngine();
	}
}