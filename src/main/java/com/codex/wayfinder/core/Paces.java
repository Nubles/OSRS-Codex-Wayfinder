package com.codex.wayfinder.core;

import java.util.HashMap;
import java.util.Map;

/**
 * How long this player's own trips really take: seconds per trip, from the click to standing at
 * the landing, and how much slower than a straight run their walking is. Saved as JSON; fields
 * are plain so older saves still read.
 */
public final class Paces
{
	/** Running is two tiles a game tick of 0.6 seconds. */
	public static final double RUN_SECONDS_PER_TILE = 0.3;
	/** Walking in the world is slower than a straight run: corners, doors, a step back. */
	public static final double DEFAULT_WALK_FACTOR = 1.15;
	/** Trips more than this many times the estimate were interrupted, not slow. */
	static final double OUTLIER = 3.0;

	public Map<String, Lap> laps = new HashMap<>();
	public double walkFactor = DEFAULT_WALK_FACTOR;
	public int walkSamples;

	public static final class Lap
	{
		public double seconds;
		public int count;
		public long last;

		public Lap()
		{
		}

		Lap(double seconds, int count, long last)
		{
			this.seconds = seconds;
			this.count = count;
			this.last = last;
		}
	}

	/** This player's seconds for a trip, or 0 when it has never been timed. */
	public int seconds(String key)
	{
		Lap l = laps == null ? null : laps.get(key);
		return l == null || l.count == 0 ? 0 : (int) Math.round(l.seconds);
	}

	public int count(String key)
	{
		Lap l = laps == null ? null : laps.get(key);
		return l == null ? 0 : l.count;
	}

	/**
	 * Adds a timed trip. The first few laps average; after that each new lap moves the figure a
	 * fifth of the way, so it follows a player who gets quicker.
	 *
	 * @return false when the lap was thrown out as an interruption
	 */
	public boolean add(String key, double seconds, double estimate, long now)
	{
		if (seconds <= 0 || (estimate > 0 && seconds > estimate * OUTLIER + 20))
		{
			return false;
		}
		if (laps == null)
		{
			laps = new HashMap<>();
		}
		Lap l = laps.get(key);
		if (l == null)
		{
			laps.put(key, new Lap(seconds, 1, now));
			return true;
		}
		double weight = l.count < 4 ? 1.0 / (l.count + 1) : 0.2;
		l.seconds += (seconds - l.seconds) * weight;
		l.count++;
		l.last = now;
		return true;
	}

	/** Adds a measured walk: seconds taken over the seconds a straight run would take. */
	public void addWalk(double seconds, double straightSeconds)
	{
		if (straightSeconds < 6 || seconds <= 0)
		{
			return;
		}
		double factor = seconds / straightSeconds;
		if (factor < 0.9 || factor > 3.0)
		{
			return;
		}
		double weight = walkSamples < 4 ? 1.0 / (walkSamples + 1) : 0.15;
		walkFactor += (factor - walkFactor) * weight;
		walkSamples++;
	}

	public double getWalkFactor()
	{
		return walkFactor <= 0 ? DEFAULT_WALK_FACTOR : walkFactor;
	}

	/** Hundredths of a second for one tile of walking at this player's pace. */
	public int tileCost()
	{
		return (int) Math.round(RUN_SECONDS_PER_TILE * getWalkFactor() * 100);
	}

	public int timedTrips()
	{
		return laps == null ? 0 : laps.size();
	}
}
