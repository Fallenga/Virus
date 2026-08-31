/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 */
package handlers.voicedcommandhandlers;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Info implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"info"
	};
	
	private static final Map<String, String> INFO_PAGES = new HashMap<>();
	static
	{
		INFO_PAGES.put("main", "welcome.htm");
		INFO_PAGES.put("rates", "rates.htm");
		INFO_PAGES.put("clanes", "clanes.htm");
		INFO_PAGES.put("subclass", "subclass.htm");
		INFO_PAGES.put("raidboss", "raidboss.htm");
		INFO_PAGES.put("comandos", "comandos.htm");
		INFO_PAGES.put("eventos", "eventos.htm");
		INFO_PAGES.put("donaciones", "donaciones.htm");
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("Este comando no puede utilizarse durante las Olimpiadas.");
			return true;
		}
		
		String page = "main";
		if ((target != null) && !target.trim().isEmpty())
		{
			page = target.trim().split("\\s+")[0].toLowerCase();
		}
		
		final String file = INFO_PAGES.getOrDefault(page, "welcome.htm");
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(activeChar, "data/html/info/" + file);
		html.replace("%name%", activeChar.getName());
		activeChar.sendPacket(html);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
