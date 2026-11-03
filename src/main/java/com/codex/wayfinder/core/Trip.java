package com.codex.wayfinder.core;

/**
 * A journey this player makes, remembered so Wayfinder can tell which unlock would save the
 * most time on the trips they really take. Plain fields for saving as JSON.
 */
public final class Trip
{
	public int from;
	public int to;
	public String name;
	public int count;
	public long last;

	public Trip()
	{
	}

	public Trip(int from, int to, String name, int count, long last)
	{
		this.from = from;
		this.to = to;
		this.name = name;
		this.count = count;
		this.last = last;
	}

	/** Two trips are the same when they start and end within a short run of each other. */
	public boolean same(int otherFrom, int otherTo)
	{
		return Pt.flatDistance(from, otherFrom) <= 24 && Pt.flatDistance(to, otherTo) <= 8
			&& Pt.plane(to) == Pt.plane(otherTo);
	}
}
