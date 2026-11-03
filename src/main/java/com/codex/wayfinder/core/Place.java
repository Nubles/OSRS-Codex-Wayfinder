package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** A named spot to travel to: a teleport's landing, a network stop, a bank, an altar or an anvil. */
public final class Place
{
	public enum Group
	{
		LANDING("Landing"),
		BANK("Bank"),
		ALTAR("Altar"),
		ANVIL("Anvil"),
		FAIRY_RING("Fairy ring"),
		DUNGEON("Dungeon"),
		MINIGAME("Minigame"),
		COURSE("Agility course"),
		RUNE_ALTAR("Runecrafting altar"),
		CUSTOM("Marked");

		private final String title;

		Group(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	private final String name;
	private final int tile;
	private final Group group;
	private final String code;
	private final String search;

	public Place(String name, int tile, Group group)
	{
		this(name, tile, group, "");
	}

	public Place(String name, int tile, Group group, String code)
	{
		this.name = name;
		this.tile = tile;
		this.group = group;
		this.code = code;
		this.search = (name + " " + group.getTitle() + " " + code).toLowerCase(Locale.ROOT);
	}

	/** A fairy ring's code ("CKS"), or empty. */
	public String getCode()
	{
		return code;
	}

	public String getName()
	{
		return name;
	}

	public int getTile()
	{
		return tile;
	}

	public Group getGroup()
	{
		return group;
	}

	/** The name with its kind when that helps, such as "Al Kharid bank". */
	public String title()
	{
		switch (group)
		{
			case BANK:
				return name + " bank";
			case ALTAR:
				return name + " altar";
			case ANVIL:
				return name + " anvil";
			case COURSE:
				return name.toLowerCase(Locale.ROOT).contains("course") ? name : name + " course";
			default:
				return name;
		}
	}

	/** How well this place matches what was typed: 0 for no match, higher is better. */
	public int match(String query)
	{
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty())
		{
			return 1;
		}
		String n = name.toLowerCase(Locale.ROOT);
		if (n.equals(q) || (!code.isEmpty() && code.equalsIgnoreCase(q.replace(" ", ""))))
		{
			return 100;
		}
		if (n.startsWith(q))
		{
			return 80;
		}
		for (String word : n.split("[\\s'-]+"))
		{
			if (word.startsWith(q))
			{
				return 60;
			}
		}
		if (search.contains(q))
		{
			return 40;
		}
		// Every typed word somewhere in the name: "lum bank".
		for (String part : q.split("\\s+"))
		{
			if (!search.contains(part))
			{
				return 0;
			}
		}
		return 20;
	}

	static List<Place> gather(List<Transport> transports, Function<String, String> resources)
	{
		Map<String, Place> byName = new LinkedHashMap<>();
		Map<String, String> rings = codes(resources.apply("places/fairy_rings.tsv"));
		for (Transport t : transports)
		{
			if (t.getKind() == Kind.FAIRY_RING)
			{
				ring(t, rings, byName);
				continue;
			}
			if (t.getKind().isWalking() || t.getLabel().isEmpty() || Advisor.EVENT_ONLY.contains(t.source()))
			{
				continue;
			}
			String name = t.place();
			// Charter and spirit tree stops are numbered for the menu: "4: Grand Exchange".
			name = name.replaceFirst("^[0-9A-Z]: ", "").trim();
			if (name.isEmpty() || name.length() > 40)
			{
				continue;
			}
			byName.putIfAbsent(name.toLowerCase(Locale.ROOT) + "|landing", new Place(name, t.getDestination(), Group.LANDING));
		}
		readList(resources.apply("places/bank.tsv"), Group.BANK, byName);
		readList(resources.apply("places/altar.tsv"), Group.ALTAR, byName);
		readList(resources.apply("places/anvil.tsv"), Group.ANVIL, byName);
		readGrouped(resources.apply("places/runelite.tsv"), byName);
		return new ArrayList<>(byName.values());
	}

	private static void readList(String tsv, Group group, Map<String, Place> out)
	{
		if (tsv == null)
		{
			return;
		}
		for (String line : tsv.split("\r?\n"))
		{
			if (line.startsWith("#") || line.trim().isEmpty())
			{
				continue;
			}
			String[] c = line.split("\t", -1);
			if (c.length < 2 || c[1].trim().isEmpty())
			{
				continue;
			}
			// Built or unlocked spots (a Sailing island's bank chest) carry varbit or varp columns and are
			// left out; banks behind a quest or a level (Canifis, Aldarin) stay, since the route says so.
			boolean gated = false;
			for (int i = 4; i < c.length; i++)
			{
				if (!c[i].trim().isEmpty())
				{
					gated = true;
					break;
				}
			}
			int tile = Pt.parse(c[0]);
			if (gated || tile == Pt.NONE || Pt.inHouse(tile))
			{
				continue;
			}
			String name = c[1].trim();
			out.putIfAbsent(name.toLowerCase(Locale.ROOT) + "|" + group, new Place(name, tile, group));
		}
	}

	/** Fairy ring stops, named from the wiki's list: "Canifis" with code "CKS" in the search text. */
	private static void ring(Transport t, Map<String, String> names, Map<String, Place> out)
	{
		String label = t.getLabel().trim();
		if (t.getDestination() == Pt.NONE || label.contains("-") || label.equalsIgnoreCase("ZANARIS"))
		{
			return;
		}
		String code = label.replace(" ", "").toUpperCase(Locale.ROOT);
		if (code.length() != 3)
		{
			return;
		}
		String name = names.getOrDefault(code, "Fairy ring " + code);
		out.putIfAbsent(code + "|ring", new Place(name, t.getDestination(), Group.FAIRY_RING, code));
	}

	private static Map<String, String> codes(String tsv)
	{
		Map<String, String> out = new LinkedHashMap<>();
		if (tsv == null)
		{
			return out;
		}
		for (String line : tsv.split("\r?\n"))
		{
			String[] c = line.split("\t");
			if (!line.startsWith("#") && c.length >= 2)
			{
				out.put(c[0].trim(), c[1].trim());
			}
		}
		return out;
	}

	/** Dungeons, minigames, courses and runecrafting altars from RuneLite's world map markers. */
	private static void readGrouped(String tsv, Map<String, Place> out)
	{
		if (tsv == null)
		{
			return;
		}
		for (String line : tsv.split("\r?\n"))
		{
			String[] c = line.split("\t");
			if (line.startsWith("#") || c.length < 3)
			{
				continue;
			}
			Group group;
			try
			{
				group = Group.valueOf(c[2].trim().equals("ALTAR") ? "RUNE_ALTAR" : c[2].trim());
			}
			catch (IllegalArgumentException e)
			{
				continue;
			}
			int tile = Pt.parse(c[1]);
			if (tile == Pt.NONE || Pt.inHouse(tile))
			{
				continue;
			}
			String name = c[0].trim();
			out.putIfAbsent(name.toLowerCase(Locale.ROOT) + "|" + group, new Place(name, tile, group));
		}
	}

	@Override
	public String toString()
	{
		return title();
	}
}
