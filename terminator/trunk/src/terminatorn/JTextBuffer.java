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

public class JTextBuffer extends JComponent implements FocusListener {
	private static final boolean ANTIALIAS = false;
	private static final boolean MAC_OS = (System.getProperty("os.name").indexOf("Mac") != -1);

	private TextBuffer model;
	private Location caretPosition = new Location(0, 0);
	private boolean hasFocus = false;
	private boolean displayCaret = true;
	private HashMap highlighters = new HashMap();
	
	/**
	* The highlights present in each line.  The highlights for a line are stored at the index in
	* lineHighlights corresponding to the line index.  The object at that index is another ArrayList
	* containing all Highlight objects which appear on that line.  Note that a highlight object which
	* appears on several lines will appear several times within this structure (once within the
	* ArrayList for each line upon which the highlight appears).  This ArrayList is not guaranteed to
	* be the same size as the number of lines in the model, and likewise there is no guarantee that
	* the reference at a certain index will be a real ArrayList - it could be null.  Use the already
	* implemented methods for accessing this structure whenever possible in order to hide all the
	* necessary checks.
	*/
	private ArrayList lineHighlights = new ArrayList();
	
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
				Highlight[] lights = getHighlightsForLocation(viewToModel(event.getPoint()));
				for (int i = 0; i < lights.length; i++) {
					lights[i].getHighlighter().highlightClicked(JTextBuffer.this, lights[i], getText(lights[i]), event);
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			private Location lastLocation = new Location(-1, -1);

			public void mouseMoved(MouseEvent event) {
				Location location = viewToModel(event.getPoint());
				if (location.equals(lastLocation)) {
					return;
				}
				lastLocation = location;
				Cursor cursor = null;
				Highlight[] lights = getHighlightsForLocation(viewToModel(event.getPoint()));
				for (int i = 0; i < lights.length; i++) {
					if (lights[i].getCursor() != null) {
						cursor = lights[i].getCursor();
						break;
					}
				}
				if (cursor == null) {
					cursor = Cursor.getDefaultCursor();
				}
				setCursor(cursor);
			}
		});
		addHighlighter(new HyperlinkHighlighter());
		becomeDropTarget();
		new Selector(this);
	}
	
	private void becomeDropTarget() {
		new TerminalDropTarget(this);
	}
	
	public TextBuffer getModel() {
		return model;
	}
	
	public void insertText(String text) {
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			int keyCode = KeyEvent.VK_UNDEFINED;
			KeyEvent event = new KeyEvent(this, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, keyCode, ch);
			processKeyEvent(event);
		}
	}
	
	// Methods used by TextBuffer in order to update the display.
	
	public void lineSectionChanged(int lineIndex, int charStartIndex, int charEndIndex) {
		FontMetrics metrics = getFontMetrics(getFont());
		Point baseline = getBaseline(new Location(lineIndex, charStartIndex));
		int charHeight = metrics.getHeight();
		redoHighlightsFrom(lineIndex);
		repaint(baseline.x, baseline.y - charHeight, (charEndIndex - charStartIndex) * metrics.charWidth('W'), charHeight);
	}
	
	public void sizeChanged() {
		Dimension size = getOptimalViewSize();
		setMaximumSize(size);
		setPreferredSize(size);
		setSize(size);
		redoHighlightsFrom(lineHighlights.size());
	}
	
	public void scrollToBottom() {
		scrollRectToVisible(new Rectangle(0, getHeight() - 10, getWidth(), 10));
	}
	
	/**
	 * Scrolls to the bottom of the output if doing so fits the user's
	 * configuration, or is over-ridden by the fact that we're trying to
	 * stay where we were but that *was* the bottom.
	 */
	public void scrollOnTtyOutput(boolean wereAtBottom) {
		if (wereAtBottom || Options.getSharedInstance().isScrollTtyOutput()) {
			scrollToBottom();
		}
	}
	
	/**
	 * Tests whether we're currently at the bottom of the output. Code
	 * that's causing output will need to keep the result of invoking this
	 * method so it can invoke scrollOnTtyOutput correctly afterwards.
	 */
	public boolean isAtBottom() {
		Rectangle visibleRectangle = getVisibleRect();
		boolean atBottom = visibleRectangle.y + visibleRectangle.height >= getHeight();
		return atBottom;
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
	
	public Location viewToModel(Point point) {
		FontMetrics metrics = getFontMetrics(getFont());
		int lineIndex = Math.max(0, point.y / metrics.getHeight());
		int charOffset = Math.max(0, point.x / metrics.charWidth('W'));
		return new Location(lineIndex, charOffset);
	}
	
	private Point getLineTop(Location charCoords) {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Point(charCoords.getCharOffset() * metrics.charWidth('W'),
				charCoords.getLineIndex() * metrics.getHeight());
	}
	
	private Point getLineBottom(Location charCoords) {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Point(charCoords.getCharOffset() * metrics.charWidth('W'),
				(charCoords.getLineIndex() + 1) * metrics.getHeight());
	}
	
	private Point getBaseline(Location charCoords) {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Point(charCoords.getCharOffset() * metrics.charWidth('W'),
				(charCoords.getLineIndex() + 1) * metrics.getHeight() - metrics.getMaxDescent());
	}
	
	public Dimension getOptimalViewSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Dimension(model.getWidth() * metrics.charWidth('W'), model.getLineCount() * metrics.getHeight());
	}
	
	// Highlighting support.
	
	public void addHighlighter(Highlighter highlighter) {
		highlighters.put(highlighter.getName(), highlighter);
	}
	
	public Highlighter getHighlighter(String name) {
		return (Highlighter) highlighters.get(name);
	}
	
	public Highlighter[] getHighlighters() {
		return (Highlighter[]) highlighters.values().toArray(new Highlighter[0]);
	}
	
	public void redoHighlightsFrom(int firstLineIndex) {
		removeHighlightsFrom(firstLineIndex);
		Highlighter[] lighters = getHighlighters();
		for (int i = 0; i < lighters.length; i++) {
			lighters[i].addHighlights(this, firstLineIndex);
		}
	}
	
	public void removeHighlightsFrom(int firstLineIndex) {
		while (lineHighlights.size() > firstLineIndex) {
			ArrayList list = (ArrayList) lineHighlights.remove(firstLineIndex);
			if (list != null) {
				Highlight[] highlights = (Highlight[]) list.toArray(new Highlight[list.size()]);
				for (int j = 0; j < highlights.length; j++) {
					repaintHighlight(highlights[j]);
				}
			}
		}
	}
	
	public void removeHighlightsFrom(Highlighter highlighter, int firstLineIndex) {
		for (int i = firstLineIndex; i < lineHighlights.size(); i++) {
			ArrayList list = (ArrayList) lineHighlights.get(i);
			if (list != null) {
				Highlight[] highlights = (Highlight[]) list.toArray(new Highlight[list.size()]);
				for (int j = 0; j < highlights.length; j++) {
					if (highlights[j].getHighlighter() == highlighter) {
						list.remove(highlights[j]);
						repaintHighlight(highlights[j]);
					}
				}
			}
		}
	}
	
	public void addHighlight(Highlight highlight) {
		for (int i = highlight.getStart().getLineIndex(); i <= highlight.getEnd().getLineIndex(); i++) {
			addHighlightAtLine(highlight, i);
		}
		repaintHighlight(highlight);
	}
	
	private void repaintHighlight(Highlight highlight) {
		Point redrawStart = getLineTop(highlight.getStart());
		Point redrawEnd = getLineBottom(highlight.getEnd());
		if (highlight.getStart().getLineIndex() == highlight.getEnd().getLineIndex()) {
			repaint(redrawStart.x, redrawStart.y, redrawEnd.x - redrawStart.x, redrawEnd.y - redrawStart.y);
		} else {
			repaint(0, redrawStart.y, getSize().width,redrawEnd.y - redrawStart.y);
		}
	}
	
	public void addHighlightAtLine(Highlight highlight, int lineIndex) {
		if (lineIndex >= lineHighlights.size() || lineHighlights.get(lineIndex) == null) {
			for (int i = lineHighlights.size(); i <= lineIndex; i++) {
				lineHighlights.add(i, null);
			}
			lineHighlights.set(lineIndex, new ArrayList());
		}
		((ArrayList) lineHighlights.get(lineIndex)).add(highlight);
	}
	
	public Highlight[] getHighlightsForLocation(Location location) {
		Highlight[] lineLights = getHighlightsForLine(location.getLineIndex());
		ArrayList result = new ArrayList();
		for (int i = 0; i < lineLights.length; i++) {
			Location start = lineLights[i].getStart();
			Location end = lineLights[i].getEnd();
			boolean startOK = (location.getLineIndex() > start.getLineIndex()) ||
					(location.getCharOffset() >= start.getCharOffset());
			boolean endOK = (location.getLineIndex() < end.getLineIndex()) ||
					(location.getCharOffset() < end.getCharOffset());
			if (startOK && endOK) {
				result.add(lineLights[i]);
			}
		}
		return (Highlight[]) result.toArray(new Highlight[result.size()]);
	}
	
	/** Returns an array containing all highlights in the indexed line.  This method never returns null. */
	public Highlight[] getHighlightsForLine(int lineIndex) {
		if (lineIndex >= lineHighlights.size() || lineHighlights.get(lineIndex) == null) {
			return new Highlight[0];
		} else {
			ArrayList highlights = (ArrayList) lineHighlights.get(lineIndex);
			return (Highlight[]) highlights.toArray(new Highlight[highlights.size()]);
		}
	}
	
	public String getText(Highlight highlight) {
		return model.getCharSequence(highlight.getStart(), highlight.getEnd()).toString();
	}
	
	// Redraw code.
	
	private void redrawCaretPosition() {
		Point top = getLineTop(caretPosition);
		FontMetrics metrics = getFontMetrics(getFont());
		repaint(top.x, top.y, metrics.charWidth(' '), metrics.getHeight());
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
				boolean drawCaret = (displayCaret && i == caretPosition.getLineIndex());
				x += paintStyledText(graphics, lineText[j], x, baseline, drawCaret);
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
	
	/**
	 * Paints the caret. Three caret styles are supported, though there's
	 * no interface yet to choose between them, and the block caret has
	 * the problem of needing to know whether to influence the next
	 * character's foreground color (and having some way of doing so).
	 */
	private void paintCaret(Graphics graphics, FontMetrics metrics) {
		graphics.setColor(Options.getSharedInstance().getColor("cursorColor"));
		Point top = getLineTop(caretPosition);
		final int bottomY = top.y + metrics.getHeight() - 1;
		if (hasFocus) {
			// Block.
			//graphics.fillRect(top.x, top.y, metrics.charWidth(' '), metrics.getHeight());
			// Vertical Bar.
			//graphics.drawLine(top.x, top.y, top.x, bottomY);
			// Underline.
			graphics.drawLine(top.x, bottomY, top.x + metrics.charWidth(' ') - 1, bottomY);
		} else {
			// For some reason, terminals always seem to use an
			// empty block for the unfocused cursor, regardless
			// of what shape they're using for the focused cursor.
			// It's not obvious what else they could do that would
			// look better.
			graphics.drawRect(top.x, top.y, metrics.charWidth(' ') - 1, metrics.getHeight() - 1);
		}
	}
	
	/**
	 * Paints the text. Returns how many pixels wide the text was.
	 */
	private int paintStyledText(Graphics graphics, StyledText text, int x, int y, boolean drawCaret) {
		FontMetrics metrics = getFontMetrics(getFont());
		Style style = text.getStyle();
		int textWidth = metrics.stringWidth(text.getText());
		graphics.setColor(style.getBackground());
		graphics.fillRect(x, y - metrics.getMaxAscent(), textWidth, metrics.getHeight());
		if (drawCaret) {
			paintCaret(graphics, metrics);
		}
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
			if (MAC_OS) {
				// A font doesn't necessarily have a bold.
				// Mac OS X's "Monaco" font is an example.
				// The trouble is, you can't tell from the
				// Font you get back from deriveFont. isBold
				// will return true, and getting the WEIGHT
				// attribute will give you WEIGHT_BOLD.
				// So the test above shouldn't be for Mac OS,
				// it should be for a font without a bold,
				// but I know no way of doing that.
				graphics.drawString(text.getText(), x + 1, y);
			}
			graphics.setFont(oldFont);
		}
		return textWidth;
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
}
