package com.codex.wayfinder.core;

/** One way of getting from a spot (or from anywhere) to a landing tile. */
public final class Transport
{
	private final int index;
	private final Kind kind;
	private final int origin;
	private final int destination;
	private final String label;
	private final String action;
	private final int ticks;
	private final Need need;
	private final boolean consumable;
	private final int maxWilderness;

	Transport(int index, Kind kind, int origin, int destination, String label, String action, int ticks, Need need,
		boolean consumable, int maxWilderness)
	{
		this.index = index;
		this.kind = kind;
		this.origin = origin;
		this.destination = destination;
		this.label = label;
		this.action = action;
		this.ticks = ticks;
		this.need = need;
		this.consumable = consumable;
		this.maxWilderness = maxWilderness;
	}

	public int getIndex()
	{
		return index;
	}

	public Kind getKind()
	{
		return kind;
	}

	/** Where you board, or {@link Pt#NONE} for a teleport. */
	public int getOrigin()
	{
		return origin;
	}

	public int getDestination()
	{
		return destination;
	}

	/** The table's own name for the trip, such as "Varrock Teleport: Grand Exchange" or "A I Q". */
	public String getLabel()
	{
		return label;
	}

	/** The menu option on the thing you use, such as "Travel Holgart", or empty. */
	public String getAction()
	{
		return action;
	}

	/** Game ticks the trip itself takes, from the table. */
	public int getTicks()
	{
		return ticks;
	}

	public Need getNeed()
	{
		return need;
	}

	/** Used up by the trip: a tablet or a charge-limited jewel. */
	public boolean isConsumable()
	{
		return consumable;
	}

	/** The deepest Wilderness level this teleport still works from (20 or 30), or 0 when not a teleport. */
	public int getMaxWilderness()
	{
		return maxWilderness;
	}

	/** A stable name for this trip, used for learned times and for the shared atlas. */
	public String key()
	{
		return kind.name() + ":" + (label.isEmpty() ? Pt.toString(destination) : label);
	}

	/**
	 * Where the trip goes, for people: the part after the colon of an item teleport
	 * ("Grand Exchange"), the code of a fairy ring, or the name of a stop.
	 */
	public String place()
	{
		String l = label;
		int colon = l.indexOf(": ");
		if (colon >= 0)
		{
			l = l.substring(colon + 2);
		}
		if (kind == Kind.SPELL || kind == Kind.HOME || kind == Kind.MINIGAME)
		{
			l = l.replace(" Minigame Teleport", "").replace(" Teleport", "").replace("Teleport to ", "").trim();
		}
		else if (kind == Kind.ITEM)
		{
			// "Varrock tablet" lands in Varrock: drop the item word so the town reads once.
			l = l.replaceFirst("(?i) (tablet|teleport|scroll)$", "").trim();
		}
		// Menu numbering: "2. Varrock", "4: Grand Exchange".
		return l.replaceFirst("^[0-9A-Z][.:] ", "").trim();
	}

	/** The source of the trip, for people: "Ardougne cloak", "Varrock Teleport", "Fairy ring". */
	public String source()
	{
		int colon = label.indexOf(": ");
		if ((kind == Kind.ITEM || kind == Kind.WHISTLE || kind == Kind.SPELL) && colon > 0)
		{
			return label.substring(0, colon);
		}
		if (kind == Kind.ITEM || kind == Kind.SPELL || kind == Kind.MINIGAME || kind == Kind.HOME)
		{
			return label;
		}
		return kind.getNoun();
	}

	@Override
	public String toString()
	{
		return key();
	}
}
