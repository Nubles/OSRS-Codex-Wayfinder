package com.codex.wayfinder.overlay;

import com.codex.wayfinder.core.Account;
import com.codex.wayfinder.core.Atlas;
import com.codex.wayfinder.core.Paces;
import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Router;
import com.codex.wayfinder.core.Step;
import com.codex.wayfinder.core.Trail;
import com.codex.wayfinder.core.Transport;
import com.codex.wayfinder.core.Words;
import com.codex.wayfinder.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import net.runelite.api.gameval.VarbitID;

/**
 * Paints the trail overlays to PNGs without a game, for checking how they look: a stand-in
 * scene, minimap and world map, with a real route from the atlas (Draynor to Canifis by fairy
 * ring). Not a test; run by hand with -Djava.awt.headless=true and an output folder argument.
 */
public final class TrailShots
{
	static final Color PATH = new Color(214, 178, 106, 230);
	static final Color MARK = new Color(96, 184, 170, 220);

	public static void main(String[] args) throws Exception
	{
		File dir = new File(args.length > 0 ? args[0] : ".");
		dir.mkdirs();
		Atlas atlas = Atlas.load();
		Router router = new Router(atlas);
		Account a = Account.builder().known(true).questsKnown(true).bankKnown(true)
			.quest("Priest in Peril").quest("Nature Spirit").varbit(VarbitID.FAIRY2_QUEENCURE_QUEST, 40).wear(772).build();
		Paces paces = new Paces();
		paces.addWalk(70, 60);
		int start = Pt.of(3093, 3244, 0);
		int canifis = Pt.of(3493, 3480, 0);
		Router.Options o = new Router.Options();
		o.paces = paces;
		Route r = router.route(start, canifis, a, o);
		for (Step s : r.getSteps())
		{
			System.out.println(s.getType() + " " + (s.getTransport() == null ? s.getTiles() + " tiles" : s.getTransport().key()));
		}
		Trail trail = Trail.of(r);
		Trail.Ahead ahead = trail.ahead(start);
		Step next = ahead.getNext();
		String callout = Words.title(next, atlas.getPlaces(), null);
		int nearRing = ahead.getTiles()[ahead.getTiles().length - 16];
		ImageIO.write(ground(r, trail.ahead(nearRing), nearRing, callout, atlas, paces), "png", new File(dir, "trail-ground.png"));
		ImageIO.write(minimap(ahead, trail, 0, start), "png", new File(dir, "trail-minimap.png"));
		// Further along the road, turned: the ring is past the rim, so the dart points to it.
		int[] path = ahead.getTiles();
		ImageIO.write(minimap(trail.ahead(Pt.of(3093, 3260, 0)), trail, 5200, Pt.of(3093, 3260, 0)), "png", new File(dir, "trail-minimap-arrow.png"));
		ImageIO.write(map(trail, r), "png", new File(dir, "trail-worldmap.png"));
		System.out.println("ring at " + Pt.toString(ahead.getWaypoint()) + ", " + path.length + " tiles ahead; " + Words.eta(r, paces));
	}

