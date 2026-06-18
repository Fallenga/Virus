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
package ai.areas.PaganTemple;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

import ai.AbstractNpcAI;

/**
 * @author KingHanker
 */
public class PaganMonsters extends AbstractNpcAI
{
	private static final int FORGOTTEN_VICTIM = 22141;
	private static final int RITUAL_OFFERING = 22149;
	
	public PaganMonsters()
	{
		addSpawnId(FORGOTTEN_VICTIM, RITUAL_OFFERING);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		npc.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.VP_KEEP);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new PaganMonsters();
	}
}
