package com.codex.wayfinder.overlay;

import com.codex.wayfinder.core.Pt;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** A thin outline on the tile where the next trip is boarded: the ring, the tree, the ship's gangway. */
public class BoardingMark extends Overlay
{
	private final Client client;
	private volatile int tile = Pt.NONE;
	private volatile Color colour = new Color(96, 184, 170, 200);

	public BoardingMark(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void show(int tile, Color colour)
	{
		this.tile = tile;
		this.colour = colour;
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
		int t = tile;
		if (t == Pt.NONE || client.getTopLevelWorldView() == null)
		{
			return null;
		}
		WorldPoint wp = new WorldPoint(Pt.x(t), Pt.y(t), Pt.plane(t));
		if (wp.getPlane() != client.getTopLevelWorldView().getPlane())
		{
			return null;
		}
		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
		if (lp == null)
		{
			return null;
		}
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null)
		{
			return null;
		}
		g.setStroke(new BasicStroke(2f));
		g.setColor(colour);
		g.draw(poly);
		g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(60, colour.getAlpha() / 3)));
		g.fill(poly);
		return null;
	}
}
