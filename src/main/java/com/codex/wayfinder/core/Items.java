package com.codex.wayfinder.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Named groups of item ids the travel tables use, such as AIR_RUNE or ARDOUGNE_CLOAK, with the
 * staves and off-hands that stand in for a rune. From the Shortest Path plugin's
 * item-variations table (BSD 2-Clause).
 */
public final class Items
{
	public static final Items NONE = new Items(Collections.emptyMap(), Collections.emptyMap());

	private final Map<String, int[]> ids;
	private final Map<String, int[]> standIns;

	Items(Map<String, int[]> ids, Map<String, int[]> standIns)
	{
		this.ids = ids;
		this.standIns = standIns;
	}

	/** Parses the table: name, a display id, the ids, the staves, then the off-hands. */
	public static Items parse(String tsv)
	{
		Map<String, int[]> ids = new HashMap<>();
		Map<String, int[]> standIns = new HashMap<>();
		for (String line : tsv.split("\r?\n"))
		{
			if (line.isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			String[] c = line.split("\t", -1);
			if (c.length < 2)
			{
				continue;
			}
			ids.put(c[0], numbers(c[1]));
			int[] staves = c.length > 2 ? numbers(c[2]) : new int[0];
			int[] offhands = c.length > 3 ? numbers(c[3]) : new int[0];
			if (staves.length + offhands.length > 0)
			{
				int[] all = new int[staves.length + offhands.length];
				System.arraycopy(staves, 0, all, 0, staves.length);
				System.arraycopy(offhands, 0, all, staves.length, offhands.length);
				standIns.put(c[0], all);
			}
		}
		return new Items(ids, standIns);
	}

	static int[] numbers(String cell)
	{
		if (cell == null || cell.trim().isEmpty())
		{
			return new int[0];
		}
		String[] parts = cell.split(",");
		int[] out = new int[parts.length];
		int n = 0;
		for (String p : parts)
		{
			try
			{
				out[n++] = Integer.parseInt(p.trim());
			}
			catch (NumberFormatException e)
			{
				// A stray label in an id column is skipped.
			}
		}
		return n == out.length ? out : java.util.Arrays.copyOf(out, n);
	}

	/** The ids for a name, or null when the name is not a known group. */
	public int[] ids(String name)
	{
		return ids.get(name);
	}

	/** Worn items that supply this rune without carrying it, or an empty array. */
	public int[] standIns(String name)
	{
		int[] s = standIns.get(name);
		return s == null ? new int[0] : s;
	}
}
