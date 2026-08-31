package handlers.itemhandlers;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.instancemanager.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

public class Vip24h implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, Item item, boolean forceUse)
	{
		if (!(playable instanceof Player))
		{
			return false;
		}
		
		Player activeChar = (Player) playable;
		
		// Bloqueos de uso comunes
		if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isDead())
		{
			activeChar.sendMessage("No puedes usar este item en este momento.");
			return false;
		}
		
		// Evitar que usen el item si ya tienen premium activo (opcional, remueve esta condición si quieres acumulativo)
		if (activeChar.hasPremiumStatus())
		{
			activeChar.sendMessage("Ya posees un estado Premium activo.");
			return false;
		}
		
		// Consumimos 1 unidad del ítem
		if (activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
		{
			// Agrega 1 día de Premium a la cuenta del jugador
			PremiumManager.getInstance().addPremiumTime(activeChar.getAccountName(), 7, TimeUnit.DAYS);
			
			// Obtiene la fecha de expiración calculada por el PremiumManager
			long expireTime = PremiumManager.getInstance().getPremiumExpiration(activeChar.getAccountName());
			String formattedDate = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(expireTime);
			
			activeChar.sendMessage("¡Tu cuenta ahora tiene estado Premium de 7 Dias!");
			activeChar.sendMessage("Expiración: " + formattedDate + ".");
			
			// Actualiza la interfaz del usuario
			activeChar.broadcastUserInfo();
			activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		}
		return true;
	}
}