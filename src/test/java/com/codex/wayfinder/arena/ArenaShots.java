package com.codex.wayfinder.arena;

import com.codex.wayfinder.core.Account;
import com.codex.wayfinder.core.Atlas;
import com.codex.wayfinder.core.Overview;
import com.codex.wayfinder.core.Paces;
import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Router;
import com.codex.wayfinder.core.Step;
import com.codex.wayfinder.core.Words;
import com.codex.wayfinder.ui.Actions;
import com.codex.wayfinder.ui.Tab;
import com.codex.wayfinder.ui.Theme;
import com.codex.wayfinder.ui.Ui;
import com.codex.wayfinder.ui.View;
import com.codex.wayfinder.ui.WayfinderPanel;
import com.codex.wayfinder.core.Place;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import net.runelite.api.gameval.ItemID;

/**
 * Paints the Mage Arena II hunt to PNGs without a game: the Go tab with the three followers
 * (one slain, one seen with a real route from the atlas, one whose spot has probably rotated)
 * and their pins on a stand-in world map. The spots are samples, not real spawn points. Not a
 * test; run by hand with -Djava.awt.headless=true and an output folder argument.
 */
public final class ArenaShots
{
	static final long NOW = 1_700_000_000_000L;

	public static void main(String[] args) throws Exception
	{
		File dir = new File(args.length > 0 ? args[0] : ".");
		dir.mkdirs();
		Atlas atlas = Atlas.load();
		Router router = new Router(atlas);
		Account account = Account.builder().known(true).questsKnown(true).bankKnown(true)
			.carry(ItemID.BURNING_AMULET_5, 1).wear(ItemID.AMULET_OF_GLORY_4).build();
		Paces paces = new Paces();
		int start = Pt.of(3094, 3491, 0);

		ArenaHunt hunt = new ArenaHunt();
		hunt.saw(ArenaBoss.ZACHARIAH, Pt.of(3214, 3812, 0), NOW - 50 * 60_000L);
		hunt.prove(ArenaBoss.ZACHARIAH, ArenaHunt.Proof.CARRIED);
		hunt.saw(ArenaBoss.DERWEN, Pt.of(3180, 3880, 0), NOW - 3 * 3600_000L);
		for (int i = 0; i < 3000; i++)
		{
			hunt.wildTick();
		}
		hunt.saw(ArenaBoss.PORAZDIR, Pt.of(3150, 3850, 0), NOW - 12 * 60_000L);
		for (int i = 0; i < 1600; i++)
		{
			hunt.wildTick();
		}
		List<ArenaHunt.Line> lines = hunt.lines(NOW);
		Router.Options o = new Router.Options();
		o.paces = paces;
		for (ArenaHunt.Line l : lines)
		{
			if (!l.isSeen() || l.isDone())
			{
				continue;
			}
			Route r = router.route(start, l.tile, account, o);
			l.routeSeconds = r.isFound() ? r.getSeconds() : -1;
			l.routeVia = ArenaWords.via(r, atlas.getPlaces());
			l.routeEta = r.isFound() ? Words.eta(r, paces) : null;
			System.out.println(l.boss.getTitle() + ": " + ArenaWords.way(l));
			for (Step s : r.getSteps())
			{
				System.out.println("  " + Words.title(s, atlas.getPlaces(), null));
			}
		}
		ImageIO.write(panel(lines, atlas, account), "png", new File(dir, "arena-panel.png"));
		ImageIO.write(map(lines), "png", new File(dir, "arena-worldmap.png"));
		lines.forEach(l -> System.out.println(ArenaWords.tooltip(l)));
	}

	static BufferedImage panel(List<ArenaHunt.Line> lines, Atlas atlas, Account account)
	{
		WayfinderPanel p = new WayfinderPanel(new Actions()
		{
			public void tab(Tab tab)
			{
			}

			public void search(String query)
			{
			}

			public void go(Place place)
			{
			}

			public void clear()
			{
			}

			public void replan()
			{
			}

			public void rank()
			{
			}
		});
		p.update(new View(true, true, Overview.build(atlas, account), null, null, null, false, "", null, Collections.emptyList(), null, null, null, null,
			atlas.getPlaces(), atlas.getItems(), lines));
		p.show(Tab.GO);
		int w = 225;
		int h = 700;
		p.setSize(w, h);
		layout(p);
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Theme.INK);
		g.fillRect(0, 0, w, h);
		p.printAll(g);
		g.dispose();
		return img;
	}

	private static void layout(Component c)
	{
		if (c instanceof Container)
		{
			((Container) c).doLayout();
			for (Component k : ((Container) c).getComponents())
			{
				layout(k);
			}
		}
		if (c instanceof JComponent)
		{
			((JComponent) c).validate();
		}
	}

	/** A stand-in for the world map over the deep Wilderness, two pixels a tile, with the pins where the game would put them. */
	static BufferedImage map(List<ArenaHunt.Line> lines)
	{
		int x0 = 2990;
		int y1 = 3960;
		int w = 520;
		int h = 360;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Theme.smooth(g);
		g.setPaint(new GradientPaint(0, 0, new Color(72, 64, 52), w, h, new Color(54, 48, 40)));
		g.fillRect(0, 0, w, h);
		// Lava and rock, roughly: a lake for the isle and a few dark patches.
		g.setColor(new Color(150, 64, 30));
		g.fillOval(w / 2 + 20, 70, 150, 110);
		g.setColor(new Color(90, 80, 64));
		g.fillOval(w / 2 + 70, 100, 50, 40);
		g.setColor(new Color(0, 0, 0, 40));
		g.setStroke(new BasicStroke(1f));
		for (int x = 0; x < w; x += 16)
		{
			g.drawLine(x, 0, x, h);
		}
		for (int y = 0; y < h; y += 16)
		{
			g.drawLine(0, y, w, y);
		}
		g.setFont(Theme.small());
		Ui.shadowed(g, "Lava Dragon Isle (stand-in)", w / 2 + 34, 196, new Color(255, 236, 190));
		ArenaHunt.Line hover = null;
		int hx = 0;
		int hy = 0;
		for (ArenaHunt.Line l : lines)
		{
			if (!l.isSeen())
			{
				continue;
			}
			int px = (Pt.x(l.tile) - x0) * 2;
			int py = (y1 - Pt.y(l.tile)) * 2;
			g.drawImage(ArenaPaint.pin(l.boss, l.isDone(), l.moved), px - ArenaPaint.PIN_W / 2, py - ArenaPaint.PIN_H + 1, null);
			if (!l.isDone() && !l.moved)
			{
				hover = l;
				hx = px;
				hy = py;
			}
		}
		if (hover != null)
		{
			// The game's own tooltip box, as RuneLite draws world map tooltips.
			String t = ArenaWords.tooltip(hover);
			int tw = g.getFontMetrics().stringWidth(t) + 8;
			int tx = Math.max(4, Math.min(w - tw - 4, hx + 10));
			int ty = hy + 6;
			g.setColor(new Color(30, 30, 30, 220));
			g.fillRect(tx, ty, tw, 18);
			g.setColor(new Color(90, 82, 69));
			g.drawRect(tx, ty, tw, 18);
			Ui.shadowed(g, t, tx + 4, ty + 13, Color.WHITE);
		}
		g.dispose();
		return img;
	}
}
