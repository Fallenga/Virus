package handlers.voicedcommandhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ability.ExAcquireAPSkillList;

/**
 * @author L2JMod Comando de voz para comprar/gestionar Ability Points manualmente.
 */
public class Apoints implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"apoint"
	};
	
	// CONFIGURACIÓN LOCAL DEL COMANDO
	private static final int REQ_LEVEL = 80; // Nivel mínimo para usar el sistema AP
	private static final int COST_ITEM_ID = 5575; // Ítem requerido: 5575 (Ancient Adena)
	private static final long COST_ITEM_COUNT = 10000000; // Cantidad: 10,000,000 Ancient Adenas por cada AP
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (player == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("apoint"))
		{
			// Si solo escribe ".apoint", le mostramos la información actual de su cuenta
			if ((target == null) || target.isEmpty())
			{
				player.sendMessage("--- Sistema de Ability Points ---");
				player.sendMessage("Tus AP totales actuales: " + player.getAbilityPoints());
				player.sendMessage("AP usados: " + player.getAbilityPointsUsed());
				player.sendMessage("AP disponibles para asignar: " + (player.getAbilityPoints() - player.getAbilityPointsUsed()));
				player.sendMessage("Máximo AP permitido en el servidor: " + Config.ABILITY_MAX_POINTS);
				player.sendMessage("Escribe '.apoint buy' para comprar 1 AP por " + COST_ITEM_COUNT + " Ancient Adenas.");
				return true;
			}
			
			// Si escribe ".apoint buy"
			if (target.equalsIgnoreCase("buy"))
			{
				if (player.getLevel() < REQ_LEVEL)
				{
					player.sendPacket(SystemMessageId.REACH_LEVEL_85_TO_USE_THE_ABILITY);
					return false;
				}
				
				// Validamos que no supere el límite máximo configurado en Character.properties (AbilityMaxPoints)
				if (player.getAbilityPoints() >= Config.ABILITY_MAX_POINTS)
				{
					player.sendMessage("Ya has alcanzado el límite máximo permitido de Ability Points (" + Config.ABILITY_MAX_POINTS + ").");
					return false;
				}
				
				// Comprobar si el jugador tiene suficientes ítems en su inventario
				if (player.getInventory().getInventoryItemCount(COST_ITEM_ID, -1) < COST_ITEM_COUNT)
				{
					player.sendMessage("No tienes los items necesarios.");
					return false;
				}
				
				// Consumir el ítem y entregar el punto de forma segura
				if (player.destroyItemByItemId("AbilityPointBuy", COST_ITEM_ID, COST_ITEM_COUNT, player, true))
				{
					player.setAbilityPoints(player.getAbilityPoints() + 1);
					player.sendMessage("¡Felicidades! Has comprado 1 Ability Point.");
					player.sendMessage("Tus AP totales actuales: " + player.getAbilityPoints());
					player.sendPacket(new ExAcquireAPSkillList(player));
					player.broadcastUserInfo();
				}
				else
				{
					player.sendMessage("Ocurrió un error inesperado al procesar tu compra.");
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}