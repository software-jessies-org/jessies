package terminator.view;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.util.*;

import terminator.*;
import terminator.model.*;
import terminator.terminal.*;
import terminator.view.highlight.*;

/**
A JTextBuffer provides the visible display of the virtual terminal.

@author Phil Norman
@author Elliott Hughes
*/

public class JTextBuffer extends JComponent implements FocusListener {
	private static final boolean ANTIALIAS = Options.getSharedInstance().isAntiAliased();
	private static final boolean MAC_OS = GuiUtilities.isMacOs();

	private TextBuffer model;
	private Location cursorPosition = new Location(0, 0);
	private boolean hasFocus = false;
	private boolean displayCursor = true;
	private boolean blinkOn = true;
	private CursorBlinker cursorBlinker;
	private HashMap<Class, Highlighter> highlighters = new HashMap<Class, Highlighter>();
	
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
	private ArrayList<ArrayList<Highlight>> lineHighlights = new ArrayList<ArrayList<Highlight>>();
	
	public JTextBuffer() {
		Options options = Options.getSharedInstance();
		model = new TextBuffer(this, options.getInitialColumnCount(), options.getInitialRowCount());
		ComponentUtilities.disableFocusTraversal(this);
		setFont(options.getFont());
		setForeground(options.getColor("foreground"));
		setBackground(options.getColor("background"));
		addFocusListener(this);
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				requestFocus();
				if (SwingUtilities.isLeftMouseButton(event)) {
					highlightClicked(event);
				}
			}

