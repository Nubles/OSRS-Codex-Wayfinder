package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A planned route laid out for drawing: the tiles of each walk, where the trips start and land,
 * and, for wherever the player stands now, what is left of the walk they are on and where it
 * ends. Built once per plan, so the overlays only look things up while the game draws.
 */
public final class Trail
{
	/** A player this close to a tile of a walk is on that walk. */
	static final int ON_PATH = 2;

	private final Route route;
	private final List<Leg> legs;

	/** One step of the route, ready to draw. */
	public static final class Leg
	{
		private final Step step;
		private final int index;
		private final List<int[]> runs;

		Leg(Step step, int index)
		{
			this.step = step;
			this.index = index;
			this.runs = step.getType() == Step.Type.WALK ? runs(step.getPath()) : Collections.emptyList();
		}

		public Step getStep()
		{
			return step;
		}

		public int getIndex()
		{
			return index;
		}

		public boolean isWalk()
		{
			return step.getType() == Step.Type.WALK;
		}

		/**
		 * The walk as straight runs on one plane: each run is the tiles where the way turns,
		 * from its first tile to its last. A door, a stair or a ladder ends one run and starts
		 * the next. Empty for a trip.
		 */
		public List<int[]> getRuns()
		{
			return runs;
		}
	}

	/** What is left of the leg the player is on, from where they stand. */
	public static final class Ahead
	{
		private final Leg leg;
		private final int[] tiles;
		private final int waypoint;
		private final Step next;

		Ahead(Leg leg, int[] tiles, int waypoint, Step next)
		{
			this.leg = leg;
			this.tiles = tiles;
			this.waypoint = waypoint;
			this.next = next;
		}

		/** The leg the player is on. */
		public Leg getLeg()
		{
			return leg;
		}

		/** The tiles still to walk on this leg, nearest first; empty when the leg is a trip. */
		public int[] getTiles()
		{
			return tiles;
		}

		/** Where this leg ends: the tile to board the next trip, or the place itself; NONE when a teleport is next. */
		public int getWaypoint()
		{
			return waypoint;
		}

		/** The trip that follows this walk, or null when the walk ends at the place. */
		public Step getNext()
		{
			return next;
		}
	}

	private Trail(Route route)
	{
		this.route = route;
		List<Leg> l = new ArrayList<>();
		for (int i = 0; i < route.getSteps().size(); i++)
		{
			l.add(new Leg(route.getSteps().get(i), i));
		}
		this.legs = Collections.unmodifiableList(l);
	}

	/** The trail of a found route, or null when there is nothing to draw. */
	public static Trail of(Route route)
	{
		return route == null || !route.isFound() || route.getSteps().isEmpty() ? null : new Trail(route);
	}

	public Route getRoute()
	{
		return route;
	}

	public List<Leg> getLegs()
	{
		return legs;
	}

	/**
	 * What is left of the leg the player is on. The route is planned from where the player stood
	 * a few ticks ago, so the player may be partway along its first walk, or already past a trip
	 * and on a later walk: the first walk with a tile within {@link #ON_PATH} of the player wins,
	 * and the tiles before the nearest one are dropped. Off every walk, the first step stands.
	 */
	public Ahead ahead(int here)
	{
		for (Leg leg : legs)
		{
			if (!leg.isWalk() || here == Pt.NONE)
			{
				continue;
			}
			int[] path = leg.getStep().getPath();
			int best = -1;
			int bestDistance = Integer.MAX_VALUE;
			for (int i = 0; i < path.length; i++)
			{
				int d = Pt.distance(here, path[i]);
				// The later of two equal tiles: a player on a tile has walked up to it.
				if (d <= bestDistance)
				{
					bestDistance = d;
					best = i;
				}
			}
			if (best >= 0 && bestDistance <= ON_PATH)
			{
				int from = bestDistance == 0 ? best + 1 : best;
				int[] rest = new int[Math.max(0, path.length - from)];
				System.arraycopy(path, from, rest, 0, rest.length);
				return walk(leg, rest);
			}
		}
		Leg first = legs.get(0);
		if (first.isWalk())
		{
			return walk(first, first.getStep().getPath());
		}
		Step trip = first.getStep();
		int board = trip.getTransport() == null ? Pt.NONE : trip.getTransport().getOrigin();
		// A teleport is used where the player stands: nothing to walk to, so no waypoint.
		return new Ahead(first, new int[0], board, trip);
	}

	private Ahead walk(Leg leg, int[] rest)
	{
		int i = leg.getIndex();
		Step next = i + 1 < legs.size() ? legs.get(i + 1).getStep() : null;
		int waypoint;
		if (next != null && next.getTransport() != null && next.getTransport().getOrigin() != Pt.NONE)
		{
			waypoint = next.getTransport().getOrigin();
		}
		else if (next == null)
		{
			waypoint = route.getTarget();
		}
		else
		{
			waypoint = leg.getStep().getTo();
		}
		return new Ahead(leg, rest, waypoint, next);
	}

	/** Splits a walk into straight runs on one plane, keeping only the tiles where it turns. */
	static List<int[]> runs(int[] path)
	{
		List<int[]> out = new ArrayList<>();
		if (path == null || path.length == 0)
		{
			return out;
		}
		List<Integer> run = new ArrayList<>();
		run.add(path[0]);
		boolean heading = false;
		int dx = 0;
		int dy = 0;
		for (int i = 1; i < path.length; i++)
		{
			int a = path[i - 1];
			int b = path[i];
			if (Pt.distance(a, b) != 1)
			{
				// A door, a stair or a ladder: end this run where it is left and start the next where it lands.
				end(out, run, a);
				run = new ArrayList<>();
				run.add(b);
				heading = false;
				continue;
			}
			int ndx = Pt.x(b) - Pt.x(a);
			int ndy = Pt.y(b) - Pt.y(a);
			if (heading && (ndx != dx || ndy != dy))
			{
				run.add(a);
			}
			dx = ndx;
			dy = ndy;
			heading = true;
		}
		end(out, run, path[path.length - 1]);
		return out;
	}

	private static void end(List<int[]> out, List<Integer> run, int last)
	{
		if (run.get(run.size() - 1) != last)
		{
			run.add(last);
		}
		if (run.size() > 1)
		{
			out.add(toArray(run));
		}
	}

	private static int[] toArray(List<Integer> list)
	{
		int[] out = new int[list.size()];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = list.get(i);
		}
		return out;
	}
}
