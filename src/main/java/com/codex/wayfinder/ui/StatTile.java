package com.codex.wayfinder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A small engraved tile: a quiet label over a value, with a lamp that is lit when the source is. */
public class StatTile extends JComponent
{
	static final int HEIGHT = 38;

	private final String label;
	private final String value;
	private final boolean live;

	public StatTile(String label, String value, boolean live)
	{
		this.label = label;
		this.value = value;
		this.live = live;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setToolTipText(label + ": " + value);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.55), 0, h, Theme.INK_DEEP));
		g.fill(shape);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(shape);

		Color lamp = live ? Theme.GREEN : Theme.TEXT_FAINT;
		if (live)
		{
			g.setColor(Theme.alpha(lamp, 60));
			g.fill(new Ellipse2D.Float(w - 13, 5, 8, 8));
		}
		g.setColor(lamp);
		g.fill(new Ellipse2D.Float(w - 11, 7, 4, 4));

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(label.toUpperCase(), small, w - 22), 7, 14, Theme.TEXT_FAINT);
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(value, body, w - 12), 7, 30, live ? Theme.TEXT : Theme.TEXT_MUTED);
		g.dispose();
	}
}
