package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plans the quickest way from one tile to another: a search over every walkable tile of the
 * world map, where doors, stairs and shortcuts join the tiles they link and every trip the
 * account may take is one more edge. Teleports only start a route, since one usable from
 * anywhere is best used first. Costs are hundredths of a second.
 */
public final class Router
{
	private static final int[] DX = {0, 0, 1, -1, 1, -1, 1, -1};
	private static final int[] DY = {1, -1, 0, 0, 1, 1, -1, -1};
	/** A search gives up after this many tiles, so a route to nowhere cannot hang the plugin. */
	static final int TILE_BUDGET = 6_000_000;

	private final Atlas atlas;
	private Map<Integer, int[]> byDestination;

	public Router(Atlas atlas)
	{
		this.atlas = atlas;
	}

	public Atlas getAtlas()
	{
		return atlas;
	}

	/** How to plan: what may be used and how fast this player is. */
	public static final class Options
	{
		public Need.Reach reach = Need.Reach.CARRIED;
		public boolean avoidWilderness = true;
		public Paces paces = new Paces();
		public int limitSeconds = 3600;
		public Set<Kind> off = EnumSet.noneOf(Kind.class);

		public Options copy()
		{
			Options o = new Options();
			o.reach = reach;
			o.avoidWilderness = avoidWilderness;
			o.paces = paces;
			o.limitSeconds = limitSeconds;
			o.off = off.isEmpty() ? EnumSet.noneOf(Kind.class) : EnumSet.copyOf(off);
			return o;
		}
	}

	/** Which trips this account may take, by index, with the given reach and the kinds left on. */
	public boolean[] usable(Account a, Options o)
	{
		List<Transport> all = atlas.getTransports();
		boolean[] out = new boolean[all.size()];
		for (Transport t : all)
		{
			out[t.getIndex()] = !o.off.contains(t.getKind()) && atlas.usable(t, a, o.reach);
		}
		return out;
	}

	/** Seconds this trip takes this player: their own time when known, else the table's ticks and a little to start it. */
	public int seconds(Transport t, Paces paces)
	{
		int learned = paces == null ? 0 : paces.seconds(t.key());
		if (learned > 0)
		{
			return learned;
		}
		return (int) Math.round(estimate(t));
	}

	static double estimate(Transport t)
	{
		if (t.getKind().isWalking())
		{
			return Math.max(1, t.getTicks()) * 0.6;
		}
		return t.getTicks() * 0.6 + t.getKind().getOverhead();
	}

	int cost(Transport t, Paces paces)
	{
		int learned = paces == null ? 0 : paces.seconds(t.key());
		if (learned > 0)
		{
			return learned * 100;
		}
		return (int) Math.round(estimate(t) * 100);
	}

	public Route route(int start, int target, Account a, Options o)
	{
		boolean[] usable = usable(a, o);
		boolean[] carried = usable;
		if (o.reach == Need.Reach.OWNED)
		{
			Options c = o.copy();
			c.reach = Need.Reach.CARRIED;
			carried = usable(a, c);
		}
		return route(start, target, usable, carried, o);
	}

	/** Plans with precomputed trip lists: usable now, and of those, the ones that need no bank visit. */
	public Route route(int start, int target, boolean[] usable, boolean[] carried, Options o)
	{
		CollisionMap map = atlas.getMap();
		if (map == null || start == Pt.NONE || target == Pt.NONE)
		{
			return Route.none(start, target, 0);
		}
		Set<Integer> goal = goalTiles(target);
		boolean avoid = o.avoidWilderness && Pt.wildernessLevel(start) == 0 && Pt.wildernessLevel(target) == 0;
		Search s = new Search();
		int found = run(s, Collections.singletonList(start), true, usable, o, avoid, start, goal, o.limitSeconds * 100);
		if (found == Pt.NONE)
		{
			return Route.none(start, target, s.expanded);
		}
		return build(s, start, found, target, carried, o);
	}

	/** One search from every source at once, until a goal tile is reached or the limit is passed. */
	Search search(List<Integer> sources, boolean forward, boolean[] usable, Options o, boolean avoid, int teleportFrom, int limit)
	{
		Search s = new Search();
		run(s, sources, forward, usable, o, avoid, teleportFrom, null, limit);
		return s;
	}

