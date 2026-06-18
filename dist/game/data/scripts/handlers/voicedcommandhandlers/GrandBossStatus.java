package handlers.voicedcommandhandlers;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.instancemanager.DBSpawnManager;
import org.l2jmobius.gameserver.instancemanager.GrandBossManager;
import org.l2jmobius.gameserver.instancemanager.PremiumManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class GrandBossStatus implements IVoicedCommandHandler
{
	
	private static final String[] VOICED_COMMANDS =
	{
		"grandboss",
		"raidboss"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.startsWith("grandboss"))
		{
			return GrandStatus(activeChar);
		}
		if (command.startsWith("raidboss"))
		{
			return BossStatus(activeChar);
		}
		return true;
	}
	
	public boolean GrandStatus(Player activeChar)
	{
		final long endDate = PremiumManager.getInstance().getPremiumExpiration(activeChar.getAccountName());
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		final StringBuilder html = new StringBuilder();
		
		int[] BOSSES =
		{
			
			29001, // AQ
			29006, // CORE
			29014, // ORFEN
			29068, // ANTHARAS (buvo)
			29022, // ZAKEN
			29118, // Beleth
			29020, // BAIUM
			29045, // Finteza
			29046, // Halisha
			29047, // Halisha 2
			29099, // Baylor
			29150, // Ekimus
			29163, // Tiat
			29175, // Tiat2
			29028 // VALAKAS
		};
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
		if (endDate == 0)
		{
			html.append("<html><body><title>Grand Bosss Status</title><center>");
			html.append("<table>");
			html.append("<tr><td><center>Status: <font color=\"LEVEL\">Sorry, only premium<br></font></td></tr>");
		}
		else
		{
			html.append("<html><body><title>Grand Raids Status</title><center>");
			html.append("<table>");
			html.append("<tr><td><center>Account Status: <font color=\"LEVEL\">Premium<br></font></td></tr>");
			
		}
		
		for (int boss : BOSSES)
		{
			// if doesnt work remove toString - invoke
			String name = NpcData.getInstance().getTemplate(boss).getName().toString();
			StatSet stats = GrandBossManager.getInstance().getStatSet(boss);
			if (stats == null)
			{
				if (endDate > 0)
				{
					html.append("<tr><td>Status for GrandBoss <font color=\"LEVEL\"> " + boss + " </font> is not found</td></tr>");
				}
				continue;
			}
			if (boss == 29019)
			{
				long dmax = 0;
				for (int i = 29066; i <= 29068; i++)
				{
					StatSet s = GrandBossManager.getInstance().getStatSet(i);
					if (s == null)
					{
						continue;
					}
					long d = s.getLong("respawn_time");
					if (d >= dmax)
					{
						dmax = d;
						stats = s;
					}
				}
			}
			long delay = stats.getLong("respawn_time");
			long currentTime = System.currentTimeMillis();
			if (delay <= currentTime)
			{
				if (endDate > 0)
				{
					html.append("<tr><td><font color=\"LEVEL\"> " + name + " </font> is Alive</td></tr>");
				}
				
				// activeChar.sendMessage(" " + name + " Is Alive");
			}
			else
			{
				if (endDate > 0)
				{
					html.append("<tr><td><font color=\"LEVEL\"> " + name + " </font> is Death (" + sdf.format(new Date(delay)) + " )</td></tr>");
				}
			}
		}
		html.append("<tr><td>==============================</td></tr>");
		html.append("</table>");
		html.append("</center></body></html>");
		msg.setHtml(html.toString());
		activeChar.sendPacket(msg);
		
		return true;
	}
	
	public boolean BossStatus(Player activeChar)
	{
		
		int[] RBOSSES =
		{
			25001, // Greyclaw Kutus
			25004, // Turek Mercenary Captain
			25007, // Retreat Spider Cletu
			25010, // Furious Thieles
			25013, // Ghost of Peasant Leader
			25016, // The 3rd Underwater Guardian
			25019, // Pan Dryad
			25020, // Breka Warlock Pastu
			25023, // Stakato Queen Zyrnna
			25026, // Katu Van Leader Atui
			25029, // Atraiban
			25032, // Eva's Guardian Millenu
			25035, // Shilen's Messenger Cabrio
			25038, // Tirak
			25041, // Remmel
			25044, // Barion
			25047, // Karte
			25050, // Verfa
			25051, // Rahha
			25054, // Kernon
			25057, // Biconne of Blue Sky
			25060, // Unrequited Kael
			25063, // Chertuba of Great Soul
			25064, // Wizard of Storm Teruk
			25067, // Captain of Red Flag Shaka
			25070, // Enchanted Forest Watcher Ruell
			25073, // Bloody Priest Rudelto
			25076, // Princess Molrang
			25079, // Cat's Eye Bandit
			25082, // Leader of Cat Gang
			25085, // Timak Orc Chief Ranger
			25088, // Crazy Mechanic Golem
			25089, // Soulless Wild Boar
			25092, // Korim
			25095, // Elf Renoa
			25098, // Sejarr's Servitor
			25099, // Rotten Tree Repiro
			25102, // Shacram
			25103, // Sorcerer Isirr
			25106, // Ghost of the Well Lidia
			25109, // Antharas Priest Cloe
			25112, // Agent of Beres, Meana
			25115, // Icarus Sample 1
			25118, // Guilotine, Warden of the Execution Grounds
			25119, // Messenger of Fairy Queen Berun
			25122, // Refugee Hopeful Leo
			25125, // Fierce Tiger King Angel
			25126, // Longhorn Golkonda
			25127, // Langk Matriarch Rashkos
			25128, // Vuku Grand Seer Gharmash
			25131, // Carnage Lord Gato
			25134, // Leto Chief Talkin
			25137, // Beleth's Seer Sephia
			25140, // Hekaton Prime
			25143, // Fire of Wrath Shuriel
			25146, // Serpent Demon Bifrons
			25149, // Zombie Lord Crowl
			25152, // Flame Lord Shadar
			25155, // Shaman King Selu
			25158, // King Tarlk
			25159, // Paniel the Unicorn
			25162, // Giant Marpanak
			25163, // Roaring Skylancer
			25166, // Ikuntai
			25169, // Ragraman
			25170, // Lizardmen Leader Hellion
			25173, // Tiger King Karuta
			25176, // Black Lily
			25179, // Guardian of the Statue of Giant Karum
			25182, // Demon Kurikups
			25185, // Tasaba Patriarch Hellena
			25188, // Apepi
			25189, // Cronos's Servitor Mumu
			25192, // Earth Protector Panathen
			25198, // Fafurion's Herald Lokness
			25199, // Water Dragon Seer Sheshark
			25202, // Krokian Padisha Sobekk
			25205, // Ocean Flame Ashakiel
			25208, // Water Couatle Ateka
			25211, // Sebek
			25214, // Fafurion's Page Sika
			25217, // Cursed Clara
			25220, // Death Lord Hallate
			25223, // Soul Collector Acheron
			25226, // Roaring Lord Kastor
			25229, // Storm Winged Naga
			25230, // Timak Seer Ragoth
			25233, // Spirit of Andras, the Betrayer
			25234, // Ancient Weird Drake
			25235, // Vanor Chief Kandra
			25238, // Abyss Brukunt
			25241, // Harit Hero Tamash
			25244, // Last Lesser Giant Olkuth
			25245, // Last Lesser Giant Glaki
			25248, // Doom Blade Tanatos
			25249, // Palatanos of Horrific Power
			25252, // Palibati Queen Themis
			25255, // Gargoyle Lord Tiphon
			25256, // Taik High Prefect Arak
			25259, // Zaken's Butcher Krantz
			25260, // Iron Giant Totem
			25263, // Kernon's Faithful Servant Kelone
			25266, // Bloody Empress Decarbia
			25269, // Beast Lord Behemoth
			25272, // Partisan Leader Talakin
			25273, // Carnamakos
			25276, // Death Lord Ipos
			25277, // Lilith's Witch Marilion
			25280, // Pagan Watcher Cerberon
			25281, // Anakim's Nemesis Zakaron
			25282, // Death Lord Shax
			25283, // Lilith
			25286, // Anakim
			25290, // Daimon the White-Eyed
			25293, // Hestia, Guardian Deity of the Hot Springs
			25296, // Icicle Emperor Bumbalump
			25299, // Ketra's Hero Hekaton
			25302, // Ketra's Commander Tayr
			25305, // Ketra's Chief Brakki
			25306, // Soul of Fire Nastron
			25309, // Varka's Hero Shadith
			25312, // Varka's Commander Mos
			25315, // Varka's Chief Horus
			25316, // Soul of Water Ashutar
			25319, // Ember
			25322, // Demon's Agent Falston
			25325, // Flame of Splendor Barakiel
			25328, // Eilhalder von Hellmann
			25352, // Giant Wasteland Basilisk
			25354, // Gargoyle Lord Sirocco
			25357, // Sukar Wererat Chief
			25360, // Tiger Hornet
			25362, // Tracker Leader Sharuk
			25365, // Patriarch Kuroboros
			25366, // Kuroboros' Priest
			25369, // Soul Scavenger
			25372, // Discarded Guardian
			25373, // Malex Herald of Dagoniel
			25375, // Zombie Lord Farakelsus
			25378, // Madness Beast
			25380, // Kaysha Herald of Icarus
			25383, // Revenant of Sir Calibus
			25385, // Evil Spirit Tempest
			25388, // Red Eye Captain Trakia
			25391, // Nurka's Messenger
			25392, // Captain of Queen's Royal Guards
			25394, // Premo Prime
			25395, // Archon Suscepter
			25398, // Eye of Beleth
			25401, // Skyla
			25404, // Corsair Captain Kylon
			25407, // Lord Ishka
			25410, // Road Scavenger Leader
			25412, // Necrosentinel Royal Guard
			25415, // Nakondas
			25418, // Dread Avenger Kraven
			25420, // Orfen's Handmaiden
			25423, // Fairy Queen Timiniel
			25426, // Betrayer of Urutu Freki
			25429, // Mammon Collector Talos
			25431, // Flamestone Golem
			25434, // Bandit Leader Barda
			25437, // Timak Orc Gosmos
			25438, // Thief Kelbar
			25441, // Evil Spirit Cyrion
			25444, // Enmity Ghost Ramdal
			25447, // Immortal Savior Mardil
			25450, // Cherub Galaxia
			25453, // Meanas Anor
			25456, // Mirror of Oblivion
			25460, // Deadman Ereve
			25463, // Harit Guardian Garangky
			25467, // Gorgolos
			25470, // Last Titan Utenus
			25473, // Grave Robber Kim
			25475, // Ghost Knight Kabed
			25478, // Shilen's Priest Hisilrome
			25481, // Magus Kenishee
			25484, // Zaken's Chief Mate Tillion
			25487, // Water Spirit Lian
			25490, // Gwindorr
			25493, // Eva's Spirit Niniel
			25496, // Fafurion's Envoy Pingolpin
			25498, // Fafurion's Henchman Istary
			25501, // Boss Akata
			25504, // Nellis' Vengeful Spirit
			25506, // Rayito the Looter
			25509, // Dark Shaman Varangka
			25512, // Gigantic Chaos Golem
			25514, // Queen Shyeed
			25523, // Plague Golem
			25524, // Flamestone Giant
			25527, // Uruka
			25701, // Lord of Splendor Anais (Master Anays)
			29054, // Benom
			29056, // Ice Fairy Sirra
			29060, // Captain of the Ice Queen's Royal Guard
			29062, // Andreas Van Halter
			29065, // Sailren
			29118, // Beleth
			29119 // Beleth
		};
		
		final long endDate = PremiumManager.getInstance().getPremiumExpiration(activeChar.getAccountName());
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		final StringBuilder html = new StringBuilder();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
		if (endDate == 0)
		{
			html.append("<html><body><title>Grand Bosss Status</title><center>");
			html.append("<table style=\"width:100%\"");
			html.append("<tr><td><center>Status: <font color=\"LEVEL\">Sorry, only premium<br></font></td></tr>");
		}
		else
		{
			html.append("<html><body><title>Grand Raids Status</title><center>");
			html.append("<table style=\"width:100%\">");
			html.append("<tr><td><center>Account Status: <font color=\"LEVEL\">Premium<br></font></td></tr>");
			
		}
		
		for (int rboss : RBOSSES)
		{
			String namer = null;
			try
			{
				namer = NpcData.getInstance().getTemplate(rboss).getName();
			}
			catch (NullPointerException npe)
			{
				
			}
			
			if (namer == null)
			{
				// if (endDate > 0)
				// {
				// html.append("<tr><td>Npc template for ID: <font color=\"LEVEL\"> " + rboss + " <br1></font> isn't exists!</td></tr>");
				// }
				continue;
			}
			
			StatSet statsr = DBSpawnManager.getInstance().getStoredInfo().get(rboss);
			if (statsr == null)
			{
				// if (endDate > 0)
				// {
				// html.append("<tr><td>ID: <font color=\"LEVEL\"> " + namer + " </font> not found!</td></tr>");
				// }
				continue;
			}
			
			long delayr = statsr.getLong("respawnTime");
			long currentTime = System.currentTimeMillis();
			if (delayr <= currentTime)
			{
				if (endDate > 0)
				{
					html.append("<tr><td><font color=\"LEVEL\"> " + namer + " </font> <font color=\"green\">is Alive! </font></td></tr>");
				}
				
			}
			else
			{
				if (endDate > 0)
				{
					html.append("<tr><td><font color=\"LEVEL\"> " + namer + " </font> <font color=\"red\">is Death! </font>" + sdf.format(new Date(delayr)) + "</td></tr>");
				}
			}
		}
		html.append("<tr><td>==============================</td></tr>");
		html.append("</table>");
		html.append("</center></body></html>");
		msg.setHtml(html.toString());
		activeChar.sendPacket(msg);
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
