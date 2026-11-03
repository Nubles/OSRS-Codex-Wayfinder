package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which travel unlock would save this player the most time on the trips they really take. For
 * each trip it searches once forward from the start and once backward from the end, so any trip
 * can then be priced with one extra way of travelling in a single sum: reach its boarding spot,
 * take it, then finish from where it lands.
 */
public final class Advisor
{
	/** A whole network, a quest, a level or an item that would open new trips. */
	public static final class Unlock
	{
		private final String key;
		private final String title;
		private final String how;
		private final List<Integer> transports = new ArrayList<>();
		private int saved;
		private int tripsHelped;
		private String example;

		Unlock(String key, String title, String how)
		{
			this.key = key;
			this.title = title;
			this.how = how;
		}

		public String getKey()
		{
			return key;
		}

		/** What to get: "Fairy rings", "Xeric's talisman", "Song of the Elves". */
		public String getTitle()
		{
			return title;
		}

		/** How to get it, in a few words. */
		public String getHow()
		{
			return how;
		}

		/** Seconds saved across the remembered trips, each counted as often as it was made. */
		public int getSaved()
		{
			return saved;
		}

		public int getTripsHelped()
		{
			return tripsHelped;
		}

		/** The trip it helps most, by name. */
		public String getExample()
		{
			return example;
		}

		public int getTrips()
		{
			return transports.size();
		}
	}

	private final Router router;

	public Advisor(Router router)
	{
		this.router = router;
	}

	/** The unlocks this account lacks that would open new trips, grouped the way a player would get them. */
	public List<Unlock> candidates(Account a)
	{
		Atlas atlas = router.getAtlas();
		Map<String, Unlock> out = new LinkedHashMap<>();
		for (Transport t : atlas.getTransports())
		{
			if (t.getKind().isWalking() || atlas.usable(t, a, Need.Reach.OWNED))
			{
				continue;
			}
			Unlock u = classify(atlas, t, a, out);
			if (u != null)
			{
				u.transports.add(t.getIndex());
			}
		}
		return new ArrayList<>(out.values());
	}

	private static Unlock classify(Atlas atlas, Transport t, Account a, Map<String, Unlock> out)
	{
		Kind k = t.getKind();
		if (!Atlas.networkOpen(k, a))
		{
			String key = "network:" + k.name();
			return out.computeIfAbsent(key, x -> new Unlock(key, plural(k), Atlas.networkNeed(k)));
		}
		Need need = t.getNeed();
		List<String> missing = need.missingUnlocks(a, 2);
		if (missing.size() > 1 || need.levelsShort(a) > 10)
		{
			// Too far off to be a next step.
			return null;
		}
		if (missing.size() == 1)
		{
			String m = missing.get(0);
			if ("an unlock".equals(m))
			{
				return null;
			}
			String key = "need:" + m;
			boolean level = m.matches("\\d+ .*");
			return out.computeIfAbsent(key, x -> new Unlock(key, m, level ? "Train to " + m : "Finish the quest"));
		}
		if (atlas.treePatch(t) != null && !a.treeGrown(atlas.treePatch(t)))
		{
			String key = "tree:" + atlas.treePatch(t);
			return out.computeIfAbsent(key, x -> new Unlock(key, "Spirit tree at " + atlas.treePatch(t), "Grow one in the patch there (83 Farming)"));
		}
		if (k == Kind.FAIRY_RING)
		{
			return out.computeIfAbsent("item:staff", x -> new Unlock("item:staff", "Dramen staff", "Carry a dramen or lunar staff for the rings"));
		}
		if ((k == Kind.ITEM || k == Kind.WHISTLE) && need.hasItems() && !need.itemsMet(a, Need.Reach.OWNED, false))
		{
			String source = t.source();
			if (EVENT_ONLY.contains(source) || source.toLowerCase(java.util.Locale.ROOT).endsWith(" cape"))
			{
				// Capes take a 99, every quest or every diary: a goal of its own, not a travel tip.
				return null;
			}
			String key = "item:" + source;
			String how = t.isConsumable() ? "Keep a few in the bank" : "Get one and keep it";
			return out.computeIfAbsent(key, x -> new Unlock(key, source, how));
		}
		return null;
	}

