package com.codex.wayfinder.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Perspective;
import org.junit.Test;

public class MinimapMathTest
{
	/** RuneLite's own sums in Perspective.localToMinimap, in whole numbers. */
	private static int[] runelite(int dx, int dy, double zoomPerTile, int yaw)
	{
		double zoom = zoomPerTile / Perspective.LOCAL_TILE_SIZE;
		int x = (int) (dx * zoom);
		int y = (int) (dy * zoom);
		int angle = yaw & 0x3fff;
		int sin = Perspective.SINE14[angle];
		int cos = Perspective.COSINE14[angle];
		return new int[]{cos * x + sin * y >> 16, sin * x - cos * y >> 16};
	}

	@Test
	public void northIsUpWhenFacingNorth()
	{
		double[] o = MinimapMath.offset(0, 10 * 128, 4, 0);
		assertEquals(0, o[0], 0.01);
		assertEquals(-40, o[1], 0.01);
	}

	@Test
	public void matchesRuneLiteAtEveryTurn()
	{
		int[][] points = {{640, 0}, {0, 640}, {-1280, 384}, {2048, -2560}, {128 * 30, 128 * 7}};
		for (int yaw = 0; yaw < 16384; yaw += 512)
		{
			for (int[] p : points)
			{
				int[] want = runelite(p[0], p[1], 4.0, yaw);
				double[] got = MinimapMath.offset(p[0], p[1], 4.0, yaw);
				assertTrue("yaw " + yaw, Math.abs(want[0] - got[0]) <= 1.5 && Math.abs(want[1] - got[1]) <= 1.5);
			}
		}
	}
}
