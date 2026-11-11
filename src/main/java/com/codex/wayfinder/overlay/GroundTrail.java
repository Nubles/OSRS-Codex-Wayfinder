package com.codex.wayfinder.overlay;

import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.ui.Theme;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The walk ahead, drawn on the ground: a gilt ribbon from the player's feet along the planned
 * tiles to where the next ride is boarded, with the ride named above that spot, or to the place
 * itself, outlined in gold. Only the tiles still ahead are drawn, and only so far.
 */
public class GroundTrail extends Overlay
{
	/** Tiles of ribbon drawn ahead of the player; the scene fogs out past this anyway. */
	static final int AHEAD = 48;
	/** Height above the tile for the call-out's point, in local units (a tile is 128). */
	private static final int CALLOUT_HEIGHT = 180;

	private final Client client;
	private volatile TrailShown shown;

	public GroundTrail(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	public void show(TrailShown s)
	{
		shown = s;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Drawn on a copy, so no clip, stroke, font or hint set here reaches RuneLite's renderer.
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			return draw(g);
		}
		finally
		{
			g.dispose();
		}
	}

	private Dimension draw(Graphics2D g)
	{
		TrailShown s = shown;
		WorldView wv = client.getTopLevelWorldView();
		Player me = client.getLocalPlayer();
		if (s == null || !s.ground || s.ahead == null || wv == null || me == null)
		{
			return null;
		}
		int plane = wv.getPlane();
		int[] tiles = s.ahead.getTiles();
		int n = Math.min(tiles.length, AHEAD);
		int[] xs = new int[2 * n + 2];
		int[] ys = new int[2 * n + 2];
		int count = 0;
		Point start = Perspective.localToCanvas(client, me.getLocalLocation(), plane);
		int prev = s.here;
		if (start != null)
		{
			xs[0] = start.getX();
			ys[0] = start.getY();
			count = 1;
		}
		for (int i = 0; i < n; i++)
		{
			int t = tiles[i];
			// Off the scene or up a stair: the ribbon stops there.
			Point c = Pt.plane(t) == plane ? canvas(wv, t, plane, 0) : null;
			if (c == null)
			{
				break;
			}
			if (prev != Pt.NONE && Pt.distance(prev, t) > 1)
			{
				// Through a door or a gap: leave the ribbon open across it.
				xs[count] = Integer.MIN_VALUE;
				ys[count++] = 0;
			}
			xs[count] = c.getX();
			ys[count++] = c.getY();
			prev = t;
		}
		TrailPaint.ribbon(g, xs, ys, count, s.path);

		int end = s.ahead.getWaypoint();
		if (end == Pt.NONE || Pt.plane(end) != plane)
		{
			return null;
		}
		LocalPoint lp = LocalPoint.fromWorld(wv, Pt.x(end), Pt.y(end));
		if (lp == null)
		{
			return null;
		}
		if (s.endsAtPlace())
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				TrailPaint.tile(g, poly, Theme.alpha(Theme.GOLD_BRIGHT, 220));
			}
		}
		Point tip = Perspective.localToCanvas(client, lp, plane, CALLOUT_HEIGHT);
		if (tip != null)
		{
			TrailPaint.chip(g, s.endsAtPlace() ? s.place : s.callout, tip.getX(), tip.getY(), s.endsAtPlace() ? Theme.GOLD : s.mark);
		}
		return null;
	}

	private Point canvas(WorldView wv, int tile, int plane, int height)
	{
		LocalPoint lp = LocalPoint.fromWorld(wv, Pt.x(tile), Pt.y(tile));
		return lp == null ? null : Perspective.localToCanvas(client, lp, plane, height);
	}
}