	private int run(Search s, List<Integer> sources, boolean forward, boolean[] usable, Options o, boolean avoid,
		int teleportFrom, Set<Integer> goal, int limit)
	{
		CollisionMap map = atlas.getMap();
		Heap heap = new Heap();
		int tile = o.paces == null ? new Paces().tileCost() : o.paces.tileCost();
		for (int src : sources)
		{
			if (s.improve(src, 0, Search.START))
			{
				heap.push(0, src);
			}
		}
		if (forward && teleportFrom != Pt.NONE)
		{
			int wild = Pt.wildernessLevel(teleportFrom);
			for (Transport t : atlas.getTeleports())
			{
				if (!usable[t.getIndex()] || (wild > 0 && wild > t.getMaxWilderness()))
				{
					continue;
				}
				if (avoid && Pt.wildernessLevel(t.getDestination()) > 0)
				{
					continue;
				}
				int c = cost(t, o.paces);
				if (c <= limit && s.improve(t.getDestination(), c, Search.BY_TRIP))
				{
					s.via(t.getDestination(), t.getIndex(), teleportFrom);
					heap.push(c, t.getDestination());
				}
			}
		}
		int[] next = new int[8];
		while (!heap.isEmpty())
		{
			long v = heap.pop();
			int cost = Heap.cost(v);
			int p = Heap.tile(v);
			if (cost > s.cost(p))
			{
				continue;
			}
			if (goal != null && goal.contains(p))
			{
				return p;
			}
			if (cost > limit || ++s.expanded > TILE_BUDGET)
			{
				break;
			}
			int x = Pt.x(p);
			int y = Pt.y(p);
			int z = Pt.plane(p);
			int n = map.neighbours(p, next);
			if (n == 0)
			{
				// On a tile the map calls blocked (a ring, a booth, or where the player stands): step off it to any open tile.
				for (int d = 0; d < 8; d++)
				{
					if (map.walkable(x + DX[d], y + DY[d], z))
					{
						next[n++] = Pt.of(x + DX[d], y + DY[d], z);
					}
				}
			}
			for (int i = 0; i < n; i++)
			{
				int q = next[i];
				if (avoid && Pt.wildernessLevel(q) > 0)
				{
					continue;
				}
				int dx = Pt.x(q) - x;
				int dy = Pt.y(q) - y;
				byte dir = direction(forward ? dx : -dx, forward ? dy : -dy);
				int nc = cost + tile;
				if (s.improve(q, nc, dir))
				{
					heap.push(nc, q);
				}
			}
			int[] trips = forward ? atlas.from(p) : to(p);
			for (int idx : trips)
			{
				if (!usable[idx])
				{
					continue;
				}
				Transport t = atlas.get(idx);
				int q = forward ? t.getDestination() : t.getOrigin();
				if (avoid && Pt.wildernessLevel(q) > 0)
				{
					continue;
				}
				int nc = cost + cost(t, o.paces);
				if (s.improve(q, nc, Search.BY_TRIP))
				{
					s.via(q, idx, p);
					heap.push(nc, q);
				}
			}
		}
		return Pt.NONE;
	}

	private int[] to(int tile)
	{
		if (byDestination == null)
		{
			Map<Integer, List<Integer>> m = new HashMap<>();
			for (Transport t : atlas.getTransports())
			{
				if (t.getOrigin() != Pt.NONE)
				{
					for (int tile2 : Atlas.boarding(atlas.getMap(), t.getDestination()))
					{
						m.computeIfAbsent(tile2, k -> new ArrayList<>()).add(t.getIndex());
					}
				}
			}
			Map<Integer, int[]> out = new HashMap<>();
			m.forEach((k, v) -> out.put(k, v.stream().mapToInt(Integer::intValue).toArray()));
			byDestination = out;
		}
		int[] t = byDestination.get(tile);
		return t == null ? new int[0] : t;
	}

