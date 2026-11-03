package com.codex.wayfinder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/** A small lamp at the end of a row: lit when the way is open, dark when it is not. */
public class Lamp extends JComponent
{
	private final boolean lit;
	private final Color colour;

	public Lamp(boolean lit, Color colour)
	{
		this.lit = lit;
		this.colour = colour;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 10));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int cy = 8;
		if (lit)
		{
			g.setColor(Theme.alpha(colour, 60));
			g.fill(new Ellipse2D.Float(1, cy - 4, 8, 8));
			g.setColor(colour);
		}
		else
		{
			g.setColor(Theme.TEXT_FAINT);
		}
		g.fill(new Ellipse2D.Float(3, cy - 2, 4, 4));
		g.dispose();
	}
}
