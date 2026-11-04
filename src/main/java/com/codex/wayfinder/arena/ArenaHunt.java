package com.codex.wayfinder.arena;

import com.codex.wayfinder.core.Pt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One account's Mage Arena II hunt, kept with the rest of Wayfinder's memory: where each god
 * follower was last seen, how much Wilderness time has passed since, and which are already
 * dealt with. Fields are plain so Gson reads older saves; anything missing reads as empty.
 *
 * <p>The wiki says the followers' spots are per player and "change approximately every 45
 * minutes", and that the rotation timer "only progresses while the player is inside the
 * Wilderness". So a spot is dated both by the clock and by the Wilderness time this account has
 * spent since it was first seen there, and is called likely moved past 45 minutes of the latter.
 */
public final class ArenaHunt
{
	/** The wiki's rotation: about 45 minutes of the player's Wilderness time. */
	public static final int ROTATION_SECONDS = 45 * 60;
	/** A game tick, in milliseconds. */
	static final int TICK_MS = 600;
	/** A follower seen within this many tiles of its remembered spot is at the same spot. */
	static final int SAME_SPOT = 8;

	/** How sure we are a follower is done with. Later ones outrank earlier ones. */
	public enum Proof
	{
		/** Seen dying. */
		KILLED,
		/** Its component is in the pack. */
		CARRIED,
		/** Its component is with Kolodion. */
		HANDED
	}

	/** A spot a follower was seen at. */
	public static final class Sighting
	{
		public int x;
		public int y;
		public int plane;
		/** When it was first seen at this spot, epoch ms. */
		public long first;
		/** When it was last seen there, epoch ms. */
		public long last;
		/** The account's Wilderness ticks when it was first seen there. */
		public long wildAt;
		/** The game said the spots rotated after this sighting. */
		public boolean rotated;

		public int tile()
		{
			return Pt.of(x, y, plane);
		}
	}

	/** Spots by follower (the ArenaBoss name). */
	public Map<String, Sighting> seen = new LinkedHashMap<>();
	/** What is known of each follower being dealt with, by ArenaBoss name. */
	public Map<String, Proof> done = new LinkedHashMap<>();
	/** Game ticks this account has spent in the Wilderness while Wayfinder watched. */
	public long wildTicks;

	/** Fills anything an older or damaged save left out. */
	public ArenaHunt mend()
	{
		if (seen == null)
		{
			seen = new LinkedHashMap<>();
		}
		seen.values().removeIf(s -> s == null);
		if (done == null)
		{
			done = new LinkedHashMap<>();
		}
		done.values().removeIf(p -> p == null);
		return this;
	}

	/** One more game tick in the Wilderness. */
	public void wildTick()
	{
		wildTicks++;
	}

	/**
	 * A follower stands at a tile now. Returns true when that is a new spot, worth saving; seeing
	 * it again at the spot it was already known at only moves the "last seen" time.
	 */
	public boolean saw(ArenaBoss b, int tile, long now)
	{
		Sighting s = seen.get(b.name());
		if (s != null && Pt.flatDistance(s.tile(), tile) <= SAME_SPOT && Pt.plane(tile) == s.plane)
		{
			s.last = now;
			if (s.rotated)
			{
				// Seen at the old spot after a rotation: the rotation picked the same area again.
				s.rotated = false;
				s.first = now;
				s.wildAt = wildTicks;
				return true;
			}
			return false;
		}
		Sighting n = new Sighting();
		n.x = Pt.x(tile);
		n.y = Pt.y(tile);
		n.plane = Pt.plane(tile);
		n.first = now;
		n.last = now;
		n.wildAt = wildTicks;
		seen.put(b.name(), n);
		return true;
	}

	/** Records proof that a follower is done with; a weaker proof never replaces a stronger one. Returns true on a change. */
	public boolean prove(ArenaBoss b, Proof p)
	{
		Proof had = done.get(b.name());
		if (had != null && had.compareTo(p) >= 0)
		{
			return false;
		}
		done.put(b.name(), p);
		return true;
	}

	/**
	 * The player died. The wiki: a component "is always lost upon death", and "If you're killed,
	 * you'll have to kill the follower again". Anything not yet with Kolodion is undone.
	 * Returns true on a change.
	 */
	public boolean died()
	{
		return done.values().removeIf(p -> p != Proof.HANDED);
	}

	/** The game rotated the spots: every remembered one is now probably wrong. */
	public boolean rotated()
	{
		boolean changed = false;
		for (Sighting s : seen.values())
		{
			changed |= !s.rotated;
			s.rotated = true;
		}
		return changed;
	}

	public Proof proof(ArenaBoss b)
	{
		return done.get(b.name());
	}

	public Sighting sighting(ArenaBoss b)
	{
		return seen.get(b.name());
	}

	/** Seconds of Wilderness time since the follower was first seen at its spot. */
	public int wildSecondsSince(Sighting s)
	{
		return (int) Math.max(0, (wildTicks - s.wildAt) * TICK_MS / 1000);
	}

	/** True when the spot has probably rotated: the game said so, or a rotation's worth of Wilderness time has passed. */
	public boolean likelyMoved(Sighting s)
	{
		return s.rotated || wildSecondsSince(s) >= ROTATION_SECONDS;
	}

	public int slainCount()
	{
		return done.size();
	}

	/** What the panel and the map show for each follower, in the game's order: Saradomin, Guthix, Zamorak. */
	public List<Line> lines(long now)
	{
		List<Line> out = new ArrayList<>();
		for (ArenaBoss b : ArenaBoss.values())
		{
			Sighting s = sighting(b);
			out.add(new Line(b, s == null ? Pt.NONE : s.tile(), proof(b),
				s == null ? -1 : (int) Math.max(0, (now - s.last) / 1000),
				s == null ? 0 : wildSecondsSince(s), s != null && likelyMoved(s)));
		}
		return out;
	}

	/** One follower as shown: where, how long ago, and whether it is done with. Route fields are filled by the plugin. */
	public static final class Line
	{
		public final ArenaBoss boss;
		/** The remembered spot, or Pt.NONE when never seen. */
		public final int tile;
		public final Proof proof;
		/** Seconds since last seen, or -1 when never seen. */
		public final int ago;
		public final int wildSeconds;
		public final boolean moved;
		/** Seconds of the quickest way there, or -1 when not planned. */
		public int routeSeconds = -1;
		/** The first ride of that way in words, or null when it is all on foot. */
		public String routeVia;
		/** That time in words, saying whose pace it is: "2m 10s at your pace", "~2m 10s". */
		public String routeEta;

		public Line(ArenaBoss boss, int tile, Proof proof, int ago, int wildSeconds, boolean moved)
		{
			this.boss = boss;
			this.tile = tile;
			this.proof = proof;
			this.ago = ago;
			this.wildSeconds = wildSeconds;
			this.moved = moved;
		}

		public boolean isSeen()
		{
			return tile != Pt.NONE;
		}

		public boolean isDone()
		{
			return proof != null;
		}
	}
}
