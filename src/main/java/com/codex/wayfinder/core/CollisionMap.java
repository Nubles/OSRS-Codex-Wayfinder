package com.codex.wayfinder.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Which edges of each tile can be walked through, for every region of the game world. The data
 * and its layout come from the Shortest Path plugin by Skretzo (BSD 2-Clause, see
 * THIRD-PARTY-NOTICES): each region is a bit set of four flags a tile, north open, east open,
 * then two wall markers this plugin does not need.
 */
public final class CollisionMap
{
	private static final int REGION = 64;
	private static final int FLAGS = 4;
	private static final int PLANE_BITS = REGION * REGION * FLAGS;
	private static final int NORTH = 0;
	private static final int EAST = 1;

	private final int minX;
	private final int minY;
	private final int width;
	private final int height;
	private final long[][] regions;
	private final byte[] planes;

	private CollisionMap(Map<Integer, byte[]> data, int minX, int minY, int maxX, int maxY)
	{
		this.minX = minX;
		this.minY = minY;
		this.width = maxX - minX + 1;
		this.height = maxY - minY + 1;
		this.regions = new long[width * height][];
		this.planes = new byte[width * height];
		for (Map.Entry<Integer, byte[]> e : data.entrySet())
		{
			int rx = e.getKey() & 0xFFFF;
			int ry = e.getKey() >>> 16;
			int index = (rx - minX) + (ry - minY) * width;
			BitSet bits = BitSet.valueOf(e.getValue());
			planes[index] = (byte) ((bits.size() + PLANE_BITS - 1) / PLANE_BITS);
			regions[index] = bits.toLongArray();
		}
	}

	/** Reads the zipped map, one entry per region named "x_y". */
	public static CollisionMap read(InputStream zipped)
	{
		Map<Integer, byte[]> data = new HashMap<>();
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = 0;
		int maxY = 0;
		try (ZipInputStream in = new ZipInputStream(zipped))
		{
			ZipEntry entry;
			byte[] buffer = new byte[8192];
			while ((entry = in.getNextEntry()) != null)
			{
				String[] n = entry.getName().split("_");
				int x = Integer.parseInt(n[0]);
				int y = Integer.parseInt(n[1]);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int read;
				while ((read = in.read(buffer)) > 0)
				{
					out.write(buffer, 0, read);
				}
				data.put(x | (y << 16), out.toByteArray());
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return new CollisionMap(data, minX, minY, maxX, maxY);
	}

	private boolean get(int x, int y, int z, int flag)
	{
		if (x < 0 || y < 0)
		{
			return false;
		}
		int rx = x / REGION - minX;
		int ry = y / REGION - minY;
		if (rx < 0 || ry < 0 || rx >= width || ry >= height)
		{
			return false;
		}
		int index = rx + ry * width;
		long[] words = regions[index];
		if (words == null || z < 0 || z >= planes[index])
		{
			return false;
		}
		int bit = (z * REGION * REGION + (y % REGION) * REGION + (x % REGION)) * FLAGS + flag;
		int word = bit >> 6;
		return word < words.length && (words[word] >>> (bit & 63) & 1L) != 0L;
	}

	public boolean n(int x, int y, int z)
	{
		return get(x, y, z, NORTH);
	}

	public boolean s(int x, int y, int z)
	{
		return get(x, y - 1, z, NORTH);
	}

	public boolean e(int x, int y, int z)
	{
		return get(x, y, z, EAST);
	}

	public boolean w(int x, int y, int z)
	{
		return get(x - 1, y, z, EAST);
	}

	public boolean walkable(int x, int y, int z)
	{
		return n(x, y, z) || s(x, y, z) || e(x, y, z) || w(x, y, z);
	}

	/**
	 * Fills {@code out} with the tiles one step from {@code p} and returns how many. A diagonal
	 * step is open only when both of its straight steps are, as in the game.
	 */
	public int neighbours(int p, int[] out)
	{
		int x = Pt.x(p);
		int y = Pt.y(p);
		int z = Pt.plane(p);
		boolean n = n(x, y, z);
		boolean s = s(x, y, z);
		boolean e = e(x, y, z);
		boolean w = w(x, y, z);
		int c = 0;
		if (n)
		{
			out[c++] = Pt.of(x, y + 1, z);
		}
		if (s)
		{
			out[c++] = Pt.of(x, y - 1, z);
		}
		if (e)
		{
			out[c++] = Pt.of(x + 1, y, z);
		}
		if (w)
		{
			out[c++] = Pt.of(x - 1, y, z);
		}
		if (n && e && e(x, y + 1, z) && n(x + 1, y, z))
		{
			out[c++] = Pt.of(x + 1, y + 1, z);
		}
		if (n && w && w(x, y + 1, z) && n(x - 1, y, z))
		{
			out[c++] = Pt.of(x - 1, y + 1, z);
		}
		if (s && e && e(x, y - 1, z) && s(x + 1, y, z))
		{
			out[c++] = Pt.of(x + 1, y - 1, z);
		}
		if (s && w && w(x, y - 1, z) && s(x - 1, y, z))
		{
			out[c++] = Pt.of(x - 1, y - 1, z);
		}
		return c;
	}
}
