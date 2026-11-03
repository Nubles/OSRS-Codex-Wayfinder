package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What Wayfinder shares with the other Codex apps, saved under the RuneLite profile as
 * {@code codexwayfinder.atlasV1}: every teleport this account can use with what it owns, the
 * networks it has open, and how fast it walks, so any app can price "how long to get there"
 * without its own copy of the travel tables. Read-only for everyone else.
 */
public final class Shared
{
	public static final String GROUP = "codexwayfinder";
	public static final String KEY = "atlasV1";

	public int version = 1;
	public long savedAt;
	/** Seconds a tile of walking takes this player. */
	public double walkSecondsPerTile;
	public List<Teleport> teleports = new ArrayList<>();
	/** Network kind (FAIRY_RING, SPIRIT_TREE, GLIDER, QUETZAL, MUSHTREE...) to whether it is open. */
	public Map<String, Boolean> networks = new LinkedHashMap<>();
	public int timedTrips;

	public static final class Teleport
	{
		public String name;
		public String source;
		public String kind;
		public int x;
		public int y;
		public int plane;
		public int seconds;
		/** In the pack or worn when this was saved; otherwise in the bank. */
		public boolean carried;
		/** Timed from this player's own trips. */
		public boolean timed;
	}

	public static Shared build(Atlas atlas, Router router, Account a, Paces paces, long now)
	{
		Shared s = new Shared();
		s.savedAt = now;
		s.walkSecondsPerTile = Math.round(Paces.RUN_SECONDS_PER_TILE * paces.getWalkFactor() * 1000) / 1000.0;
		s.timedTrips = paces.timedTrips();
		Set<Integer> seen = new HashSet<>();
		for (Transport t : atlas.getTeleports())
		{
			if (!atlas.usable(t, a, Need.Reach.OWNED) || !seen.add(t.getDestination()))
			{
				continue;
			}
			Teleport tp = new Teleport();
			tp.name = t.getLabel();
			tp.source = t.source();
			tp.kind = t.getKind().name();
			tp.x = Pt.x(t.getDestination());
			tp.y = Pt.y(t.getDestination());
			tp.plane = Pt.plane(t.getDestination());
			tp.seconds = router.seconds(t, paces);
			tp.carried = atlas.usable(t, a, Need.Reach.CARRIED);
			tp.timed = paces.seconds(t.key()) > 0;
			s.teleports.add(tp);
		}
		for (Kind k : new Kind[]{Kind.FAIRY_RING, Kind.SPIRIT_TREE, Kind.GLIDER, Kind.QUETZAL, Kind.MUSHTREE, Kind.CHARTER, Kind.CANOE, Kind.BALLOON, Kind.MINECART})
		{
			boolean open = false;
			for (Transport t : atlas.getTransports())
			{
				if (t.getKind() == k && atlas.usable(t, a, Need.Reach.OWNED))
				{
					open = true;
					break;
				}
			}
			s.networks.put(k.name(), open);
		}
		return s;
	}
}
