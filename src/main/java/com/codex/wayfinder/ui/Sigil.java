package com.codex.wayfinder.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The Wayfinder mark: a compass rose in aged gold, its north point lit teal, over a faint ring
 * like the bezel of a traveller's compass.
 */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float c = size / 2f;
		float cx = x + c;
		float cy = y + c;
		float r = c * 0.92f;

		// The bezel.
		g.setStroke(new BasicStroke(Math.max(0.8f, size / 18f)));
		g.setPaint(new GradientPaint(x, y, Theme.GOLD_BRIGHT, x, y + size, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		g.setColor(Theme.alpha(Theme.GOLD, 60));
		float r2 = r * 0.72f;
		g.draw(new Ellipse2D.Float(cx - r2, cy - r2, r2 * 2, r2 * 2));

		// The four lesser points, set between the great ones.
		float small = r * 0.62f;
		float waist = r * 0.13f;
		for (int i = 0; i < 4; i++)
		{
			double a = Math.PI / 4 + i * Math.PI / 2;
			g.setColor(Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.15));
			g.fill(point(cx, cy, a, small, waist));
		}

		// The four great points, each half light and half shade like an engraved rose.
		float big = r * 1.02f;
		float bw = r * 0.2f;
		for (int i = 0; i < 4; i++)
		{
			double a = -Math.PI / 2 + i * Math.PI / 2;
			boolean north = i == 0;
			Color light = north ? Theme.mix(Theme.TEAL, Color.WHITE, 0.25) : Theme.GOLD_BRIGHT;
			Color shade = north ? Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.35) : Theme.GOLD_DIM;
			Path2D[] halves = halves(cx, cy, a, big, bw);
			g.setColor(light);
			g.fill(halves[0]);
			g.setColor(shade);
			g.fill(halves[1]);
		}

		// The hub.
		float hub = Math.max(1.2f, r * 0.16f);
		g.setPaint(new RadialGradientPaint(cx - hub * 0.3f, cy - hub * 0.3f, hub * 1.6f, new float[]{0, 1},
			new Color[]{Theme.GOLD_BRIGHT, Theme.GOLD_DIM}));
		g.fill(new Ellipse2D.Float(cx - hub, cy - hub, hub * 2, hub * 2));
		g.setStroke(new BasicStroke(Math.max(0.4f, size / 48f)));
		g.setColor(Theme.INK_DEEP);
		g.draw(new Ellipse2D.Float(cx - hub, cy - hub, hub * 2, hub * 2));
	}

	private static Path2D point(float cx, float cy, double angle, float length, float waist)
	{
		double tx = cx + Math.cos(angle) * length;
		double ty = cy + Math.sin(angle) * length;
		double lx = cx + Math.cos(angle - Math.PI / 2) * waist;
		double ly = cy + Math.sin(angle - Math.PI / 2) * waist;
		double rx = cx + Math.cos(angle + Math.PI / 2) * waist;
		double ry = cy + Math.sin(angle + Math.PI / 2) * waist;
		Path2D p = new Path2D.Float();
		p.moveTo(tx, ty);
		p.lineTo(lx, ly);
		p.lineTo(cx, cy);
		p.lineTo(rx, ry);
		p.closePath();
		return p;
	}

	private static Path2D[] halves(float cx, float cy, double angle, float length, float waist)
	{
		double tx = cx + Math.cos(angle) * length;
		double ty = cy + Math.sin(angle) * length;
		double lx = cx + Math.cos(angle - Math.PI / 2) * waist;
		double ly = cy + Math.sin(angle - Math.PI / 2) * waist;
		double rx = cx + Math.cos(angle + Math.PI / 2) * waist;
		double ry = cy + Math.sin(angle + Math.PI / 2) * waist;
		Path2D a = new Path2D.Float();
		a.moveTo(tx, ty);
		a.lineTo(lx, ly);
		a.lineTo(cx, cy);
		a.closePath();
		Path2D b = new Path2D.Float();
		b.moveTo(tx, ty);
		b.lineTo(rx, ry);
		b.lineTo(cx, cy);
		b.closePath();
		return new Path2D[]{a, b};
	}
}
