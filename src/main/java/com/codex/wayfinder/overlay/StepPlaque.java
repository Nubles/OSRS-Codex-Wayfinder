package com.codex.wayfinder.overlay;

import com.codex.wayfinder.ui.Sigil;
import com.codex.wayfinder.ui.Theme;
import com.codex.wayfinder.ui.Ui;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The step plaque in the game view: where the round is, the one thing to do next in gilt
 * letters, a line of detail, and a thin bar for how much of the round is done.
 */
public class StepPlaque extends Overlay
{
	private static final int PAD = 8;
	private static final int MIN_WIDTH = 190;
	private static final int MAX_WIDTH = 290;

	private volatile Shown shown;

	/** What the plaque shows; null hides it. */
	public static final class Shown
	{
		final String over;
		final String title;
		final String detail;
		final String clock;
		final int done;
		final int total;

		public Shown(String over, String title, String detail, String clock, int done, int total)
		{
			this.over = over;
			this.title = title;
			this.detail = detail;
			this.clock = clock;
			this.done = done;
			this.total = total;
		}
	}

	public StepPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_HIGH);
	}

	public void show(Shown s)
	{
		shown = s;
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
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		int icon = 18;
		int textX = PAD + icon + 8;
		int want = textX + Math.max(bold.stringWidth(s.title), Math.max(small.stringWidth(s.detail), small.stringWidth(s.over) + small.stringWidth(s.clock) + 14)) + PAD;
		int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, want));
		int room = width - textX - PAD;
		String[] lines = wrap(s.detail, small, room, 2);
		int height = PAD + small.getHeight() + bold.getHeight() + lines.length * small.getHeight() + 10;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 236), 0, height, Theme.alpha(Theme.INK_DEEP, 236)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);
		g.setColor(Theme.alpha(java.awt.Color.WHITE, 14));
		g.drawLine(6, 1, width - 6, 1);

		Sigil.paint(g, PAD, PAD + 2, icon);

		int y = PAD + small.getAscent();
		g.setFont(Theme.small());
		Ui.shadowed(g, Ui.clip(s.over, small, room - small.stringWidth(s.clock) - 6), textX, y, Theme.TEXT_MUTED);
		if (!s.clock.isEmpty())
		{
			Ui.shadowed(g, s.clock, width - PAD - small.stringWidth(s.clock), y, Theme.GOLD_DIM);
		}
		y += bold.getHeight();
		g.setFont(Theme.title());
		Ui.shadowed(g, Ui.clip(s.title, bold, room), textX, y, Theme.GOLD_BRIGHT);
		g.setFont(Theme.small());
		for (String line : lines)
		{
			y += small.getHeight();
			Ui.shadowed(g, line, textX, y, Theme.TEXT);
		}

		if (s.total > 0)
		{
			int barY = height - 5;
			int barW = width - 2 * PAD;
			g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
			g.fillRoundRect(PAD, barY, barW, 3, 3, 3);
			double t = Math.min(1, s.done / (double) s.total);
			int fill = (int) Math.round(barW * t);
			if (fill > 0)
			{
				g.setPaint(new GradientPaint(PAD, 0, Theme.GOLD_DIM, PAD + barW, 0, Theme.progressColor(t)));
				g.fillRoundRect(PAD, barY, fill, 3, 3, 3);
			}
		}
		return new Dimension(width, height);
	}

	static String[] wrap(String text, FontMetrics fm, int width, int maxLines)
	{
		if (text == null || text.isEmpty())
		{
			return new String[0];
		}
		java.util.List<String> out = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String trial = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(trial) > width && line.length() > 0)
			{
				out.add(line.toString());
				line = new StringBuilder(word);
				if (out.size() == maxLines)
				{
					break;
				}
			}
			else
			{
				line = new StringBuilder(trial);
			}
		}
		if (out.size() < maxLines && line.length() > 0)
		{
			out.add(line.toString());
		}
		else if (out.size() == maxLines)
		{
			String last = out.get(maxLines - 1);
			out.set(maxLines - 1, Ui.clip(last + " ...", fm, width));
		}
		return out.toArray(new String[0]);
	}
}
