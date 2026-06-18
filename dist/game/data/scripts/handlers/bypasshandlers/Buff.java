package handlers.bypasshandlers;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;

import handlers.voicedcommandhandlers.BuffCommand;

/**
 * @author Lucas
 */
public class Buff implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"buffCommandFight",
		"buffCommandMage",
		"buffCommand",
		"cancelBuffs"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!BuffCommand.check(player))
		{
			return false;
		}
		
		try
		{
			if (command.startsWith("buffCommandFight"))
			{
				BuffCommand.getFullBuff(player, false);
			}
			else if (command.startsWith("buffCommandMage"))
			{
				BuffCommand.getFullBuff(player, true);
			}
			else if (command.startsWith("buffCommand"))
			{
				// Soporta formatos "buffCommand 1204", "buffCommand_1204" y "buffCommand1204"
				String idBuff = command.replace("buffCommand", "").trim();
				if (idBuff.startsWith("_"))
				{
					idBuff = idBuff.substring(1);
				}
				
				if (!idBuff.isEmpty())
				{
					int parseIdBuff = Integer.parseInt(idBuff);
					SkillData.getInstance().getSkill(parseIdBuff, SkillData.getInstance().getMaxLevel(parseIdBuff)).applyEffects(player, player);
					BuffCommand.showHtml(player);
				}
			}
			else if (command.startsWith("cancelBuffs"))
			{
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				BuffCommand.showHtml(player);
			}
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}