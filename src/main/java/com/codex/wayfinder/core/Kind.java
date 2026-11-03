package com.codex.wayfinder.core;

/**
 * The families of travel in the atlas, each read from one of the travel tables. Teleports
 * work from anywhere; the rest leave from a fixed spot. Doors, stairs and shortcuts are part
 * of walking and are never shown as a step of their own.
 */
public enum Kind
{
	DOOR("transports", "Walk", false, true, 0, 0),
	SHORTCUT("agility_shortcuts", "Shortcut", false, true, 0, 0),
	BOAT("boats", "Boat", false, false, 0, 4),
	CANOE("canoes", "Canoe", false, false, 0, 4),
	CHARTER("charter_ships", "Charter ship", false, false, 0, 4),
	SHIP("ships", "Ship", false, false, 0, 4),
	FAIRY_RING("fairy_rings", "Fairy ring", false, false, 6, 3),
	GLIDER("gnome_gliders", "Gnome glider", false, false, 6, 4),
	BALLOON("hot_air_balloons", "Balloon", false, false, 7, 4),
	CARPET("magic_carpets", "Magic carpet", false, false, 0, 4),
	MUSHTREE("magic_mushtrees", "Mushtree", false, false, 5, 2),
	MINECART("minecarts", "Minecart", false, false, 0, 3),
	QUETZAL("quetzals", "Quetzal", false, false, 5, 3),
	SPIRIT_TREE("spirit_trees", "Spirit tree", false, false, 5, 3),
	LEVER("teleportation_levers", "Lever", false, false, 0, 1),
	PORTAL("teleportation_portals", "Portal", false, false, 0, 1),
	OBELISK("wilderness_obelisks", "Obelisk", false, false, 0, 2),
	ITEM("teleportation_items", "Teleport", true, false, 0, 3),
	WHISTLE("quetzal_whistle", "Quetzal whistle", true, false, 0, 3),
	SPELL("teleportation_spells", "Spell", true, false, 0, 2),
	HOME("teleportation_spells_home", "Home teleport", true, false, 0, 1),
	MINIGAME("teleportation_minigames", "Grouping teleport", true, false, 0, 4);

	private final String table;
	private final String noun;
	private final boolean teleport;
	private final boolean walking;
	private final int radius;
	private final int overhead;

	Kind(String table, String noun, boolean teleport, boolean walking, int radius, int overhead)
	{
		this.table = table;
		this.noun = noun;
		this.teleport = teleport;
		this.walking = walking;
		this.radius = radius;
		this.overhead = overhead;
	}

	/** The travel table's file name, without ".tsv". */
	public String getTable()
	{
		return table;
	}

	public String getNoun()
	{
		return noun;
	}

	/** Usable from anywhere, so only ever the first step of a route. */
	public boolean isTeleport()
	{
		return teleport;
	}

	/** Folded into walking: doors, stairs, ladders and shortcuts. */
	public boolean isWalking()
	{
		return walking;
	}

	/**
	 * For networks dialled from any of their stops: a stop this close to where you board is the
	 * same stop, so it is not offered as a trip.
	 */
	public int getRadius()
	{
		return radius;
	}

	/**
	 * Rough seconds spent before the trip itself starts: taking out the item, the menu, a short
	 * chat. A guess until this account's own trips replace it.
	 */
	public int getOverhead()
	{
		return overhead;
	}
}
