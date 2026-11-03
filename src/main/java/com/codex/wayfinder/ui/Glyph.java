package com.codex.wayfinder.ui;

import com.codex.wayfinder.core.Kind;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/** A step's roundel: its number on a disc in the colour of how you travel. */
public class Glyph extends JComponent
{
	private final String text;
	private final Color colour;
	private final boolean lit;

	public Glyph(String text, Color colour, boolean lit, int size)
	{
		this.text = text;
		this.colour = colour;
		this.lit = lit;
		setOpaque(false);
		Dimension d = new Dimension(size, size);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
	}

	/** The colour of a way of travelling: teal teleports, green networks, gold ships and carts. */
	public static Color colour(Kind kind)
	{
		if (kind == null)
		{
			return Theme.TEXT_MUTED;
		}
		switch (kind)
		{
			case ITEM:
			case SPELL:
			case HOME:
			case MINIGAME:
			case WHISTLE:
				return Theme.TEAL;
			case FAIRY_RING:
			case SPIRIT_TREE:
			case QUETZAL:
			case GLIDER:
			case MUSHTREE:
			case BALLOON:
				return Theme.GREEN;
			case LEVER:
			case PORTAL:
			case OBELISK:
				return Theme.VIOLET;
			default:
				return Theme.GOLD;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int d = Math.min(getWidth(), getHeight());
		Ellipse2D disc = new Ellipse2D.Float(1, 1, d - 2, d - 2);
		if (lit)
		{
			g.setPaint(new RadialGradientPaint(d * 0.4f, d * 0.32f, d * 0.75f, new float[]{0, 1},
				new Color[]{Theme.mix(colour, Color.WHITE, 0.12), Theme.mix(colour, Theme.INK_DEEP, 0.62)}));
			g.fill(disc);
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(Theme.mix(colour, Color.WHITE, 0.2));
			g.draw(disc);
		}
		else
		{
			g.setColor(Theme.INK_DEEP);
			g.fill(disc);
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 2f}, 0));
			g.setColor(Theme.alpha(colour, 140));
			g.draw(disc);
		}
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int tx = (d - fm.stringWidth(text)) / 2 + 1;
		int ty = (d + fm.getAscent()) / 2 - 2;
		Ui.shadowed(g, text, tx, ty, lit ? Theme.mix(Theme.TEXT, Color.WHITE, 0.3) : Theme.alpha(colour, 200));
		g.dispose();
	}
}
