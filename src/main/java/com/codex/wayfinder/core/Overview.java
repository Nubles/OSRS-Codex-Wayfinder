package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The atlas as the panel shows it: each network and each teleport source, and where this account stands with it. */
public final class Overview
{
	public enum State
	{
		/** Ready now, with what is carried or for a network. */
		READY,
		/** Owned, but in the bank. */
		BANKED,
		/** Unlocked, but the item is not owned. */
		NO_ITEM,
		/** A level, quest or unlock is missing. */
		LOCKED
	}

	public static final class Line
	{
		private final String title;
		private final Kind kind;
		private final State state;
		private final int open;
		private final int total;
		private final String note;
		private final Set<String> stops;

		Line(String title, Kind kind, State state, int open, int total, String note, Set<String> stops)
		{
			this.title = title;
			this.kind = kind;
			this.state = state;
			this.open = open;
			this.total = total;
			this.note = note;
			this.stops = stops;
		}

		public String getTitle()
		{
			return title;
		}

		public Kind getKind()
		{
			return kind;
		}

		public State getState()
		{
			return state;
		}

		/** Places this source can take you to now. */
		public int getOpen()
		{
			return open;
		}

		public int getTotal()
		{
			return total;
		}

		/** What is missing, or a word on the stops. */
		public String getNote()
		{
			return note;
		}

		public Set<String> getStops()
		{
			return stops;
		}
	}

	private final List<Line> networks;
	private final List<Line> teleports;

	private Overview(List<Line> networks, List<Line> teleports)
	{
		this.networks = networks;
		this.teleports = teleports;
	}

	public List<Line> getNetworks()
	{
		return networks;
	}

	public List<Line> getTeleports()
	{
		return teleports;
	}

	public long count(List<Line> lines, State state)
	{
		return lines.stream().filter(l -> l.state == state).count();
	}

	static final Kind[] NETWORKS = {Kind.FAIRY_RING, Kind.SPIRIT_TREE, Kind.QUETZAL, Kind.GLIDER, Kind.CHARTER, Kind.SHIP,
		Kind.BOAT, Kind.CANOE, Kind.BALLOON, Kind.CARPET, Kind.MINECART, Kind.MUSHTREE, Kind.OBELISK};

	public static Overview build(Atlas atlas, Account a)
	{
		List<Line> nets = new ArrayList<>();
		for (Kind k : NETWORKS)
		{
			Set<String> all = new LinkedHashSet<>();
			Set<String> open = new LinkedHashSet<>();
			boolean any = false;
			String firstMissing = null;
			for (Transport t : atlas.getTransports())
			{
				if (t.getKind() != k)
				{
					continue;
				}
				String stop = stopName(t);
				all.add(stop);
				if (atlas.usable(t, a, Need.Reach.OWNED))
				{
					any = true;
					open.add(stop);
				}
				else if (firstMissing == null)
				{
					List<String> m = t.getNeed().missingUnlocks(a, 1);
					if (!m.isEmpty() && !"an unlock".equals(m.get(0)))
					{
						firstMissing = m.get(0);
					}
				}
			}
			if (all.isEmpty())
			{
				continue;
			}
			State state;
			String note;
			if (!Atlas.networkOpen(k, a))
			{
				state = State.LOCKED;
				note = Atlas.networkNeed(k);
			}
			else if (!any)
			{
				state = State.LOCKED;
				note = k == Kind.FAIRY_RING ? "Needs a dramen or lunar staff" : firstMissing != null ? "Needs " + firstMissing : lockedNote(k, all.isEmpty() ? null : firstOf(atlas, k), atlas, a);
			}
			else
			{
				state = State.READY;
				note = open.size() + " of " + all.size() + " stops";
			}
			nets.add(new Line(title(k), k, state, open.size(), all.size(), note, open));
		}

		Map<String, List<Transport>> bySource = new LinkedHashMap<>();
		for (Transport t : atlas.getTeleports())
		{
			bySource.computeIfAbsent(t.source(), x -> new ArrayList<>()).add(t);
		}
		List<Line> tele = new ArrayList<>();
		for (Map.Entry<String, List<Transport>> e : bySource.entrySet())
		{
			int ready = 0;
			int banked = 0;
			int unlocked = 0;
			Set<String> stops = new LinkedHashSet<>();
			Set<String> allStops = new LinkedHashSet<>();
			String missing = null;
			Kind kind = e.getValue().get(0).getKind();
			for (Transport t : e.getValue())
			{
				allStops.add(t.place());
				if (atlas.usable(t, a, Need.Reach.CARRIED))
				{
					ready++;
					stops.add(t.place());
				}
				else if (atlas.usable(t, a, Need.Reach.OWNED))
				{
					banked++;
					stops.add(t.place());
				}
				else if (atlas.unlocked(t, a))
				{
					unlocked++;
				}
				else if (missing == null)
				{
					List<String> m = t.getNeed().missingUnlocks(a, 1);
					missing = m.isEmpty() ? null : m.get(0);
				}
			}
			State state;
			String note;
			if (ready > 0)
			{
				state = State.READY;
				note = readyNote(e.getKey(), kind, stops);
			}
			else if (banked > 0)
			{
				state = State.BANKED;
				note = "In your bank";
			}
			else if (unlocked > 0)
			{
				state = State.NO_ITEM;
				String words = e.getValue().get(0).getNeed().itemWords(atlas.getItems());
				note = kind == Kind.SPELL ? "Needs runes" + (words.isEmpty() ? "" : ": " + words) : "Not owned";
			}
			else
			{
				state = State.LOCKED;
				note = missing == null || "an unlock".equals(missing) ? "Not unlocked" : "Needs " + missing;
			}
			tele.add(new Line(kind == Kind.MINIGAME ? e.getKey().replace(" Minigame Teleport", "") : e.getKey(), kind, state, stops.size(), allStops.size(), note, state == State.READY || state == State.BANKED ? stops : allStops));
		}
		tele.sort(Comparator.comparing((Line l) -> l.state).thenComparing(l -> l.kind == Kind.MINIGAME).thenComparing(l -> -l.open).thenComparing(l -> l.title));
		return new Overview(nets, tele);
	}

