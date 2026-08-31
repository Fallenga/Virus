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
package org.l2jmobius.gameserver.ai;

import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2jmobius.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.AISkillScope;
import org.l2jmobius.gameserver.enums.AIType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jmobius.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2jmobius.gameserver.model.AggroInfo;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.Guard;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.model.actor.instance.RiftInvader;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnAttackableFactionCall;
import org.l2jmobius.gameserver.model.events.impl.creature.npc.OnAttackableHate;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.taskmanager.AttackableThinkTaskManager;
import org.l2jmobius.gameserver.taskmanager.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Util;

/**
 * This class manages AI of Attackable.
 */
public class AttackableAI extends CreatureAI
{
	private static final Logger LOGGER = Logger.getLogger(AttackableAI.class.getName());
	
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	
	/**
	 * The delay after which the attacked is stopped.
	 */
	private int _attackTimeout;
	/**
	 * The Attackable aggro counter.
	 */
	private int _globalAggro;
	/**
	 * The flag used to indicate that a thinking action is in progress, to prevent recursive thinking.
	 */
	private boolean _thinking;
	
	private int chaostime = 0;
	private long _lastSpreadTime = 0; // COOLDOWN para evitar que los monstruos "bailen" constantemente sin golpear.
	private long _lastFactionCallTime = 0; // COOLDOWN de optimización para evitar escaneos constantes de facción.
	
	public AttackableAI(Attackable attackable)
	{
		super(attackable);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
	}
	
	/**
	 * @param target The targeted WorldObject
	 * @return {@code true} if target can be auto attacked due aggression.
	 */
	private boolean isAggressiveTowards(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			return false;
		}
		
		// Check if the target isn't a Folk or a Door
		if (target.isDoor())
		{
			return false;
		}
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height
		if (target.isAlikeDead())
		{
			return false;
		}
		
		// Check if the target is a Playable and if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode
		final Attackable me = getActiveChar();
		if (target.isPlayable() && !(me.isRaid()) && !(me.canSeeThroughSilentMove()) && target.asPlayable().isSilentMovingAffected())
		{
			return false;
		}
		
		// Gets the player if there is any.
		final Player player = target.asPlayer();
		if (player != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
			if (!player.getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// check if the target is within the grace period for JUST getting up from fake death
			if (player.isRecentFakeDeath())
			{
				return false;
			}
			
			final Party party = player.getParty();
			if (player.isInParty() && party.isInDimensionalRift())
			{
				final byte riftType = party.getDimensionalRift().getType();
				final byte riftRoom = party.getDimensionalRift().getCurrentRoom();
				if ((me instanceof RiftInvader) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
				{
					return false;
				}
			}
			
			if (me instanceof Guard)
			{
				World.getInstance().forEachVisibleObjectInRange(me, Guard.class, 500, guard ->
				{
					if (guard.isAttackingNow() && (guard.getTarget() == player))
					{
						me.getAI().startFollow(player);
						me.addDamageHate(player, 0, 10);
					}
				});
				if (player.getReputation() < 0)
				{
					// MEJORA DE ACIS: Evita que los guardias ataquen PKs a través de paredes/pisos de ciudades sin visual directa.
					return GeoEngine.getInstance().canSeeTarget(me, player);
				}
			}
		}
		// MEJORA DE ACIS: Permite que los guardias de los pueblos salgan a defender la zona atacando de forma activa a monstruos agresivos que estén cerca.
		else if ((me instanceof Guard) && target.isMonster() && target.asMonster().isAggressive())
		{
			return GeoEngine.getInstance().canSeeTarget(me, target);
		}
		else if (me.isMonster())
		{
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE) && target.isInsideZone(ZoneId.NO_PVP))
			{
				return false;
			}
			
			if (!me.isAggressive())
			{
				return false;
			}
		}
		
		if (me.isChampion() && Config.CHAMPION_PASSIVE)
		{
			return false;
		}
		
