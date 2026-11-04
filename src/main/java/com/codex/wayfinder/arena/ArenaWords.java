package com.codex.wayfinder.arena;

import com.codex.wayfinder.core.Format;
import com.codex.wayfinder.core.Place;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Step;
import com.codex.wayfinder.core.Words;
import java.util.List;

/** The hunt put into words for the panel and the world map. */
public final class ArenaWords
{
	private ArenaWords()
	{
	}

	/** "just now", "14 min ago", "3 h ago", "2 days ago". */
	public static String ago(int seconds)
	{
		if (seconds < 60)
		{
			return "just now";
		}
		if (seconds < 3600)
		{
			return (seconds / 60) + " min ago";
		}
		if (seconds < 2 * 86400)
		{
			return (seconds / 3600) + " h ago";
		}
		return (seconds / 86400) + " days ago";
	}

	/** The short state of a follower, for the line under its name. */
	public static String state(ArenaHunt.Line l)
	{
		if (l.proof != null)
		{
			switch (l.proof)
			{
				case HANDED:
					return l.boss.getDrop() + " is with Kolodion";
				case CARRIED:
					return "Slain. " + l.boss.getDrop() + " in your pack: take it to Kolodion";
				default:
					return "Slain. Pick up the " + l.boss.getDrop().toLowerCase() + " and take it to Kolodion";
			}
		}
		if (!l.isSeen())
		{
			return "Not seen yet. Use the enchanted symbol: the followers rise around Lava Dragon Isle";
		}
		return "Seen here " + ago(l.ago);
	}

	/** How long the spot has stood in Wilderness time, or why it is probably stale; null when there is nothing to say. */
	public static String age(ArenaHunt.Line l)
	{
		if (l.proof != null || !l.isSeen())
		{
			return null;
		}
		if (l.moved)
		{
			return "Likely moved: spots turn over every ~45 min of Wilderness time. Check with the symbol";
		}
		int left = ArenaHunt.ROTATION_SECONDS - l.wildSeconds;
		return "About " + Math.max(1, left / 60) + " min of Wilderness time before it moves";
	}

	/** The rides of a route in order, "Burning amulet to Chaos Temple, then Obelisk to Level 35 Wilderness", or null when it is all on foot. */
	public static String via(Route r, List<Place> places)
	{
		if (r == null || !r.isFound())
		{
			return null;
		}
		StringBuilder b = new StringBuilder();
		for (Step s : r.getSteps())
		{
			if (s.getType() == Step.Type.TRIP)
			{
				b.append(b.length() == 0 ? "" : ", then ").append(Words.title(s, places, null));
			}
		}
		return b.length() == 0 ? null : b.toString();
	}

	/** The quickest way there in one line, or null when not planned. */
	public static String way(ArenaHunt.Line l)
	{
		if (l.routeSeconds < 0 || l.proof != null || !l.isSeen())
		{
			return null;
		}
		String t = l.routeEta != null ? l.routeEta : "~" + Format.duration(l.routeSeconds);
		return (l.routeVia == null ? "On foot" : l.routeVia) + ": " + t;
	}

	/** The world map tooltip. */
	public static String tooltip(ArenaHunt.Line l)
	{
		StringBuilder b = new StringBuilder(l.boss.getTitle()).append(" (").append(l.boss.getGod()).append(")");
		if (l.proof != null)
		{
			b.append(": ").append(state(l)).append(". Seen here ").append(ago(l.ago));
			return b.toString();
		}
		b.append(": seen here ").append(ago(l.ago));
		if (l.proof == null && l.moved)
		{
			b.append(", likely moved since");
		}
		if (l.routeSeconds >= 0)
		{
			b.append(", ").append(l.routeEta != null ? l.routeEta : Format.duration(l.routeSeconds)).append(" away");
		}
		return b.toString();
	}
}
