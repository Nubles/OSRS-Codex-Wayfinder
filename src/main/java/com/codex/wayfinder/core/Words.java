package com.codex.wayfinder.core;

import java.util.List;

/** Steps and trips put into words for the panel and the plaque. */
public final class Words
{
	private Words()
	{
	}

	/** The nearest named place to a tile, within a short walk, or null. */
	public static Place near(List<Place> places, int tile, int within)
	{
		Place best = null;
		int bestD = Integer.MAX_VALUE;
		for (Place p : places)
		{
			if (Pt.plane(p.getTile()) != Pt.plane(tile))
			{
				continue;
			}
			int d = Pt.distance(p.getTile(), tile);
			// A named landing beats a bank of the same spot, so the town reads before its booth.
			int score = d * 2 + (p.getGroup() == Place.Group.LANDING ? 0 : 3);
			if (d <= within && score < bestD)
			{
				bestD = score;
				best = p;
			}
		}
		return best;
	}

	/** Where a trip lands, in words: the table's name for it, or the nearest named place. */
	public static String landing(Transport t, List<Place> places)
	{
		String place = t.place().replaceFirst("^[0-9A-Z]: ", "").trim();
		if (!place.isEmpty() && t.getKind() != Kind.FAIRY_RING)
		{
			return place;
		}
		if (t.getKind() == Kind.FAIRY_RING)
		{
			for (Place p : places)
			{
				if (p.getGroup() == Place.Group.FAIRY_RING && p.getTile() == t.getDestination())
				{
					return p.getName();
				}
			}
		}
		Place p = near(places, t.getDestination(), 60);
		return p == null ? null : p.getName();
	}

	/**
	 * Where the walk at step i is heading: the destination for the last walk, and the next ride
	 * for one before it ("the fairy ring"), so no line reads just "Walk 48 tiles".
	 */
	public static String goal(List<Step> steps, int i, String destination)
	{
		if (i >= steps.size() - 1)
		{
			return destination;
		}
		Step next = steps.get(i + 1);
		if (next.getType() == Step.Type.TRIP)
		{
			return "the " + next.getTransport().getKind().getNoun().toLowerCase(java.util.Locale.ROOT);
		}
		return null;
	}

	/** The thing to do, such as "Cast Camelot Teleport" or "Fairy ring CKS to Canifis". */
	public static String title(Step s, List<Place> places, String destination)
	{
		if (s.getType() == Step.Type.WALK)
		{
			return destination != null ? "Walk to " + destination : "Walk " + s.getTiles() + (s.getTiles() == 1 ? " tile" : " tiles");
		}
		Transport t = s.getTransport();
		String to = landing(t, places);
		switch (t.getKind())
		{
			case SPELL:
				return "Cast " + t.source();
			case HOME:
				return "Cast " + t.getLabel();
			case ITEM:
			case WHISTLE:
				return to == null ? "Teleport with " + t.source() : t.source() + " to " + to;
			case FAIRY_RING:
				return "Fairy ring " + t.getLabel().replace(" ", "") + (to == null ? "" : " to " + to);
			case MINIGAME:
				return "Grouping: " + (to == null ? t.getLabel() : to);
			default:
				return t.getKind().getNoun() + (to == null ? "" : " to " + to);
		}
	}

	/** The small line under a step: what kind it is, how long, and what it takes. */
	public static String detail(Step s, Items items)
	{
		String time = (s.isLearned() ? "" : "~") + Format.duration(s.getSeconds());
		if (s.getType() == Step.Type.WALK)
		{
			return s.getTiles() + (s.getTiles() == 1 ? " tile, " : " tiles, ") + time;
		}
		Transport t = s.getTransport();
		StringBuilder sb = new StringBuilder();
		sb.append(t.getKind().getNoun()).append(", ").append(time);
		if (s.isLearned())
		{
			sb.append(" (your time)");
		}
		int fare = t.getNeed().fare();
		if (fare > 0)
		{
			sb.append(", ").append(Format.number(fare)).append(" coins");
		}
		else if (t.getKind() == Kind.SPELL)
		{
			String runes = t.getNeed().itemWords(items);
			if (!runes.isEmpty())
			{
				sb.append(", ").append(runes);
			}
		}
		if (s.isFromBank())
		{
			sb.append(". From your bank");
		}
		return sb.toString();
	}

	/** Where to board, for a trip that does not start where you stand: "at the ring by Zanaris". */
	public static String boarding(Step s, List<Place> places)
	{
		if (s.getType() != Step.Type.TRIP || s.getTransport().getOrigin() == Pt.NONE)
		{
			return null;
		}
		String action = s.getTransport().getAction();
		return action.isEmpty() ? null : action;
	}

	/**
	 * The time a route takes, and how much of it comes from this player's own trips: "1m 40s at
	 * your pace" when every trip is timed and the walking pace is learned, "~1m 40s, partly your
	 * pace" when some of it is, and "~1m 40s" for a plain estimate.
	 */
	public static String eta(Route r, Paces paces)
	{
		if (r == null || !r.isFound())
		{
			return "";
		}
		int timed = 0;
		int parts = 0;
		boolean walkLearned = paces != null && paces.walkSamples > 0;
		for (Step s : r.getSteps())
		{
			parts++;
			if (s.getType() == Step.Type.WALK ? walkLearned : s.isLearned())
			{
				timed++;
			}
		}
		String time = Format.duration(r.getSeconds());
		if (parts > 0 && timed == parts)
		{
			return time + " at your pace";
		}
		return "~" + time + (timed > 0 ? ", partly your pace" : "");
	}

	/** The short call-out for a trip on the map: a fairy ring's code, a teleport's source, or the kind of ride. */
	public static String chip(Transport t)
	{
		if (t == null)
		{
			return "";
		}
		switch (t.getKind())
		{
			case FAIRY_RING:
				return t.getLabel().replace(" ", "");
			case ITEM:
			case WHISTLE:
			case SPELL:
			case HOME:
			case MINIGAME:
				return t.source();
			default:
				return t.getKind().getNoun();
		}
	}
}
