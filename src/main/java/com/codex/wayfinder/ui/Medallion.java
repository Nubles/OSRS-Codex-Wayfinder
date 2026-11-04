package com.codex.wayfinder.ui;

import com.codex.wayfinder.arena.ArenaBoss;
import com.codex.wayfinder.arena.ArenaPaint;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** A Mage Arena II follower's medallion in the panel, drawn as on the world map. */
public class Medallion extends JComponent
{
	private final ArenaBoss boss;
	private final boolean done;
	private final boolean moved;

	public Medallion(ArenaBoss boss, boolean done, boolean moved, int size)
	{
		this.boss = boss;
		this.done = done;
		this.moved = moved;
		setOpaque(false);
		Dimension d = new Dimension(size, size);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		int d = Math.min(getWidth(), getHeight());
		ArenaPaint.medallion(g, 0, 0, d, boss, done, moved);
		g.dispose();
	}
}
