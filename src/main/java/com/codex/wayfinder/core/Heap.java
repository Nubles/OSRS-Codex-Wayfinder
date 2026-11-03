package com.codex.wayfinder.core;

import java.util.Arrays;

/** A min-heap of (cost, tile) pairs packed in longs, so the search makes no garbage per tile. */
final class Heap
{
	private long[] items = new long[1 << 12];
	private int size;

	void push(int cost, int tile)
	{
		if (size == items.length)
		{
			items = Arrays.copyOf(items, size * 2);
		}
		long v = ((long) cost << 32) | (tile & 0xFFFFFFFFL);
		int i = size++;
		while (i > 0)
		{
			int parent = (i - 1) >>> 1;
			if (items[parent] <= v)
			{
				break;
			}
			items[i] = items[parent];
			i = parent;
		}
		items[i] = v;
	}

	boolean isEmpty()
	{
		return size == 0;
	}

	/** Removes the cheapest; read it with {@link #cost} and {@link #tile}. */
	long pop()
	{
		long top = items[0];
		long last = items[--size];
		int i = 0;
		int half = size >>> 1;
		while (i < half)
		{
			int child = 2 * i + 1;
			if (child + 1 < size && items[child + 1] < items[child])
			{
				child++;
			}
			if (last <= items[child])
			{
				break;
			}
			items[i] = items[child];
			i = child;
		}
		if (size > 0)
		{
			items[i] = last;
		}
		return top;
	}

	static int cost(long v)
	{
		return (int) (v >>> 32);
	}

	static int tile(long v)
	{
		return (int) v;
	}
}
