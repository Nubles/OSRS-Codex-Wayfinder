package com.codex.wayfinder.ui;

import com.codex.wayfinder.core.Advisor;
import com.codex.wayfinder.core.Format;
import com.codex.wayfinder.core.Kind;
import com.codex.wayfinder.core.Overview;
import com.codex.wayfinder.core.Place;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Step;
import com.codex.wayfinder.core.Trip;
import com.codex.wayfinder.core.Words;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Wayfinder side panel: a crest with how long the trip ahead takes, then three views. Go
 * finds a place and lays out the quickest way there with what you carry. Atlas shows every
 * network and teleport and where this account stands with each. Unlocks ranks what would save
 * the most time on the trips you really make.
 */
public class WayfinderPanel extends PluginPanel
{
	static final int WIDTH = 200;
	static final int TEXT = WIDTH - 30;
	private static final int LIST_LIMIT = 10;

	private final Actions actions;
	private final Crest crest = new Crest();
	private final JPanel tabs = new JPanel(new GridLayout(1, 0, 4, 0));
	private final SearchField search;
	private final JPanel searchRow = new JPanel(new BorderLayout());
	private final JScrollPane scroll = new JScrollPane();
	private View view = View.empty();
	private Tab tab = Tab.GO;

	public WayfinderPanel(Actions actions)
	{
		super(false);
		this.actions = actions;
		this.search = new SearchField("Where to?", actions::search);
		setLayout(new BorderLayout());
		setBackground(Theme.INK);
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(crest, BorderLayout.NORTH);
		JPanel under = new JPanel(new BorderLayout(0, 8));
		under.setOpaque(false);
		under.setBorder(new EmptyBorder(8, 8, 0, 8));
		tabs.setOpaque(false);
		under.add(tabs, BorderLayout.NORTH);
		searchRow.setOpaque(false);
		searchRow.add(search, BorderLayout.CENTER);
		under.add(searchRow, BorderLayout.SOUTH);
		top.add(under, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		add(scroll, BorderLayout.CENTER);
		rebuild(false);
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		search.setQuietly(view.query);
		rebuild(false);
	}

	/** Called on the Swing thread to switch view. */
	public void show(Tab tab)
	{
		boolean changed = this.tab != tab;
		this.tab = tab;
		rebuild(changed);
	}

	public Tab getTab()
	{
		return tab;
	}

	private void rebuild(boolean toTop)
	{
		updateCrest();
		tabs.removeAll();
		for (Tab t : Tab.values())
		{
			tabs.add(new CodexButton(t.getTitle(), t == tab, 20, () -> actions.tab(t)));
		}
		searchRow.setVisible(tab == Tab.GO && view.ready);
		int y = toTop ? 0 : scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(build());
		scroll.getVerticalScrollBar().setValue(y);
		revalidate();
		repaint();
	}

	private void updateCrest()
	{
		Route r = view.route;
		if (view.destination != null && r != null && r.isFound())
		{
			String to = "TO " + view.destination.getName().toUpperCase();
			long trips = r.trips();
			String status = trips == 0 ? "On foot all the way" : trips == 1 ? "One trip, then walk" : trips + " trips";
			if (r.needsBank())
			{
				status += ", bank first";
			}
			crest.updateText(to, clock(r.getSeconds()), r.getSeconds() < 60 ? "sec" : "min", status, null,
				r.needsBank() ? Theme.EMBER : Theme.TEAL, Theme.GOLD);
			return;
		}
		Overview ov = view.overview;
		if (!view.loggedIn || ov == null)
		{
			crest.update("YOUR ATLAS", null, "way", "ways", view.ready ? "Log in to read your travel" : "Reading the atlas...",
				null, Theme.TEXT_MUTED, Theme.GOLD);
			return;
		}
		int ready = (int) ov.count(ov.getTeleports(), Overview.State.READY);
		long nets = ov.count(ov.getNetworks(), Overview.State.READY);
		crest.update("READY TO GO", ready, "teleport", "teleports",
			nets + " of " + ov.getNetworks().size() + " networks open", nets + " networks open", Theme.TEAL, Theme.GOLD);
	}

	/** "1:12" for a minute or more, "45" for seconds. */
	static String clock(int seconds)
	{
		if (seconds < 60)
		{
			return String.valueOf(seconds);
		}
		if (seconds >= 3600)
		{
			return (seconds / 3600) + "h" + String.format("%02d", (seconds % 3600) / 60);
		}
		return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
	}

	private StackPanel build()
	{
		switch (tab)
		{
			case ATLAS:
				return atlas();
			case UNLOCKS:
				return unlocks();
			default:
				return go();
		}
	}

	// ----- Go --------------------------------------------------------------------------------

	private StackPanel go()
	{
		StackPanel list = new StackPanel(6, 8);
		if (!view.ready)
		{
			list.add(note("Reading the atlas: every teleport, ring, tree, boat and route in the game."));
			return list;
		}
		if (!view.query.trim().isEmpty())
		{
			list.add(new SectionHeader("Places", view.results.isEmpty() ? "none" : null));
			if (view.results.isEmpty())
			{
				list.add(note("No place by that name. Try a town, a bank, or right-click the world map and pick Wayfinder."));
			}
			for (Place p : view.results)
			{
				list.add(placeRow(p));
			}
			return list;
		}
		if (view.destination != null)
		{
			list.add(new SectionHeader("The way", view.planning ? "planning..." : null));
			list.add(routeCard());
			if (view.bankRoute != null && view.route != null && view.route.isFound())
			{
				list.add(bankCard());
			}
		}
		else
		{
			list.add(note(view.loggedIn
				? "Type a place above, or right-click the world map and pick Wayfinder. The quickest way there appears here, with what you carry."
				: "Log in, then type a place above. Wayfinder plans with the teleports and networks this account has unlocked."));
		}
		if (view.note != null)
		{
			list.add(note(view.note));
		}
		if (!view.recent.isEmpty())
		{
			list.space(2);
			list.add(new SectionHeader("Recent"));
			for (Place p : view.recent)
			{
				list.add(placeRow(p));
			}
		}
		return list;
	}

	private JComponent placeRow(Place p)
	{
		Card card = new Card(new BorderLayout(6, 0)).accent(groupColour(p.getGroup()));
		card.setBorder(new EmptyBorder(5, 10, 5, 8));
		card.add(Ui.label(Ui.clip(p.getName(), card.getFontMetrics(Theme.body()), TEXT - 46), Theme.body(), Theme.TEXT), BorderLayout.CENTER);
		card.add(Ui.label(tag(p), Theme.small(), Theme.TEXT_FAINT), BorderLayout.EAST);
		card.onClick(() -> actions.go(p));
		return card;
	}

	/** The short kind shown at the end of a place row: "Bank", "CKS", "RC altar". */
	static String tag(Place p)
	{
		switch (p.getGroup())
		{
			case FAIRY_RING:
				return p.getCode();
			case COURSE:
				return "Course";
			case RUNE_ALTAR:
				return "RC altar";
			default:
				return p.getGroup().getTitle();
		}
	}

	private static Color groupColour(Place.Group g)
	{
		switch (g)
		{
			case BANK:
				return Theme.GOLD;
			case ALTAR:
				return Theme.VIOLET;
			case ANVIL:
				return Theme.EMBER;
			case CUSTOM:
				return Theme.GOLD_BRIGHT;
			case FAIRY_RING:
				return Theme.GREEN;
			case DUNGEON:
				return Theme.BLOCKED;
			case RUNE_ALTAR:
				return Theme.VIOLET;
			default:
				return Theme.TEAL;
		}
	}

	private JComponent routeCard()
	{
		Route r = view.route;
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.TEAL).highlighted(true);
		StackPanel body = new StackPanel(5, 0);
		if (r == null)
		{
			body.add(Ui.wrapped("Finding the quickest way to " + view.destination.getName() + "...", Theme.small(), Theme.TEXT_MUTED, TEXT));
		}
		else if (!r.isFound())
		{
			body.add(Ui.wrapped("No way found", Theme.title(), Theme.GOLD_BRIGHT, TEXT));
			body.add(Ui.wrapped("Wayfinder could not reach " + view.destination.getName() + " from here with what this account can use. "
				+ "Inside an instance or a house, step outside first.", Theme.small(), Theme.TEXT, TEXT));
		}
		else
		{
			List<Step> steps = r.getSteps();
			for (int i = 0; i < steps.size(); i++)
			{
				Step s = steps.get(i);
				body.add(stepRow(i + 1, s, Words.goal(r.getSteps(), i, view.destination.getName()), i == 0));
			}
			if (steps.isEmpty())
			{
				body.add(Ui.wrapped("You are there.", Theme.title(), Theme.GOLD_BRIGHT, TEXT));
			}
		}
		card.add(body, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new BorderLayout(6, 0));
		buttons.setOpaque(false);
		buttons.add(new CodexButton("Plan again", false, 20, actions::replan), BorderLayout.WEST);
		buttons.add(new CodexButton("Clear", false, 20, actions::clear), BorderLayout.EAST);
		card.add(buttons, BorderLayout.SOUTH);
		return card;
	}

