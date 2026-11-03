package com.codex.wayfinder.core;

/** The real atlas, loaded once for every test. */
final class World
{
	static final Atlas ATLAS;
	static final Router ROUTER;

	static
	{
		long t = System.currentTimeMillis();
		ATLAS = Atlas.load();
		ROUTER = new Router(ATLAS);
		System.out.println("atlas " + ATLAS.getTransports().size() + " trips, " + ATLAS.getPlaces().size() + " places in "
			+ (System.currentTimeMillis() - t) + "ms");
	}

	private World()
	{
	}
}