	/**
	 * Items a main account cannot get, so they are never suggested. The trinket of fairies drops
	 * only in Deadman Mode (OSRS Wiki, "Trinket of fairies").
	 */
	static final java.util.Set<String> EVENT_ONLY = java.util.Collections.singleton("Trinket of fairies");

	/** The cheapest cost at a tile or, when it is an object, at the open tiles around it. */
	private static int cheapest(Search s, Atlas atlas, int tile)
	{
		int best = Search.UNSEEN;
		for (int b : Atlas.boarding(atlas.getMap(), tile))
		{
			best = Math.min(best, s.cost(b));
		}
		return best;
	}

	private static String plural(Kind k)
	{
		switch (k)
		{
			case FAIRY_RING:
				return "Fairy rings";
			case SPIRIT_TREE:
				return "Spirit trees";
			case GLIDER:
				return "Gnome gliders";
			case MUSHTREE:
				return "Magic mushtrees";
			default:
				return k.getNoun();
		}
	}

	/**
	 * Prices every candidate against the remembered trips and returns those that save time,
	 * best first. Each trip is searched with what the account owns, so an item in the bank is
	 * not mistaken for something to unlock.
	 */
	public List<Unlock> rank(Account a, Router.Options base, List<Trip> trips, int limit)
	{
		List<Unlock> cands = candidates(a);
		if (cands.isEmpty() || trips.isEmpty())
		{
			return Collections.emptyList();
		}
		Router.Options o = base.copy();
		o.reach = Need.Reach.OWNED;
		boolean[] usable = router.usable(a, o);
		Atlas atlas = router.getAtlas();
		Map<Unlock, Integer> bestSaving = new LinkedHashMap<>();
		for (Trip trip : trips)
		{
			Route now = router.route(trip.from, trip.to, usable, usable, o);
			if (!now.isFound())
			{
				continue;
			}
			int limitCs = now.getSeconds() * 100;
			boolean avoid = o.avoidWilderness && Pt.wildernessLevel(trip.from) == 0 && Pt.wildernessLevel(trip.to) == 0;
			Search forward = router.search(Collections.singletonList(trip.from), true, usable, o, avoid, trip.from, limitCs);
			Search back = router.search(new ArrayList<>(router.goalTiles(trip.to)), false, usable, o, avoid, Pt.NONE, limitCs);
			for (Unlock u : cands)
			{
				int best = Integer.MAX_VALUE;
				for (int idx : u.transports)
				{
					Transport t = atlas.get(idx);
					if (avoid && (Pt.wildernessLevel(t.getDestination()) > 0 || Pt.wildernessLevel(t.getOrigin()) > 0))
					{
						continue;
					}
					int reach = t.getOrigin() == Pt.NONE ? 0 : cheapest(forward, atlas, t.getOrigin());
					int finish = cheapest(back, atlas, t.getDestination());
					if (reach == Search.UNSEEN || finish == Search.UNSEEN)
					{
						continue;
					}
					best = Math.min(best, reach + router.cost(t, o.paces) + finish);
				}
				if (best == Integer.MAX_VALUE)
				{
					continue;
				}
				int saving = now.getSeconds() - Math.round(best / 100f);
				if (saving >= 5)
				{
					int weight = Math.max(1, trip.count);
					u.saved += saving * weight;
					u.tripsHelped++;
					Integer prior = bestSaving.get(u);
					if (prior == null || saving > prior)
					{
						bestSaving.put(u, saving);
						u.example = trip.name;
					}
				}
			}
		}
		List<Unlock> ranked = new ArrayList<>();
		for (Unlock u : cands)
		{
			if (u.saved > 0)
			{
				ranked.add(u);
			}
		}
		ranked.sort(Comparator.comparingInt(Unlock::getSaved).reversed().thenComparing(Unlock::getTitle));
		return ranked.size() > limit ? new ArrayList<>(ranked.subList(0, limit)) : ranked;
	}
}