			public void highlightClicked(MouseEvent event) {
				List<Highlight> highlights = getHighlightsForLocation(viewToModel(event.getPoint()));
				for (Highlight highlight : highlights) {
					highlight.getHighlighter().highlightClicked(JTextBuffer.this, highlight, getTabbedText(highlight), event);
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
				List<Highlight> highlights = getHighlightsForLocation(viewToModel(event.getPoint()));
				for (Highlight highlight : highlights) {
					if (highlight.getCursor() != null) {
						cursor = highlight.getCursor();
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
		addHighlighter(new FindHighlighter());
		becomeDropTarget();
		cursorBlinker = new CursorBlinker(this);
		new SelectionHighlighter(this);
	}
	
	public void userIsTyping() {
		blinkOn = true;
		redrawCursorPosition();
		setCursor(GuiUtilities.INVISIBLE_CURSOR);
	}
	
	private void becomeDropTarget() {
		new TerminalDropTarget(this);
	}
	
	public TextBuffer getModel() {
		return model;
	}
	
	/**
	 * Pastes the text on the clipboard into the terminal.
	 */
	public void paste() {
		pasteClipboard(getToolkit().getSystemClipboard());
	}
	
	/**
	 * Pastes the system selection, generally only available on X11.
	 */
	public void pasteSystemSelection() {
		Clipboard systemSelection = getToolkit().getSystemSelection();
		if (systemSelection != null) {
			pasteClipboard(systemSelection);
		}
	}
	
	private void pasteClipboard(Clipboard clipboard) {
		try {
			Transferable contents = clipboard.getContents(this);
			DataFlavor[] transferFlavors = contents.getTransferDataFlavors();
			String string = (String) contents.getTransferData(DataFlavor.stringFlavor);
			insertText(string);
		} catch (Exception ex) {
			Log.warn("Couldn't paste.", ex);
		}
	}
	
	private TerminalControl terminalControl;
	
	public TerminalControl getTerminalControl() {
		return terminalControl;
	}
	
	public void setTerminalControl(TerminalControl terminalControl) {
		this.terminalControl = terminalControl;
	}
	
	public void insertText(final String text) {
		new Thread() {
			public void run() {
				for (int i = 0; i < text.length(); ++i) {
					terminalControl.sendChar(text.charAt(i));
				}
				/*
				String remainder = text;
				while (remainder.length() > 0) {
					int chunkLength = Math.min(1024, remainder.length());
					terminalControl.sendString(remainder.substring(0, chunkLength));
					remainder = remainder.substring(chunkLength);
				}
				*/
			}
		}.start();
	}
	
	/** Returns our visible size. */
	public Dimension getVisibleSize() {
		JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		return scrollPane.getViewport().getExtentSize();
	}
	
	public Dimension getCharUnitSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Dimension(metrics.charWidth('W'), metrics.getHeight());
	}
	
	/**
	 * Returns our size in character units, where 'width' is the number of
	 * columns and 'height' the number of rows. (In case you were concerned
	 * about the fact that terminals tend to refer to y,x coordinates.)
	 */
	public Dimension getVisibleSizeInCharacters() {
		Dimension result = getVisibleSize();
		Dimension character = getCharUnitSize();
		result.width /= character.width;
		result.height /= character.height;
		return result;
	}
	
	// Methods used by TextBuffer in order to update the display.
	
	public void linesChangedFrom(int lineIndex) {
		Point redrawTop = getLineTop(new Location(lineIndex, 0));
		redoHighlightsFrom(lineIndex);
		Dimension size = getSize();
		repaint(redrawTop.x, redrawTop.y, size.width, size.height - redrawTop.y);
	}
	
	public void sizeChanged() {
		Dimension size = getOptimalViewSize();
		setMaximumSize(size);
		setPreferredSize(size);
		setSize(size);
		revalidate();
	}
	
	public void sizeChanged(Dimension oldSizeInChars, Dimension newSizeInChars) {
		sizeChanged();
		redoHighlightsFrom(Math.min(oldSizeInChars.height, newSizeInChars.height));
	}
	
	public void scrollToBottom() {
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		
		BoundedRangeModel verticalModel = pane.getVerticalScrollBar().getModel();
		verticalModel.setValue(verticalModel.getMaximum() - verticalModel.getExtent());
		
		scrollHorizontallyToShowCursor();
	}
	
	private boolean isLineVisible(int lineIndex) {
		return (lineIndex >= getFirstVisibleLine() && lineIndex <= getLastVisibleLine());
	}
	
	private void scrollHorizontallyToShowCursor() {
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		
		if (isLineVisible(getCursorPosition().getLineIndex()) == false) {
			// We shouldn't be jumping the horizontal scroll bar
			// about because of new output if the user's trying to
			// review the history.
			return;
		}
		
		// FIXME: we don't necessarily have a horizontal position that
		// corresponds to where the cursor is. This is probably a
		// mistake that should be fixed.
		
		// [To reproduce the problem underlying this code, simply
		// "cat > /dev/null" and then type more characters than fit
		// on a line.]
		
		int leftCursorEdge = getCursorPosition().getCharOffset() * getCharUnitSize().width;
		int rightCursorEdge = leftCursorEdge + getCharUnitSize().width;
		
		BoundedRangeModel horizontalModel = pane.getHorizontalScrollBar().getModel();
		
		int leftWindowEdge = horizontalModel.getValue();
		int rightWindowEdge = leftWindowEdge + horizontalModel.getExtent();
		
		// We don't want to scroll back as the user moves the
		// cursor back; we should just ensure that the cursor
		// is visible, and do nothing if it is already visible.
		if (leftCursorEdge < leftWindowEdge) {
			horizontalModel.setValue(leftCursorEdge - horizontalModel.getExtent() / 2);
		} else if (rightCursorEdge > rightWindowEdge) {
			horizontalModel.setValue(rightCursorEdge - horizontalModel.getExtent() / 2);
		}
	}
	
	public void scrollToTop() {
		scrollTo(0, 0, 0);
	}
	
	private void scrollTo(final int lineNumber, final int charStart, final int charEnd) {
		Dimension character = getCharUnitSize();
		final int x0 = charStart * character.width;
		final int y0 = lineNumber * character.height - 10;
		final int width = (charEnd - charStart) * character.width;
		final int height = character.height + 20;
		// Showing the beginning of the line first lets us scroll
		// horizontally as far as necessary but no further. We'd rather
		// show more of the beginning of the line in case we've jumped
		// here from a long way away; the beginning is where the
		// context is.
		scrollRectToVisible(new Rectangle(0, y0, 0, height));
		scrollRectToVisible(new Rectangle(x0, y0, width, height));
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
	
	public Location getCursorPosition() {
		return cursorPosition;
	}
	
	public void setCursorPosition(Location newCursorPosition) {
		if (cursorPosition.equals(newCursorPosition)) {
			return;
		}
		redrawCursorPosition();
		cursorPosition = newCursorPosition;
		redrawCursorPosition();
		scrollHorizontallyToShowCursor();
	}
	
	/** Sets whether the cursor should be displayed. */
	public void setCursorVisible(boolean displayCursor) {
		if (this.displayCursor != displayCursor) {
			this.displayCursor = displayCursor;
			redrawCursorPosition();
		}
	}
	
	public boolean shouldShowCursor() {
		return displayCursor;
	}
	
	public Color getCursorColor() {
		return Options.getSharedInstance().getColor(blinkOn ? "cursorColor" : "background");
	}
	
	public void blinkCursor() {
		blinkOn = !blinkOn;
		redrawCursorPosition();
	}
	
	public Location viewToModel(Point point) {
		Dimension character = getCharUnitSize();
		int lineIndex = point.y / character.height;
		int charOffset = 0;
		// If the line index is off the top or bottom, we leave charOffset = 0.  This gives us nicer
		// selection functionality.
		if (lineIndex >= model.getLineCount()) {
			lineIndex = model.getLineCount();
		} else if (lineIndex < 0) {
			lineIndex = 0;
		} else {
			charOffset = Math.max(0, point.x / character.width);
			charOffset = Math.min(charOffset, model.get(lineIndex).length());
		}
		return new Location(lineIndex, charOffset);
	}
	
	private Point getLineTop(Location charCoords) {
		Dimension character = getCharUnitSize();
		return new Point(charCoords.getCharOffset() * character.width, charCoords.getLineIndex() * character.height);
	}
	
	private Point getLineBottom(Location charCoords) {
		Dimension character = getCharUnitSize();
		return new Point(charCoords.getCharOffset() * character.width, (charCoords.getLineIndex() + 1) * character.height);
	}
	
	public Dimension getOptimalViewSize() {
		Dimension character = getCharUnitSize();
		return new Dimension(model.getMaxLineWidth() * character.width, model.getLineCount() * character.height);
	}
	
	// Highlighting support.
	
	/**
	 * Adds a highlighter. Highlighters are referred to by class, so it's
	 * a bad idea to have more than one of the same class.
	 */
	public void addHighlighter(Highlighter highlighter) {
		Class kind = highlighter.getClass();
		if (highlighters.get(kind) != null) {
			throw new IllegalArgumentException("duplicate " + kind);
		}
		highlighters.put(kind, highlighter);
	}
	
	/**
	 * Returns the highlighter of the given class.
	 */
	public Highlighter getHighlighterOfClass(Class kind) {
		return highlighters.get(kind);
	}
	
	public Collection<Highlighter> getHighlighters() {
		return Collections.unmodifiableCollection(highlighters.values());
	}
	
	private void redoHighlightsFrom(int firstLineIndex) {
		removeHighlightsFrom(firstLineIndex);
		Iterator it = getHighlighters().iterator();
		while (it.hasNext()) {
			Highlighter highlighter = (Highlighter) it.next();
			highlighter.addHighlights(this, firstLineIndex);
		}
	}
	
	public void removeHighlightsFrom(int firstLineIndex) {
		if (firstLineIndex == 0) {
			lineHighlights.clear();
			repaint();
		} else {
			// We use a backwards loop because going forwards results in N array copies if
			// we're removing N lines.
			for (int i = (lineHighlights.size() - 1); i >= firstLineIndex; i--) {
				lineHighlights.remove(i);
			}
			repaintFromLine(firstLineIndex);
		}
	}
	
	public void removeHighlightsFrom(Highlighter highlighter, int firstLineIndex) {
		for (int i = firstLineIndex; i < lineHighlights.size(); i++) {
			ArrayList<Highlight> list = lineHighlights.get(i);
			if (list != null) {
				Iterator it = list.iterator();
				while (it.hasNext()) {
					Highlight highlight = (Highlight) it.next();
					if (highlight.getHighlighter() == highlighter) {
						it.remove();
						repaintHighlight(highlight);
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
	
	private void repaintFromLine(int firstLineToRepaint) {
		int top = getLineTop(new Location(firstLineToRepaint, 0)).y;
		Dimension size = getSize();
		repaint(0, top, size.width, size.height - top);
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
			lineHighlights.set(lineIndex, new ArrayList<Highlight>());
		}
		ArrayList<Highlight> line = lineHighlights.get(lineIndex);
		if (line.indexOf(highlight) == -1) {
			line.add(highlight);
		}
	}
	
	/**
	 * Searches from startLine to endLine inclusive, incrementing the
	 * current line by 'direction', looking for a line with a find highlight.
	 * When one is found, the cursor is moved there.
	 */
	private void findAgain(Class highlighterClass, int startLine, int endLine, int direction) {
		for (int i = startLine; i != endLine; i += direction) {
			List<Highlight> highlights = getHighlightsForLine(i);
			Highlight match = firstHighlightOfClass(highlights, highlighterClass);
			if (match != null) {
				scrollTo(i, match.getStart().getCharOffset(), match.getEnd().getCharOffset());
				return;
			}
		}
	}
	
	/**
	 * Tests whether any of the Highlight objects in the list is a FindHighlighter.
	 */
	private static Highlight firstHighlightOfClass(List<Highlight> highlights, Class highlighterClass) {
		for (Highlight highlight : highlights) {
			if (highlight.getHighlighter().getClass() == highlighterClass) {
				return highlight;
			}
		}
		return null;
	}
	
	/**
	 * Scrolls the display down to the next highlight of the given class not currently on the display.
	 */
	public void findNext(Class highlighterClass) {
		findAgain(highlighterClass, getLastVisibleLine() + 1, getModel().getLineCount() + 1, 1);
	}
	
	/**
	 * Scrolls the display up to the next highlight of the given class not currently on the display.
	 */
	public void findPrevious(Class highlighterClass) {
		findAgain(highlighterClass, getFirstVisibleLine() - 1, -1, -1);
	}
	
	public JViewport getViewport() {
		return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
	}
	
	public int getFirstVisibleLine() {
		Dimension character = getCharUnitSize();
		Rectangle visibleBounds = getViewport().getViewRect();
		return visibleBounds.y / character.height;
	}
	
	public int getLastVisibleLine() {
		Dimension character = getCharUnitSize();
		Rectangle visibleBounds = getViewport().getViewRect();
		return (visibleBounds.y + visibleBounds.height) / character.height;
	}

	public List<Highlight> getHighlightsForLocation(Location location) {
		List<Highlight> highlights = getHighlightsForLine(location.getLineIndex());
		ArrayList<Highlight> result = new ArrayList<Highlight>();
		for (Highlight highlight : highlights) {
			Location start = highlight.getStart();
			Location end = highlight.getEnd();
			boolean startOK = (location.getLineIndex() > start.getLineIndex()) ||
					(location.getCharOffset() >= start.getCharOffset());
			boolean endOK = (location.getLineIndex() < end.getLineIndex()) ||
					(location.getCharOffset() < end.getCharOffset());
			if (startOK && endOK) {
				result.add(highlight);
			}
		}
		return result;
	}
	
	/** Returns a (possibly empty) list containing all highlights in the indexed line. */
	private List<Highlight> getHighlightsForLine(int lineIndex) {
		if (lineIndex >= lineHighlights.size() || lineHighlights.get(lineIndex) == null) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(lineHighlights.get(lineIndex));
		}
	}
	
	public String getTabbedText(Highlight highlight) {
		Location start = highlight.getStart();
		Location end = highlight.getEnd();
		StringBuilder buf = new StringBuilder();
		for (int i = start.getLineIndex(); i <= end.getLineIndex(); i++) {
			// Necessary to cope with selections extending to the bottom of the buffer.
			if (i == end.getLineIndex() && end.getCharOffset() == 0) {
				break;
			}
			TextLine textLine = model.get(i);
			int lineStart = (i == start.getLineIndex()) ? start.getCharOffset() : 0;
			int lineEnd = (i == end.getLineIndex()) ? end.getCharOffset() : textLine.length();
			buf.append(textLine.getTabbedText(lineStart, lineEnd));
			if (i != end.getLineIndex()) {
				buf.append('\n');
			}
		}
		return buf.toString();
	}
	
	// Redraw code.
	
	private void redrawCursorPosition() {
		Point top = getLineTop(cursorPosition);
		Dimension character = getCharUnitSize();
		repaint(top.x, top.y, character.width, character.height);
	}
	
	public void paintComponent(Graphics graphics) {
		((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, ANTIALIAS ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		FontMetrics metrics = getFontMetrics(getFont());
		Rectangle rect = graphics.getClipBounds();
		graphics.setColor(getBackground());
		graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
		int firstTextLine = rect.y / metrics.getHeight();
		int lastTextLine = (rect.y + rect.height + metrics.getHeight() - 1) / metrics.getHeight();
		lastTextLine = Math.min(lastTextLine, model.getLineCount() - 1);
		int lineNotToDraw = model.usingAlternateBuffer() ? model.getFirstDisplayLine() - 1 : -1;
		for (int i = firstTextLine; i <= lastTextLine; i++) {
			if (i == lineNotToDraw) {
				continue;
			}
			boolean drawCursor = (shouldShowCursor() && i == cursorPosition.getLineIndex());
			int x = 0;
			int baseline = metrics.getHeight() * (i + 1) - metrics.getMaxDescent();
			int startOffset = 0;
			Iterator it = getLineText(i).iterator();
			while (it.hasNext()) {
				StyledText chunk = (StyledText) it.next();
				x += paintStyledText(graphics, chunk, x, baseline);
				String chunkText = chunk.getText();
				if (drawCursor && cursorPosition.charOffsetInRange(startOffset, startOffset + chunkText.length())) {
					final int charOffsetUnderCursor = cursorPosition.getCharOffset() - startOffset;
					paintCursor(graphics, metrics, chunkText.substring(charOffsetUnderCursor, charOffsetUnderCursor + 1), baseline);
					drawCursor = false;
				}
				startOffset += chunkText.length();
			}
			if (drawCursor) {
				// A cursor at the end of the line is in a
				// position past the end of the text.
				paintCursor(graphics, metrics, "", baseline);
			}
		}
		if (ANTIALIAS) {
			((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		}
	}
	
	public List<StyledText> getLineText(int line) {
		List<StyledText> result = model.getLineText(line);
		List<Highlight> highlights = getHighlightsForLine(line);
		for (Highlight highlight : highlights) {
			result = highlight.applyHighlight(result, new Location(line, 0));
		}
		return result;
	}
	
	/**
	 * Paints the cursor, which is either a solid block or an underline.
	 * The cursor may actually be invisible because it's blinking and in
	 * the 'off' state.
	 */
	private void paintCursor(Graphics graphics, FontMetrics metrics, String characterUnderCursor, int baseline) {
		graphics.setColor(getCursorColor());
		Point top = getLineTop(cursorPosition);
		final int bottomY = top.y + metrics.getHeight() - 1;
		if (hasFocus) {
			if (Options.getSharedInstance().isBlockCursor()) {
				// Block.
				if (blinkOn) {
					// Paint over the character underneath.
					graphics.fillRect(top.x, top.y, metrics.charWidth(' '), metrics.getHeight());
					// Redraw the character in the
					// background color.
					graphics.setColor(Options.getSharedInstance().getColor("background"));
					graphics.drawString(characterUnderCursor, top.x, baseline);
				}
			} else {
				// Underline.
				if (blinkOn) {
					graphics.drawLine(top.x, bottomY, top.x + metrics.charWidth(' ') - 1, bottomY);
				}
			}
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
	private int paintStyledText(Graphics graphics, StyledText text, int x, int y) {
		FontMetrics metrics = getFontMetrics(getFont());
		Style style = text.getStyle();
		Color foreground = style.getForeground();
		Color background = style.getBackground();
		
		if (style.isReverseVideo()) {
			Color oldForeground = foreground;
			foreground = background;
			background = oldForeground;
		}
		
		int textWidth = metrics.stringWidth(text.getText());
		graphics.setColor(background);
		// Special continueToEnd flag used for drawing the backgrounds of Highlights which extend
		// over the end of lines.  Used for multi-line selection.
		int backgroundWidth = text.continueToEnd() ? (getSize().width - x) : textWidth;
		graphics.fillRect(x, y - metrics.getMaxAscent() - metrics.getLeading(), backgroundWidth, metrics.getHeight());
		if (style.isUnderlined()) {
			graphics.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 128));
			graphics.drawLine(x, y + 1, x + textWidth, y + 1);
		}
		graphics.setColor(foreground);
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
	
	public void focusGained(FocusEvent event) {
		hasFocus = true;
		blinkOn = true;
		cursorBlinker.start();
		redrawCursorPosition();
	}
	
	public void focusLost(FocusEvent event) {
		hasFocus = false;
		blinkOn = true;
		cursorBlinker.stop();
		redrawCursorPosition();
	}
	
	public boolean hasFocus() {
		return hasFocus;
	}
}
