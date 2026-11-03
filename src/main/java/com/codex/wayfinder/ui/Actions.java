package com.codex.wayfinder.ui;

import com.codex.wayfinder.core.Place;

/** What the panel asks the plugin to do. */
public interface Actions
{
	void tab(Tab tab);

	/** The search box changed. */
	void search(String query);

	/** Plan a route to this place. */
	void go(Place place);

	/** Forget the current route. */
	void clear();

	/** Plan again from where the player stands now. */
	void replan();

	/** Price the unlocks again. */
	void rank();
}
