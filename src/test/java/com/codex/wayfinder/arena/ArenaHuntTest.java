package com.codex.wayfinder.arena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.codex.wayfinder.core.Memory;
import com.codex.wayfinder.core.Pt;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public class ArenaHuntTest
{
	private static final int SPOT = Pt.of(3200, 3830, 0);
	private static final long T0 = 1_700_000_000_000L;

	/** The ids are the wiki infobox's: Zachariah 5977/7858, Derwen 7513/7859, Porazdir 7515/7860; drops 21797-21799. */
	@Test
	public void followersAndDropsByWikiIds()
	{
		assertSame(ArenaBoss.ZACHARIAH, ArenaBoss.ofNpc(5977));
		assertSame(ArenaBoss.ZACHARIAH, ArenaBoss.ofNpc(7858));
		assertSame(ArenaBoss.DERWEN, ArenaBoss.ofNpc(7513));
		assertSame(ArenaBoss.DERWEN, ArenaBoss.ofNpc(7859));
		assertSame(ArenaBoss.PORAZDIR, ArenaBoss.ofNpc(7515));
		assertSame(ArenaBoss.PORAZDIR, ArenaBoss.ofNpc(7860));
		// Derwen's healing orbs are not Derwen.
		assertNull(ArenaBoss.ofNpc(7514));
		assertSame(ArenaBoss.ZACHARIAH, ArenaBoss.ofDrop(21797));
		assertSame(ArenaBoss.DERWEN, ArenaBoss.ofDrop(21798));
		assertSame(ArenaBoss.PORAZDIR, ArenaBoss.ofDrop(21799));
		assertNull(ArenaBoss.ofDrop(21800));
		assertEquals("Justiciar's hand", ArenaBoss.ZACHARIAH.getDrop());
		assertEquals("Ent's roots", ArenaBoss.DERWEN.getDrop());
		assertEquals("Demon's heart", ArenaBoss.PORAZDIR.getDrop());
	}

	@Test
	public void seeingAgainAtTheSameSpotOnlyMovesTheTime()
	{
		ArenaHunt h = new ArenaHunt();
		assertTrue(h.saw(ArenaBoss.PORAZDIR, SPOT, T0));
		// A follower walks about while it fights: a few tiles off is the same spot.
		assertFalse(h.saw(ArenaBoss.PORAZDIR, Pt.of(3204, 3833, 0), T0 + 60_000));
		ArenaHunt.Sighting s = h.sighting(ArenaBoss.PORAZDIR);
		assertEquals(SPOT, s.tile());
		assertEquals(T0, s.first);
		assertEquals(T0 + 60_000, s.last);
		// Far off is a new spot.
		assertTrue(h.saw(ArenaBoss.PORAZDIR, Pt.of(3260, 3880, 0), T0 + 120_000));
		assertEquals(Pt.of(3260, 3880, 0), h.sighting(ArenaBoss.PORAZDIR).tile());
	}

	@Test
	public void aSpotGoesStaleAfterFortyFiveMinutesOfWildernessTimeOnly()
	{
		ArenaHunt h = new ArenaHunt();
		h.saw(ArenaBoss.DERWEN, SPOT, T0);
		ArenaHunt.Sighting s = h.sighting(ArenaBoss.DERWEN);
		// Time outside the Wilderness does not count: no ticks, not stale, however long ago.
		assertFalse(h.likelyMoved(s));
		// 44 minutes in the Wilderness: 4400 ticks of 0.6 s.
		for (int i = 0; i < 4400; i++)
		{
			h.wildTick();
		}
		assertEquals(2640, h.wildSecondsSince(s));
		assertFalse(h.likelyMoved(s));
		for (int i = 0; i < 100; i++)
		{
			h.wildTick();
		}
		assertTrue(h.likelyMoved(s));
		// Seen at a new spot: the clock starts again.
		h.saw(ArenaBoss.DERWEN, Pt.of(3100, 3900, 0), T0 + 1);
		assertFalse(h.likelyMoved(h.sighting(ArenaBoss.DERWEN)));
	}

	@Test
	public void aRotationMarksEverySpotUntilSeenAgain()
	{
		ArenaHunt h = new ArenaHunt();
		h.saw(ArenaBoss.ZACHARIAH, SPOT, T0);
		h.saw(ArenaBoss.DERWEN, Pt.of(3100, 3900, 0), T0);
		assertTrue(h.rotated());
		assertFalse("a second rotation changes nothing new", h.rotated());
		assertTrue(h.likelyMoved(h.sighting(ArenaBoss.ZACHARIAH)));
		// Seen at the old spot after the rotation: a fresh sighting, worth saving.
		assertTrue(h.saw(ArenaBoss.ZACHARIAH, SPOT, T0 + 5));
		assertFalse(h.likelyMoved(h.sighting(ArenaBoss.ZACHARIAH)));
		assertTrue(h.likelyMoved(h.sighting(ArenaBoss.DERWEN)));
	}

	@Test
	public void proofOnlyGrowsAndDeathUndoesWhatKolodionHasNot()
	{
		ArenaHunt h = new ArenaHunt();
		assertTrue(h.prove(ArenaBoss.PORAZDIR, ArenaHunt.Proof.KILLED));
		assertTrue(h.prove(ArenaBoss.PORAZDIR, ArenaHunt.Proof.CARRIED));
		assertFalse(h.prove(ArenaBoss.PORAZDIR, ArenaHunt.Proof.KILLED));
		assertTrue(h.prove(ArenaBoss.ZACHARIAH, ArenaHunt.Proof.HANDED));
		assertFalse(h.prove(ArenaBoss.ZACHARIAH, ArenaHunt.Proof.CARRIED));
		assertEquals(2, h.slainCount());
		// The wiki: the component "is always lost upon death".
		assertTrue(h.died());
		assertNull(h.proof(ArenaBoss.PORAZDIR));
		assertEquals(ArenaHunt.Proof.HANDED, h.proof(ArenaBoss.ZACHARIAH));
		assertFalse(h.died());
	}

	@Test
	public void linesAndWords()
	{
		ArenaHunt h = new ArenaHunt();
		h.saw(ArenaBoss.PORAZDIR, SPOT, T0);
		h.prove(ArenaBoss.ZACHARIAH, ArenaHunt.Proof.CARRIED);
		for (int i = 0; i < 1000; i++)
		{
			h.wildTick();
		}
		List<ArenaHunt.Line> lines = h.lines(T0 + 14 * 60_000);
		assertEquals(3, lines.size());
		ArenaHunt.Line zach = lines.get(0);
		ArenaHunt.Line derwen = lines.get(1);
		ArenaHunt.Line por = lines.get(2);
		assertSame(ArenaBoss.ZACHARIAH, zach.boss);
		assertTrue(zach.isDone());
		assertFalse(zach.isSeen());
		assertEquals("Slain. Justiciar's hand in your pack: take it to Kolodion", ArenaWords.state(zach));
		assertFalse(derwen.isSeen());
		assertTrue(ArenaWords.state(derwen).startsWith("Not seen yet"));
		assertNull(ArenaWords.age(derwen));
		assertEquals("Seen here 14 min ago", ArenaWords.state(por));
		assertEquals("About 35 min of Wilderness time before it moves", ArenaWords.age(por));
		assertNull("no way until planned", ArenaWords.way(por));
		por.routeSeconds = 130;
		por.routeVia = "Burning amulet to Lava Maze";
		por.routeEta = "~2m 10s";
		assertEquals("Burning amulet to Lava Maze: ~2m 10s", ArenaWords.way(por));
		assertEquals("Porazdir (Zamorak): seen here 14 min ago, ~2m 10s away", ArenaWords.tooltip(por));
		por.routeVia = null;
		assertEquals("On foot: ~2m 10s", ArenaWords.way(por));
		assertEquals("just now", ArenaWords.ago(30));
		assertEquals("3 h ago", ArenaWords.ago(3 * 3600 + 5));
		assertEquals("2 days ago", ArenaWords.ago(2 * 86400));
	}

	@Test
	public void keptWithTheRestOfMemoryAndOlderSavesStillRead()
	{
		Gson gson = new Gson();
		// A save from before the hunt existed.
		Memory old = gson.fromJson("{\"trips\":[],\"recent\":[],\"bank\":{},\"bankTime\":0}", Memory.class).mend();
		assertTrue(old.arena.seen.isEmpty());
		assertEquals(0, old.arena.wildTicks);
		Memory m = new Memory();
		m.arena.saw(ArenaBoss.DERWEN, SPOT, T0);
		m.arena.prove(ArenaBoss.PORAZDIR, ArenaHunt.Proof.KILLED);
		m.arena.wildTick();
		Memory back = gson.fromJson(gson.toJson(m), Memory.class).mend();
		assertEquals(SPOT, back.arena.sighting(ArenaBoss.DERWEN).tile());
		assertEquals(ArenaHunt.Proof.KILLED, back.arena.proof(ArenaBoss.PORAZDIR));
		assertEquals(1, back.arena.wildTicks);
		// A damaged hunt reads as empty rather than failing.
		Memory broken = gson.fromJson("{\"arena\":{\"seen\":null,\"done\":null}}", Memory.class).mend();
		assertTrue(broken.arena.seen.isEmpty());
		assertTrue(broken.arena.done.isEmpty());
	}
}
