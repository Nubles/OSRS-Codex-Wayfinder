package com.codex.wayfinder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** The "Where to?" box: an inked well with a gilt rim and a quiet hint while empty. */
public class SearchField extends JTextField
{
	private final String hint;
	private boolean quiet;

	public SearchField(String hint, Consumer<String> onChange)
	{
		this.hint = hint;
		setOpaque(false);
		setFont(Theme.body());
		setForeground(Theme.TEXT);
		setCaretColor(Theme.GOLD_BRIGHT);
		setSelectionColor(Theme.alpha(Theme.GOLD, 90));
		setBorder(new EmptyBorder(4, 9, 4, 9));
		setPreferredSize(new Dimension(10, 26));
		getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				fire(onChange);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				fire(onChange);
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				fire(onChange);
			}
		});
	}

	private void fire(Consumer<String> onChange)
	{
		if (!quiet)
		{
			onChange.accept(getText());
		}
	}

	/** Sets the text without telling the listener, for when the plugin clears the box. */
	public void setQuietly(String text)
	{
		if (getText().equals(text))
		{
			return;
		}
		quiet = true;
		setText(text);
		quiet = false;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(shape);
		g.setColor(isFocusOwner() ? Theme.alpha(Theme.GOLD, 170) : Theme.STROKE);
		g.draw(shape);
		g.dispose();
		super.paintComponent(graphics);
		if (getText().isEmpty())
		{
			Graphics2D h2 = (Graphics2D) graphics.create();
			h2.setFont(Theme.body());
			h2.setColor(Theme.TEXT_FAINT);
			h2.drawString(hint, getInsets().left, (h + h2.getFontMetrics().getAscent()) / 2 - 2);
			h2.dispose();
		}
	}

	@Override
	public Color getBackground()
	{
		return Theme.INK_DEEP;
	}
}