	/** A stand-in for the game view: grass seen from behind the player, the route ribbon, the ring's tile and its call-out, and the plaque. */
	static BufferedImage ground(Route r, Trail.Ahead ahead, int me, String callout, Atlas atlas, Paces paces)
	{
		int w = 760;
		int h = 480;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Theme.smooth(g);
		g.setPaint(new GradientPaint(0, 0, new Color(58, 74, 44), 0, h, new Color(78, 98, 52)));
		g.fillRect(0, 0, w, h);
		int[] tiles = ahead.getTiles();
		// Faint tile grid, so the ribbon reads as lying on tiles.
		g.setColor(new Color(0, 0, 0, 28));
		for (int dx = -12; dx <= 12; dx++)
		{
			for (int dy = -22; dy <= 3; dy++)
			{
				g.draw(tilePoly(dx, dy, w, h));
			}
		}
		// The player.
		java.awt.Point p0 = proj(0, 0, w, h);
		g.setColor(new Color(30, 30, 40));
		g.fillOval(p0.x - 9, p0.y - 46, 18, 46);
		g.setColor(new Color(160, 40, 40));
		g.fillOval(p0.x - 7, p0.y - 54, 14, 14);

		int n = Math.min(tiles.length, GroundTrail.AHEAD);
		int[] xs = new int[n + 1];
		int[] ys = new int[n + 1];
		xs[0] = p0.x;
		ys[0] = p0.y;
		int count = 1;
		for (int i = 0; i < n; i++)
		{
			java.awt.Point p = proj(Pt.x(tiles[i]) - Pt.x(me), Pt.y(tiles[i]) - Pt.y(me), w, h);
			if (p.y < 40 || Pt.y(me) - Pt.y(tiles[i]) < -4)
			{
				break;
			}
			xs[count] = p.x;
			ys[count++] = p.y;
		}
		TrailPaint.ribbon(g, xs, ys, count, PATH);
		int end = ahead.getWaypoint();
		int ex = Pt.x(end) - Pt.x(me);
		int ey = Pt.y(end) - Pt.y(me);
		Polygon poly = tilePoly(ex, ey, w, h);
		g.setStroke(new BasicStroke(2f));
		g.setColor(MARK);
		g.draw(poly);
		g.setColor(Theme.alpha(MARK, 60));
		g.fill(poly);
		java.awt.Point tip = proj(ex, ey, w, h);
		TrailPaint.chip(g, callout, tip.x, tip.y - 30, MARK);

		StepPlaque plaque = new StepPlaque();
		Step now = ahead.getLeg().getStep();
		int total = r.getSeconds();
		plaque.show(new StepPlaque.Shown("TO CANIFIS", Words.title(now, atlas.getPlaces(), Words.goal(r.getSteps(), 0, "Canifis")),
			Words.detail(now, atlas.getItems()), Words.eta(r, paces), 18, total + 18));
		Graphics2D pg = (Graphics2D) g.create(10, 10, 300, 100);
		plaque.render(pg);
		pg.dispose();

		g.dispose();
		return img;
	}

	static java.awt.Point proj(double dx, double dy, int w, int h)
	{
		// The camera looks south, down the road to the ring.
		dx = -dx;
		dy = -dy;
		double depth = Math.max(0.8, dy + 6);
		return new java.awt.Point((int) Math.round(w / 2.0 + dx * 360 / depth), (int) Math.round(60 + 1800 / depth));
	}

	static Polygon tilePoly(int dx, int dy, int w, int h)
	{
		Polygon p = new Polygon();
		java.awt.Point a = proj(dx - 0.5, dy - 0.5, w, h);
		java.awt.Point b = proj(dx + 0.5, dy - 0.5, w, h);
		java.awt.Point c = proj(dx + 0.5, dy + 0.5, w, h);
		java.awt.Point d = proj(dx - 0.5, dy + 0.5, w, h);
		p.addPoint(a.x, a.y);
		p.addPoint(b.x, b.y);
		p.addPoint(c.x, c.y);
		p.addPoint(d.x, d.y);
		return p;
	}

