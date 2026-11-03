package com.codex.wayfinder.core;

import java.util.Collections;
import java.util.List;

/** A planned way from one tile to another: its steps and how long they take altogether. */
public final class Route
{
	private final int start;
	private final int target;
	private final List<Step> steps;
	private final int seconds;
	private final boolean found;
	private final int expanded;

	Route(int start, int target, List<Step> steps, int seconds, boolean found, int expanded)
	{
		this.start = start;
		this.target = target;
		this.steps = Collections.unmodifiableList(steps);
		this.seconds = seconds;
		this.found = found;
		this.expanded = expanded;
	}

	static Route none(int start, int target, int expanded)
	{
		return new Route(start, target, Collections.emptyList(), -1, false, expanded);
	}

	public int getStart()
	{
		return start;
	}

	public int getTarget()
	{
		return target;
	}

	public List<Step> getSteps()
	{
		return steps;
	}

	public int getSeconds()
	{
		return seconds;
	}

	public boolean isFound()
	{
		return found;
	}

	public int getExpanded()
	{
		return expanded;
	}

	/** The trips only, the steps a player has to choose. */
	public long trips()
	{
		return steps.stream().filter(s -> s.getType() == Step.Type.TRIP).count();
	}

	/** Whether any step uses an item that is in the bank, not the pack. */
	public boolean needsBank()
	{
		return steps.stream().anyMatch(Step::isFromBank);
	}

	/** The first trip, or null when the route is all walking. */
	public Step firstTrip()
	{
		return steps.stream().filter(s -> s.getType() == Step.Type.TRIP).findFirst().orElse(null);
	}
}
