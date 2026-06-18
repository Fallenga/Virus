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
package org.l2jmobius.commons.util;

/**
 * @author Lucas
 */
public class RewardHolder
{
	private int _id;
	private int _min;
	private int _max;
	private int _chance;
	
	/**
	 * @param rewardId
	 * @param rewardMin
	 * @param rewardMax
	 */
	public RewardHolder(int rewardId, int rewardMin, int rewardMax)
	{
		_id = rewardId;
		_min = rewardMin;
		_max = rewardMax;
		_chance = 100;
	}
	
	/**
	 * @param rewardId
	 * @param rewardMin
	 * @param rewardMax
	 * @param rewardChance
	 */
	public RewardHolder(int rewardId, int rewardMin, int rewardMax, int rewardChance)
	{
		_id = rewardId;
		_min = rewardMin;
		_max = rewardMax;
		_chance = rewardChance;
	}
	
	public int getRewardId()
	{
		return _id;
	}
	
	public int getRewardMin()
	{
		return _min;
	}
	
	public int getRewardMax()
	{
		return _max;
	}
	
	public int getRewardChance()
	{
		return _chance;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public void setMin(int min)
	{
		_min = min;
	}
	
	public void setMax(int max)
	{
		_max = max;
	}
	
	public void setChance(int chance)
	{
		_chance = chance;
	}
}
