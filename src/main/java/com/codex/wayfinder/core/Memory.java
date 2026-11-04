package com.codex.wayfinder.core;

import com.codex.wayfinder.arena.ArenaHunt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything Wayfinder keeps for one account, as JSON in the RuneLite profile: timed trips,
 * the journeys made, recent places and the bank as last seen. Fields are plain so Gson reads
 * older saves; anything missing reads as empty.
 */
public final class Memory
{
	static final int TRIPS = 40;
	static final int RECENT = 6;

	public Paces paces = new Paces();
	public List<Trip> trips = new ArrayList<>();
	public List<Spot> recent = new ArrayList<>();
	public Map<Integer, Integer> bank = new HashMap<>();
	public long bankTime;
	/** The Mage Arena II hunt: where each follower was seen and which are done with. */
	public ArenaHunt arena = new ArenaHunt();

	/** A place as saved: its name, tile and kind. */
	public static final class Spot
	{
		public String name;
		public int x;
		public int y;
		public int plane;
		public String group;
		public String code;

		public Spot()
		{
		}

		Spot(Place p)
		{
			name = p.getName();
			x = Pt.x(p.getTile());
			y = Pt.y(p.getTile());
			plane = Pt.plane(p.getTile());
			group = p.getGroup().name();
			code = p.getCode();
		}

		public Place place()
		{
			Place.Group g;
			try
			{
				g = group == null ? Place.Group.CUSTOM : Place.Group.valueOf(group);
			}
			catch (IllegalArgumentException e)
			{
				g = Place.Group.CUSTOM;
			}
			return new Place(name == null ? "A marked spot" : name, Pt.of(x, y, plane), g, code == null ? "" : code);
		}
	}

	/** Fills anything an older or damaged save left out. */
	public Memory mend()
	{
		if (paces == null)
		{
			paces = new Paces();
		}
		if (paces.laps == null)
		{
			paces.laps = new HashMap<>();
		}
		if (trips == null)
		{
			trips = new ArrayList<>();
		}
		trips.removeIf(t -> t == null);
		if (recent == null)
		{
			recent = new ArrayList<>();
		}
		recent.removeIf(s -> s == null);
		if (bank == null)
		{
			bank = new HashMap<>();
		}
		arena = arena == null ? new ArenaHunt() : arena.mend();
		return this;
	}

	/** Puts a place first in the recent list. */
	public void visit(Place p)
	{
		recent.removeIf(s -> s.name != null && s.name.equals(p.getName()) && Pt.of(s.x, s.y, s.plane) == p.getTile());
		recent.add(0, new Spot(p));
		while (recent.size() > RECENT)
		{
			recent.remove(recent.size() - 1);
		}
	}

	public List<Place> recentPlaces()
	{
		List<Place> out = new ArrayList<>();
		for (Spot s : recent)
		{
			out.add(s.place());
		}
		return out;
	}

	/** Remembers a journey, counting it again when it repeats one already known. */
	public void journey(int from, int to, String name, long now)
	{
		for (Trip t : trips)
		{
			if (t.same(from, to))
			{
				t.count++;
				t.last = now;
				t.name = name;
				return;
			}
		}
		trips.add(new Trip(from, to, name, 1, now));
		if (trips.size() > TRIPS)
		{
			// Forget the trip made least, oldest first.
			Trip worst = null;
			for (Trip t : trips)
			{
				if (worst == null || t.count < worst.count || (t.count == worst.count && t.last < worst.last))
				{
					worst = t;
				}
			}
			trips.remove(worst);
		}
	}

	/** The journeys, most made first. */
	public List<Trip> tripsByUse()
	{
		List<Trip> out = new ArrayList<>(trips);
		out.sort((a, b) -> a.count != b.count ? Integer.compare(b.count, a.count) : Long.compare(b.last, a.last));
		return out;
	}
}
