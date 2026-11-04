package com.codex.wayfinder.overlay;

/**
 * Where a point of the world falls on the minimap, worked out the way RuneLite's
 * Perspective.localToMinimap does it but with no limit on distance, so a stop far beyond the
 * map's rim still gives a direction for the arrow.
 */
final class MinimapMath
{
	/** Local units in one tile. */
	static final int TILE = 128;
	/** Camera yaw runs 0 to 16383 for a full turn (Perspective.UNIT14). */
	private static final double UNIT = 2 * Math.PI / 16384;

	private MinimapMath()
	{
	}

	/**
	 * The offset in pixels from the minimap centre to a point dx, dy local units from the camera's
	 * focus (east and north positive), for a minimap of zoom pixels per tile and the camera's yaw.
	 * Screen y grows downward, so north is up when the yaw is 0.
	 */
	static double[] offset(double dx, double dy, double zoom, int yaw)
	{
		double x = dx * zoom / TILE;
		double y = dy * zoom / TILE;
		double a = (yaw & 0x3fff) * UNIT;
		double sin = Math.sin(a);
		double cos = Math.cos(a);
		return new double[]{cos * x + sin * y, sin * x - cos * y};
	}
}
