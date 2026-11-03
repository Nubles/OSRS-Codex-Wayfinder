package com.codex.wayfinder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class NetworkTest
{
	static final int DRAYNOR = Pt.of(3093, 3244, 0);
	static final int CANIFIS = Pt.of(3493, 3480, 0);

	static Account.Builder ringsAccount()
	{
		return Account.builder().known(true).questsKnown(true).bankKnown(true)
			.quest("Priest in Peril").quest("Nature Spirit")
			.varbit(VarbitID.FAIRY2_QUEENCURE_QUEST, 40);
	}

	@Test
	public void fairyRingWithStaff()
	{
		Account a = ringsAccount().wear(772).build();
		Route r = RouterTest.router.route(DRAYNOR, CANIFIS, a, new Router.Options());
		RouterTest.print(r);
		Step trip = r.firstTrip();
		assertNotNull(trip);
		assertEquals(Kind.FAIRY_RING, trip.getTransport().getKind());
		assertEquals("C K S", trip.getTransport().getLabel());
	}

	@Test
	public void noFairyRingWithoutStaff()
	{
		Account a = ringsAccount().build();
		Route r = RouterTest.router.route(DRAYNOR, CANIFIS, a, new Router.Options());
		for (Step s : r.getSteps())
		{
			assertFalse(s.getTransport() != null && s.getTransport().getKind() == Kind.FAIRY_RING);
		}
	}

	@Test
	public void eliteDiaryNeedsNoStaff()
	{
		Account a = ringsAccount().varbit(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, 1).build();
		Route r = RouterTest.router.route(DRAYNOR, CANIFIS, a, new Router.Options());
		assertEquals(Kind.FAIRY_RING, r.firstTrip().getTransport().getKind());
	}

	@Test
	public void bankedStaffIsMarked()
	{
		Account a = ringsAccount().bank(772, 1).build();
		Router.Options o = new Router.Options();
		o.reach = Need.Reach.OWNED;
		Route r = RouterTest.router.route(DRAYNOR, CANIFIS, a, o);
		assertTrue(r.firstTrip().isFromBank());
		assertTrue(r.needsBank());
	}

	@Test
	public void advisorRanksFairyRingsForCanifisTrips()
	{
		Account a = Account.builder().known(true).questsKnown(true).bankKnown(true)
			.quest("Priest in Peril").quest("Nature Spirit").wear(772).build();
		Advisor adv = new Advisor(RouterTest.router);
		List<Trip> trips = Arrays.asList(new Trip(DRAYNOR, CANIFIS, "Canifis", 3, 0));
		long t = System.currentTimeMillis();
		List<Advisor.Unlock> ranked = adv.rank(a, new Router.Options(), trips, 50);
		System.out.println("advisor " + (System.currentTimeMillis() - t) + "ms");
		for (Advisor.Unlock u : ranked)
		{
			System.out.println("  " + u.getTitle() + " saves " + u.getSaved() + "s (" + u.getHow() + ")");
		}
		Advisor.Unlock rings = ranked.stream().filter(u -> u.getTitle().equals("Fairy rings")).findFirst().orElse(null);
		assertNotNull(rings);
		// The ring at the Wizards' Tower to C K S beats the long way round by about a minute a trip.
		assertTrue(rings.getSaved() >= 3 * 60);
		assertEquals("Canifis", rings.getExample());
	}

	@Test
	public void tracksATeleport()
	{
		Tracker tr = new Tracker();
		tr.clicked("Cast", "Varrock Teleport", 1_000);
		Tracker.Lap lap = tr.moved(RouterTest.atlas, RouterTest.LUMBRIDGE, Pt.of(3213, 3424, 0), 5_200);
		assertNotNull(lap);
		assertEquals("Varrock Teleport", lap.getTransport().getLabel());
		assertEquals(4.2, lap.getSeconds(), 0.01);
		// A second jump with no new click is not timed.
		assertEquals(null, tr.moved(RouterTest.atlas, RouterTest.LUMBRIDGE, Pt.of(3213, 3424, 0), 9_000));
	}

	@Test
	public void learnedTimesReplaceEstimates()
	{
		Paces p = new Paces();
		assertTrue(p.add("SPELL:Varrock Teleport", 6.0, 4.4, 1));
		assertFalse(p.add("SPELL:Varrock Teleport", 200.0, 4.4, 2));
		Account a = Account.builder().known(true).questsKnown(true).bankKnown(true).level("Magic", 25)
			.carry(556, 3).carry(554, 1).carry(563, 1).build();
		Router.Options o = new Router.Options();
		o.paces = p;
		Route r = RouterTest.router.route(RouterTest.LUMBRIDGE, RouterTest.VARROCK_SQUARE, a, o);
		assertTrue(r.firstTrip().isLearned());
		assertEquals(6, r.firstTrip().getSeconds());
	}

	@Test
	public void overviewAndShared()
	{
		Account a = ringsAccount().wear(772).level("Magic", 45).carry(556, 30).carry(563, 10).carry(554, 10).carry(555, 10).bank(13393, 1).build();
		Overview ov = Overview.build(RouterTest.atlas, a);
		Overview.Line rings = ov.getNetworks().get(0);
		assertEquals("Fairy rings", rings.getTitle());
		assertEquals(Overview.State.READY, rings.getState());
		boolean xeric = false;
		for (Overview.Line l : ov.getTeleports())
		{
			if (l.getTitle().equals("Xeric's talisman"))
			{
				xeric = true;
				assertEquals(Overview.State.BANKED, l.getState());
			}
		}
		assertTrue(xeric);
		Shared s = Shared.build(RouterTest.atlas, RouterTest.router, a, new Paces(), 1);
		assertTrue(s.networks.get("FAIRY_RING"));
		assertTrue(s.teleports.stream().anyMatch(t -> t.name.equals("Camelot Teleport") && t.carried));
	}
}
