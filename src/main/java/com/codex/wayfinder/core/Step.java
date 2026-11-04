package com.codex.wayfinder.core;

/** One step of a route: a walk between two tiles, or a trip. */
public final class Step
{
	public enum Type
	{
		WALK,
		TRIP
	}

	private final Type type;
	private final int from;
	private final int to;
	private final Transport transport;
	private final int seconds;
	private final int tiles;
	private final boolean learned;
	private final boolean fromBank;
	private final int[] path;

	Step(Type type, int from, int to, Transport transport, int seconds, int tiles, boolean learned, boolean fromBank)
	{
		this(type, from, to, transport, seconds, tiles, learned, fromBank, null);
	}

	Step(Type type, int from, int to, Transport transport, int seconds, int tiles, boolean learned, boolean fromBank, int[] path)
	{
		this.type = type;
		this.from = from;
		this.to = to;
		this.transport = transport;
		this.seconds = seconds;
		this.tiles = tiles;
		this.learned = learned;
		this.fromBank = fromBank;
		this.path = path != null ? path : new int[]{from, to};
	}

	public Type getType()
	{
		return type;
	}

	public int getFrom()
	{
		return from;
	}

	public int getTo()
	{
		return to;
	}

	/** The trip, for a trip step; null for a walk. */
	public Transport getTransport()
	{
		return transport;
	}

	public int getSeconds()
	{
		return seconds;
	}

	/** Tiles walked, for a walk step. */
	public int getTiles()
	{
		return tiles;
	}

	/** Timed from this player's own trips rather than estimated. */
	public boolean isLearned()
	{
		return learned;
	}

	/** Needs an item the player owns but is not carrying. */
	public boolean isFromBank()
	{
		return fromBank;
	}

	/**
	 * The tiles of this step in order, from its first tile to its last: every tile walked for a
	 * walk (a door or a stair joins two tiles that are not neighbours), or just the boarding and
	 * landing tiles for a trip.
	 */
	public int[] getPath()
	{
		return path;
	}
}
