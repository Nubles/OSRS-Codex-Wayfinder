package com.codex.wayfinder.core;

import java.util.Locale;

/**
 * Times this player's own trips. A trip shows up as the player landing far from where they
 * stood one tick before; the trip that lands there, and leaves from where they were (or from
 * anywhere, for a teleport), is the one taken. Its time runs from the click that started it.
 */
public final class Tracker
{
	/** A jump this far in one tick is a trip, not a step. */
	static final int JUMP = 12;
	/** A landing this close to the table's landing tile counts. */
	static final int LANDING = 4;
	/** A boarding spot this close to where the player stood counts. */
	static final int BOARDING = 8;
	/** A click older than this did not start the trip. */
	static final long CLICK_MS = 30_000;

	/** A timed trip. */
	public static final class Lap
	{
		private final Transport transport;
		private final double seconds;

		Lap(Transport transport, double seconds)
		{
			this.transport = transport;
			this.seconds = seconds;
		}

		public Transport getTransport()
		{
			return transport;
		}

		public double getSeconds()
		{
			return seconds;
		}
	}

	private long clickMs;
	private String clickText = "";

	/** A menu click: the option and its target, as the game wrote them (tags stripped). */
	public void clicked(String option, String target, long ms)
	{
		clickMs = ms;
		clickText = ((option == null ? "" : option) + " " + (target == null ? "" : target)).toLowerCase(Locale.ROOT);
	}

	/** Called each tick with where the player was and is; returns a timed trip when one just ended. */
	public Lap moved(Atlas atlas, int before, int now, long ms)
	{
		if (before == Pt.NONE || now == Pt.NONE)
		{
			return null;
		}
		if (Pt.flatDistance(before, now) <= JUMP)
		{
			return null;
		}
		Transport best = null;
		int bestScore = Integer.MAX_VALUE;
		for (Transport t : atlas.getTransports())
		{
			if (t.getKind().isWalking() || Pt.plane(t.getDestination()) != Pt.plane(now))
			{
				continue;
			}
			int land = Pt.flatDistance(t.getDestination(), now);
			if (land > LANDING)
			{
				continue;
			}
			if (t.getOrigin() != Pt.NONE && Pt.flatDistance(t.getOrigin(), before) > BOARDING)
			{
				continue;
			}
			int score = land * 10 + mentioned(t);
			if (score < bestScore)
			{
				bestScore = score;
				best = t;
			}
		}
		if (best == null)
		{
			return null;
		}
		long age = ms - clickMs;
		if (clickMs == 0 || age < 0 || age > CLICK_MS)
		{
			return null;
		}
		clickMs = 0;
		return new Lap(best, age / 1000.0);
	}

	/** 0 when the click named this trip's item, spell or action; 2 for only its place; 5 for neither. */
	private int mentioned(Transport t)
	{
		String s = t.source().toLowerCase(Locale.ROOT);
		String a = t.getAction().toLowerCase(Locale.ROOT);
		String place = t.place().toLowerCase(Locale.ROOT);
		if ((!s.isEmpty() && clickText.contains(s)) || (!a.isEmpty() && clickText.contains(a)))
		{
			return 0;
		}
		return !place.isEmpty() && clickText.contains(place) ? 2 : 5;
	}
}
