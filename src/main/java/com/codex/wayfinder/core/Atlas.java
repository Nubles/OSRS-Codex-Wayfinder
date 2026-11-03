package com.codex.wayfinder.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import net.runelite.api.gameval.VarbitID;

/**
 * Every way of travelling the game offers, read once from the travel tables bundled with the
 * plugin (from the Shortest Path plugin by Skretzo, BSD 2-Clause), with the walking map, the
 * named places to travel to, and the rules for who may use each.
 */
public final class Atlas
{
	/** Fairy Tale II progress at which the fairy queen lets you use the rings (Shortest Path's rule). */
	static final int FAIRY_RING_PROGRESS = 39;

	private final List<Transport> transports;
	private final Map<Integer, int[]> originIndex;
	private final List<Transport> teleports;
	private final List<Place> places;
	private final Items items;
	private final Map<Integer, String> treePatches;
	private final int[] varbitIds;
	private final int[] varpIds;
	private final List<String> quests;
	private final CollisionMap map;

	private Atlas(List<Transport> transports, List<Place> places, Items items, Map<Integer, String> treePatches, CollisionMap map)
	{
		this.transports = Collections.unmodifiableList(transports);
		this.places = Collections.unmodifiableList(places);
		this.items = items;
		this.treePatches = treePatches;
		this.map = map;
		Map<Integer, List<Integer>> origins = new HashMap<>();
		List<Transport> tele = new ArrayList<>();
		TreeSet<Integer> vb = new TreeSet<>();
		TreeSet<Integer> vp = new TreeSet<>();
		TreeSet<String> qs = new TreeSet<>();
		for (Transport t : transports)
		{
			if (t.getOrigin() == Pt.NONE)
			{
				tele.add(t);
			}
			else
			{
				for (int tile : boarding(map, t.getOrigin()))
				{
					origins.computeIfAbsent(tile, k -> new ArrayList<>()).add(t.getIndex());
				}
			}
			t.getNeed().getVarbits().forEach(v -> vb.add(v.getId()));
			t.getNeed().getVarps().forEach(v -> vp.add(v.getId()));
			qs.addAll(Arrays.asList(t.getNeed().getQuests()));
		}
		vb.add(VarbitID.FAIRY2_QUEENCURE_QUEST);
		vb.add(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
		this.teleports = Collections.unmodifiableList(tele);
		this.originIndex = new HashMap<>();
		origins.forEach((k, v) -> originIndex.put(k, v.stream().mapToInt(Integer::intValue).toArray()));
		this.varbitIds = vb.stream().mapToInt(Integer::intValue).toArray();
		this.varpIds = vp.stream().mapToInt(Integer::intValue).toArray();
		this.quests = Collections.unmodifiableList(new ArrayList<>(qs));
	}

	/**
	 * The tiles a trip can be taken from: its own tile, or when that is the object itself (a
	 * fairy ring, a booth, a tree), the open tiles around it.
	 */
	public static int[] boarding(CollisionMap map, int tile)
	{
		if (map == null)
		{
			return new int[]{tile};
		}
		int x = Pt.x(tile);
		int y = Pt.y(tile);
		int z = Pt.plane(tile);
		if (map.walkable(x, y, z))
		{
			return new int[]{tile};
		}
		int[] out = new int[9];
		int n = 0;
		out[n++] = tile;
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				if ((dx != 0 || dy != 0) && map.walkable(x + dx, y + dy, z))
				{
					out[n++] = Pt.of(x + dx, y + dy, z);
				}
			}
		}
		return java.util.Arrays.copyOf(out, n);
	}

	/** Loads the bundled tables and map from the plugin's resources. */
	public static Atlas load()
	{
		Function<String, String> res = name ->
		{
			InputStream in = Atlas.class.getResourceAsStream("/com/codex/wayfinder/" + name);
			if (in == null)
			{
				return null;
			}
			return read(in);
		};
		InputStream zip = Atlas.class.getResourceAsStream("/com/codex/wayfinder/collision-map.zip");
		return build(res, zip == null ? null : CollisionMap.read(zip));
	}

	static String read(InputStream in)
	{
		try (InputStream s = in)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int n;
			while ((n = s.read(buffer)) > 0)
			{
				out.write(buffer, 0, n);
			}
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	/** Builds the atlas from a resource reader, so tests can feed small tables. */
	public static Atlas build(Function<String, String> resources, CollisionMap map)
	{
		String variations = resources.apply("item-variations.tsv");
		Items items = variations == null ? Items.NONE : Items.parse(variations);
		List<Transport> all = new ArrayList<>();
		for (Kind kind : Kind.values())
		{
			String table = resources.apply("transports/" + kind.getTable() + ".tsv");
			if (table != null)
			{
				parseTable(kind, table, items, all);
			}
		}
		Map<Integer, String> trees = new HashMap<>();
		String patches = resources.apply("spirit-tree-patches.tsv");
		Map<String, String> treeByName = parseTreePatches(patches);
		for (Transport t : all)
		{
			if (t.getKind() == Kind.SPIRIT_TREE)
			{
				String patch = treeByName.get(t.place());
				if (patch != null)
				{
					trees.put(t.getIndex(), patch);
				}
			}
		}
		List<Place> places = Place.gather(all, resources);
		return new Atlas(all, places, items, trees, map);
	}

	/** Patch name to its line "region varbit", keyed by the patch name the spirit tree table uses. */
	static Map<String, String> parseTreePatches(String tsv)
	{
		Map<String, String> out = new LinkedHashMap<>();
		if (tsv == null)
		{
			return out;
		}
		for (String line : tsv.split("\r?\n"))
		{
			if (line.isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			String[] c = line.split("\t");
			if (c.length >= 3)
			{
				out.put(c[0], c[1] + " " + c[2]);
			}
		}
		return out;
	}

	/** The planted spirit trees: patch name to "region varbit", for reading Time Tracking's record. */
	public static Map<String, String> treePatches()
	{
		InputStream in = Atlas.class.getResourceAsStream("/com/codex/wayfinder/spirit-tree-patches.tsv");
		return in == null ? Collections.emptyMap() : parseTreePatches(read(in));
	}

	static void parseTable(Kind kind, String tsv, Items items, List<Transport> out)
	{
		String[] lines = tsv.split("\r?\n");
		Map<String, Integer> cols = null;
		List<String[]> origins = new ArrayList<>();
		List<String[]> destinations = new ArrayList<>();
		for (String line : lines)
		{
			if (line.startsWith("#"))
			{
				if (cols == null)
				{
					cols = header(line.substring(1));
				}
				continue;
			}
			if (cols == null || line.trim().isEmpty())
			{
				continue;
			}
			String[] c = line.split("\t", -1);
			int origin = Pt.parse(cell(c, cols, "origin"));
			int dest = Pt.parse(cell(c, cols, "destination"));
			if (Pt.inHouse(origin) || Pt.inHouse(dest))
			{
				continue;
			}
			if (origin == Pt.NONE && dest == Pt.NONE)
			{
				continue;
			}
			if (origin != Pt.NONE && dest != Pt.NONE)
			{
				if (origin != dest)
				{
					out.add(make(out.size(), kind, origin, dest, c, cols, items, null));
				}
			}
			else if (origin == Pt.NONE && kind.isTeleport())
			{
				out.add(make(out.size(), kind, Pt.NONE, dest, c, cols, items, null));
			}
			else if (origin != Pt.NONE)
			{
				origins.add(c);
			}
			else
			{
				destinations.add(c);
			}
		}
		if (cols == null)
		{
			return;
		}
		// A network: from any boarding spot to any listed stop that is not where you stand.
		for (String[] o : origins)
		{
			int origin = Pt.parse(cell(o, cols, "origin"));
			Need from = need(o, cols, items);
			for (String[] d : destinations)
			{
				int dest = Pt.parse(cell(d, cols, "destination"));
				if (Pt.flatDistance(origin, dest) <= kind.getRadius())
				{
					continue;
				}
				out.add(make(out.size(), kind, origin, dest, d, cols, items, from));
			}
		}
	}

	private static Transport make(int index, Kind kind, int origin, int dest, String[] c, Map<String, Integer> cols,
		Items items, Need extra)
	{
		Need need = need(c, cols, items);
		if (extra != null)
		{
			need = extra.and(need);
		}
		String label = cell(c, cols, "display info").trim();
		String action = cell(c, cols, "menuoption menutarget objectid").trim().replaceAll("\\s+\\d+$", "");
		int ticks = number(cell(c, cols, "duration"), 0);
		boolean consumable = "T".equalsIgnoreCase(cell(c, cols, "consumable").trim());
		int wild = number(cell(c, cols, "wilderness level"), kind.isTeleport() ? 20 : 0);
		return new Transport(index, kind, origin, dest, label, action, ticks, need, consumable, wild);
	}

	private static Need need(String[] c, Map<String, Integer> cols, Items items)
	{
		return Need.parse(cell(c, cols, "skills"), cell(c, cols, "items"), cell(c, cols, "quests"),
			cell(c, cols, "varbits"), cell(c, cols, "varplayers"), items);
	}

	private static int number(String s, int fallback)
	{
		try
		{
			return s == null || s.trim().isEmpty() ? fallback : Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	private static Map<String, Integer> header(String line)
	{
		Map<String, Integer> cols = new HashMap<>();
		String[] names = line.split("\t", -1);
		for (int i = 0; i < names.length; i++)
		{
			cols.put(names[i].trim().toLowerCase(Locale.ROOT), i);
		}
		return cols.containsKey("destination") ? cols : null;
	}

	private static String cell(String[] c, Map<String, Integer> cols, String name)
	{
		Integer i = cols.get(name);
		return i == null || i >= c.length ? "" : c[i];
	}

	public List<Transport> getTransports()
	{
		return transports;
	}

	public Transport get(int index)
	{
		return transports.get(index);
	}

	/** Trips that leave from this tile, as indexes. */
	public int[] from(int tile)
	{
		int[] t = originIndex.get(tile);
		return t == null ? new int[0] : t;
	}

	public List<Transport> getTeleports()
	{
		return teleports;
	}

	public List<Place> getPlaces()
	{
		return places;
	}

	public Items getItems()
	{
		return items;
	}

	public CollisionMap getMap()
	{
		return map;
	}

	/** The varbits the tables test, to read from the game. */
	public int[] varbitIds()
	{
		return varbitIds.clone();
	}

	public int[] varpIds()
	{
		return varpIds.clone();
	}

	/** Every quest any trip needs, by the name the tables use. */
	public List<String> quests()
	{
		return quests;
	}

	/** The planted tree a spirit tree trip lands at, or null. */
	public String treePatch(Transport t)
	{
		return treePatches.get(t.getIndex());
	}

	/**
	 * Whether the account may take this trip at all, items aside: its levels, quests and unlocks,
	 * and the rules the tables leave to the plugin (the networks' own quests, a fairy ring's staff,
	 * a planted spirit tree that has grown).
	 */
	public boolean unlocked(Transport t, Account a)
	{
		return networkOpen(t.getKind(), a) && t.getNeed().unlocked(a) && treeOk(t, a);
	}

	/** Whether this account may take the trip now with what it carries, or with what it owns. */
	public boolean usable(Transport t, Account a, Need.Reach reach)
	{
		return unlocked(t, a) && t.getNeed().itemsMet(a, reach, false) && staffOk(t, a, reach);
	}

	private boolean treeOk(Transport t, Account a)
	{
		String patch = treePatches.get(t.getIndex());
		return patch == null || a.treeGrown(patch);
	}

	/** The quest or progress a whole network asks before any trip on it. */
	public static boolean networkOpen(Kind kind, Account a)
	{
		switch (kind)
		{
			case FAIRY_RING:
				return a.varbit(VarbitID.FAIRY2_QUEENCURE_QUEST) > FAIRY_RING_PROGRESS || (!a.isKnown());
			case GLIDER:
				return quest(a, "The Grand Tree");
			case SPIRIT_TREE:
				return quest(a, "Tree Gnome Village");
			case MUSHTREE:
				return quest(a, "Bone Voyage");
			default:
				return true;
		}
	}

	/** The words for what opens a whole network, for the atlas. */
	public static String networkNeed(Kind kind)
	{
		switch (kind)
		{
			case FAIRY_RING:
				return "Fairy Tale II started, to the point you may use the rings";
			case GLIDER:
				return "The Grand Tree";
			case SPIRIT_TREE:
				return "Tree Gnome Village";
			case MUSHTREE:
				return "Bone Voyage";
			default:
				return null;
		}
	}

	private static boolean quest(Account a, String name)
	{
		return !a.isQuestsKnown() || a.questDone(name);
	}

	private boolean staffOk(Transport t, Account a, Need.Reach reach)
	{
		if (t.getKind() != Kind.FAIRY_RING || a.varbit(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) == 1)
		{
			return true;
		}
		int[] staves = items.ids("DRAMEN_STAFF");
		if (staves == null)
		{
			return true;
		}
		for (int id : staves)
		{
			if (reach == Need.Reach.CARRIED ? a.carried(id) > 0 : a.owned(id) > 0)
			{
				return true;
			}
		}
		return reach == Need.Reach.OWNED && !a.isBankKnown();
	}

	/** Whether this account needs a staff for the rings; false once the Lumbridge elite diary is done. */
	public boolean needsStaff(Account a)
	{
		return a.varbit(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) != 1;
	}
}