		return target.isAutoAttackable(me) && GeoEngine.getInstance().canSeeTarget(me, target);
	}
	
	public void startAITask()
	{
		AttackableThinkTaskManager.getInstance().add(getActiveChar());
	}
	
	@Override
	public void stopAITask()
	{
		AttackableThinkTaskManager.getInstance().remove(getActiveChar());
		super.stopAITask();
	}
	
	/**
	 * Set the Intention of this CreatureAI and create an AI Task executed every 1s (call onEvtThink method) for this Attackable.<br>
	 * <font color=#FF0000><b><u>Caution</u>: If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</b></font>
	 * @param newIntention The new Intention to set to the AI
	 * @param args The first parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention newIntention, Object... args)
	{
		CtrlIntention intention = newIntention;
		if ((intention == AI_INTENTION_IDLE) || (intention == AI_INTENTION_ACTIVE))
		{
			// Check if actor is not dead
			final Attackable npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (!World.getInstance().getVisibleObjects(npc, Player.class).isEmpty())
				{
					intention = AI_INTENTION_ACTIVE;
				}
				else if ((npc.getSpawn() != null) && !npc.isInsideRadius3D(npc.getSpawn(), Config.MAX_DRIFT_RANGE + Config.MAX_DRIFT_RANGE))
				{
					intention = AI_INTENTION_ACTIVE;
				}
			}
			
			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE);
				
				stopAITask();
				
				// Cancel the AI
				_actor.detachAI();
				
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to intention
		super.changeIntention(intention, args);
		
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		startAITask();
	}
	
	@Override
	protected void changeIntentionToCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Set the AI cast target
		setTarget(target);
		super.changeIntentionToCast(skill, target, item, forceUse, dontMove);
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.
	 * @param target The Creature to attack
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		super.onIntentionAttack(target);
	}
	
	protected void thinkCast()
	{
		final WorldObject target = _skill.getTarget(_actor, getTarget(), _forceUse, _dontMove, false);
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		setIntention(AI_INTENTION_ACTIVE);
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	/**
	 * Manage AI standard thinks of a Attackable (called by onEvtThink). <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable Creature in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor is a GuardInstance that can't attack, order to it to return to its home location</li>
	 * <li>If the actor is a Monster that can't attack, order to it to random walk (1/100)</li>
	 * </ul>
	 */
	protected void thinkActive()
	{
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		WorldObject target = getTarget();
		
		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Add all autoAttackable Creature in Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			if (npc.isFakePlayer() && npc.isAggressive())
			{
				final List<Item> droppedItems = npc.getFakePlayerDrops();
				if (droppedItems.isEmpty())
				{
					Creature nearestTarget = null;
					double closestDistance = Double.MAX_VALUE;
					for (Creature t : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, npc.getAggroRange()))
					{
						if ((t == _actor) || (t == null) || t.isDead())
						{
							continue;
						}
						if ((Config.FAKE_PLAYER_AGGRO_FPC && t.isFakePlayer()) //
							|| (Config.FAKE_PLAYER_AGGRO_MONSTERS && t.isMonster() && !t.isFakePlayer()) //
							|| (Config.FAKE_PLAYER_AGGRO_PLAYERS && t.isPlayer()))
						{
							final long hating = npc.getHating(t);
							final double distance = npc.calculateDistance2D(t);
							if ((hating == 0) && (closestDistance > distance))
							{
								nearestTarget = t;
								closestDistance = distance;
							}
						}
					}
					if (nearestTarget != null)
					{
						npc.addDamageHate(nearestTarget, 0, 1);
					}
				}
				else if (!npc.isInCombat()) // must pickup items
				{
					final int itemIndex = npc.getFakePlayerDrops().size() - 1; // last item dropped - can also use 0 for first item dropped
					final Item droppedItem = npc.getFakePlayerDrops().get(itemIndex);
					if ((droppedItem != null) && droppedItem.isSpawned())
					{
						if (npc.calculateDistance2D(droppedItem) > 50)
						{
							moveTo(droppedItem);
						}
						else
						{
							npc.getFakePlayerDrops().remove(itemIndex);
							droppedItem.pickupMe(npc);
							if (Config.SAVE_DROPPED_ITEM)
							{
								ItemsOnGroundManager.getInstance().removeObject(droppedItem);
							}
							if (droppedItem.getTemplate().hasExImmediateEffect())
							{
								for (SkillHolder skillHolder : droppedItem.getTemplate().getAllSkills())
								{
									SkillCaster.triggerCast(npc, null, skillHolder.getSkill(), null, false);
								}
								npc.broadcastInfo(); // ? check if this is necessary
							}
						}
					}
					else
					{
						npc.getFakePlayerDrops().remove(itemIndex);
					}
					npc.setRunning();
				}
			}
			else if (npc.isAggressive() || (npc instanceof Guard))
			{
				final int range = npc instanceof Guard ? 500 : npc.getAggroRange(); // TODO Make sure how guards behave towards players.
				World.getInstance().forEachVisibleObjectInRange(npc, Creature.class, range, t ->
				{
					// For each Creature check if the target is autoattackable
					if (isAggressiveTowards(t)) // check aggression
					{
						if (t.isFakePlayer())
						{
							if (!npc.isFakePlayer() || (npc.isFakePlayer() && Config.FAKE_PLAYER_AGGRO_FPC))
							{
								final long hating = npc.getHating(t);
								if (hating == 0)
								{
									npc.addDamageHate(t, 0, 0);
								}
							}
						}
						else if (t.isPlayable())
						{
							if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_HATE, getActiveChar()))
							{
								final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnAttackableHate(getActiveChar(), t.asPlayer(), t.isSummon()), getActiveChar(), TerminateReturn.class);
								if ((term != null) && term.terminate())
								{
									return;
								}
							}
							
							// Get the hate level of the Attackable against this Creature target contained in _aggroList
							final long hating = npc.getHating(t);
							
							// Add the attacker to the Attackable _aggroList with 0 damage and 1 hate
							if (hating == 0)
							{
								npc.addDamageHate(t, 0, 0);
							}
							if (npc instanceof Guard)
							{
								World.getInstance().forEachVisibleObjectInRange(npc, Guard.class, 500, guard -> guard.addDamageHate(t, 0, 10));
							}
						}
					}
				});
			}
			
			// Chose a target from its aggroList
			Creature hated;
			if (npc.isConfused() && (target != null) && target.isCreature())
			{
				hated = target.asCreature(); // effect handles selection
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the Attackable to attack the target
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList
				final long aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(AI_INTENTION_ATTACK, hated);
				}
				
				return;
			}
		}
		
		// Chance to forget attackers after some time
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()) && !npc.getAttackByList().isEmpty() && (Rnd.get(500) == 0))
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
		}
		
		// Check if the mob should not return to spawn point
		if (!npc.canReturnToSpawnPoint()
		/* || npc.isReturningToSpawnPoint() */ ) // Commented because sometimes it stops movement.
		{
			return;
		}
		
		// Order this attackable to return to its spawn because there's no target to attack
		if (!npc.isWalker() && (npc.getSpawn() != null) && (npc.calculateDistance2D(npc.getSpawn()) > Config.MAX_DRIFT_RANGE) && ((getTarget() == null) || getTarget().isInvisible() || (getTarget().isPlayer() && !Config.ATTACKABLES_CAMP_PLAYER_CORPSES && getTarget().asPlayer().isAlikeDead())))
		{
			npc.setWalking();
			npc.returnHome();
			return;
		}
		
		// Do not leave dead player
		if ((getTarget() != null) && getTarget().isPlayer() && getTarget().asPlayer().isAlikeDead())
		{
			return;
		}
		
		// Minions following leader
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			int offset; // Cambiado a no-final para permitir dinamismo
			final int minRadius = 30;
			
			// Si el líder está en combate, los minions se agrupan mucho más cerca (Mejora de Orion)
			if (leader.isInCombat())
			{
				offset = npc.isRaidMinion() ? 120 : 60;
			}
			else
			{
				offset = npc.isRaidMinion() ? 500 : 200;
			}
			
			if (leader.isRunning())
			{
				npc.setRunning();
			}
			else
			{
				npc.setWalking();
			}
			
			if (npc.calculateDistanceSq2D(leader) > (offset * offset))
			{
				int x1 = Rnd.get(minRadius * 2, offset * 2); // x
				int y1 = Rnd.get(x1, offset * 2); // distance
				y1 = (int) Math.sqrt((y1 * y1) - (x1 * x1)); // y
				if (x1 > (offset + minRadius))
				{
					x1 = (leader.getX() + x1) - offset;
				}
				else
				{
					x1 = (leader.getX() - x1) + minRadius;
				}
				if (y1 > (offset + minRadius))
				{
					y1 = (leader.getY() + y1) - offset;
				}
				else
				{
					y1 = (leader.getY() - y1) + minRadius;
				}
				
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
				moveTo(x1, y1, leader.getZ());
			}
			else if (Rnd.get(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					target = skillTargetReconsider(sk, true);
					if (target != null)
					{
						setTarget(target);
						npc.doCast(sk);
					}
				}
			}
		}
		// Order to the Monster to random walk (1/100)
		else if ((npc.getSpawn() != null) && (Rnd.get(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				target = skillTargetReconsider(sk, true);
				if (target != null)
				{
					setTarget(target);
					npc.doCast(sk);
					return;
				}
			}
			
			int x1 = npc.getSpawn().getX();
			int y1 = npc.getSpawn().getY();
			int z1 = npc.getSpawn().getZ();
			if (npc.isInsideRadius2D(x1, y1, 0, Config.MAX_DRIFT_RANGE))
			{
				final int deltaX = Rnd.get(Config.MAX_DRIFT_RANGE * 2); // x
				int deltaY = Rnd.get(deltaX, Config.MAX_DRIFT_RANGE * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - Config.MAX_DRIFT_RANGE;
				y1 = (deltaY + y1) - Config.MAX_DRIFT_RANGE;
				z1 = npc.getZ();
			}
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
			final Location moveLoc = _actor.isFlying() ? new Location(x1, y1, z1) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceWorld());
			if (Util.calculateDistance(npc.getSpawn(), moveLoc, false, false) <= Config.MAX_DRIFT_RANGE)
			{
				moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
			}
		}
	}
	
	/**
	 * Manage AI attack thinks of a Attackable (called by onEvtThink). <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li>
	 * </ul>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if ((npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		if (Config.AGGRO_DISTANCE_CHECK_ENABLED && npc.isMonster() && !npc.isWalker() && !(npc instanceof GrandBoss))
		{
			final Spawn spawn = npc.getSpawn();
			if ((spawn != null) && (npc.calculateDistance2D(spawn.getLocation()) > (spawn.getChaseRange() > 0 ? Math.max(Config.MAX_DRIFT_RANGE, spawn.getChaseRange()) : npc.isRaid() ? Config.AGGRO_DISTANCE_CHECK_RAID_RANGE : Config.AGGRO_DISTANCE_CHECK_RANGE)))
			{
				if ((Config.AGGRO_DISTANCE_CHECK_RAIDS || !npc.isRaid()) && (Config.AGGRO_DISTANCE_CHECK_INSTANCES || !npc.isInInstance()))
				{
					if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
					{
						npc.setCurrentHp(npc.getMaxHp());
						npc.setCurrentMp(npc.getMaxMp());
					}
					npc.abortAttack();
					npc.clearAggroList();
					npc.getAttackByList().clear();
					if (npc.hasAI())
					{
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, spawn.getLocation());
					}
					else
					{
						npc.teleToLocation(spawn.getLocation(), true);
					}
					
					// Minions should return as well.
					if (_actor.asMonster().hasMinions())
					{
						for (Monster minion : _actor.asMonster().getMinionList().getSpawnedMinions())
						{
							if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
							{
								minion.setCurrentHp(minion.getMaxHp());
								minion.setCurrentMp(minion.getMaxMp());
							}
							minion.abortAttack();
							minion.clearAggroList();
							minion.getAttackByList().clear();
							if (minion.hasAI())
							{
								minion.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, spawn.getLocation());
							}
							else
							{
								minion.teleToLocation(spawn.getLocation(), true);
							}
						}
					}
					return;
				}
			}
		}
		
		Creature target = npc.getMostHated();
		if (target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		if (getTarget() != target)
		{
			setTarget(target);
		}
		
		// Check if target is dead or if timeout is expired to stop this attack
		if (target.isAlikeDead())
		{
			// Stop hating this target after the attack timeout or if target is dead
			npc.stopHating(target);
			return;
		}
		
		if (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks())
		{
			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);
			
			if (!_actor.isFakePlayer())
			{
				npc.setWalking();
			}
			
			// Monster teleport to spawn
			if (npc.isMonster() && (npc.getSpawn() != null) && !npc.isInInstance() && (npc.isInCombat() || World.getInstance().getVisibleObjects(npc, Player.class).isEmpty()))
			{
				npc.teleToLocation(npc.getSpawn(), false);
			}
			return;
		}
		
		// Actor should be able to see target.
		if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
		{
			if (_actor.calculateDistance3D(target) < 6000)
			{
				moveTo(target);
			}
			return;
		}
		
		final NpcTemplate template = npc.getTemplate();
		final int collision = template.getCollisionRadius();
		
		// [OPTIMIZACIÓN DE RUSACIS]: El escaneo de facciones (Clan Help/Faction Call) ha sido removido de aquí.
		// Ya no se ejecuta cada 1 segundo en este AI Tick. Ahora se maneja exclusivamente por eventos en 'onEvtAttacked'.
		
		if (npc.isCoreAIDisabled())
		{
			return;
		}
		
		final List<Skill> aiSuicideSkills = template.getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30) && npc.hasSkillChance())
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (SkillCaster.checkUseConditions(npc, skill) && checkSkillTarget(skill, target))
			{
				npc.doCast(skill);
				// LOGGER.finer(this + " used suicide skill " + skill);
				return;
			}
		}
		
		// ------------------------------------------------------------------------------
		// REUBICACIÓN MELEÉ OPTIMIZADA: Evita el pixel-stacking sin generar "baile" infinito.
		// Solo se ejecuta si el mob es cuerpo a cuerpo, no está inmóvil, y no está ya en movimiento.
		if (!npc.isMovementDisabled() && !npc.isMoving() && (npc.getAiType() != AIType.MAGE) && (npc.getAiType() != AIType.ARCHER) && (npc.getPhysicalAttackRange() <= 150))
		{
			final long currentTime = System.currentTimeMillis();
			// Cooldown de 3 segundos para que ataquen fijamente y no sufran desincronización
			if ((currentTime - _lastSpreadTime) > 3000)
			{
				final int combinedCollision = collision + target.getTemplate().getCollisionRadius();
				final int minSeparation = Math.max(30, collision * 2); // Separación mínima segura de 30 o el doble de su radio
				boolean needsToSpread = false;
				
				// Buscamos si hay otro mob colisionando en el mismo píxel atacando al mismo target
				for (Attackable nearby : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, minSeparation))
				{
					if ((nearby != npc) && (nearby.getTarget() == target) && !nearby.isDead() && (npc.calculateDistance2D(nearby) < minSeparation))
					{
						needsToSpread = true;
						break;
					}
				}
				
				if (needsToSpread)
				{
					final int desiredDistance = (npc.getPhysicalAttackRange() + combinedCollision) - 10;
					final Location spreadLoc = findSpreadLocation(target, desiredDistance, minSeparation);
					if (spreadLoc != null)
					{
						_lastSpreadTime = currentTime; // Guardamos el tiempo de esta reubicación
						moveTo(spreadLoc.getX(), spreadLoc.getY(), spreadLoc.getZ());
						return; // Retornamos para ejecutar el paso hacia la nueva coordenada antes de golpear
					}
				}
			}
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance2 = npc.calculateDistanceSq2D(target);
			final int combinedCollision = collision + target.getTemplate().getCollisionRadius();
			if (Math.sqrt(distance2) <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (target.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (target.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceWorld()))
				{
					setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		// ------------------------------------------------------------------------------
		// BOSS/Raid Minion Target Reconsider
		if (npc.isRaid() || npc.isRaidMinion())
		{
			chaostime++;
			boolean changeTarget = false;
			if ((npc instanceof RaidBoss) && (chaostime > Config.RAID_CHAOS_TIME))
			{
				final double multiplier = npc.asMonster().hasMinions() ? 200 : 100;
				changeTarget = Rnd.get(100) <= (100 - ((npc.getCurrentHp() * multiplier) / npc.getMaxHp()));
			}
			else if ((npc instanceof GrandBoss) && (chaostime > Config.GRAND_CHAOS_TIME))
			{
				final double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
				changeTarget = ((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate));
			}
			else if (chaostime > Config.MINION_CHAOS_TIME)
			{
				changeTarget = Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()));
			}
			
			if (changeTarget)
			{
				target = targetReconsider(true);
				if (target != null)
				{
					setTarget(target);
					chaostime = 0;
					return;
				}
			}
		}
		
		if (target == null)
		{
			target = targetReconsider(false);
			if (target == null)
			{
				return;
			}
			
			setTarget(target);
		}
		
		// Cast skills.
		if ((!npc.isMoving() && npc.hasSkillChance()) || (npc.getAiType() == AIType.MAGE))
		{
			// First use the most important skill - heal. Even reconsider target.
			if (!template.getAISkills(AISkillScope.HEAL).isEmpty())
			{
				final Skill healSkill = template.getAISkills(AISkillScope.HEAL).get(Rnd.get(template.getAISkills(AISkillScope.HEAL).size()));
				if (SkillCaster.checkUseConditions(npc, healSkill))
				{
					final Creature healTarget = skillTargetReconsider(healSkill, false);
					if (healTarget != null)
					{
						final double healChance = (100 - healTarget.getCurrentHpPercent()) * 1.5; // Ensure heal chance is always 100% if HP is below 33%.
						if ((Rnd.get(100) < healChance) && checkSkillTarget(healSkill, healTarget))
						{
							setTarget(healTarget);
							npc.doCast(healSkill);
							// LOGGER.finer(this + " used heal skill " + healSkill + " with target " + getTarget());
							return;
						}
					}
				}
			}
			
			// Then use the second most important skill - buff. Even reconsider target.
			if (!template.getAISkills(AISkillScope.BUFF).isEmpty())
			{
				final Skill buffSkill = template.getAISkills(AISkillScope.BUFF).get(Rnd.get(template.getAISkills(AISkillScope.BUFF).size()));
				if (SkillCaster.checkUseConditions(npc, buffSkill))
				{
					final Creature buffTarget = skillTargetReconsider(buffSkill, true);
					if (checkSkillTarget(buffSkill, buffTarget))
					{
						setTarget(buffTarget);
						npc.doCast(buffSkill);
						// LOGGER.finer(this + " used buff skill " + buffSkill + " with target " + getTarget());
						return;
					}
				}
			}
			
			// Then try to immobolize target if moving.
			if (target.isMoving() && !template.getAISkills(AISkillScope.IMMOBILIZE).isEmpty())
			{
				final Skill immobolizeSkill = template.getAISkills(AISkillScope.IMMOBILIZE).get(Rnd.get(template.getAISkills(AISkillScope.IMMOBILIZE).size()));
				if (SkillCaster.checkUseConditions(npc, immobolizeSkill) && checkSkillTarget(immobolizeSkill, target))
				{
					npc.doCast(immobolizeSkill);
					// LOGGER.finer(this + " used immobolize skill " + immobolizeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Then try to mute target if he is casting.
			if (target.isCastingNow() && !template.getAISkills(AISkillScope.COT).isEmpty())
			{
				final Skill muteSkill = template.getAISkills(AISkillScope.COT).get(Rnd.get(template.getAISkills(AISkillScope.COT).size()));
				if (SkillCaster.checkUseConditions(npc, muteSkill) && checkSkillTarget(muteSkill, target))
				{
					npc.doCast(muteSkill);
					// LOGGER.finer(this + " used mute skill " + muteSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Try cast short range skill.
			if (!npc.getShortRangeSkills().isEmpty() && (npc.calculateDistance2D(target) <= 150))
			{
				final Skill shortRangeSkill = npc.getShortRangeSkills().get(Rnd.get(npc.getShortRangeSkills().size()));
				if (SkillCaster.checkUseConditions(npc, shortRangeSkill) && checkSkillTarget(shortRangeSkill, target))
				{
					npc.doCast(shortRangeSkill);
					// LOGGER.finer(this + " used short range skill " + shortRangeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Try cast long range skill.
			if (!npc.getLongRangeSkills().isEmpty())
			{
				final Skill longRangeSkill = npc.getLongRangeSkills().get(Rnd.get(npc.getLongRangeSkills().size()));
				if (SkillCaster.checkUseConditions(npc, longRangeSkill) && checkSkillTarget(longRangeSkill, target))
				{
					npc.doCast(longRangeSkill);
					// LOGGER.finer(this + " used long range skill " + longRangeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Finally, if none succeed, try to cast any skill.
			if (!template.getAISkills(AISkillScope.GENERAL).isEmpty())
			{
				final Skill generalSkill = template.getAISkills(AISkillScope.GENERAL).get(Rnd.get(template.getAISkills(AISkillScope.GENERAL).size()));
				if (SkillCaster.checkUseConditions(npc, generalSkill) && checkSkillTarget(generalSkill, target))
				{
					npc.doCast(generalSkill);
					// LOGGER.finer(this + " used general skill " + generalSkill + " with target " + getTarget());
					return;
				}
			}
		}
		
		// Check if target is within range or move.
		final int combinedCollision = collision + target.getTemplate().getCollisionRadius();
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		
		// Si el objetivo se está moviendo, ampliamos el rango para mitigar el lag/desincronización (Mejora de Orion)
		if (target.isMoving())
		{
			range += 50;
		}
		
		if (npc.isMoving())
		{
			range *= 2;
		}
		if (npc.getAiType() == AIType.ARCHER)
		{
			range = 850 + combinedCollision; // Base bow range for NPCs.
		}
		
		if (npc.calculateDistance2D(target) > range)
		{
			if (checkTarget(target))
			{
				/*
				 * IMPORTANTE: no interpolar Z manualmente. GeoEngine debe decidir la capa/altura transitable completa. Esto evita saltos de capa en pendientes, curvas, puentes y obstáculos, que visualmente terminan mostrando al NPC cayendo del cielo.
				 */
				final Location validLoc = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), target.getX(), target.getY(), target.getZ(), npc.getInstanceWorld());
				if (npc.calculateDistanceSq2D(validLoc) > 25)
				{
					moveTo(validLoc.getX(), validLoc.getY(), validLoc.getZ());
					return;
				}
			}
			
			target = targetReconsider(false);
			if (target == null)
			{
				return;
			}
			
			setTarget(target);
		}
		
		// Attacks target
		_actor.doAutoAttack(target);
	}
	
	private boolean checkSkillTarget(Skill skill, WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		// Check if target is valid and within cast range.
		final Attackable attackable = getActiveChar();
		if (skill.getTarget(attackable, target, false, attackable.isMovementDisabled(), false) == null)
		{
			return false;
		}
		
		if (!Util.checkIfInRange(skill.getCastRange(), attackable, target, true))
		{
			return false;
		}
		
		if (target.isCreature())
		{
			// Skip if target is already affected by such skill.
			if (skill.isContinuous())
			{
				if (target.asCreature().getEffectList().hasAbnormalType(skill.getAbnormalType(), i -> (i.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel())))
				{
					return false;
				}
				
				// There are cases where bad skills (negative effect points) are actually buffs and NPCs cast them on players, but they shouldn't.
				if ((!skill.isDebuff() || !skill.isBad()) && target.isAutoAttackable(attackable))
				{
					return false;
				}
			}
			
			// Check if target had buffs if skill is bad cancel, or debuffs if skill is good cancel.
			if (skill.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
			{
				if (skill.isBad())
				{
					if (target.asCreature().getEffectList().getBuffCount() == 0)
					{
						return false;
					}
				}
				else if (target.asCreature().getEffectList().getDebuffCount() == 0)
				{
					return false;
				}
			}
			
			// Check for damaged targets if using healing skill.
			if ((target.asCreature().getCurrentHp() == target.asCreature().getMaxHp()) && skill.hasEffectType(EffectType.HEAL))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private boolean checkTarget(WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		final Attackable npc = getActiveChar();
		if (target.isCreature())
		{
			if (target.asCreature().isDead())
			{
				return false;
			}
			
			if (npc.isMovementDisabled())
			{
				if (!npc.isInsideRadius2D(target, npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.asCreature().getTemplate().getCollisionRadius()))
				{
					return false;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(npc, target))
				{
					return false;
				}
			}
			
			if (!target.isAutoAttackable(npc))
			{
				return false;
			}
		}
		
		return GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), target.getX(), target.getY(), target.getZ(), npc.getInstanceWorld());
	}
	
	private Creature skillTargetReconsider(Skill skill, boolean insideCastRange)
	{
		// Check if skill can be casted.
		final Attackable npc = getActiveChar();
		if (!SkillCaster.checkUseConditions(npc, skill))
		{
			return null;
		}
		
		// There are cases where bad skills (negative effect points) are actually buffs and NPCs cast them on players, but they shouldn't.
		final boolean isBad = skill.isContinuous() ? skill.isDebuff() : skill.isBad();
		
		// Check current target first.
		final int range = insideCastRange ? skill.getCastRange() + getActiveChar().getTemplate().getCollisionRadius() : 2000; // TODO need some forget range
		
		final List<Creature> result = new ArrayList<>();
		if (isBad)
		{
			for (AggroInfo aggro : npc.getAggroList().values())
			{
				if (checkSkillTarget(skill, aggro.getAttacker()))
				{
					result.add(aggro.getAttacker());
				}
			}
		}
		else
		{
			for (Creature creature : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, range))
			{
				if (checkSkillTarget(skill, creature))
				{
					result.add(creature);
				}
			}
			
			// Maybe add self to the list of targets since getVisibleObjects doesn't return yourself.
			if (checkSkillTarget(skill, npc))
			{
				result.add(npc);
			}
			
			// For heal skills sort by hp missing.
			if (skill.hasEffectType(EffectType.HEAL))
			{
				int searchValue = Integer.MAX_VALUE;
				Creature creature = null;
				
				for (Creature c : result)
				{
					final int hpPer = c.getCurrentHpPercent();
					if (hpPer < searchValue)
					{
						searchValue = hpPer;
						creature = c;
					}
				}
				
				if (creature != null)
				{
					return creature;
				}
			}
		}
		
		// Return any target.
		if (!result.isEmpty())
		{
			return result.get(Rnd.get(result.size()));
		}
		
		return null;
	}
	
	private Creature targetReconsider(boolean randomTarget)
	{
		final Attackable npc = getActiveChar();
		if (randomTarget)
		{
			final List<Creature> result = new ArrayList<>();
			for (AggroInfo aggro : npc.getAggroList().values())
			{
				if (checkTarget(aggro.getAttacker()))
				{
					result.add(aggro.getAttacker());
				}
			}
			
			// If npc is aggressive, add characters within aggro range too.
			if (npc.isAggressive())
			{
				for (Creature creature : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, npc.getAggroRange()))
				{
					if (checkTarget(creature))
					{
						result.add(creature);
					}
				}
			}
			
			if (!result.isEmpty())
			{
				return result.get(Rnd.get(result.size()));
			}
		}
		
		long searchValue = Long.MIN_VALUE;
		Creature creature = null;
		for (AggroInfo aggro : npc.getAggroList().values())
		{
			if (checkTarget(aggro.getAttacker()) && (aggro.getHate() > searchValue))
			{
				searchValue = aggro.getHate();
				creature = aggro.getAttacker();
			}
		}
		
		if ((creature == null) && npc.isAggressive())
		{
			for (Creature nearby : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, npc.getAggroRange()))
			{
				if (checkTarget(nearby))
				{
					return nearby;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Manage AI thinking actions of a Attackable.
	 */
	@Override
	public void onEvtThink()
	{
		// Check if a thinking action is already in progress.
		if (_thinking)
		{
			return;
		}
		
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		// Check if the actor is all skills disabled.
		if (getActiveChar().isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable
			switch (getIntention())
			{
				case AI_INTENTION_ACTIVE:
				{
					thinkActive();
					break;
				}
				case AI_INTENTION_ATTACK:
				{
					thinkAttack();
					break;
				}
				case AI_INTENTION_CAST:
				{
					thinkCast();
					break;
				}
			}
		}
		catch (Exception e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": " + this.getActor().getName() + " - onEvtThink() failed!");
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Event Attacked.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player</li>
	 * <li>Set the Intention to AI_INTENTION_ATTACK</li>
	 * </ul>
	 * @param attacker The Creature that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
		final Attackable me = getActiveChar();
		final WorldObject target = getTarget();
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor if not present.
		if (!me.isInAggroList(attacker))
		{
			me.addDamageHate(attacker, 0, 1);
		}
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		if (!getActiveChar().isCoreAIDisabled())
		{
			// Set the Intention to AI_INTENTION_ATTACK
			if (getIntention() != AI_INTENTION_ATTACK)
			{
				setIntention(AI_INTENTION_ATTACK, attacker);
			}
			else if (me.getMostHated() != target)
			{
				setIntention(AI_INTENTION_ATTACK, attacker);
			}
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
		}
		
		// [MEJORA DE RUSACIS - EVENT DRIVEN FACTION CALL]
		// Ejecuta el escaneo de Clan/Facción de manera controlada por eventos con Cooldown de 5 segundos.
		// Al hacer esto en 'onEvtAttacked' en vez de 'thinkAttack', liberamos una carga gigantesca de CPU en el servidor.
		final long currentTime = System.currentTimeMillis();
		if ((currentTime - _lastFactionCallTime) > 5000)
		{
			_lastFactionCallTime = currentTime;
			handleFactionCall(attacker);
		}
		
		super.onEvtAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Event Aggression.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is GuardInstance check if it isn't too far from its home location)</li>
	 * </ul>
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
		final Attackable me = getActiveChar();
		if (me.isDead())
		{
			return;
		}
		
		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);
			
			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != AI_INTENTION_ATTACK)
			{
				// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
				if (!me.isRunning())
				{
					me.setRunning();
				}
				
				setIntention(AI_INTENTION_ATTACK, target);
			}
			
			if (me.isMonster())
			{
				Monster master = me.asMonster();
				if (master.hasMinions())
				{
					master.getMinionList().onAssist(me, target);
				}
				
				master = master.getLeader();
				if ((master != null) && master.hasMinions())
				{
					master.getMinionList().onAssist(me, target);
				}
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	@Override
	public void setTarget(WorldObject target)
	{
		// NPCs share their regular target with AI target.
		_actor.setTarget(target);
	}
	
	@Override
	public WorldObject getTarget()
	{
		// NPCs share their regular target with AI target.
		return _actor.getTarget();
	}
	
	public Attackable getActiveChar()
	{
		return _actor.asAttackable();
	}
	
	/**
	 * Busca una ubicación adecuada alrededor del objetivo para dispersar a los mobs, evitando que se amontonen unos sobre otros. Se redujo de 12 a 8 ángulos (pasos de 45°) para aliviar drásticamente el consumo de CPU y llamadas a GeoEngine.
	 * @param target El personaje que está siendo atacado.
	 * @param desiredDistance La distancia ideal desde el objetivo donde el mob debe pararse.
	 * @param minSeparation El radio de separación mínimo entre este mob y los demás.
	 * @return Una Location válida, o null si no se encuentra espacio disponible.
	 */
	private Location findSpreadLocation(Creature target, int desiredDistance, int minSeparation)
	{
		final Attackable npc = getActiveChar();
		final double baseAngle = Math.atan2(npc.getY() - target.getY(), npc.getX() - target.getX());
		
		// 8 pasos cubren los 360 grados en saltos de 45 grados.
		for (int i = 0; i < 8; i++)
		{
			final double angleOffset = (i * Math.PI) / 4;
			for (double sign : new double[]
			{
				1.0,
				-1.0
			})
			{
				if ((i == 0) && (sign == -1.0))
				{
					continue;
				}
				
				final double testAngle = baseAngle + (sign * angleOffset);
				final int testX = target.getX() + (int) (desiredDistance * Math.cos(testAngle));
				final int testY = target.getY() + (int) (desiredDistance * Math.sin(testAngle));
				
				/*
				 * No usar target.getZ() como Z del punto lateral: en pendientes ese X/Y puede pertenecer a otra altura. Pedimos a GeoEngine el último punto realmente alcanzable desde la posición actual del NPC.
				 */
				final Location validLoc = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), testX, testY, target.getZ(), npc.getInstanceWorld());
				
				// Si GeoEngine tuvo que recortar mucho el destino, este punto no sirve para dispersión.
				if ((Math.abs(validLoc.getX() - testX) > 32) || (Math.abs(validLoc.getY() - testY) > 32))
				{
					continue;
				}
				
				if (!GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), validLoc.getX(), validLoc.getY(), validLoc.getZ(), npc.getInstanceWorld()))
				{
					continue;
				}
				
				boolean spotOccupied = false;
				for (Attackable nearby : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, minSeparation * 2))
				{
					if ((nearby == npc) || nearby.isDead() || (nearby.getTarget() != target))
					{
						continue;
					}
					
					if (nearby.calculateDistance2D(validLoc.getX(), validLoc.getY(), validLoc.getZ()) < minSeparation)
					{
						spotOccupied = true;
						break;
					}
				}
				
				if (!spotOccupied)
				{
					return validLoc;
				}
			}
		}
		return null;
	}
	
	@Override
	protected boolean maybeMoveToPawn(WorldObject target, int range)
	{
		final Attackable npc = getActiveChar();
		if (npc.isMovementDisabled() || (target == null))
		{
			return false;
		}
		
		// Si ya estamos dentro del rango solicitado, no iniciar un nuevo movimiento.
		if (npc.isInsideRadius2D(target, range))
		{
			return false;
		}
		
		/*
		 * Usar directamente la ubicación devuelta por GeoEngine. No aplicar getNearestZ, getSmoothZ ni interpolación vertical después, porque podría cambiar de capa.
		 */
		final Location validLoc = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), target.getX(), target.getY(), target.getZ(), npc.getInstanceWorld());
		if (npc.calculateDistanceSq2D(validLoc) > 25)
		{
			moveTo(validLoc.getX(), validLoc.getY(), validLoc.getZ());
			return true;
		}
		
		return false;
	}
	
	/**
	 * [OPTIMIZACIÓN DE RUSACIS] Método auxiliar de Faction Call. Ejecuta el llamado de asistencia una sola vez al ser atacado. Reduce drásticamente las solicitudes de búsqueda de objetos en el Grid del servidor.
	 */
	private void handleFactionCall(Creature attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		final NpcTemplate template = npc.getTemplate();
		final Set<Integer> clans = template.getClans();
		
		if ((clans != null) && !clans.isEmpty())
		{
			final int collision = template.getCollisionRadius();
			final int factionRange = template.getClanHelpRange() + collision;
			
			try
			{
				World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, factionRange, nearby ->
				{
					// Don't call dead npcs, npcs without ai or npcs which are too far away.
					if (nearby.isDead() || !nearby.hasAI() || (Math.abs(attacker.getZ() - nearby.getZ()) > 600))
					{
						return;
					}
					// Don't call npcs who are already doing some action (e.g. attacking, casting).
					if ((nearby.getAI()._intention != CtrlIntention.AI_INTENTION_IDLE) && (nearby.getAI()._intention != CtrlIntention.AI_INTENTION_ACTIVE))
					{
						return;
					}
					// Don't call npcs who aren't in the same clan.
					final NpcTemplate nearbytemplate = nearby.getTemplate();
					if (!template.isClan(nearbytemplate.getClans()) || (nearbytemplate.hasIgnoreClanNpcIds() && nearbytemplate.getIgnoreClanNpcIds().contains(npc.getId())))
					{
						return;
					}
					
					// MEJORA DE ACIS: Evita que los aliados acudan en ayuda si están separados por paredes sólidas en habitaciones cerradas de mazmorras.
					if (!GeoEngine.getInstance().canSeeTarget(npc, nearby))
					{
						return;
					}
					
					if (attacker.isPlayable())
					{
						// Dimensional Rift check.
						if (attacker.isInParty() && attacker.getParty().isInDimensionalRift())
						{
							final byte riftType = attacker.getParty().getDimensionalRift().getType();
							final byte riftRoom = attacker.getParty().getDimensionalRift().getCurrentRoom();
							if ((npc instanceof RiftInvader) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
							{
								return;
							}
						}
						
						// By default, when a faction member calls for help, attack the caller's attacker.
						// Notify the AI with EVT_AGGRESSION
						nearby.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 1);
						
						if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, nearby))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(nearby, npc, attacker.asPlayer(), attacker.isSummon()), nearby);
						}
					}
					else if (nearby.getAI()._intention != AI_INTENTION_ATTACK)
					{
						nearby.addDamageHate(attacker, 0, npc.getHating(attacker));
						nearby.getAI().setIntention(AI_INTENTION_ATTACK, attacker);
					}
				});
			}
			catch (NullPointerException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": handleFactionCall() faction call failed: " + e.getMessage());
			}
		}
	}
}