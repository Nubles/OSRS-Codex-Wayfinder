package com.codex.wayfinder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PlaceTest
{
	private static Place find(String name, Place.Group group)
	{
		for (Place p : World.ATLAS.getPlaces())
		{
			if (p.getName().equals(name) && p.getGroup() == group)
			{
				return p;
			}
		}
		return null;
	}

	@Test
	public void gazetteerHasTownsRingsAndDungeons()
	{
		// Fairy rings take the wiki's names and keep their codes for search.
		Place canifis = find("Canifis", Place.Group.FAIRY_RING);
		assertNotNull(canifis);
		assertEquals("CKS", canifis.getCode());
		assertEquals(100, canifis.match("cks"));
		assertEquals(100, canifis.match("c k s"));
		// Quest-gated banks stay in the list; varbit-built bank chests do not.
		assertNotNull(find("Aldarin", Place.Group.BANK));
		// World map markers from RuneLite.
		assertNotNull(find("Varrock Sewers", Place.Group.DUNGEON));
		// Menu numbering and item words are dropped: no "2. Varrock" or "Varrock tablet".
		for (Place p : World.ATLAS.getPlaces())
		{
			assertFalse(p.getName(), p.getName().matches("^[0-9A-Z][.:] .*"));
			assertFalse(p.getName(), p.getName().toLowerCase().endsWith(" tablet"));
			// The Deadman-only trinket never names a place.
			assertFalse(p.getName(), p.getName().startsWith("Fairy Ring - "));
		}
	}

	@Test
	public void matchingPrefersWholeNames()
	{
		Place v = new Place("Varrock", Pt.of(3213, 3424, 0), Place.Group.LANDING);
		Place vs = new Place("Varrock Sewers", Pt.of(3237, 3458, 0), Place.Group.DUNGEON);
		assertEquals(100, v.match("varrock"));
		assertTrue(v.match("var") > vs.match("sew") - 30);
		assertEquals(60, vs.match("sew"));
		assertEquals(0, v.match("falador"));
		Place bank = new Place("Lumbridge", Pt.of(3208, 3220, 2), Place.Group.BANK);
		assertTrue(bank.match("lum bank") > 0);
		assertEquals("Lumbridge bank", bank.title());
	}

	@Test
	public void walksSayWhereTheyGo()
	{
		Transport ring = null;
		for (Transport t : World.ATLAS.getTransports())
		{
			if (t.getKind() == Kind.FAIRY_RING && t.getLabel().equals("C K S"))
			{
				ring = t;
				break;
			}
		}
		assertNotNull(ring);
		Step walk = new Step(Step.Type.WALK, Pt.of(3100, 3200, 0), ring.getOrigin(), null, 20, 48, false, false);
		Step trip = new Step(Step.Type.TRIP, ring.getOrigin(), ring.getDestination(), ring, 6, 0, false, false);
		List<Step> steps = Arrays.asList(walk, trip);
		List<Place> places = World.ATLAS.getPlaces();
		assertEquals("Walk to the fairy ring", Words.title(walk, places, Words.goal(steps, 0, "Canifis")));
		assertEquals("Fairy ring CKS to Canifis", Words.title(trip, places, Words.goal(steps, 1, "Canifis")));
		assertEquals("1 tile, ~0s", Words.detail(new Step(Step.Type.WALK, 0, 0, null, 0, 1, false, false), World.ATLAS.getItems()));
	}

	@Test
	public void memorySurvivesARoundTrip()
	{
		Memory m = new Memory();
		Place p = new Place("Canifis", Pt.of(3447, 3470, 0), Place.Group.FAIRY_RING, "CKS");
		m.visit(p);
		m.visit(new Place("Varrock", Pt.of(3213, 3424, 0), Place.Group.LANDING));
		m.visit(p);
		assertEquals(2, m.recent.size());
		assertEquals("Canifis", m.recentPlaces().get(0).getName());
		int lumbridge = Pt.of(3222, 3218, 0);
		m.journey(lumbridge, Pt.of(3447, 3470, 0), "Canifis", 10);
		m.journey(lumbridge, Pt.of(3449, 3471, 0), "Canifis", 20);
		m.journey(lumbridge, Pt.of(3213, 3424, 0), "Varrock", 30);
		assertEquals(2, m.tripsByUse().get(0).count);

		Gson gson = new Gson();
		Memory back = gson.fromJson(gson.toJson(m), Memory.class).mend();
		assertEquals(Place.Group.FAIRY_RING, back.recentPlaces().get(0).getGroup());
		assertEquals("CKS", back.recentPlaces().get(0).getCode());
		assertEquals(2, back.trips.size());
		// An empty or damaged save mends into something usable.
		Memory broken = gson.fromJson("{\"trips\":[null],\"paces\":{}}", Memory.class).mend();
		assertTrue(broken.trips.isEmpty());
		assertNotNull(broken.paces.laps);
		assertNull(gson.fromJson("null", Memory.class));
	}

	@Test
	public void lapsIgnoreInterruptions()
	{
		Paces paces = new Paces();
		assertTrue(paces.add("SPELL:Varrock Teleport", 4.0, 3.0, 1));
		assertFalse(paces.add("SPELL:Varrock Teleport", 60.0, 3.0, 2));
		assertEquals(4, paces.seconds("SPELL:Varrock Teleport"));
		assertTrue(paces.add("SPELL:Varrock Teleport", 6.0, 3.0, 3));
		assertEquals(5, paces.seconds("SPELL:Varrock Teleport"));
	}

	@Test
	public void wildernessLevels()
	{
		// Edgeville bank, the ditch, Ferox Enclave: none of them Wilderness.
		assertEquals(0, Pt.wildernessLevel(Pt.of(3094, 3492, 0)));
		assertEquals(0, Pt.wildernessLevel(Pt.of(3094, 3522, 0)));
		assertEquals(0, Pt.wildernessLevel(Pt.of(3130, 3630, 0)));
		assertEquals(1, Pt.wildernessLevel(Pt.of(3094, 3525, 0)));
		// The lines Shortest Path uses for the level 20 and 30 teleport limits.
		assertEquals(20, Pt.wildernessLevel(Pt.of(3100, 3679, 0)));
		assertEquals(21, Pt.wildernessLevel(Pt.of(3100, 3680, 0)));
		assertEquals(30, Pt.wildernessLevel(Pt.of(3100, 3759, 0)));
		assertEquals(31, Pt.wildernessLevel(Pt.of(3100, 3760, 0)));
		assertEquals(21, Pt.wildernessLevel(Pt.of(3100, 10075, 0)));
		assertEquals(31, Pt.wildernessLevel(Pt.of(3100, 10155, 0)));
	}
}