	private JComponent stepRow(int n, Step s, String destination, boolean now)
	{
		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setOpaque(false);
		Kind kind = s.getTransport() == null ? null : s.getTransport().getKind();
		Color c = s.isFromBank() ? Theme.EMBER : Glyph.colour(kind);
		JPanel glyph = new JPanel(new BorderLayout());
		glyph.setOpaque(false);
		glyph.add(new Glyph(String.valueOf(n), c, now, 18), BorderLayout.NORTH);
		row.add(glyph, BorderLayout.WEST);
		StackPanel text = new StackPanel(1, 0);
		String title = Words.title(s, view.places, destination);
		text.add(Ui.wrapped(title, Theme.body(), now ? Theme.GOLD_BRIGHT : Theme.TEXT, TEXT - 26));
		text.add(Ui.wrapped(Words.detail(s, view.items), Theme.small(), s.isFromBank() ? Theme.EMBER : Theme.TEXT_MUTED, TEXT - 26));
		row.add(text, BorderLayout.CENTER);
		return row;
	}

	private JComponent bankCard()
	{
		Route b = view.bankRoute;
		int faster = view.route.getSeconds() - b.getSeconds();
		Card card = new Card(new BorderLayout(0, 4)).accent(Theme.EMBER);
		StackPanel body = new StackPanel(3, 0);
		body.add(Ui.wrapped(Format.duration(faster) + " quicker from the bank", Theme.body(), Theme.GOLD_BRIGHT, TEXT));
		Step first = null;
		for (Step s : b.getSteps())
		{
			if (s.isFromBank())
			{
				first = s;
				break;
			}
		}
		String what = first == null ? "something in your bank" : first.getTransport().source();
		body.add(Ui.wrapped("Bring " + what + " next time: " + Format.duration(b.getSeconds()) + " in all.", Theme.small(), Theme.TEXT, TEXT));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	// ----- Atlas -----------------------------------------------------------------------------

	private StackPanel atlas()
	{
		StackPanel list = new StackPanel(6, 8);
		Overview ov = view.overview;
		if (ov == null)
		{
			list.add(note(view.ready ? "Log in and Wayfinder reads which teleports and networks this account can use." : "Reading the atlas..."));
			return list;
		}
		JPanel tiles = new JPanel(new GridLayout(1, 3, 5, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile("Ready", String.valueOf(ov.count(ov.getTeleports(), Overview.State.READY)), true));
		tiles.add(new StatTile("Banked", String.valueOf(ov.count(ov.getTeleports(), Overview.State.BANKED)), ov.count(ov.getTeleports(), Overview.State.BANKED) > 0));
		tiles.add(new StatTile("Open", ov.count(ov.getNetworks(), Overview.State.READY) + "/" + ov.getNetworks().size(), true));
		list.add(tiles);
		list.space(2);
		list.add(new SectionHeader("Networks"));
		for (Overview.Line l : ov.getNetworks())
		{
			list.add(line(l));
		}
		section(list, ov, Overview.State.READY, "Teleports ready");
		section(list, ov, Overview.State.BANKED, "In your bank");
		section(list, ov, Overview.State.NO_ITEM, "Unlocked, not owned");
		return list;
	}

	private void section(StackPanel list, Overview ov, Overview.State state, String title)
	{
		long n = ov.count(ov.getTeleports(), state);
		if (n == 0)
		{
			return;
		}
		list.space(2);
		list.add(new SectionHeader(title, String.valueOf(n)));
		int shown = 0;
		for (Overview.Line l : ov.getTeleports())
		{
			if (l.getState() != state)
			{
				continue;
			}
			if (shown++ >= LIST_LIMIT)
			{
				list.add(Ui.label("  and " + (n - LIST_LIMIT) + " more", Theme.small(), Theme.TEXT_FAINT));
				break;
			}
			list.add(line(l));
		}
	}

	private JComponent line(Overview.Line l)
	{
		boolean ready = l.getState() == Overview.State.READY;
		Color c = l.getState() == Overview.State.BANKED ? Theme.EMBER : Glyph.colour(l.getKind());
		Card card = new Card(new BorderLayout(7, 0)).accent(ready ? c : Theme.alpha(c, 90));
		card.setBorder(new EmptyBorder(5, 10, 5, 8));
		StackPanel text = new StackPanel(1, 0);
		text.add(Ui.label(Ui.clip(l.getTitle(), card.getFontMetrics(Theme.body()), TEXT - 30), Theme.body(), ready ? Theme.TEXT : Theme.TEXT_MUTED));
		text.add(Ui.wrapped(l.getNote(), Theme.small(), l.getState() == Overview.State.LOCKED ? Theme.TEXT_FAINT : Theme.TEXT_MUTED, TEXT - 30));
		card.add(text, BorderLayout.CENTER);
		card.add(new Lamp(ready, c), BorderLayout.EAST);
		if (!l.getStops().isEmpty())
		{
			StringBuilder tip = new StringBuilder("<html>");
			int i = 0;
			for (String s : l.getStops())
			{
				if (i++ >= 24)
				{
					tip.append("and more");
					break;
				}
				tip.append(Ui.escape(s)).append("<br>");
			}
			card.setToolTipText(tip.append("</html>").toString());
		}
		return card;
	}

	// ----- Unlocks ---------------------------------------------------------------------------

	private StackPanel unlocks()
	{
		StackPanel list = new StackPanel(6, 8);
		if (!view.loggedIn)
		{
			list.add(note("Log in and Wayfinder ranks which travel unlock would save you the most time."));
			return list;
		}
		list.add(new SectionHeader("Best next unlock", view.unlocks == null ? "pricing..." : null));
		if (view.trips.isEmpty())
		{
			list.add(note("Plan a few trips on the Go tab. Wayfinder remembers where you go and works out which "
				+ "network, quest or item would cut those trips the most."));
		}
		else if (view.unlocks == null)
		{
			list.add(note("Pricing every unlock against your " + view.trips.size() + (view.trips.size() == 1 ? " trip..." : " trips...")));
		}
		else if (view.unlocks.isEmpty())
		{
			list.add(note("Nothing within reach would make your usual trips quicker. You travel well already."));
		}
		else
		{
			int rank = 1;
			for (Advisor.Unlock u : view.unlocks)
			{
				list.add(unlockCard(rank++, u));
			}
		}
		if (!view.trips.isEmpty())
		{
			list.space(2);
			list.add(new SectionHeader("Your trips", String.valueOf(view.trips.size())));
			int shown = 0;
			for (Trip t : view.trips)
			{
				if (shown++ >= LIST_LIMIT)
				{
					break;
				}
				list.add(tripRow(t));
			}
		}
		list.space(2);
		list.add(new SectionHeader("Your pace"));
		list.add(paceCard());
		return list;
	}

	private JComponent unlockCard(int rank, Advisor.Unlock u)
	{
		Card card = new Card(new BorderLayout(7, 0)).accent(rank == 1 ? Theme.GOLD : Theme.GOLD_DIM).highlighted(rank == 1);
		JPanel glyph = new JPanel(new BorderLayout());
		glyph.setOpaque(false);
		glyph.add(new Glyph(String.valueOf(rank), Theme.GOLD, rank == 1, 18), BorderLayout.NORTH);
		card.add(glyph, BorderLayout.WEST);
		StackPanel text = new StackPanel(2, 0);
		text.add(Ui.wrapped(u.getTitle(), Theme.body(), rank == 1 ? Theme.GOLD_BRIGHT : Theme.TEXT, TEXT - 26));
		String helps = "Saves " + Format.duration(u.getSaved()) + (u.getTripsHelped() > 1 ? " over " + u.getTripsHelped() + " of your trips" : " on your trips");
		text.add(Ui.wrapped(helps, Theme.small(), Theme.GREEN, TEXT - 26));
		String how = u.getHow() == null ? "" : u.getHow();
		if (u.getExample() != null)
		{
			how += (how.isEmpty() ? "" : ". ") + "Most on " + u.getExample();
		}
		if (!how.isEmpty())
		{
			text.add(Ui.wrapped(how, Theme.small(), Theme.TEXT_MUTED, TEXT - 26));
		}
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	private JComponent tripRow(Trip t)
	{
		Card card = new Card(new BorderLayout(6, 0)).accent(Theme.TEAL);
		card.setBorder(new EmptyBorder(5, 10, 5, 8));
		card.add(Ui.label(Ui.clip(t.name == null ? "A marked spot" : t.name, card.getFontMetrics(Theme.body()), TEXT - 40), Theme.body(), Theme.TEXT), BorderLayout.CENTER);
		card.add(Ui.label(t.count + "x", Theme.small(), Theme.TEXT_FAINT), BorderLayout.EAST);
		return card;
	}

	private JComponent paceCard()
	{
		Card card = new Card(new BorderLayout(0, 4)).accent(Theme.TEAL);
		StackPanel body = new StackPanel(3, 0);
		int timed = view.paces.timedTrips();
		body.add(Ui.wrapped(timed == 0 ? "No trips timed yet" : timed + (timed == 1 ? " trip timed" : " trips timed"), Theme.body(), Theme.TEXT, TEXT));
		String walk;
		double f = view.paces.getWalkFactor();
		if (view.paces.walkSamples == 0)
		{
			walk = "Each teleport, ring or boat you take is timed from your click to your landing, and those times replace the estimates.";
		}
		else
		{
			int pct = (int) Math.round((f - 1) * 100);
			walk = "Your walking runs " + (pct <= 0 ? "as fast as a straight run" : pct + "% slower than a straight run") + ", from " + view.paces.walkSamples + " walks.";
		}
		body.add(Ui.wrapped(walk, Theme.small(), Theme.TEXT_MUTED, TEXT));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private JComponent note(String text)
	{
		Card card = new Card(new BorderLayout());
		JLabel l = Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, TEXT);
		card.add(l, BorderLayout.CENTER);
		return card;
	}
}
