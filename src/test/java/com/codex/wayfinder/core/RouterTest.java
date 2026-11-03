package com.codex.wayfinder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RouterTest
{
	static final Atlas atlas = World.ATLAS;
	static final Router router = World.ROUTER;

	static final int LUMBRIDGE = Pt.of(3222, 3218, 0);
	static final int VARROCK_SQUARE = Pt.of(3213, 3424, 0);

	static Account fresh()
	{
		return Account.builder().known(true).questsKnown(true).bankKnown(true).build();
	}

	static void print(Route r)
	{
		System.out.println("route " + Format.duration(r.getSeconds()) + " expanded " + r.getExpanded());
		for (Step s : r.getSteps())
		{
			System.out.println("  " + s.getType() + " " + (s.getTransport() == null ? s.getTiles() + " tiles" : s.getTransport().key()) + " " + s.getSeconds() + "s");
		}
	}

	@Test
	public void walksLumbridgeToVarrock()
	{
		long t = System.currentTimeMillis();
		Route r = router.route(LUMBRIDGE, VARROCK_SQUARE, fresh(), new Router.Options());
		System.out.println("walk search " + (System.currentTimeMillis() - t) + "ms");
		print(r);
		assertTrue(r.isFound());
		assertEquals(0, r.trips());
		// About 210 tiles north as the crow flies; the road bends, so a little more.
		assertTrue(r.getSteps().get(0).getTiles() >= 206);
		assertTrue(r.getSteps().get(0).getTiles() < 300);
	}

	@Test
	public void teleportsWithRunes()
	{
		Account a = Account.builder().known(true).questsKnown(true).bankKnown(true).level("Magic", 25)
			.carry(556, 3).carry(554, 1).carry(563, 1).build();
		Route r = router.route(LUMBRIDGE, VARROCK_SQUARE, a, new Router.Options());
		print(r);
		Step first = r.firstTrip();
		assertNotNull(first);
		assertEquals("Varrock Teleport", first.getTransport().getLabel());
		assertTrue(r.getSeconds() < 30);
	}

	@Test
	public void noTeleportWithoutLevel()
	{
		Account a = Account.builder().known(true).questsKnown(true).bankKnown(true).level("Magic", 24)
			.carry(556, 3).carry(554, 1).carry(563, 1).build();
		Route r = router.route(LUMBRIDGE, VARROCK_SQUARE, a, new Router.Options());
		assertEquals(0, r.trips());
	}

	@Test
	public void wrongSpellbookHasNoVarrockTeleport()
	{
		Account a = Account.builder().known(true).questsKnown(true).bankKnown(true).level("Magic", 99)
			.varbit(4070, 1).carry(556, 3).carry(554, 1).carry(563, 1).build();
		Route r = router.route(LUMBRIDGE, VARROCK_SQUARE, a, new Router.Options());
		for (Step s : r.getSteps())
		{
			assertFalse(s.getTransport() != null && "Varrock Teleport".equals(s.getTransport().getLabel()));
		}
	}
}