	private static byte direction(int dx, int dy)
	{
		for (byte d = 0; d < 8; d++)
		{
			if (DX[d] == dx && DY[d] == dy)
			{
				return d;
			}
		}
		return Search.START;
	}

	/** The tiles that count as arriving: the target, or the open tiles nearest it when it is a booth or a wall. */
	Set<Integer> goalTiles(int target)
	{
		CollisionMap map = atlas.getMap();
		Set<Integer> goal = new HashSet<>();
		int x = Pt.x(target);
		int y = Pt.y(target);
		int z = Pt.plane(target);
		if (map.walkable(x, y, z))
		{
			goal.add(target);
			return goal;
		}
		for (int r = 1; r <= 3 && goal.isEmpty(); r++)
		{
			for (int dx = -r; dx <= r; dx++)
			{
				for (int dy = -r; dy <= r; dy++)
				{
					if (Math.max(Math.abs(dx), Math.abs(dy)) == r && map.walkable(x + dx, y + dy, z))
					{
						goal.add(Pt.of(x + dx, y + dy, z));
					}
				}
			}
		}
		if (goal.isEmpty())
		{
			goal.add(target);
		}
		return goal;
	}

	private Route build(Search s, int start, int end, int target, boolean[] carried, Options o)
	{
		// Walk back from the end to the start, then read the trail forwards.
		List<Integer> tiles = new ArrayList<>();
		List<Integer> vias = new ArrayList<>();
		int p = end;
		int guard = 0;
		while (guard++ < 2_000_000)
		{
			byte step = s.step(p);
			if (step == Search.START)
			{
				tiles.add(p);
				vias.add(-1);
				break;
			}
			if (step == Search.BY_TRIP)
			{
				int idx = s.via(p);
				Transport t = atlas.get(idx);
				tiles.add(p);
				vias.add(idx);
				p = s.boardedAt(p);
				continue;
			}
			if (step < 0 || step > 7)
			{
				break;
			}
			tiles.add(p);
			vias.add(-1);
			p = Pt.of(Pt.x(p) - DX[step], Pt.y(p) - DY[step], Pt.plane(p));
		}
		Collections.reverse(tiles);
		Collections.reverse(vias);

		List<Step> steps = new ArrayList<>();
		int tileCost = o.paces == null ? new Paces().tileCost() : o.paces.tileCost();
		int walkFrom = tiles.get(0);
		int walkStart = 0;
		int walkTiles = 0;
		int walkCost = 0;
		for (int i = 1; i < tiles.size(); i++)
		{
			int idx = vias.get(i);
			Transport t = idx < 0 ? null : atlas.get(idx);
			if (t == null || t.getKind().isWalking())
			{
				walkTiles++;
				walkCost += t == null ? tileCost : cost(t, o.paces);
				continue;
			}
			if (walkTiles > 0)
			{
				steps.add(new Step(Step.Type.WALK, walkFrom, tiles.get(i - 1), null, Math.round(walkCost / 100f), walkTiles, false, false,
					slice(tiles, walkStart, i)));
			}
			boolean learned = o.paces != null && o.paces.seconds(t.key()) > 0;
			steps.add(new Step(Step.Type.TRIP, tiles.get(i - 1), tiles.get(i), t,
				seconds(t, o.paces), 0, learned, !carried[idx]));
			walkFrom = tiles.get(i);
			walkStart = i;
			walkTiles = 0;
			walkCost = 0;
		}
		// A step or two off a landing is not worth its own line; the arrival check allows for it.
		if (walkTiles > 2 || (walkTiles > 0 && steps.isEmpty()))
		{
			steps.add(new Step(Step.Type.WALK, walkFrom, tiles.get(tiles.size() - 1), null, Math.round(walkCost / 100f), walkTiles, false, false,
				slice(tiles, walkStart, tiles.size())));
		}
		return new Route(start, target, steps, Math.round(s.cost(end) / 100f), true, s.expanded);
	}

	private static int[] slice(List<Integer> tiles, int from, int to)
	{
		int[] out = new int[to - from];
		for (int i = from; i < to; i++)
		{
			out[i - from] = tiles.get(i);
		}
		return out;
	}
}
