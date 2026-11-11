package com.codex.wayfinder.core;

/** Real routes from the shared test atlas, for tests outside this package. */
public final class Samples
{
	private Samples()
	{
	}

	/** On foot from Lumbridge to Varrock square, with nothing carried. */
	public static Route walkToVarrock()
	{
		return World.ROUTER.route(Pt.of(3222, 3218, 0), Pt.of(3213, 3424, 0),
			Account.builder().known(true).questsKnown(true).bankKnown(true).build(), new Router.Options());
	}
}
