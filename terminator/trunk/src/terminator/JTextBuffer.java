package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
A JTextBuffer provides the visible display of the virtual terminal.

@author Phil Norman
@author Elliott Hughes
*/

public class JTextBuffer extends JComponent implements FocusListener, Scrollable {
	private static final boolean ANTIALIAS = false;

	private TextBuffer model;
	private Location caretPosition = new Location(0, 0);
	private boolean hasFocus = false;
	private boolean displayCaret = true;
	private HashMap highlighters = new HashMap();
	
	public JTextBuffer() {
		model = new TextBuffer(this, 80, 24);
		setFont(Options.getSharedInstance().getFont());
		setForeground(Options.getSharedInstance().getColor("foreground"));
		setBackground(Options.getSharedInstance().getColor("background"));
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
		Point baseline = getBaseline(new Location(lineIndex, charStartIndex));
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
	
	public Location getCaretPosition() {
		return caretPosition;
	}
	
	public void setCaretPosition(Location caretPosition) {
		redrawCaretPosition();
		this.caretPosition = caretPosition;
		redrawCaretPosition();
	}
	
	/** Sets whether the caret should be displayed. */
	public void setCaretDisplay(boolean displayCaret) {
		if (this.displayCaret != displayCaret) {
			this.displayCaret = displayCaret;
			redrawCaretPosition();
		}
	}
	
	private Point getBaseline(Location charCoords) {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Point(charCoords.getCharOffset() * metrics.charWidth('W'),
				(charCoords.getLineIndex() + 1) * metrics.getHeight());
	}
	
	public Dimension getOptimalViewSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Dimension(model.getWidth() * metrics.charWidth('W'), model.getLineCount() * metrics.getHeight());
	}
	
	// Highlighting support.
	
	public void setHighlighter(String name, Highlighter highlighter) {
		highlighters.put(name, highlighter);
	}
	
	public Highlighter getHighlighter(String name) {
		return (Highlighter) highlighters.get(name);
	}
	
	public Highlight[] getHighlightsForLine(int lineIndex) {
		return new Highlight[] {
			new Highlight(null, new Location(lineIndex, 5), new Location(lineIndex, 10), new Style(null, Color.blue, null, null))
		};
	}
	
	// Redraw code.
	
	private void redrawCaretPosition() {
		Point baseline = getBaseline(caretPosition);
		FontMetrics metrics = getFontMetrics(getFont());
		repaint(baseline.x, baseline.y - metrics.getHeight(), 1, metrics.getHeight());
	}
	
	public void paintComponent(Graphics graphics) {
		((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, ANTIALIAS ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
		FontMetrics metrics = getFontMetrics(getFont());
		Rectangle rect = graphics.getClipBounds();
		graphics.setColor(getBackground());
		graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
		int firstTextLine = rect.y / metrics.getHeight();
		int lastTextLine = (rect.y + rect.height + metrics.getHeight() - 1) / metrics.getHeight();
		lastTextLine = Math.min(lastTextLine, model.getLineCount() - 1);
		for (int i = firstTextLine; i <= lastTextLine; i++) {
			StyledText[] lineText = getLineText(i);
			int x = 0;
			int baseline = metrics.getHeight() * (i + 1) - metrics.getMaxDescent();
			for (int j = 0; j < lineText.length; j++) {
				paintStyledText(graphics, lineText[j], x, baseline);
				x += metrics.stringWidth(lineText[j].getText());
			}
			if (displayCaret && i == caretPosition.getLineIndex()) {
				graphics.setColor(hasFocus ? Color.RED : Color.BLACK);
				int caretX = caretPosition.getCharOffset() * metrics.charWidth('W');
				graphics.drawLine(caretX, baseline - metrics.getMaxAscent(), caretX, baseline);
			}
		}
		if (ANTIALIAS) {
			((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		}
	}
	
	public StyledText[] getLineText(int line) {
		StyledText[] result = model.getLineText(line);
		Highlight[] highlights = getHighlightsForLine(line);
		for (int i = 0; i < highlights.length; i++) {
			result = highlights[i].applyHighlight(result, new Location(line, 0));
		}
		return result;
	}
	
	private void paintStyledText(Graphics graphics, StyledText text, int x, int y) {
		FontMetrics metrics = getFontMetrics(getFont());
		Style style = text.getStyle();
		int textWidth = metrics.stringWidth(text.getText());
//		if (text.getBackground() != StyledText.WHITE) {
			graphics.setColor(style.getBackground());
			graphics.fillRect(x, y - metrics.getMaxAscent(), textWidth, metrics.getHeight());
//		}
		graphics.setColor(style.getForeground());
		if (style.isUnderlined()) {
			graphics.drawLine(x, y + 1, x + textWidth, y + 1);
		}
		Font oldFont = graphics.getFont();
		if (style.isBold()) {
			graphics.setFont(oldFont.deriveFont(Font.BOLD));
		}
		graphics.drawString(text.getText(), x, y);
		if (style.isBold()) {
			graphics.setFont(oldFont);
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
