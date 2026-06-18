package handlers.itemhandlers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.instancemanager.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

public class VipItemClick implements IItemHandler
{
	private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(d|dias|h|horas)", Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean useItem(Playable playable, Item item, boolean forceUse)
	{
		if (!(playable instanceof Player))
		{
			return false;
		}
		
		Player activeChar = (Player) playable;
		
		if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isDead() || activeChar.hasPremiumStatus())
		{
			activeChar.sendMessage("SYS: Cannot use item now.");
			return false;
		}
		
		String name = item.getItemName();
		Matcher matcher = DURATION_PATTERN.matcher(name);
		long durationMs = 0;
		
		if (durationMs <= 0)
		{
			activeChar.sendMessage("SYS: VIP duration is invalid.");
			return false;
		}
		
		if (activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
		{
			long now = Calendar.getInstance().getTimeInMillis();
			
			long currentEnd = Math.max(now, activeChar.getVipEndTime());
			long newEnd = currentEnd + durationMs;
			
			PremiumManager.getInstance().addPremiumTime(activeChar.getAccountName(), 15, TimeUnit.DAYS);
			activeChar.sendMessage("Tu cuenta ahora tendra estado premium hasta " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(PremiumManager.getInstance().getPremiumExpiration(activeChar.getAccountName())) + ".");
			
			String type = (durationMs >= 86400000L) ? "day(s)" : "hour(s)";
			long amount = (type.equals("day(s)")) ? (durationMs / 86400000L) : (durationMs / 3600000L);
			
			activeChar.broadcastUserInfo();
			activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		}
		return true;
	}
}