	/** A stand-in minimap: a green disc in a stone frame, with the route drawn the way MinimapTrail draws it. */
	static BufferedImage minimap(Trail.Ahead ahead, Trail trail, int yaw, int here)
	{
		int size = 220;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Theme.smooth(g);
		g.setColor(new Color(40, 36, 30));
		g.fillRect(0, 0, size, size);
		int cx = size / 2;
		int cy = size / 2;
		int radius = 76;
		g.setPaint(new RadialGradientPaint(cx, cy, radius, new float[]{0f, 1f}, new Color[]{new Color(70, 96, 52), new Color(54, 76, 42)}));
		g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
		g.setColor(new Color(92, 84, 70));
		g.setStroke(new BasicStroke(6f));
		g.draw(new Ellipse2D.Float(cx - radius - 3, cy - radius - 3, radius * 2 + 6, radius * 2 + 6));
		double zoom = 4;
		java.awt.Shape outside = g.getClip();
		g.clip(new Ellipse2D.Float(cx - radius + 2, cy - radius + 2, (radius - 2) * 2, (radius - 2) * 2));
		for (Trail.Leg leg : trail.getLegs())
		{
			if (!leg.isWalk() || leg.getIndex() < ahead.getLeg().getIndex())
			{
				continue;
			}
			int[] path = leg == ahead.getLeg() ? ahead.getTiles() : leg.getStep().getPath();
			Color c = leg == ahead.getLeg() ? PATH : Theme.alpha(Theme.mix(PATH, Theme.INK, 0.35), 200);
			for (int t : path)
			{
				if (Pt.distance(here, t) > 40)
				{
					continue;
				}
				double[] o = MinimapMath.offset((Pt.x(t) - Pt.x(here)) * 128, (Pt.y(t) - Pt.y(here)) * 128, zoom, yaw);
				if (o[0] * o[0] + o[1] * o[1] < (radius - 3) * (radius - 3))
				{
					TrailPaint.dot(g, (int) Math.round(cx + o[0]), (int) Math.round(cy + o[1]), c);
				}
			}
		}
		g.setColor(Color.WHITE);
		g.fillRect(cx - 1, cy - 1, 3, 3);
		int end = ahead.getWaypoint();
		double[] o = MinimapMath.offset((Pt.x(end) - Pt.x(here)) * 128, (Pt.y(end) - Pt.y(here)) * 128, zoom, yaw);
		double far = Math.hypot(o[0], o[1]);
		if (far < radius - 10)
		{
			TrailPaint.diamond(g, (int) Math.round(cx + o[0]), (int) Math.round(cy + o[1]), 4, MARK);
		}
		else
		{
			g.setClip(outside);
			TrailPaint.rimArrow(g, cx, cy, radius - 3, Math.atan2(o[1], o[0]), MARK, String.valueOf(Pt.flatDistance(here, end)));
		}
		g.dispose();
		return img;
	}

	/** A stand-in world map: parchment land and sea, with the whole route drawn the way MapTrail draws it, and the pin. */
	static BufferedImage map(Trail trail, Route r)
	{
		int w = 640;
		int h = 480;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Theme.smooth(g);
		g.setColor(new Color(64, 80, 108));
		g.fillRect(0, 0, w, h);
		g.setColor(new Color(118, 128, 76));
		g.fillRoundRect(-40, 20, 470, h + 40, 120, 120);
		g.fillRoundRect(470, 20, 220, h + 40, 120, 120);
		g.setColor(new Color(108, 118, 70));
		g.fillOval(160, 120, 240, 180);
		// Map frame: 1.4 px per tile, Draynor at the lower left.
		double ppt = 1.1;
		int ox = 3040;
		int oy = 3130;
		for (Trail.Leg leg : trail.getLegs())
		{
			if (!leg.isWalk())
			{
				continue;
			}
			for (int[] run : leg.getRuns())
			{
				int[] xs = new int[run.length];
				int[] ys = new int[run.length];
				for (int i = 0; i < run.length; i++)
				{
					xs[i] = (int) Math.round(40 + (Pt.x(run[i]) - ox) * ppt);
					ys[i] = (int) Math.round(h - 30 - (Pt.y(run[i]) - oy) * ppt);
				}
				TrailPaint.mapWalk(g, xs, ys, run.length, PATH);
			}
		}
		for (Trail.Leg leg : trail.getLegs())
		{
			if (leg.isWalk())
			{
				continue;
			}
			Step s = leg.getStep();
			Transport t = s.getTransport();
			int from = t.getOrigin() != Pt.NONE ? t.getOrigin() : s.getFrom();
			int x1 = (int) Math.round(40 + (Pt.x(from) - ox) * ppt);
			int y1 = (int) Math.round(h - 30 - (Pt.y(from) - oy) * ppt);
			int x2 = (int) Math.round(40 + (Pt.x(s.getTo()) - ox) * ppt);
			int y2 = (int) Math.round(h - 30 - (Pt.y(s.getTo()) - oy) * ppt);
			TrailPaint.mapLeap(g, x1, y1, x2, y2, MARK);
			TrailPaint.chip(g, Words.chip(t), x2, y2 - 4, MARK);
		}
		BufferedImage pin = TrailPaint.pin();
		int px = (int) Math.round(40 + (Pt.x(r.getTarget()) - ox) * ppt);
		int py = (int) Math.round(h - 30 - (Pt.y(r.getTarget()) - oy) * ppt);
		g.drawImage(pin, px - 13, py - 33, null);
		g.dispose();
		return img;
	}
}
