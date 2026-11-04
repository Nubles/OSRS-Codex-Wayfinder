package com.codex.wayfinder.arena;

import com.codex.wayfinder.core.Pt;
import net.runelite.api.Point;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/** The followers' pins on the world map: one per follower seen, redrawn only when its look changes. */
public final class ArenaPins
{
	private final WorldMapPointManager manager;
	private final Map<ArenaBoss, WorldMapPoint> points = new EnumMap<>(ArenaBoss.class);
	private final Map<ArenaBoss, String> looks = new EnumMap<>(ArenaBoss.class);

	public ArenaPins(WorldMapPointManager manager)
	{
		this.manager = manager;
	}

	/** Shows these followers, or clears every pin when null. */
	public void show(List<ArenaHunt.Line> lines)
	{
		if (lines == null)
		{
			clear();
			return;
		}
		for (ArenaHunt.Line l : lines)
		{
			WorldMapPoint p = points.get(l.boss);
			if (!l.isSeen())
			{
				if (p != null)
				{
					manager.remove(p);
					points.remove(l.boss);
					looks.remove(l.boss);
				}
				continue;
			}
			String look = l.tile + ":" + l.isDone() + ":" + l.moved;
			if (p == null || !look.equals(looks.get(l.boss)))
			{
				if (p != null)
				{
					manager.remove(p);
				}
				WorldPoint wp = new WorldPoint(Pt.x(l.tile), Pt.y(l.tile), Pt.plane(l.tile));
				p = new WorldMapPoint(wp, ArenaPaint.pin(l.boss, l.isDone(), l.moved));
				p.setImagePoint(new Point(ArenaPaint.PIN_W / 2, ArenaPaint.PIN_H - 1));
				p.setName(l.boss.getTitle());
				p.setJumpOnClick(true);
				p.setTarget(wp);
				manager.add(p);
				points.put(l.boss, p);
				looks.put(l.boss, look);
			}
			p.setTooltip(ArenaWords.tooltip(l));
		}
	}

	public void clear()
	{
		for (WorldMapPoint p : points.values())
		{
			manager.remove(p);
		}
		points.clear();
		looks.clear();
	}
}
