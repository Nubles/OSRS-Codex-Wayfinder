package com.codex.wayfinder.ui;

/** The panel's three views. */
public enum Tab
{
	GO("Go"),
	ATLAS("Atlas"),
	UNLOCKS("Unlocks");

	private final String title;

	Tab(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}
}
