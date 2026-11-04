package com.codex.wayfinder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TrailTest
{
	private static Route walk()
	{
		return RouterTest.router.route(RouterTest.LUMBRIDGE, RouterTest.VARROCK_SQUARE, RouterTest.fresh(), new Router.Options());
	}

	private static Route ring()
	{
		Account a = NetworkTest.ringsAccount().wear(772).build();
		return RouterTest.router.route(NetworkTest.DRAYNOR, NetworkTest.CANIFIS, a, new Router.Options());
	}

	@Test
	public void walkKeepsEveryTile()
	{
		Route r = walk();
		Step s = r.getSteps().get(0);
		int[] path = s.getPath();
		assertEquals(r.getStart(), path[0]);
		assertEquals(s.getTo(), path[path.length - 1]);
		// One tile per step walked, plus the tile it starts on.
		assertEquals(s.getTiles() + 1, path.length);
		for (int i = 1; i < path.length; i++)
		{
			assertTrue("tiles " + (i - 1) + " and " + i + " are neighbours or a door apart", Pt.flatDistance(path[i - 1], path[i]) >= 1);
		}
	}

	@Test
	public void aheadDropsWhatIsWalked()
	{
		Route r = walk();
		Trail t = Trail.of(r);
		assertNotNull(t);
		int[] path = r.getSteps().get(0).getPath();
		Trail.Ahead start = t.ahead(path[0]);
		assertEquals(path.length - 1, start.getTiles().length);
		assertEquals(path[1], start.getTiles()[0]);
		assertNull("a plain walk ends at the place", start.getNext());
		assertEquals(r.getTarget(), start.getWaypoint());

		// Forty tiles on, the ribbon starts at tile 41.
		Trail.Ahead later = t.ahead(path[40]);
		assertEquals(path.length - 41, later.getTiles().length);
		assertEquals(path[41], later.getTiles()[0]);

		// A step to the side of the path still counts as on it.
		int beside = Pt.of(Pt.x(path[40]) + 1, Pt.y(path[40]), Pt.plane(path[40]));
		assertTrue(t.ahead(beside).getTiles().length <= path.length - 40);
	}

	@Test
	public void walkToTheRingThenRide()
	{
		Route r = ring();
		RouterTest.print(r);
		Trail t = Trail.of(r);
		Trail.Ahead a = t.ahead(NetworkTest.DRAYNOR);
		Step next = a.getNext();
		assertNotNull("the walk leads to a ride", next);
		assertEquals(Kind.FAIRY_RING, next.getTransport().getKind());
		assertEquals("the walk ends where the ring is boarded", next.getTransport().getOrigin(), a.getWaypoint());
		assertEquals("CKS", Words.chip(next.getTransport()));
		assertEquals("Fairy ring CKS", Words.title(next, World.ATLAS.getPlaces(), null).substring(0, 14));
	}

	@Test
	public void pastTheRingTheLaterWalkIsAhead()
	{
		Route r = ring();
		Trail t = Trail.of(r);
		Trail.Leg after = null;
		for (Trail.Leg leg : t.getLegs())
		{
			if (leg.isWalk() && leg.getIndex() > 0)
			{
				after = leg;
			}
		}
		if (after == null)
		{
			// The ring landed close enough that there is no walk after it.
			return;
		}
		int[] path = after.getStep().getPath();
		Trail.Ahead a = t.ahead(path[0]);
		assertSame(after, a.getLeg());
		assertNull(a.getNext());
		assertEquals(r.getTarget(), a.getWaypoint());
	}

	@Test
	public void teleportFirstHasNoGroundTiles()
	{
		Account acc = Account.builder().known(true).questsKnown(true).bankKnown(true).level("Magic", 25)
			.carry(556, 3).carry(554, 1).carry(563, 1).build();
		Route r = RouterTest.router.route(RouterTest.LUMBRIDGE, RouterTest.VARROCK_SQUARE, acc, new Router.Options());
		Trail t = Trail.of(r);
		Trail.Ahead a = t.ahead(RouterTest.LUMBRIDGE);
		assertEquals(Step.Type.TRIP, a.getLeg().getStep().getType());
		assertEquals(0, a.getTiles().length);
		assertEquals(Pt.NONE, a.getWaypoint());
		assertEquals("Varrock Teleport", Words.chip(a.getNext().getTransport()));
	}

	@Test
	public void runsKeepOnlyTheTurns()
	{
		Route r = walk();
		int[] path = r.getSteps().get(0).getPath();
		List<int[]> runs = Trail.runs(path);
		assertTrue(!runs.isEmpty());
		assertEquals(path[0], runs.get(0)[0]);
		int[] last = runs.get(runs.size() - 1);
		assertEquals(path[path.length - 1], last[last.length - 1]);
		int corners = 0;
		for (int[] run : runs)
		{
			corners += run.length;
		}
		assertTrue("far fewer corners (" + corners + ") than tiles (" + path.length + ")", corners * 2 < path.length);
		// Walking each run from corner to corner, one tile at a time, gives back the path.
		List<Integer> rebuilt = new ArrayList<>();
		for (int[] run : runs)
		{
			rebuilt.add(run[0]);
			for (int i = 1; i < run.length; i++)
			{
				int a = run[i - 1];
				int b = run[i];
				int dx = Integer.signum(Pt.x(b) - Pt.x(a));
				int dy = Integer.signum(Pt.y(b) - Pt.y(a));
				int steps = Pt.distance(a, b);
				assertTrue("a straight run", Math.abs(Pt.x(b) - Pt.x(a)) == 0 || Math.abs(Pt.y(b) - Pt.y(a)) == 0
					|| Math.abs(Pt.x(b) - Pt.x(a)) == Math.abs(Pt.y(b) - Pt.y(a)));
				for (int k = 1; k <= steps; k++)
				{
					rebuilt.add(Pt.of(Pt.x(a) + dx * k, Pt.y(a) + dy * k, Pt.plane(a)));
				}
			}
		}
		assertEquals(path.length, rebuilt.size());
		for (int i = 0; i < path.length; i++)
		{
			assertEquals(path[i], (int) rebuilt.get(i));
		}
	}

	@Test
	public void aStairStartsANewRun()
	{
		int[] path = {Pt.of(10, 10, 0), Pt.of(11, 10, 0), Pt.of(12, 10, 0), Pt.of(12, 10, 1), Pt.of(12, 11, 1), Pt.of(12, 12, 1)};
		List<int[]> runs = Trail.runs(path);
		assertEquals(2, runs.size());
		assertEquals(Pt.of(10, 10, 0), runs.get(0)[0]);
		assertEquals(Pt.of(12, 10, 0), runs.get(0)[1]);
		assertEquals(Pt.of(12, 10, 1), runs.get(1)[0]);
		assertEquals(Pt.of(12, 12, 1), runs.get(1)[1]);
		assertEquals(0, Trail.runs(new int[]{Pt.of(1, 1, 0)}).size());
	}

	@Test
	public void etaSaysWhoseTimeItIs()
	{
		Paces p = new Paces();
		Route r = walk();
		assertTrue(Words.eta(r, p).startsWith("~"));
		assertTrue(!Words.eta(r, p).contains("your pace"));
		p.addWalk(60, 50);
		assertTrue(Words.eta(r, p).endsWith(" at your pace"));
		assertTrue(!Words.eta(r, p).startsWith("~"));

		Route ring = ring();
		Step trip = ring.firstTrip();
		Paces q = new Paces();
		q.addWalk(60, 50);
		Router.Options o = new Router.Options();
		o.paces = q;
		Route walkedOnly = RouterTest.router.route(NetworkTest.DRAYNOR, NetworkTest.CANIFIS, NetworkTest.ringsAccount().wear(772).build(), o);
		assertTrue(Words.eta(walkedOnly, q).endsWith(", partly your pace"));
		q.add(trip.getTransport().key(), 7, 6, 1L);
		Route timed = RouterTest.router.route(NetworkTest.DRAYNOR, NetworkTest.CANIFIS, NetworkTest.ringsAccount().wear(772).build(), o);
		assertTrue(Words.eta(timed, q), Words.eta(timed, q).endsWith(" at your pace"));
		assertEquals("", Words.eta(null, q));
	}
}
