package com.codex.wayfinder.overlay;

import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.core.Trail;
import com.codex.wayfinder.ui.Theme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The route on the minimap: gilt dots along the walk ahead, fainter dots for the walks after the
 * next ride, a teal diamond where the ride is boarded and a gold ring at the place. When that
 * stop is past the rim, a dart on the rim points to it with the tiles left to walk.
 */
public class MinimapTrail extends Overlay
{
	/** Tiles from the player worth testing against the minimap; it shows about 19 at the usual zoom. */
	private static final int NEAR = 40;

	private final Client client;
	private volatile TrailShown shown;

	public MinimapTrail(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_LOW);
	}

	public void show(TrailShown s)
	{
		shown = s;
	}

	/** The widget the minimap is drawn in, picked the way Perspective.localToMinimap picks it. */
	private Widget minimap()
	{
		Widget w;
		if (client.isResized())
		{
			w = client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
				? client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP)
				: client.getWidget(InterfaceID.ToplevelOsrsStretch.MINIMAP);
		}
		else
		{
			w = client.getWidget(InterfaceID.Toplevel.MINIMAP);
		}
		return w == null || w.isHidden() ? null : w;
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
		if (s == null || (!s.minimap && !s.arrow) || s.ahead == null || wv == null || me == null || s.here == Pt.NONE)
		{
			return null;
		}
		Widget w = minimap();
		if (w == null)
		{
			return null;
		}
		Point loc = w.getCanvasLocation();
		int cx = loc.getX() + w.getWidth() / 2;
		int cy = loc.getY() + w.getHeight() / 2;
		int radius = Math.min(w.getWidth(), w.getHeight()) / 2;
		double zoom = client.getMinimapZoom();
		int yaw = client.getCameraYawTarget();
		LocalPoint focus = me.getLocalLocation();
		int plane = wv.getPlane();
		int baseX = wv.getBaseX();
		int baseY = wv.getBaseY();
		Theme.smooth(g);
		Shape old = g.getClip();
		g.clip(new Ellipse2D.Float(cx - radius + 2, cy - radius + 2, (radius - 2) * 2, (radius - 2) * 2));

		Trail.Leg current = s.ahead.getLeg();
		if (s.minimap)
		{
			Color later = Theme.alpha(Theme.mix(s.path, Theme.INK, 0.35), 200);
			for (Trail.Leg leg : s.trail.getLegs())
			{
				if (!leg.isWalk() || leg.getIndex() < current.getIndex())
				{
					continue;
				}
				int[] path = leg == current ? s.ahead.getTiles() : leg.getStep().getPath();
				Color c = leg == current ? s.path : later;
				for (int t : path)
				{
					if (Pt.plane(t) != plane || Pt.distance(s.here, t) > NEAR)
					{
						continue;
					}
					double[] o = offset(t, focus, baseX, baseY, zoom, yaw);
					if (o[0] * o[0] + o[1] * o[1] < (radius - 3) * (radius - 3))
					{
						TrailPaint.dot(g, (int) Math.round(cx + o[0]), (int) Math.round(cy + o[1]), c);
					}
				}
			}
		}

		int end = s.ahead.getWaypoint();
		if (end != Pt.NONE)
		{
			double[] o = offset(end, focus, baseX, baseY, zoom, yaw);
			double far = Math.hypot(o[0], o[1]);
			if (far < radius - 10 && Pt.plane(end) == plane)
			{
				if (s.minimap)
				{
					int x = (int) Math.round(cx + o[0]);
					int y = (int) Math.round(cy + o[1]);
					if (s.endsAtPlace())
					{
						TrailPaint.ring(g, x, y, 4, Theme.GOLD_BRIGHT);
					}
					else
					{
						TrailPaint.diamond(g, x, y, 4, s.mark);
					}
				}
			}
			else if (s.arrow && far > 0)
			{
				g.setClip(old);
				int tiles = Pt.flatDistance(s.here, end);
				TrailPaint.rimArrow(g, cx, cy, radius - 3, Math.atan2(o[1], o[0]), s.endsAtPlace() ? Theme.GOLD : s.mark,
					tiles < 1000 ? String.valueOf(tiles) : "");
			}
		}
		g.setClip(old);
		return null;
	}

	/** The minimap offset of a tile from the player, from world tiles without needing the tile in the scene. */
	private static double[] offset(int tile, LocalPoint focus, int baseX, int baseY, double zoom, int yaw)
	{
		double lx = (Pt.x(tile) - baseX) * MinimapMath.TILE + MinimapMath.TILE / 2.0;
		double ly = (Pt.y(tile) - baseY) * MinimapMath.TILE + MinimapMath.TILE / 2.0;
		return MinimapMath.offset(lx - focus.getX(), ly - focus.getY(), zoom, yaw);
	}
}
