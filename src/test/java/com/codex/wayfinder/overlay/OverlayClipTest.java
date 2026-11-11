package com.codex.wayfinder.overlay;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Samples;
import com.codex.wayfinder.core.Trail;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Every Wayfinder overlay leaves RuneLite's graphics as it found them: the clip above all (a
 * null clip breaks the overlay renderer for every plugin, every frame), and the stroke, font,
 * transform and hints too. Each is drawn into an image with a clip set, through the paths that
 * clip: the world map trail over the map, the minimap trail in its round frame, the plaque.
 */
public class OverlayClipTest
{
	private static Trail trail;
	private static int start;

	@BeforeClass
	public static void route()
	{
		Route r = Samples.walkToVarrock();
		trail = Trail.of(r);
		start = r.getStart();
	}

	private static TrailShown shown()
	{
		return new TrailShown(trail, trail.ahead(start), start, "Varrock", "Walk", new Color(214, 178, 106, 230),
			new Color(96, 184, 170, 200), true, true, true, true);
	}

	private static void unchanged(Overlay overlay)
	{
		BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.translate(7, 9);
		Shape clip = new Rectangle(0, 0, 380, 280);
		g.setClip(clip);
		g.setStroke(new BasicStroke(1f));
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		Shape beforeClip = g.getClip();
		AffineTransform beforeTransform = g.getTransform();
		Font beforeFont = g.getFont();
		Object beforeHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		java.awt.Stroke beforeStroke = g.getStroke();
		java.awt.Paint beforePaint = g.getPaint();
		overlay.render(g);
		overlay.render(g);
		String name = overlay.getClass().getSimpleName();
		assertEquals(name + " clip", beforeClip, g.getClip());
		assertEquals(name + " transform", beforeTransform, g.getTransform());
		assertEquals(name + " font", beforeFont, g.getFont());
		assertEquals(name + " hint", beforeHint, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
		assertEquals(name + " stroke", beforeStroke, g.getStroke());
		assertEquals(name + " paint", beforePaint, g.getPaint());
		g.dispose();
	}

	@Test
	public void plaque()
	{
		StepPlaque p = new StepPlaque();
		p.show(new StepPlaque.Shown("TO VARROCK", "Walk to Varrock", "212 tiles, ~1m 10s", "~1m 10s", 20, 70));
		unchanged(p);
		p.show(null);
		unchanged(p);
	}

	@Test
	public void worldMapTrail()
	{
		Client client = mock(Client.class);
		Widget map = mock(Widget.class);
		when(map.getBounds()).thenReturn(new Rectangle(20, 20, 300, 200));
		when(map.isHidden()).thenReturn(false);
		when(client.getWidget(anyInt())).thenReturn(map);
		WorldMapOverlay overlay = mock(WorldMapOverlay.class);
		when(overlay.mapWorldPointToGraphicsPoint(org.mockito.ArgumentMatchers.any())).thenReturn(new Point(120, 90));
		MapTrail m = new MapTrail(client, overlay);
		m.show(shown());
		unchanged(m);
	}

	@Test
	public void minimapTrail()
	{
		Client client = mock(Client.class);
		WorldView wv = mock(WorldView.class);
		when(wv.getBaseX()).thenReturn(Pt.x(start) - 52);
		when(wv.getBaseY()).thenReturn(Pt.y(start) - 52);
		when(client.getTopLevelWorldView()).thenReturn(wv);
		Player me = mock(Player.class);
		when(me.getLocalLocation()).thenReturn(new LocalPoint(52 * 128 + 64, 52 * 128 + 64, -1));
		when(client.getLocalPlayer()).thenReturn(me);
		Widget mini = mock(Widget.class);
		when(mini.getCanvasLocation()).thenReturn(new Point(200, 20));
		when(mini.getWidth()).thenReturn(150);
		when(mini.getHeight()).thenReturn(150);
		when(client.getWidget(anyInt())).thenReturn(mini);
		when(client.getMinimapZoom()).thenReturn(4.0);
		MinimapTrail m = new MinimapTrail(client);
		m.show(shown());
		unchanged(m);
	}

	@Test
	public void groundTrailAndMark()
	{
		Client client = mock(Client.class);
		GroundTrail ground = new GroundTrail(client);
		ground.show(shown());
		unchanged(ground);
		BoardingMark mark = new BoardingMark(client);
		mark.show(start, Color.CYAN);
		unchanged(mark);
	}
}
