package com.codex.wayfinder.overlay;

import com.codex.wayfinder.ui.Sigil;
import com.codex.wayfinder.ui.Theme;
import com.codex.wayfinder.ui.Ui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The drawing of a route, apart from where it is drawn: the gilt ribbon of the walk, the dashed
 * teal leap of a trip, the small plaque that names the next ride, the arrow at the rim of the
 * minimap and the pin for the world map. The overlays find the points; this paints them, so the
 * look can be checked without a game.
 */
public final class TrailPaint
{
	private static final Stroke UNDER = new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke LINE = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke THIN_UNDER = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke THIN = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke DASH = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f);
	private static final Stroke DASH_UNDER = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f);

	private TrailPaint()
	{
	}

	/**
	 * The walk ahead on the ground: an ink-edged gilt ribbon through the tile centres, brightest
	 * at the player's feet and fading toward the far end, with a chevron every few tiles that
	 * points the way.
	 *
	 * @param n how many of the points to use; a gap is marked by Integer.MIN_VALUE in xs
	 */
	public static void ribbon(Graphics2D g, int[] xs, int[] ys, int n, Color colour)
	{
		Theme.smooth(g);
		int base = colour.getAlpha();
		for (int i = 1; i < n; i++)
		{
			if (xs[i - 1] == Integer.MIN_VALUE || xs[i] == Integer.MIN_VALUE)
			{
				continue;
			}
			double fade = 1 - 0.65 * (i / (double) Math.max(1, n - 1));
			Line2D seg = new Line2D.Float(xs[i - 1], ys[i - 1], xs[i], ys[i]);
			g.setStroke(UNDER);
			g.setColor(Theme.alpha(Theme.INK_DEEP, (int) (150 * fade)));
			g.draw(seg);
			g.setStroke(LINE);
			g.setColor(Theme.alpha(colour, (int) (base * fade)));
			g.draw(seg);
		}
		for (int i = 2; i < n; i += 3)
		{
			if (xs[i - 1] == Integer.MIN_VALUE || xs[i] == Integer.MIN_VALUE)
			{
				continue;
			}
			double fade = 1 - 0.65 * (i / (double) Math.max(1, n - 1));
			chevron(g, (xs[i - 1] + xs[i]) / 2f, (ys[i - 1] + ys[i]) / 2f, Math.atan2(ys[i] - ys[i - 1], xs[i] - xs[i - 1]), 6f,
				Theme.alpha(Theme.mix(colour, Color.WHITE, 0.35), (int) (Math.min(255, base + 30) * fade)));
		}
	}

	/** A small open arrowhead at (x, y), pointing along the angle. */
	public static void chevron(Graphics2D g, float x, float y, double angle, float size, Color colour)
	{
		Path2D.Float p = new Path2D.Float();
		p.moveTo(x - size * Math.cos(angle - 0.7), y - size * Math.sin(angle - 0.7));
		p.lineTo(x, y);
		p.lineTo(x - size * Math.cos(angle + 0.7), y - size * Math.sin(angle + 0.7));
		g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.INK_DEEP, Math.min(170, colour.getAlpha())));
		g.draw(p);
		g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(colour);
		g.draw(p);
	}

	/** A walk on the world map: a gilt line edged in ink. */
	public static void mapWalk(Graphics2D g, int[] xs, int[] ys, int n, Color colour)
	{
		if (n < 2)
		{
			return;
		}
		Path2D.Float p = new Path2D.Float();
		p.moveTo(xs[0], ys[0]);
		for (int i = 1; i < n; i++)
		{
			p.lineTo(xs[i], ys[i]);
		}
		g.setStroke(THIN_UNDER);
		g.setColor(Theme.alpha(Theme.INK_DEEP, 190));
		g.draw(p);
		g.setStroke(THIN);
		g.setColor(colour);
		g.draw(p);
	}

	/** A trip on the world map: a dashed teal leap from where it is boarded to where it lands, with a ring at the landing. */
	public static void mapLeap(Graphics2D g, int x1, int y1, int x2, int y2, Color colour)
	{
		Line2D line = new Line2D.Float(x1, y1, x2, y2);
		g.setStroke(DASH_UNDER);
		g.setColor(Theme.alpha(Theme.INK_DEEP, 170));
		g.draw(line);
		g.setStroke(DASH);
		g.setColor(colour);
		g.draw(line);
		double len = Math.hypot(x2 - x1, y2 - y1);
		if (len > 24)
		{
			chevron(g, (float) (x1 + (x2 - x1) * 0.55), (float) (y1 + (y2 - y1) * 0.55), Math.atan2(y2 - y1, x2 - x1), 5f, colour);
		}
		ring(g, x2, y2, 4, colour);
	}

	/** A small ring: a landing, or a stop on the minimap. */
	public static void ring(Graphics2D g, int x, int y, int r, Color colour)
	{
		Ellipse2D e = new Ellipse2D.Float(x - r, y - r, r * 2, r * 2);
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		g.fill(e);
		g.setStroke(new BasicStroke(1.6f));
		g.setColor(colour);
		g.draw(e);
	}

	/** A dot of the path on the minimap. */
	public static void dot(Graphics2D g, int x, int y, Color colour)
	{
		g.setColor(Theme.alpha(Theme.INK_DEEP, Math.min(200, colour.getAlpha())));
		g.fillOval(x - 2, y - 2, 4, 4);
		g.setColor(colour);
		g.fillOval(x - 1, y - 1, 3, 3);
	}

	/** A diamond: where the next trip is boarded, on the minimap. */
	public static void diamond(Graphics2D g, int x, int y, int r, Color colour)
	{
		Path2D.Float p = new Path2D.Float();
		p.moveTo(x, y - r);
		p.lineTo(x + r, y);
		p.lineTo(x, y + r);
		p.lineTo(x - r, y);
		p.closePath();
		g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
		g.setStroke(new BasicStroke(3f));
		g.draw(p);
		g.setColor(colour);
		g.fill(p);
		g.setColor(Theme.alpha(Color.WHITE, 120));
		g.setStroke(new BasicStroke(1f));
		g.draw(p);
	}

	/**
	 * The arrow at the rim of the minimap toward a stop beyond it: a gilt dart on the ring of the
	 * map with the distance in tiles beside it.
	 *
	 * @param angle screen angle from the centre, 0 pointing right, growing clockwise
	 */
	public static void rimArrow(Graphics2D g, int cx, int cy, int radius, double angle, Color colour, String label)
	{
		Theme.smooth(g);
		float tipX = (float) (cx + Math.cos(angle) * radius);
		float tipY = (float) (cy + Math.sin(angle) * radius);
		float back = 13f;
		float wing = 7f;
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		float bx = (float) (tipX - c * back);
		float by = (float) (tipY - s * back);
		Path2D.Float dart = new Path2D.Float();
		dart.moveTo(tipX, tipY);
		dart.lineTo(bx - s * wing, by + c * wing);
		dart.lineTo((float) (tipX - c * back * 0.62), (float) (tipY - s * back * 0.62));
		dart.lineTo(bx + s * wing, by - c * wing);
		dart.closePath();
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 230));
		g.draw(dart);
		g.setPaint(new GradientPaint(bx, by, Theme.mix(colour, Theme.GOLD_DIM, 0.4), tipX, tipY, Theme.mix(colour, Color.WHITE, 0.3)));
		g.fill(dart);
		if (label != null && !label.isEmpty())
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			float lr = radius - back - 9;
			int lw = fm.stringWidth(label);
			int lx = (int) (cx + c * lr) - lw / 2;
			int ly = (int) (cy + s * lr) + fm.getAscent() / 2 - 1;
			g.setColor(Theme.alpha(Theme.INK_DEEP, 210));
			g.fill(new RoundRectangle2D.Float(lx - 3, ly - fm.getAscent() - 1, lw + 6, fm.getAscent() + 4, 6, 6));
			g.setColor(Theme.alpha(Theme.GOLD_DIM, 200));
			g.setStroke(new BasicStroke(1f));
			g.draw(new RoundRectangle2D.Float(lx - 3, ly - fm.getAscent() - 1, lw + 6, fm.getAscent() + 4, 6, 6));
			Ui.shadowed(g, label, lx, ly, Theme.GOLD_BRIGHT);
		}
	}

	/**
	 * A small ink plaque with a gold edge naming something on the ground or the map, centred on
	 * x with its point at y: "Fairy ring CKS", "Spirit tree", "Varrock".
	 */
	public static void chip(Graphics2D g, String text, int x, int y, Color accent)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(text) + 18;
		int h = fm.getHeight() + 6;
		int left = x - w / 2;
		int top = y - h - 5;
		RoundRectangle2D box = new RoundRectangle2D.Float(left, top, w, h, 8, 8);
		Path2D.Float tail = new Path2D.Float();
		tail.moveTo(x - 4, top + h - 0.5f);
		tail.lineTo(x, y);
		tail.lineTo(x + 4, top + h - 0.5f);
		tail.closePath();
		g.setPaint(new GradientPaint(0, top, Theme.alpha(Theme.CARD_TOP, 236), 0, top + h, Theme.alpha(Theme.INK_DEEP, 236)));
		g.fill(box);
		g.fill(tail);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 170));
		g.draw(box);
		g.setColor(accent);
		g.fillOval(left + 6, top + h / 2 - 2, 5, 5);
		Ui.shadowed(g, text, left + 13, top + 3 + fm.getAscent(), Theme.GOLD_BRIGHT);
	}

	/** An outlined tile: the place itself, in gold. */
	public static void tile(Graphics2D g, java.awt.Shape poly, Color colour)
	{
		g.setStroke(new BasicStroke(3.5f));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 150));
		g.draw(poly);
		g.setStroke(new BasicStroke(2f));
		g.setColor(colour);
		g.draw(poly);
		g.setColor(Theme.alpha(colour, Math.min(55, colour.getAlpha() / 3)));
		g.fill(poly);
	}

	/** The pin that marks the place on the world map: a gilt drop with the Wayfinder rose in its head. */
	public static BufferedImage pin()
	{
		int w = 26;
		int h = 34;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		float cx = w / 2f;
		float r = 11f;
		float cy = r + 1.5f;
		Path2D.Float drop = new Path2D.Float();
		drop.moveTo(cx, h - 1.5f);
		drop.curveTo(cx - 3, h - 9, cx - r, cy + 6, cx - r, cy);
		drop.curveTo(cx - r, cy - r * 0.6f, cx - r * 0.6f, cy - r, cx, cy - r);
		drop.curveTo(cx + r * 0.6f, cy - r, cx + r, cy - r * 0.6f, cx + r, cy);
		drop.curveTo(cx + r, cy + 6, cx + 3, h - 9, cx, h - 1.5f);
		drop.closePath();
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, h, Theme.INK_DEEP));
		g.fill(drop);
		g.setStroke(new BasicStroke(1.6f));
		g.setPaint(new GradientPaint(0, 0, Theme.GOLD_BRIGHT, 0, h, Theme.GOLD_DIM));
		g.draw(drop);
		Sigil.paint(g, cx - 8.5f, cy - 8.5f, 17);
		g.dispose();
		return image;
	}
}