	private static String stopName(Transport t)
	{
		String p = t.place().replaceFirst("^[0-9A-Z]: ", "");
		return p.isEmpty() ? Pt.toString(t.getDestination()) : p;
	}

	public static String title(Kind k)
	{
		switch (k)
		{
			case FAIRY_RING:
				return "Fairy rings";
			case SPIRIT_TREE:
				return "Spirit trees";
			case QUETZAL:
				return "Quetzals";
			case GLIDER:
				return "Gnome gliders";
			case CHARTER:
				return "Charter ships";
			case SHIP:
				return "Ships";
			case BOAT:
				return "Boats";
			case CANOE:
				return "Canoes";
			case BALLOON:
				return "Balloons";
			case CARPET:
				return "Magic carpets";
			case MINECART:
				return "Minecarts";
			case MUSHTREE:
				return "Mushtrees";
			case OBELISK:
				return "Obelisks";
			default:
				return k.getNoun();
		}
	}

	/** "To Camelot" says nothing a spell called Camelot Teleport does not, so name the kind then. */
	static String readyNote(String title, Kind kind, java.util.Collection<String> stops)
	{
		if (stops.size() != 1)
		{
			return stops.size() + " places";
		}
		String stop = stops.iterator().next();
		String t = title.toLowerCase(java.util.Locale.ROOT);
		if (kind == Kind.MINIGAME)
		{
			return "Grouping teleport";
		}
		if (t.contains(stop.toLowerCase(java.util.Locale.ROOT)))
		{
			return kind == Kind.SPELL ? "Spell, runes in hand" : kind == Kind.HOME ? "Home teleport" : "Ready to use";
		}
		return "To " + stop;
	}

	private static Transport firstOf(Atlas atlas, Kind k)
	{
		for (Transport t : atlas.getTransports())
		{
			if (t.getKind() == k)
			{
				return t;
			}
		}
		return null;
	}

	/**
	 * Why a network with no quest or level gate is shut. Balloon routes open one by one on the
	 * Entrana flights, each paid in logs (OSRS Wiki, "Balloon transport system"); canoes need an axe.
	 */
	static String lockedNote(Kind k, Transport sample, Atlas atlas, Account a)
	{
		if (k == Kind.BALLOON)
		{
			return "Unlock routes on Entrana with logs";
		}
		if (sample != null && sample.getNeed().hasItems() && !sample.getNeed().itemsMet(a, Need.Reach.OWNED, false))
		{
			String words = sample.getNeed().itemWords(atlas.getItems());
			if (!words.isEmpty())
			{
				return "Needs " + words;
			}
		}
		return "Not open yet";
	}
}
