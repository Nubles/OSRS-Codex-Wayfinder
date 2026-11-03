package com.codex.wayfinder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexWayfinderPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexWayfinderPlugin.class);
		RuneLite.main(args);
	}
}
