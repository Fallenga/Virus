package org.l2jmobius.gameserver.model.events.fos;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;

public class AdminFOSEvent implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS =
    {
        "admin_fos_add",
        "admin_fos_remove",
        "admin_fos_advance"
    };
   
    @Override
	public boolean useAdminCommand(String command, Player activeChar)
    {
        if (command.startsWith("admin_fos_add"))
        {
            WorldObject target = activeChar.getTarget();
           
            if (!(target instanceof Player))
            {
                activeChar.sendMessage("You should select a player!");
                return true;
            }
           
            add(activeChar, (Player) target);
        }
        else if (command.startsWith("admin_fos_remove"))
        {
            WorldObject target = activeChar.getTarget();
           
            if (!(target instanceof Player))
            {
                activeChar.sendMessage("You should select a player!");
                return true;
            }
           
            remove(activeChar, (Player) target);
        }
        else if (command.startsWith( "admin_fos_advance" ))
        {
            FOSManager.getInstance().skipDelay();
        }
       
        return true;
    }
   
    @Override
	public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }
   
    private static void add(Player activeChar, Player playerInstance)
    {
        if (FOSEvent.isPlayerParticipant(playerInstance.getObjectId()))
        {
            activeChar.sendMessage("Player already participated in the event!");
            return;
        }
       
        if (!FOSEvent.addParticipant(playerInstance))
        {
            activeChar.sendMessage("Player instance could not be added, it seems to be null!");
            return;
        }
       
        if (FOSEvent.isStarted())
        {
        	new FOSEventTeleporter(playerInstance, FOSEvent.getParticipantTeamCoordinates(playerInstance.getObjectId()), true, false);
        }
    }
   
    private static void remove(Player activeChar, Player playerInstance)
    {
        if (!FOSEvent.removeParticipant(playerInstance.getObjectId()))
        {
            activeChar.sendMessage("Player is not part of the event!");
            return;
        }
       
        new FOSEventTeleporter(playerInstance, FOSConfig.FOS_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
    }
}