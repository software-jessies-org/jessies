package terminatorn;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
A JTextBuffer provides the visible display of the virtual terminal.

@author Phil Norman
@author Elliott Hughes
*/

public class JTextBuffer extends JComponent implements FocusListener, Scrollable {
	private static final boolean ANTIALIAS = false;

	private TextBuffer model;
	private Point caretPosition = new Point(0, 0);
	private boolean hasFocus = false;
	
	public JTextBuffer() {
		model = new TextBuffer(this, 80, 24);
		setFont(Font.decode("Monospaced"));
		setBackground(Color.WHITE);
		addFocusListener(this);
		requestFocus();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				requestFocus();
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				requestFocus();
			}
		});
	}
	
	public TextBuffer getModel() {
		return model;
	}
	
	// Methods used by TextBuffer in order to update the display.
	
	public void lineSectionChanged(int lineIndex, int charStartIndex, int charEndIndex) {
		FontMetrics metrics = getFontMetrics(getFont());
		Point baseline = getBaseline(new Point(charStartIndex, lineIndex));
		int charHeight = metrics.getHeight();
		repaint(baseline.x, baseline.y - charHeight, (charEndIndex - charStartIndex) * metrics.charWidth('W'), charHeight);
	}
	
	public void sizeChanged() {
		Dimension size = getOptimalViewSize();
		setMaximumSize(size);
		setPreferredSize(size);
		setSize(size);
		scrollRectToVisible(new Rectangle(0, size.height - 10, size.width, 10));
	}
	
	public Point getCaretPosition() {
		return new Point(caretPosition);  // We don't want anyone else messing with our Point.
	}
	
	public void setCaretPosition(Point caretPosition) {
		redrawCaretPosition();
		this.caretPosition.x = caretPosition.x;
		this.caretPosition.y = caretPosition.y;
		redrawCaretPosition();
	}
	
	private Point getBaseline(Point charCoords) {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Point(charCoords.x * metrics.charWidth('W'), (charCoords.y + 1) * metrics.getHeight());
	}
	
	public Dimension getOptimalViewSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Dimension(model.getWidth() * metrics.charWidth('W'), model.getLineCount() * metrics.getHeight());
	}
	
	// Redraw code.
	
	private void redrawCaretPosition() {
		Point baseline = getBaseline(caretPosition);
		FontMetrics metrics = getFontMetrics(getFont());
		repaint(baseline.x, baseline.y - metrics.getHeight(), 1, metrics.getHeight());
	}
	
	public void paintComponent(Graphics graphics) {
		if (ANTIALIAS) {
			((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		FontMetrics metrics = getFontMetrics(getFont());
		Rectangle rect = graphics.getClipBounds();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
		int firstTextLine = rect.y / metrics.getHeight();
		int lastTextLine = (rect.y + rect.height + metrics.getHeight() - 1) / metrics.getHeight();
		lastTextLine = Math.min(lastTextLine, model.getLineCount() - 1);
		for (int i = firstTextLine; i <= lastTextLine; i++) {
			StyledText[] lineText = model.getLineText(i);
			int x = 0;
			int baseline = metrics.getHeight() * (i + 1) - metrics.getMaxDescent();
			for (int j = 0; j < lineText.length; j++) {
				paintStyledText(graphics, lineText[j], x, baseline);
				x += metrics.stringWidth(lineText[j].getText());
			}
			if (i == caretPosition.y) {
				graphics.setColor(hasFocus ? Color.RED : Color.BLACK);
				int caretX = caretPosition.x * metrics.charWidth('W');
				graphics.drawLine(caretX, baseline - metrics.getMaxAscent(), caretX, baseline);
			}
		}
		if (ANTIALIAS) {
			((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		}
	}
	
	private void paintStyledText(Graphics graphics, StyledText text, int x, int y) {
		FontMetrics metrics = getFontMetrics(getFont());
		int textWidth = metrics.stringWidth(text.getText());
		if (text.getBackground() != StyledText.WHITE) {
			graphics.setColor(getStyleColour(text.getBackground()));
			graphics.fillRect(x, y - metrics.getMaxAscent(), textWidth, metrics.getHeight());
		}
		graphics.setColor(getStyleColour(text.getForeground()));
		if (text.isUnderlined()) {
			graphics.drawLine(x, y + 1, x + textWidth, y + 1);
		}
		Font oldFont = graphics.getFont();
		if (text.isBold()) {
			graphics.setFont(oldFont.deriveFont(Font.BOLD));
		}
		graphics.drawString(text.getText(), x, y);
		if (text.isBold()) {
			graphics.setFont(oldFont);
		}
	}
	
	private Color getStyleColour(int colour) {
		switch (colour) {
			case StyledText.BLACK: return Color.BLACK;
			case StyledText.RED: return Color.RED;
			case StyledText.GREEN: return Color.GREEN;
			case StyledText.ORANGE: return Color.ORANGE;
			case StyledText.BLUE: return Color.BLUE;
			case StyledText.MAGENTA: return Color.MAGENTA;
			case StyledText.CYAN: return Color.CYAN;
			case StyledText.WHITE: return Color.WHITE;
			default: return Color.BLACK;
		}
	}

	/** Overridden so we can get real tabs, and they're not stolen by some cycling thing. */
	public boolean isManagingFocus() {
		return true;
	}
	
	/** Overridden so we can get real tabs, and they're not stolen by some cycling thing. */
	public boolean isFocusCycleRoot() {
		return true;
	}
	
	public void focusGained(FocusEvent event) {
		hasFocus = true;
		redrawCaretPosition();
	}
	
	public void focusLost(FocusEvent event) {
		hasFocus = false;
		redrawCaretPosition();
	}
	
	public boolean hasFocus() {
		return hasFocus;
	}
	
	//
	// Scrollable interface.
	//
	
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	
	public int getScrollableUnitIncrement(Rectangle visibleRectangle, int orientation, int direction) {
		if (orientation == SwingConstants.VERTICAL) {
			return visibleRectangle.height / 10;
		} else {
			return visibleRectangle.width / 10;
		}
	}
	
	public int getScrollableBlockIncrement(Rectangle visibleRectangle, int orientation, int direction) {
		if (orientation == SwingConstants.VERTICAL) {
			return visibleRectangle.height;
		} else {
			return visibleRectangle.width;
		}
	}
	
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}
	
	public boolean getScrollableTracksViewportHeight() {
		return false; // We want a vertical scroll-bar.
	}
}
