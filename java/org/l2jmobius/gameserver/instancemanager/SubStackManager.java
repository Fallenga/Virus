/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData; // <-- IMPORTANTE: Ajustar ruta si tu versión difiere
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player; // <-- IMPORTANTE: Ajustar ruta si tu versión difiere
import org.l2jmobius.gameserver.model.skill.Skill;

public class SubStackManager
{
	private static final Logger LOGGER = Logger.getLogger(SubStackManager.class.getName());
	
	// Modificadas las consultas para incluir la columna skill_sub_level
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level,skill_sub_level,class_index FROM character_skills WHERE charId=?";
	private static final String UPDATE_SKILL_LEVEL = "UPDATE character_skills SET skill_level=?, skill_sub_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String GET_SKILL_LEVEL_FROM_DB = "SELECT skill_level,skill_sub_level FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	
	// Clase auxiliar para manejar la tupla Level/SubLevel internamente
	private static class SkillLevelHolder
	{
		private final int _level;
		private final int _subLevel;
		
		public SkillLevelHolder(int level, int subLevel)
		{
			_level = level;
			_subLevel = subLevel;
		}
		
		public int getLevel()
		{
			return _level;
		}
		
		public int getSubLevel()
		{
			return _subLevel;
		}
	}
	
	public boolean onRestoreSkills(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final Map<Integer, SkillLevelHolder> skills = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			ps.setInt(1, player.getObjectId());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					int id = rs.getInt("skill_id");
					int level = rs.getInt("skill_level");
					int subLevel = rs.getInt("skill_sub_level"); // Lectura de skill_sub_level
					int classIndex = rs.getInt("class_index");
					
					if (player.getClassIndex() != classIndex)
					{
						// Ahora instanciamos la skill usando tanto el level como el subLevel
						Skill skill = SkillData.getInstance().getSkill(id, level, subLevel);
						
						if (skill == null)
						{
							LOGGER.log(Level.WARNING, "Skipped null skill Id: " + id + ", Level: " + level + ", SubLevel: " + subLevel + " while restoring player skills for " + player.getName());
							continue;
						}
						
						if (!Config.ACCUMULATE_PASIVE && skill.isPassive())
						{
							continue;
						}
						
						if (Config.DONT_ACCUMULATE_SKILLS.contains(id))
						{
							continue;
						}
					}
					
					// Comparamos si ya existe la habilidad para mantener la de mayor nivel/subnivel
					if (skills.get(id) != null)
					{
						SkillLevelHolder existing = skills.get(id);
						if ((existing.getLevel() > level) || ((existing.getLevel() == level) && (existing.getSubLevel() > subLevel)))
						{
							continue;
						}
					}
					
					skills.put(id, new SkillLevelHolder(level, subLevel));
				}
			}
		}
		catch (final Exception e)
		{
			LOGGER.log(Level.SEVERE, "Couldn't restore player skills.", e);
		}
		
		for (Entry<Integer, SkillLevelHolder> entry : skills.entrySet())
		{
			int id = entry.getKey();
			SkillLevelHolder holder = entry.getValue();
			int level = holder.getLevel();
			int subLevel = holder.getSubLevel();
			
			int currentLevel = player.getSkillLevel(id);
			int currentSubLevel = player.getSkillSubLevel(id);
			
			// Si el nivel en base de datos es mayor, o si es igual pero tiene mayor subnivel de encantamiento
			if ((currentLevel < level) || ((currentLevel == level) && (currentSubLevel < subLevel)))
			{
				if (currentLevel > 0)
				{
					player.removeSkill(id, false);
				}
				
				Skill skill = SkillData.getInstance().getSkill(id, level, subLevel);
				if (skill != null)
				{
					player.addSkill(skill, false);
				}
			}
		}
		
		// =========================================================================
		// NUEVO CÓDIGO AGREGADO: Control del límite de habilidades de habilidad (Ability Skills)
		// =========================================================================
		// Check ability skill count.
		int count = 0;
		for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
		{
			final Skill knownSkill = player.getKnownSkill(sk.getSkillId());
			if ((knownSkill != null) && (knownSkill.getLevel() == sk.getSkillLevel()))
			{
				count++;
			}
		}
		
		// Too many ability skills. Remove them all.
		if ((count > (Config.PLAYER_MAXIMUM_LEVEL - 80)) || (count > player.getAbilityPointsUsed()))
		{
			for (SkillLearn sk : SkillTreeData.getInstance().getAbilitySkillTree().values())
			{
				final Skill knownSkill = player.getKnownSkill(sk.getSkillId());
				if ((knownSkill != null) && (knownSkill.getLevel() == sk.getSkillLevel()))
				{
					player.removeSkill(knownSkill, false);
				}
			}
		}
		// =========================================================================
		
		return true;
	}
	
	public void storeAccumulatedSkills(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_SKILL_LEVEL))
		{
			for (Skill skill : player.getSkills().values())
			{
				SkillLevelHolder dbSkill = getSkillLevelFromDB(player, skill.getId(), player.getClassIndex());
				
				// Actualiza la DB si el nivel o el subnivel son mayores a lo guardado
				if ((skill.getLevel() > dbSkill.getLevel()) || ((skill.getLevel() == dbSkill.getLevel()) && (skill.getSubLevel() > dbSkill.getSubLevel())))
				{
					ps.setInt(1, skill.getLevel());
					ps.setInt(2, skill.getSubLevel()); // Guarda skill_sub_level
					ps.setInt(3, skill.getId());
					ps.setInt(4, player.getObjectId());
					ps.setInt(5, player.getClassIndex());
					ps.addBatch();
				}
			}
			
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Couldn't store accumulated skills.", e);
		}
	}
	
	private static SkillLevelHolder getSkillLevelFromDB(Player player, int skillId, int classIndex)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(GET_SKILL_LEVEL_FROM_DB))
		{
			ps.setInt(1, skillId);
			ps.setInt(2, player.getObjectId());
			ps.setInt(3, classIndex);
			
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return new SkillLevelHolder(rs.getInt("skill_level"), rs.getInt("skill_sub_level"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Couldn't get skill level from DB.", e);
		}
		
		return new SkillLevelHolder(0, 0);
	}
	
	// Retorna Map<Integer, Integer> para que Player.java compile sin modificaciones
	public Map<Integer, Integer> getMaxSkillLevels(Player player)
	{
		final Map<Integer, Integer> maxLevels = new HashMap<>();
		final Map<Integer, Integer> maxSubLevels = new HashMap<>(); // Auxiliar para controlar subniveles
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			ps.setInt(1, player.getObjectId());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					int skillId = rs.getInt("skill_id");
					int level = rs.getInt("skill_level");
					int subLevel = rs.getInt("skill_sub_level"); // Lectura de skill_sub_level
					
					if (!maxLevels.containsKey(skillId))
					{
						maxLevels.put(skillId, level);
						maxSubLevels.put(skillId, subLevel);
					}
					else
					{
						int existingLevel = maxLevels.get(skillId);
						int existingSubLevel = maxSubLevels.get(skillId);
						
						// Comparamos nivel, y en caso de empate, el subnivel
						if ((level > existingLevel) || ((level == existingLevel) && (subLevel > existingSubLevel)))
						{
							maxLevels.put(skillId, level);
							maxSubLevels.put(skillId, subLevel);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Couldn't get max skill levels.", e);
		}
		
		return maxLevels;
	}
	
	public static SubStackManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SubStackManager INSTANCE = new SubStackManager();
	}
}