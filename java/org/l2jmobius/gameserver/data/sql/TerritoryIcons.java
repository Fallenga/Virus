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
package org.l2jmobius.gameserver.data.sql;

/**
 * @author Lucas
 */
public class TerritoryIcons
{
	/**
	 * Returns the icon name for the territory/castle insignia by its ID.
	 * @param territoryId Territory or castle ID
	 * @return Icon path for HTML usage
	 */
	public static String getIcon(int territoryId)
	{
		switch (territoryId)
		{
			case 1049: // Gludio
			case 911:
			case 912:
			case 910: // Gludin
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Gludio";
			case 1052: // Dion
			case 916:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Dion";
			case 1053: // Giran
			case 918:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Giran";
			case 1054: // Oren
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Oren";
			case 1055: // Innadril
			case 919: // Heine
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Innadril";
			case 1057: // Rune
			case 1537:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Rune";
			case 1060: // Goddard
			case 1538:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Godard";
			case 1059: // Schuttgart
			case 1714:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Schuttgart";
			case 1248: // Aden
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Aden";
			default:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Aden";
		}
	}
	
	/**
	 * Returns the territory icon by castle name.
	 * @param castleName The name of the castle
	 * @return Icon path for HTML usage
	 */
	public static String getIconByCastleName(String castleName)
	{
		switch (castleName.toLowerCase())
		{
			case "gludio":
			case "gludin":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Gludio";
			case "dion":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Dion";
			case "giran":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Giran";
			case "oren":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Oren";
			case "innadril":
			case "heine":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Innadril";
			case "rune":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Rune";
			case "goddard":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Godard";
			case "schuttgart":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Schuttgart";
			case "aden":
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Aden";
			default:
				return "L2UI_CT1.Minimap_DF_ICN_TerritoryWar_Aden";
		}
	}
}
