package com.codex.wayfinder.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The tiles one search reached and what each cost, kept in 64 by 64 blocks that are only made
 * when the search reaches them, so a search across the whole world stays small.
 */
public final class Search
{
	static final int UNSEEN = Integer.MAX_VALUE;
	private static final int REGION = 64;
	private static final byte NO_STEP = -1;
	/** A tile reached by a trip, not a step: see {@link #via}. */
	static final byte BY_TRIP = 8;
	static final byte START = 9;

	private final Map<Integer, int[]> costs = new HashMap<>();
	private final Map<Integer, byte[]> steps = new HashMap<>();
	private final Map<Integer, Long> trips = new HashMap<>();
	private int lastKey = Integer.MIN_VALUE;
	private int[] lastCosts;
	private byte[] lastSteps;
	int expanded;

	private static int key(int p)
	{
		return (Pt.plane(p) << 20) | ((Pt.x(p) / REGION) << 10) | (Pt.y(p) / REGION);
	}

	private static int slot(int p)
	{
		return (Pt.y(p) % REGION) * REGION + (Pt.x(p) % REGION);
	}

	private boolean load(int p, boolean create)
	{
		int k = key(p);
		if (k == lastKey)
		{
			return lastCosts != null;
		}
		int[] c = costs.get(k);
		if (c == null && create)
		{
			c = new int[REGION * REGION];
			java.util.Arrays.fill(c, UNSEEN);
			costs.put(k, c);
			byte[] s = new byte[REGION * REGION];
			java.util.Arrays.fill(s, NO_STEP);
			steps.put(k, s);
		}
		lastKey = k;
		lastCosts = c;
		lastSteps = c == null ? null : steps.get(k);
		return c != null;
	}

	/** What reaching this tile cost, in hundredths of a second, or {@link #UNSEEN}. */
	public int cost(int p)
	{
		if (p == Pt.NONE || !load(p, false))
		{
			return UNSEEN;
		}
		return lastCosts[slot(p)];
	}

	boolean improve(int p, int cost, byte step)
	{
		load(p, true);
		int s = slot(p);
		if (cost >= lastCosts[s])
		{
			return false;
		}
		lastCosts[s] = cost;
		lastSteps[s] = step;
		return true;
	}

	byte step(int p)
	{
		if (!load(p, false))
		{
			return NO_STEP;
		}
		return lastSteps[slot(p)];
	}

	/** Records that this tile was reached by a trip, boarded at {@code from}. */
	void via(int p, int transport, int from)
	{
		trips.put(p, ((long) transport << 32) | (from & 0xFFFFFFFFL));
	}

	/** The trip that reached this tile, or -1. */
	int via(int p)
	{
		Long t = trips.get(p);
		return t == null ? -1 : (int) (t >>> 32);
	}

	/** Where the trip that reached this tile was boarded. */
	int boardedAt(int p)
	{
		Long t = trips.get(p);
		return t == null ? Pt.NONE : (int) (long) t;
	}

	/** Tiles looked at, for tests and field notes. */
	public int getExpanded()
	{
		return expanded;
	}

	/** Blocks of the map this search made, for tests. */
	public int blocks()
	{
		return costs.size();
	}
}
