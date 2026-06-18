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
package handlers.voicedcommandhandlers;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.util.Broadcast;

public class LiveStream implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"live"
	};
	
	// Mapa com os streamers autorizados: objId -> URL
	private static final Map<Integer, String> STREAMERS = new HashMap<>();
	
	// Guarda o último tempo em que o player usou o comando (objId -> millis)
	private static final Map<Integer, Long> LAST_USE = new HashMap<>();
	
	static
	{
		try
		{
			String[] ids = Config.LIVE_STREAMER_CHARID.split(",");
			String[] urls = Config.LIVE_STREAMER_URL.split(",");
			
			for (int i = 0; (i < ids.length) && (i < urls.length); i++)
			{
				int objId = Integer.parseInt(ids[i].trim());
				STREAMERS.put(objId, urls[i].trim());
			}
		}
		catch (Exception e)
		{
			System.out.println("Erro ao carregar configuracao de streamers: " + e.getMessage());
		}
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equalsIgnoreCase("live"))
		{
			int objId = activeChar.getObjectId();
			long now = System.currentTimeMillis();
			
			// Cooldown configurável em minutos
			int cooldownMillis = Config.LIVE_STREAMER_COOLDOWN_MINUTES * 60 * 1000;
			
			// Verifica se o player ainda está no cooldown
			if (LAST_USE.containsKey(objId))
			{
				long last = LAST_USE.get(objId);
				if ((now - last) < cooldownMillis)
				{
					long remaining = (cooldownMillis - (now - last)) / 1000;
					long minutes = remaining / 60;
					long seconds = remaining % 60;
					activeChar.sendMessage("[ESP]Aguarde " + minutes + "m " + seconds + "s para usar este comando nuevamente.");
					activeChar.sendMessage("[EN]You need to wait longer " + minutes + "m " + seconds + "s to use this command again.");
					return true;
				}
			}
			
			// Verifica se o player está autorizado
			if (STREAMERS.containsKey(objId))
			{
				String url = STREAMERS.get(objId);
				Broadcast.toAllOnlinePlayers("[Live]: El player " + activeChar.getName() + " esta en vivo en el link: " + url);
				
				// Atualiza o último uso
				LAST_USE.put(objId, now);
			}
			else
			{
				activeChar.sendMessage("[ES]No estas registrado entre los streamers del servidor.");
				activeChar.sendMessage("[EN]You are not registered among the Streamers on the server.");
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
