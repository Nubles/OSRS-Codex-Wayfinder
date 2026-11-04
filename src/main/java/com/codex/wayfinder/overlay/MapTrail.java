package com.codex.wayfinder.overlay;

import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.core.Step;
import com.codex.wayfinder.core.Trail;
import com.codex.wayfinder.core.Transport;
import com.codex.wayfinder.core.Words;
import com.codex.wayfinder.ui.Theme;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

/**
 * The whole route on the world map: each walk as a gilt line, each ride as a dashed teal leap
 * from where it is boarded to where it lands, named in a small plaque (a ring's code, the
 * teleport's source, the kind of ride). The place itself carries the Wayfinder pin, which the
 * plugin adds through RuneLite's world map points.
 */
public class MapTrail extends Overlay
{
	private final Client client;
	private final WorldMapOverlay worldMap;
	private volatile TrailShown shown;

	public MapTrail(Client client, WorldMapOverlay worldMap)
	{
		this.client = client;
		this.worldMap = worldMap;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		setPriority(PRIORITY_LOW);
		drawAfterLayer(InterfaceID.Worldmap.MAP_CONTAINER);
	}

	public void show(TrailShown s)
	{
		shown = s;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		TrailShown s = shown;
		if (s == null || !s.map || s.trail == null)
		{
			return null;
		}
		Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (map == null || map.isHidden())
		{
			return null;
		}
		Shape old = g.getClip();
		g.setClip(map.getBounds());
		Theme.smooth(g);
		Trail.Leg current = s.ahead == null ? null : s.ahead.getLeg();
		for (Trail.Leg leg : s.trail.getLegs())
		{
			if (current != null && leg.getIndex() < current.getIndex())
			{
				continue;
			}
			if (leg.isWalk())
			{
				for (int[] run : leg.getRuns())
				{
					walk(g, run, s);
				}
			}
		}
		// Rides over the walks, so their plaques are never crossed by a line.
		for (Trail.Leg leg : s.trail.getLegs())
		{
			if (leg.isWalk() || (current != null && leg.getIndex() < current.getIndex()))
			{
				continue;
			}
			Step step = leg.getStep();
			Transport t = step.getTransport();
			int from = t != null && t.getOrigin() != Pt.NONE ? t.getOrigin() : step.getFrom();
			Point a = project(from);
			Point b = project(step.getTo());
			if (b == null)
			{
				continue;
			}
			if (a != null)
			{
				TrailPaint.mapLeap(g, a.getX(), a.getY(), b.getX(), b.getY(), s.mark);
			}
			else
			{
				TrailPaint.ring(g, b.getX(), b.getY(), 4, s.mark);
			}
			TrailPaint.chip(g, Words.chip(t), b.getX(), b.getY() - 4, s.mark);
		}
		g.setClip(old);
		return null;
	}

	private void walk(Graphics2D g, int[] run, TrailShown s)
	{
		int[] xs = new int[run.length];
		int[] ys = new int[run.length];
		int n = 0;
		for (int t : run)
		{
			Point p = project(t);
			if (p == null)
			{
				TrailPaint.mapWalk(g, xs, ys, n, s.path);
				n = 0;
				continue;
			}
			xs[n] = p.getX();
			ys[n++] = p.getY();
		}
		TrailPaint.mapWalk(g, xs, ys, n, s.path);
	}

	private Point project(int tile)
	{
		return tile == Pt.NONE ? null : worldMap.mapWorldPointToGraphicsPoint(new WorldPoint(Pt.x(tile), Pt.y(tile), Pt.plane(tile)));
	}
}
