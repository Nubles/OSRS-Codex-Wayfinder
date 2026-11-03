package com.codex.wayfinder.core;

import java.util.Locale;

/** Numbers and times written the way the panel shows them. */
public final class Format
{
	private Format()
	{
	}

	public static String number(long n)
	{
		return String.format(Locale.ROOT, "%,d", n);
	}

	/** "45s", "2m 05s", "1h 12m". */
	public static String duration(int seconds)
	{
		if (seconds < 0)
		{
			return "-";
		}
		if (seconds < 60)
		{
			return seconds + "s";
		}
		if (seconds < 3600)
		{
			return String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
		}
		return String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, (seconds % 3600) / 60);
	}

	/** "about 2 min", for a rough figure. */
	public static String about(int seconds)
	{
		if (seconds < 60)
		{
			return seconds + "s";
		}
		int m = Math.round(seconds / 60f);
		return m + " min";
	}
}
