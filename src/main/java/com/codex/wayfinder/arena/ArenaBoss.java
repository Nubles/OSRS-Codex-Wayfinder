package com.codex.wayfinder.arena;

import java.awt.Color;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;

/**
 * The three god followers of Mage Arena II. Each is tracked with Kolodion's enchanted symbol,
 * spawns at a spot of its own in the deep Wilderness, and always drops one component that
 * Kolodion needs. The ids are RuneLite's gameval names, checked against the wiki's infoboxes:
 * the first NPC id is the follower while it rises, the second the one you fight.
 */
public enum ArenaBoss
{
	ZACHARIAH("Justiciar Zachariah", "Saradomin", "Justiciar's hand", 'Z',
		new int[]{NpcID.MA2_BOSS_SARADOMIN_SPAWNING, NpcID.MA2_BOSS_SARADOMIN},
		ItemID.MA2_SARADOMIN_HEART, VarbitID.MA2_SARADOMIN_COMPONENT, new Color(112, 160, 226)),
	DERWEN("Derwen", "Guthix", "Ent's roots", 'D',
		new int[]{NpcID.MA2_BOSS_GUTHIX_SPAWNING, NpcID.MA2_BOSS_GUTHIX},
		ItemID.MA2_GUTHIX_HEART, VarbitID.MA2_GUTHIX_COMPONENT, new Color(130, 196, 108)),
	PORAZDIR("Porazdir", "Zamorak", "Demon's heart", 'P',
		new int[]{NpcID.MA2_BOSS_ZAMORAK_SPAWNING, NpcID.MA2_BOSS_ZAMORAK},
		ItemID.MA2_ZAMORAK_HEART, VarbitID.MA2_ZAMORAK_COMPONENT, new Color(204, 70, 60));

	private final String title;
	private final String god;
	private final String drop;
	private final char initial;
	private final int[] npcIds;
	private final int dropId;
	private final int handedInVarbit;
	private final Color colour;

	ArenaBoss(String title, String god, String drop, char initial, int[] npcIds, int dropId, int handedInVarbit, Color colour)
	{
		this.title = title;
		this.god = god;
		this.drop = drop;
		this.initial = initial;
		this.npcIds = npcIds;
		this.dropId = dropId;
		this.handedInVarbit = handedInVarbit;
		this.colour = colour;
	}

	public String getTitle()
	{
		return title;
	}

	public String getGod()
	{
		return god;
	}

	/** The component it always drops. */
	public String getDrop()
	{
		return drop;
	}

	public char getInitial()
	{
		return initial;
	}

	public int getDropId()
	{
		return dropId;
	}

	/**
	 * The varbit that reads 1 once its component is handed to Kolodion. The meaning is Quest
	 * Helper's (its Mage Arena II helper), not the wiki's, so it only ever adds a kill, never takes one away.
	 */
	public int getHandedInVarbit()
	{
		return handedInVarbit;
	}

	public Color getColour()
	{
		return colour;
	}

	/** The follower an NPC id belongs to, rising or fighting, or null. */
	public static ArenaBoss ofNpc(int id)
	{
		for (ArenaBoss b : values())
		{
			for (int n : b.npcIds)
			{
				if (n == id)
				{
					return b;
				}
			}
		}
		return null;
	}

	/** The follower whose component this item is, or null. */
	public static ArenaBoss ofDrop(int itemId)
	{
		for (ArenaBoss b : values())
		{
			if (b.dropId == itemId)
			{
				return b;
			}
		}
		return null;
	}
}
