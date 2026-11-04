package com.codex.wayfinder;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(CodexWayfinderConfig.GROUP)
public interface CodexWayfinderConfig extends Config
{
	String GROUP = "codexwayfinder";

	@ConfigSection(name = "Planning", description = "What a route may use", position = 0)
	String planning = "planning";

	@ConfigSection(name = "In game", description = "What Wayfinder shows while you travel", position = 1)
	String inGame = "inGame";

	@ConfigSection(name = "Field notes", description = "An optional local record for checking Wayfinder's guesses", position = 2, closedByDefault = true)
	String notes = "notes";

	@ConfigItem(keyName = "planWithBank", name = "Plan with your bank", position = 0, section = planning,
		description = "Let routes use teleports that are in your bank, marked as 'from your bank'. Off: only what you carry or wear")
	default boolean planWithBank()
	{
		return false;
	}

	@ConfigItem(keyName = "avoidWilderness", name = "Avoid the Wilderness", position = 1, section = planning,
		description = "Keep routes out of the Wilderness unless you start or end there")
	default boolean avoidWilderness()
	{
		return true;
	}

	@ConfigItem(keyName = "useCharters", name = "Charter ships", position = 2, section = planning,
		description = "Let routes use charter ships (they cost coins each trip)")
	default boolean useCharters()
	{
		return true;
	}

	@ConfigItem(keyName = "useHomeTeleport", name = "Home teleports", position = 3, section = planning,
		description = "Let routes start with a home teleport. Wayfinder cannot see its 30 minute wait")
	default boolean useHomeTeleport()
	{
		return true;
	}

	@ConfigItem(keyName = "useGrouping", name = "Grouping teleports", position = 4, section = planning,
		description = "Let routes start with a minigame grouping teleport. Wayfinder cannot see its 20 minute wait")
	default boolean useGrouping()
	{
		return false;
	}

	@ConfigItem(keyName = "showPlaque", name = "Step plaque", position = 0, section = inGame,
		description = "Show the next step of the route in a plaque in the game view")
	default boolean showPlaque()
	{
		return true;
	}

	@ConfigItem(keyName = "markBoarding", name = "Mark where to board", position = 1, section = inGame,
		description = "Outline the ring, tree or ship's tile for the next trip when it is on screen")
	default boolean markBoarding()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "markColour", name = "Mark colour", position = 2, section = inGame,
		description = "The colour of the boarding mark")
	default Color markColour()
	{
		return new Color(96, 184, 170, 200);
	}

	@ConfigItem(keyName = "pathOnGround", name = "Path on the ground", position = 3, section = inGame,
		description = "Draw the walk ahead on the ground, from your feet to the next ride or the place, with the ride named above where you board it")
	default boolean pathOnGround()
	{
		return true;
	}

	@ConfigItem(keyName = "pathOnMinimap", name = "Path on the minimap", position = 4, section = inGame,
		description = "Dot the route on the minimap, with a diamond where the next ride is boarded")
	default boolean pathOnMinimap()
	{
		return true;
	}

	@ConfigItem(keyName = "nextStopArrow", name = "Arrow to the next stop", position = 5, section = inGame,
		description = "When the next ride or the place is past the minimap's edge, point to it from the rim with the tiles left")
	default boolean nextStopArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "pathOnWorldMap", name = "Path on the world map", position = 6, section = inGame,
		description = "Draw the whole route on the world map, each ride named, and pin the place")
	default boolean pathOnWorldMap()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "pathColour", name = "Path colour", position = 7, section = inGame,
		description = "The colour of the path on the ground, the minimap and the world map")
	default Color pathColour()
	{
		return new Color(214, 178, 106, 230);
	}

	@ConfigItem(keyName = "worldMapOption", name = "World map option", position = 8, section = inGame,
		description = "Add 'Wayfinder' to the world map's right-click menu, to plan a route to any spot or clear the one you are on")
	default boolean worldMapOption()
	{
		return true;
	}

	@ConfigItem(keyName = "arrivalMessage", name = "Arrival message", position = 9, section = inGame,
		description = "A line in chat when you arrive, with how long the trip took against the plan")
	default boolean arrivalMessage()
	{
		return true;
	}

	@ConfigItem(keyName = "fieldNotes", name = "Keep field notes", position = 0, section = notes,
		description = "Write what Wayfinder reads and decides to a file in .runelite/plugin-data, to check its guesses. Nothing is sent anywhere")
	default boolean fieldNotes()
	{
		return false;
	}
}
