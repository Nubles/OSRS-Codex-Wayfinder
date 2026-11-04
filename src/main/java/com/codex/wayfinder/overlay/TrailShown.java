package com.codex.wayfinder.overlay;

import com.codex.wayfinder.core.Trail;
import java.awt.Color;

/**
 * What the trail overlays draw this tick: the route laid out, the part of it ahead of the player,
 * and the words for the next ride and the place. Made on the client thread each tick and read
 * whole by the overlays while the game draws.
 */
public final class TrailShown
{
	final Trail trail;
	final Trail.Ahead ahead;
	final int here;
	final String place;
	final String callout;
	final Color path;
	final Color mark;
	final boolean ground;
	final boolean minimap;
	final boolean arrow;
	final boolean map;

	public TrailShown(Trail trail, Trail.Ahead ahead, int here, String place, String callout, Color path, Color mark,
		boolean ground, boolean minimap, boolean arrow, boolean map)
	{
		this.trail = trail;
		this.ahead = ahead;
		this.here = here;
		this.place = place;
		this.callout = callout;
		this.path = path;
		this.mark = mark;
		this.ground = ground;
		this.minimap = minimap;
		this.arrow = arrow;
		this.map = map;
	}

	/** Whether the walk ahead ends at the place itself rather than at a ride. */
	boolean endsAtPlace()
	{
		return ahead != null && ahead.getNext() == null;
	}
}
