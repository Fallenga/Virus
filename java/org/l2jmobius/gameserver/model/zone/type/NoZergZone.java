package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.enums.PartyMessageType;
import org.l2jmobius.gameserver.enums.TeleportWhereType;
import org.l2jmobius.gameserver.instancemanager.ZergManager;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Lucas
 */
public class NoZergZone extends ZoneType // Cambiado de L2NoZergZone a NoZergZone
{
	private int _maxClanMembers;
	private int _maxAllyMembers;
	private int _minPartyMembers;
	private boolean _showRules;
	private boolean _checkParty;
	private boolean _checkClan;
	private boolean _checkAlly;
	
	public NoZergZone(int id)
	{
		super(id);
		
		_maxClanMembers = 0;
		_maxAllyMembers = 0;
		_minPartyMembers = 0;
		_showRules = false;
		_checkParty = false;
		_checkClan = false;
		_checkAlly = false;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "MaxClanMembers":
				_maxClanMembers = Integer.parseInt(value);
				break;
			case "MaxAllyMembers":
				_maxAllyMembers = Integer.parseInt(value);
				break;
			case "MinPartyMembers":
				_minPartyMembers = Integer.parseInt(value);
				break;
			case "showRules":
				_showRules = Boolean.parseBoolean(value);
				break;
			case "checkParty":
				_checkParty = Boolean.parseBoolean(value);
				break;
			case "checkClan":
				_checkClan = Boolean.parseBoolean(value);
				break;
			case "checkAlly":
				_checkAlly = Boolean.parseBoolean(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.NO_ZERG, true);
		
		if (creature instanceof Player)
		{
			final Player activeChar = (Player) creature;
			
			// Primero verificar clan - si falla, no continúa
			if (_checkClan)
			{
				if (MaxClanMembersOnArea(activeChar))
				{
					// Ya fue teletransportado por ZergManager
					return;
				}
			}
			
			// Verificar alianza
			if (_checkAlly)
			{
				if (MaxAllyMembersOnArea(activeChar))
				{
					return;
				}
			}
			
			// Verificar party
			if (_checkParty)
			{
				if (!activeChar.isInParty() || (activeChar.getParty().getMemberCount() < _minPartyMembers))
				{
					activeChar.sendPacket(new ExShowScreenMessage("Your party does not have " + _minPartyMembers + " members to enter this zone!", 6000));
					activeChar.teleToLocation(TeleportWhereType.TOWN);
					return;
				}
				checkPartyMembers(activeChar);
			}
			
			// Mostrar reglas
			if (_showRules)
			{
				ZergManager.showZergHtml(activeChar, 0);
			}
		}
	}
	
	public boolean MaxClanMembersOnArea(Player activeChar)
	{
		return ZergManager.getInstance().checkClanArea(activeChar, _maxClanMembers, true);
	}
	
	public boolean MaxAllyMembersOnArea(Player activeChar)
	{
		return ZergManager.getInstance().checkAllyArea(activeChar, _maxAllyMembers, World.getInstance().getPlayers(), true);
	}
	
	public void checkPartyMembers(Player player)
	{
		Party party = player.getParty();
		
		if (party == null)
		{
			return;
		}
		
		for (Player member : party.getMembers())
		{
			if ((member == null) || !member.isOnline())
			{
				continue;
			}
			
			// Verificar si el miembro es del mismo clan/alianza
			if ((member.getClan() != player.getClan()) || ((member.getClan() != null) && (player.getClan() != null) && (member.getClan().getAllyId() != player.getClan().getAllyId())))
			{
				player.sendMessage("Only clan/alliance party members are allowed.");
				player.getParty().removePartyMember(player, PartyMessageType.LEFT);
				break;
			}
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.NO_ZERG, false);
	}
	
	@Override
	public void onDieInside(Creature character)
	{
		// No action needed
	}
	
	@Override
	public void onReviveInside(Creature character)
	{
		// No action needed
	}
}