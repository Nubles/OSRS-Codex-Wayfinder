package com.codex.wayfinder.ui;

import com.codex.wayfinder.arena.ArenaHunt;
import com.codex.wayfinder.core.Advisor;
import com.codex.wayfinder.core.Items;
import com.codex.wayfinder.core.Overview;
import com.codex.wayfinder.core.Paces;
import com.codex.wayfinder.core.Place;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Trip;
import java.util.Collections;
import java.util.List;

/** Everything the panel shows, built off the Swing thread and handed over whole. */
public final class View
{
	public final boolean loggedIn;
	public final boolean ready;
	public final Overview overview;
	/** Where the route goes, or null when none is set. */
	public final Place destination;
	/** The route with what is carried, or null while planning. */
	public final Route route;
	/** A quicker route with something from the bank, or null when there is none. */
	public final Route bankRoute;
	public final boolean planning;
	public final String query;
	public final List<Place> results;
	public final List<Place> recent;
	/** Ranked unlocks, or null while they are being priced. */
	public final List<Advisor.Unlock> unlocks;
	public final List<Trip> trips;
	public final Paces paces;
	public final String note;
	/** Every named place, for putting landings into words. */
	public final List<Place> places;
	public final Items items;
	/** The Mage Arena II followers, or null while no hunt is on. */
	public final List<ArenaHunt.Line> arena;

	public View(boolean loggedIn, boolean ready, Overview overview, Place destination, Route route, Route bankRoute,
		boolean planning, String query, List<Place> results, List<Place> recent, List<Advisor.Unlock> unlocks,
		List<Trip> trips, Paces paces, String note, List<Place> places, Items items, List<ArenaHunt.Line> arena)
	{
		this.loggedIn = loggedIn;
		this.ready = ready;
		this.overview = overview;
		this.destination = destination;
		this.route = route;
		this.bankRoute = bankRoute;
		this.planning = planning;
		this.query = query == null ? "" : query;
		this.results = results == null ? Collections.emptyList() : results;
		this.recent = recent == null ? Collections.emptyList() : recent;
		this.unlocks = unlocks;
		this.trips = trips == null ? Collections.emptyList() : trips;
		this.paces = paces == null ? new Paces() : paces;
		this.note = note;
		this.places = places == null ? Collections.emptyList() : places;
		this.items = items == null ? Items.NONE : items;
		this.arena = arena;
	}

	public static View empty()
	{
		return new View(false, false, null, null, null, null, false, "", null, null, Collections.emptyList(), null, null, null, null, null, null);
	}
}
