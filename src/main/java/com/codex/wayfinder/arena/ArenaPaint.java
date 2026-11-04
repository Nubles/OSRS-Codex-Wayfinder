package com.codex.wayfinder.arena;

import com.codex.wayfinder.ui.Theme;
import com.codex.wayfinder.ui.Ui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The drawing of a follower: a medallion in its god's colour with its initial, ringed in gold.
 * A follower already dealt with is drawn faded with a green seal; one whose spot has probably
 * rotated gets a broken ember ring. The world map wears it on Wayfinder's pin shape.
 */
public final class ArenaPaint
{
	public static final int PIN_W = 26;
	public static final int PIN_H = 34;

	private ArenaPaint()
	{
	}

	/** The medallion in a box of side d at (x, y). */
	public static void medallion(Graphics2D g, float x, float y, float d, ArenaBoss boss, boolean done, boolean moved)
	{
		Theme.smooth(g);
		Color c = done ? Theme.mix(boss.getColour(), Theme.TEXT_FAINT, 0.6) : boss.getColour();
		Ellipse2D disc = new Ellipse2D.Float(x + 1, y + 1, d - 2, d - 2);
		g.setPaint(new RadialGradientPaint(x + d * 0.42f, y + d * 0.36f, d * 0.7f, new float[]{0, 1},
			new Color[]{Theme.mix(c, Theme.INK_DEEP, 0.25), Theme.mix(c, Theme.INK_DEEP, 0.8)}));
		g.fill(disc);
		if (moved && !done)
		{
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 2.5f}, 0));
			g.setColor(Theme.EMBER);
		}
		else
		{
			g.setStroke(new BasicStroke(1.4f));
			g.setPaint(new GradientPaint(x, y, done ? Theme.GOLD_DIM : Theme.GOLD_BRIGHT, x, y + d, Theme.GOLD_DIM));
		}
		g.draw(disc);
		g.setFont(Theme.title());
		FontMetrics fm = g.getFontMetrics();
		String s = String.valueOf(boss.getInitial());
		int tx = Math.round(x + (d - fm.stringWidth(s)) / 2f) + 1;
		int ty = Math.round(y + (d + fm.getAscent()) / 2f) - 2;
		Ui.shadowed(g, s, tx, ty, done ? Theme.TEXT_MUTED : Theme.mix(c, Color.WHITE, 0.55));
		if (done)
		{
			seal(g, x + d - 8, y + d - 8, 8);
		}
	}

	/** A small green seal with a tick, for a follower dealt with. */
	static void seal(Graphics2D g, float x, float y, float d)
	{
		Ellipse2D e = new Ellipse2D.Float(x, y, d, d);
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(x - 1, y - 1, d + 2, d + 2));
		g.setColor(Theme.GREEN);
		g.fill(e);
		Path2D.Float tick = new Path2D.Float();
		tick.moveTo(x + d * 0.24f, y + d * 0.52f);
		tick.lineTo(x + d * 0.44f, y + d * 0.72f);
		tick.lineTo(x + d * 0.78f, y + d * 0.3f);
		g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.INK_DEEP);
		g.draw(tick);
	}

	/** The world map point: Wayfinder's pin with the follower's medallion in its head. The tip is at (PIN_W / 2, PIN_H - 1). */
	public static BufferedImage pin(ArenaBoss boss, boolean done, boolean moved)
	{
		BufferedImage image = new BufferedImage(PIN_W, PIN_H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		float cx = PIN_W / 2f;
		float r = 12f;
		float cy = r + 1f;
		Path2D.Float drop = new Path2D.Float();
		drop.moveTo(cx, PIN_H - 1.5f);
		drop.curveTo(cx - 3, PIN_H - 9, cx - r, cy + 6, cx - r, cy);
		drop.curveTo(cx - r, cy - r * 0.6f, cx - r * 0.6f, cy - r, cx, cy - r);
		drop.curveTo(cx + r * 0.6f, cy - r, cx + r, cy - r * 0.6f, cx + r, cy);
		drop.curveTo(cx + r, cy + 6, cx + 3, PIN_H - 9, cx, PIN_H - 1.5f);
		drop.closePath();
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, PIN_H, Theme.INK_DEEP));
		g.fill(drop);
		g.setStroke(new BasicStroke(1.4f));
		g.setPaint(new GradientPaint(0, 0, done ? Theme.GOLD_DIM : Theme.GOLD_BRIGHT, 0, PIN_H, Theme.GOLD_DIM));
		g.draw(drop);
		medallion(g, cx - 10, cy - 10, 20, boss, done, moved);
		g.dispose();
		return image;
	}
}
