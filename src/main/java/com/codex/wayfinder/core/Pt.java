package com.codex.wayfinder.core;

/**
 * A world tile packed into one int: 15 bits of x, 15 of y and 2 of plane, so routes can be
 * searched without allocating a point per tile.
 */
public final class Pt
{
	/** No tile: a teleport has no origin, an unknown position has no tile. */
	public static final int NONE = -1;

	private Pt()
	{
	}

	public static int of(int x, int y, int plane)
	{
		return (x & 0x7FFF) | ((y & 0x7FFF) << 15) | ((plane & 3) << 30);
	}

	public static int x(int p)
	{
		return p & 0x7FFF;
	}

	public static int y(int p)
	{
		return (p >>> 15) & 0x7FFF;
	}

	public static int plane(int p)
	{
		return p >>> 30;
	}

	/** Tiles between two points the way the game counts them: a diagonal step is one tile. */
	public static int distance(int a, int b)
	{
		if (a == NONE || b == NONE)
		{
			return Integer.MAX_VALUE;
		}
		if (plane(a) != plane(b))
		{
			return Integer.MAX_VALUE;
		}
		return Math.max(Math.abs(x(a) - x(b)), Math.abs(y(a) - y(b)));
	}

	/** Like {@link #distance} but ignoring the plane, for matching a landing after stairs. */
	public static int flatDistance(int a, int b)
	{
		if (a == NONE || b == NONE)
		{
			return Integer.MAX_VALUE;
		}
		return Math.max(Math.abs(x(a) - x(b)), Math.abs(y(a) - y(b)));
	}

	/** Parses "x y plane" as written in the travel tables, or NONE for an empty cell. */
	public static int parse(String text)
	{
		if (text == null)
		{
			return NONE;
		}
		String t = text.trim();
		if (t.isEmpty())
		{
			return NONE;
		}
		String[] parts = t.split("\\s+");
		if (parts.length != 3)
		{
			return NONE;
		}
		try
		{
			return of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		}
		catch (NumberFormatException e)
		{
			return NONE;
		}
	}

	public static String toString(int p)
	{
		return p == NONE ? "-" : x(p) + "," + y(p) + "," + plane(p);
	}

	/** Safe pockets inside the surface Wilderness box: Ferox Enclave and the strip by the ditch. x, y, width, height. */
	private static final int[][] SAFE = {
		{3123, 3622, 2, 10}, {3125, 3617, 16, 23}, {3138, 3636, 18, 10}, {3141, 3625, 14, 11}, {3141, 3619, 7, 6},
		{2997, 3525, 34, 9}, {3005, 3534, 21, 10}, {3000, 3534, 5, 5}, {3031, 3525, 2, 2},
	};

	/**
	 * The Wilderness level at a tile, or 0 outside it. The areas are Shortest Path's
	 * (WildernessChecker): the surface from (2944, 3525), 448 tiles square, less Ferox Enclave;
	 * the dungeons from (2944, 9918), 518 by 458. Levels run eight tiles each, so level 21 starts
	 * at y 3680 and level 31 at y 3760 above ground (10075 and 10155 below), the lines Shortest
	 * Path uses for the level 20 and 30 teleport limits.
	 */
	public static int wildernessLevel(int p)
	{
		if (p == NONE)
		{
			return 0;
		}
		int x = x(p);
		int y = y(p);
		if (x >= 2944 && x < 2944 + 448 && y >= 3525 && y < 3525 + 448)
		{
			for (int[] r : SAFE)
			{
				if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3])
				{
					return 0;
				}
			}
			return (y - 3520) / 8 + 1;
		}
		if (x >= 2944 && x < 2944 + 518 && y >= 9918 && y < 9918 + 458)
		{
			return Math.max(1, (y - 9915) / 8 + 1);
		}
		return 0;
	}

	/** The player-owned house instance, which this atlas leaves out: its rooms are per player. */
	public static boolean inHouse(int p)
	{
		if (p == NONE)
		{
			return false;
		}
		int x = x(p);
		int y = y(p);
		return x >= 1856 && x <= 2047 && y >= 7040 && y <= 7111;
	}
}
