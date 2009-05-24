package terminator.view;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;

import terminator.*;
import terminator.model.*;
import terminator.terminal.*;
import terminator.view.highlight.*;

public class TerminalView extends JComponent implements FocusListener, Scrollable {
	private static final Stopwatch paintComponentStopwatch = Stopwatch.get("TerminalView.paintComponent");
	private static final Stopwatch paintStyledTextStopwatch = Stopwatch.get("TerminalView.paintStyledText");
	
	private TerminalModel model;
	private Location cursorPosition = new Location(0, 0);
	private boolean hasFocus = false;
	private boolean displayCursor = true;
	private boolean blinkOn = true;
	private CursorBlinker cursorBlinker;
	private HashMap<Class<? extends Highlighter>, Highlighter> highlighters = new HashMap<Class<? extends Highlighter>, Highlighter>();
	private SelectionHighlighter selectionHighlighter;
	private BirdView birdView;
	private FindBirdsEye birdsEye;
	
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
	
	private List<Highlight> highlightsUnderMouse = Collections.emptyList();
	
	public TerminalView() {
		TerminatorPreferences preferences = Terminator.getPreferences();
		this.model = new TerminalModel(this, preferences.getInt(TerminatorPreferences.INITIAL_COLUMN_COUNT), preferences.getInt(TerminatorPreferences.INITIAL_ROW_COUNT));
		ComponentUtilities.disableFocusTraversal(this);
		setBorder(BorderFactory.createEmptyBorder(1, 4, 4, 4));
		setOpaque(true);
		optionsDidChange();
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
					highlight.getHighlighter().highlightClicked(TerminalView.this, highlight, event);
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
				List<Highlight> previousHighlights = highlightsUnderMouse;
				highlightsUnderMouse = getHighlightsForLocation(viewToModel(event.getPoint()));
				repaintHighlights(previousHighlights);
				repaintHighlights(highlightsUnderMouse);
				setCursor(getCursorForHighlightsUnderMouse());
			}
			
			private Cursor getCursorForHighlightsUnderMouse() {
				for (Highlight highlight : highlightsUnderMouse) {
					if (highlight.getCursor() != null) {
						return highlight.getCursor();
					}
				}
				return Cursor.getDefaultCursor();
			}
		});
		addMouseWheelListener(HorizontalScrollWheelListener.INSTANCE);
		addHighlighter(new FindHighlighter());
		addHighlighter(new UrlHighlighter());
		becomeDropTarget();
		cursorBlinker = new CursorBlinker(this);
		selectionHighlighter = new SelectionHighlighter(this);
		birdsEye = new FindBirdsEye(this);
	}
	
	public void optionsDidChange() {
		TerminatorPreferences preferences = Terminator.getPreferences();
		if (preferences.getBoolean(TerminatorPreferences.USE_ALT_AS_META)) {
			// If we want to handle key events when alt is down, we need to turn off input methods.
			enableInputMethods(false);
		}
		
		setBackground(preferences.getColor("background"));
		setForeground(preferences.getColor("foreground"));
		
		setFont(preferences.getFont(TerminatorPreferences.FONT));
		sizeChanged();
	}
	
	public BirdsEye getBirdsEye() {
		return birdsEye;
	}
	
	public void setBirdView(BirdView birdView) {
		this.birdView = birdView;
	}
	
	public SelectionHighlighter getSelectionHighlighter() {
		return selectionHighlighter;
	}
	
	public void userIsTyping() {
		blinkOn = true;
		redrawCursorPosition();
		if (Terminator.getPreferences().getBoolean(TerminatorPreferences.HIDE_MOUSE_WHEN_TYPING)) {
			setCursor(GuiUtilities.INVISIBLE_CURSOR);
		}
	}
	
	private void becomeDropTarget() {
		new TerminalDropTarget(this);
	}
	
	public TerminalModel getModel() {
		return model;
	}
	
	/**
	 * Pastes the text on the clipboard into the terminal.
	 */
	public void pasteSystemClipboard() {
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
	
	/**
	 * Pastes the system selection on X11, the clipboard on Windows
	 * and nothing on Mac OS X.
	 */
	public void middleButtonPaste() {
		if (GuiUtilities.isWindows()) {
			pasteSystemClipboard();
		} else {
			pasteSystemSelection();
		}
	}
		
	private void pasteClipboard(Clipboard clipboard) {
		try {
			Transferable contents = clipboard.getContents(this);
			String string = (String) contents.getTransferData(DataFlavor.stringFlavor);
			terminalControl.sendUtf8String(string);
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
	
	/** Returns our visible size. */
	public Dimension getVisibleSize() {
		JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		return scrollPane.getViewport().getExtentSize();
	}
	
	/**
	 * Returns the dimensions of an average character. Note that even though
	 * we use a fixed-width font, some glyphs for non-ASCII characters can
	 * be wider than this. See Markus Kuhn's UTF-8-demo.txt for examples,
	 * particularly among the Greek (where some glyphs are normal-width
	 * and others are wider) and Japanese (where most glyphs are wide).
	 * 
	 * This isn't exactly deprecated, but you should really think hard
	 * before using it.
	 */
	public Dimension getCharUnitSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		int width = metrics.charWidth('W');
		int height = metrics.getHeight();
		// Avoid divide by zero errors, so the user gets a chance to change their font.
		if (width == 0) {
			Log.warn("Insane font width for " + getFont());
			width = 1;
		}
		if (height == 0) {
			Log.warn("Insane font height for " + getFont());
			height = 1;
		}
		return new Dimension(width, height);
	}
	
	/**
	 * Returns our size in character units, where 'width' is the number of
	 * columns and 'height' the number of rows. (In case you were concerned
	 * about the fact that terminals tend to refer to y,x coordinates.)
	 */
	public Dimension getVisibleSizeInCharacters() {
		Dimension result = getVisibleSize();
		Insets insets = getInsets();
		result.width -= (insets.left + insets.right);
		result.height -= (insets.top + insets.bottom);
		Dimension character = getCharUnitSize();
		result.width /= character.width;
		result.height /= character.height;
		return result;
	}
	
	// Methods used by TerminalModel in order to update the display.
	
	public void linesChangedFrom(int lineIndex) {
		Point redrawTop = modelToView(new Location(lineIndex, 0)).getLocation();
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
	
	public void scrollToBottomButNotHorizontally() {
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		
		BoundedRangeModel verticalModel = pane.getVerticalScrollBar().getModel();
		verticalModel.setValue(verticalModel.getMaximum() - verticalModel.getExtent());
	}
	
	public void scrollToEnd() {
		scrollToBottomButNotHorizontally();
		scrollHorizontallyToShowCursor();
	}
	
	private boolean isLineVisible(int lineIndex) {
		return (lineIndex >= getFirstVisibleLine() && lineIndex <= getLastVisibleLine());
	}
	
	public void scrollHorizontallyToShowCursor() {
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
		
		if (isLineVisible(getCursorPosition().getLineIndex()) == false) {
			// We shouldn't be jumping the horizontal scroll bar
			// about because of new output if the user's trying to
			// review the history.
			return;
		}
		
		// mutt(1) likes to leave the cursor one character off the right of the bottom line.
		if (displayCursor == false) {
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
		if (wereAtBottom || Terminator.getPreferences().getBoolean(TerminatorPreferences.SCROLL_ON_TTY_OUTPUT)) {
			scrollToBottomButNotHorizontally();
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
		return Terminator.getPreferences().getColor(blinkOn ? TerminatorPreferences.CURSOR_COLOR : TerminatorPreferences.BACKGROUND_COLOR);
	}
	
	public void blinkCursor() {
		blinkOn = !blinkOn;
		redrawCursorPosition();
	}
	
	public Location viewToModel(Point point) {
		Insets insets = getInsets();
		int lineIndex = (point.y - insets.top) / getCharUnitSize().height;
		int charOffset = 0;
		// If the line index is off the top or bottom, we leave charOffset = 0.  This gives us nicer
		// selection functionality.
		if (lineIndex >= model.getLineCount()) {
			lineIndex = model.getLineCount();
		} else if (lineIndex < 0) {
			lineIndex = 0;
		} else {
			char[] chars = model.getTextLine(lineIndex).getString().toCharArray();
			if (chars.length > 0) {
				charOffset = GuiUtilities.getCharOffset(getFontMetrics(getFont()), 0, point.x - insets.left, chars);
			}
		}
		return new Location(lineIndex, charOffset);
	}
	
	public Rectangle modelToView(Location charCoords) {
		// We can be asked the view rectangle of locations that are past the bottom of the text in various circumstances. Examples:
		// 1. If the user sweeps a selection too far.
		// 2. If the user starts a new shell, types "man bash", and then clears the history; we move the cursor, and want to know the old cursor location to remove the cursor from, even though there's no longer any text there.
		// Rather than have special case code in each caller, simply return a reasonable result.
		// Note that it's okay to have the empty string as the default here because we'll pad if necessary later in this method.
		String line = "";
		if (charCoords.getLineIndex() < model.getLineCount()) {
			line = model.getTextLine(charCoords.getLineIndex()).getString();
		}
		
		final int offset = Math.max(0, charCoords.getCharOffset());
		
		String characterAtLocation;
		if (line.length() == offset) {
			// A very common case is where the location is one past the end of the line.
			// We don't need to add a single space if we're just going to  pull it off again.
			// This might not seem like much, but it can be costly if you've got very long lines.
			characterAtLocation = " ";
		} else {
			// Pad the line if we need to.
			final int desiredLength = offset + 1;
			if (line.length() < desiredLength) {
				final int charactersOfPaddingRequired = desiredLength - line.length();
				line += StringUtilities.nCopies(charactersOfPaddingRequired, " ");
			}
			characterAtLocation = line.substring(offset, offset + 1);
		}
		
		String lineBeforeOffset = line.substring(0, offset);
		FontMetrics fontMetrics = getFontMetrics(getFont());
		Insets insets = getInsets();
		final int x = insets.left + fontMetrics.stringWidth(lineBeforeOffset);
		final int width = fontMetrics.stringWidth(characterAtLocation);
		final int height = getCharUnitSize().height;
		final int y = insets.top + charCoords.getLineIndex() * height;
		return new Rectangle(x, y, width, height);
	}
	
	public Dimension getOptimalViewSize() {
		Dimension character = getCharUnitSize();
		Insets insets = getInsets();
		// FIXME: really, we need to track the maximum pixel width.
		final int width = insets.left + model.getMaxLineWidth() * character.width + insets.right;
		final int height = insets.top + model.getLineCount() * character.height + insets.bottom;
		return new Dimension(width, height);
	}
	
	// Highlighting support.
	
	/**
	 * Adds a highlighter. Highlighters are referred to by class, so it's
	 * a bad idea to have more than one of the same class.
	 */
	public <T extends Highlighter> void addHighlighter(T highlighter) {
		Class<? extends Highlighter> kind = highlighter.getClass();
		if (highlighters.get(kind) != null) {
			throw new IllegalArgumentException("duplicate " + kind);
		}
		highlighters.put(kind, highlighter);
	}
	
	/**
	 * Returns the highlighter of the given class.
	 */
	@SuppressWarnings("unchecked") // FIXME: can we give highlighters the correct type and avoid this?
	public <T extends Highlighter> T getHighlighterOfClass(Class<T> kind) {
		return (T) highlighters.get(kind);
	}
	
	public Collection<Highlighter> getHighlighters() {
		return Collections.unmodifiableCollection(highlighters.values());
	}
	
	private void redoHighlightsFrom(int firstLineIndex) {
		removeHighlightsFrom(firstLineIndex);
		for (Highlighter highlighter : getHighlighters()) {
			highlighter.addHighlights(this, firstLineIndex);
		}
	}
	
	public void removeHighlightsFrom(int firstLineIndex) {
		if (firstLineIndex == 0) {
			lineHighlights.clear();
			birdView.clearMatchingLines();
			repaint();
		} else {
			birdView.setValueIsAdjusting(true);
			try {
				// We use a backwards loop because going forwards results in N array copies if we're removing N lines.
				for (int i = (lineHighlights.size() - 1); i >= firstLineIndex; --i) {
					lineHighlights.remove(i);
					birdView.removeMatchingLine(i);
				}
				repaintFromLine(firstLineIndex);
			} finally {
				birdView.setValueIsAdjusting(false);
			}
		}
	}
	
	public void removeHighlightsFrom(Highlighter highlighter, int firstLineIndex) {
		if (highlighter instanceof FindHighlighter) {
			birdView.clearMatchingLines();
		}
		for (int i = firstLineIndex; i < lineHighlights.size(); i++) {
			ArrayList<Highlight> list = lineHighlights.get(i);
			if (list != null) {
				Iterator<Highlight> it = list.iterator();
				while (it.hasNext()) {
					Highlight highlight = it.next();
					if (highlight.getHighlighter() == highlighter) {
						it.remove();
						repaintHighlight(highlight);
					}
				}
			}
		}
	}
	
	public BirdView getBirdView() {
		return birdView;
	}
	
	public void addHighlight(Highlight highlight) {
		if (highlight.getHighlighter() instanceof FindHighlighter) {
			birdView.addMatchingLine(highlight.getStart().getLineIndex());
		}
		for (int i = highlight.getStart().getLineIndex(); i <= highlight.getEnd().getLineIndex(); i++) {
			addHighlightAtLine(highlight, i);
		}
		repaintHighlight(highlight);
	}
	
	private void repaintFromLine(int firstLineToRepaint) {
		int top = modelToView(new Location(firstLineToRepaint, 0)).y;
		Dimension size = getSize();
		repaint(0, top, size.width, size.height - top);
	}
	
	private void repaintHighlights(final Collection<Highlight> highlights) {
		for (Highlight highlight : highlights) {
			repaintHighlight(highlight);
		}
	}
	
	private void repaintHighlight(Highlight highlight) {
		Point redrawStart = modelToView(highlight.getStart()).getLocation();
		Rectangle endRect = modelToView(highlight.getEnd());
		Point redrawEnd = new Point(endRect.x + endRect.width, endRect.y + endRect.height);
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
		ArrayList<Highlight> highlightsForThisLine = lineHighlights.get(lineIndex);
		if (highlightsForThisLine.contains(highlight) == false) {
			highlightsForThisLine.add(highlight);
		}
	}
	
	/**
	 * Searches from startLine to endLine inclusive, incrementing the
	 * current line by 'direction', looking for a line with a find highlight.
	 * When one is found, the cursor is moved there.
	 */
	private void findAgain(Class<? extends Highlighter> highlighterClass, int startLine, int endLine, int direction) {
		for (int i = startLine; i != endLine; i += direction) {
			List<Highlight> highlights = getHighlightsForLine(i);
			Highlight match = firstHighlightOfClass(highlights, highlighterClass);
			if (match != null) {
				scrollTo(i, match.getStart().getCharOffset(), match.getEnd().getCharOffset());
				birdsEye.setCurrentLineIndex(i);
				// Highlight the new match in the bird view as well as in the text itself.
				birdView.repaint();
				return;
			}
		}
	}
	
	/**
	 * Tests whether any of the Highlight objects in the list is a FindHighlighter.
	 */
	private static Highlight firstHighlightOfClass(List<Highlight> highlights, Class<? extends Highlighter> highlighterClass) {
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
	public void findNext(Class<? extends Highlighter> highlighterClass) {
		findAgain(highlighterClass, getLastVisibleLine() + 1, getModel().getLineCount() + 1, 1);
	}
	
	/**
	 * Scrolls the display up to the next highlight of the given class not currently on the display.
	 */
	public void findPrevious(Class<? extends Highlighter> highlighterClass) {
		findAgain(highlighterClass, getFirstVisibleLine() - 1, -1, -1);
	}
	
	public JViewport getViewport() {
		return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
	}
	
	public int getFirstVisibleLine() {
		int lineHeight = getCharUnitSize().height;
		Rectangle visibleBounds = getViewport().getViewRect();
		return visibleBounds.y / lineHeight;
	}
	
	public int getLastVisibleLine() {
		int lineHeight = getCharUnitSize().height;
		Rectangle visibleBounds = getViewport().getViewRect();
		return (visibleBounds.y + visibleBounds.height) / lineHeight;
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
		return Collections.unmodifiableList(result);
	}
	
	public boolean isHighlightUnderMouse(Highlight highlight) {
		return highlightsUnderMouse.contains(highlight);
	}
	
	/** Returns a (possibly empty) list containing all highlights in the indexed line. */
	private List<Highlight> getHighlightsForLine(int lineIndex) {
		if (lineIndex >= lineHighlights.size() || lineHighlights.get(lineIndex) == null) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(lineHighlights.get(lineIndex));
		}
	}
	
	public String getTabbedString(Highlight highlight) {
		Location start = highlight.getStart();
		Location end = highlight.getEnd();
		StringBuilder buf = new StringBuilder();
		for (int i = start.getLineIndex(); i <= end.getLineIndex(); i++) {
			// Necessary to cope with selections extending to the bottom of the buffer.
			if (i == end.getLineIndex() && end.getCharOffset() == 0) {
				break;
			}
			TextLine textLine = model.getTextLine(i);
			int lineStart = (i == start.getLineIndex()) ? start.getCharOffset() : 0;
			int lineEnd = (i == end.getLineIndex()) ? end.getCharOffset() : textLine.length();
			buf.append(textLine.getTabbedString(lineStart, lineEnd));
			if (i != end.getLineIndex()) {
				buf.append('\n');
			}
		}
		return buf.toString();
	}
	
	// Redraw code.
	
	private void redrawCursorPosition() {
		Rectangle cursorRect = modelToView(cursorPosition);
		repaint(cursorRect);
	}
	
	public void paintComponent(Graphics oldGraphics) {
		Stopwatch.Timer timer = paintComponentStopwatch.start();
		try {
			Graphics2D g = (Graphics2D) oldGraphics;
			
			Object antiAliasHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, Terminator.getPreferences().getBoolean(TerminatorPreferences.ANTI_ALIAS) ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			
			FontMetrics metrics = getFontMetrics(getFont());
			Dimension charUnitSize = getCharUnitSize();
			
			Rectangle rect = g.getClipBounds();
			g.setColor(getBackground());
			g.fill(rect);
			
			// We manually "clip" for performance, but we're quite loose about it.
			// This avoids accidental pathological cases (hopefully) and doesn't seem to have any significant cost.
			final int maxX = rect.x + rect.width;
			final int widthHintInChars = maxX / charUnitSize.width * 2;
			
			Insets insets = getInsets();
			int firstTextLine = (rect.y - insets.top) / charUnitSize.height;
			int lastTextLine = (rect.y - insets.top + rect.height + charUnitSize.height - 1) / charUnitSize.height;
			lastTextLine = Math.min(lastTextLine, model.getLineCount() - 1);
			int lineNotToDraw = model.usingAlternateBuffer() ? model.getFirstDisplayLine() - 1 : -1;
			for (int i = firstTextLine; i <= lastTextLine; i++) {
				if (i == lineNotToDraw) {
					continue;
				}
				boolean drawCursor = (shouldShowCursor() && i == cursorPosition.getLineIndex());
				int x = insets.left;
				int baseline = insets.top + charUnitSize.height * (i + 1) - metrics.getMaxDescent();
				int startOffset = 0;
				Iterator<StyledText> it = getLineStyledText(i, widthHintInChars).iterator();
				while (it.hasNext() && x < maxX) {
					StyledText chunk = it.next();
					x += paintStyledText(g, metrics, chunk, x, baseline);
					String chunkText = chunk.getText();
					if (drawCursor && cursorPosition.charOffsetInRange(startOffset, startOffset + chunkText.length())) {
						final int charOffsetUnderCursor = cursorPosition.getCharOffset() - startOffset;
						paintCursor(g, chunkText.substring(charOffsetUnderCursor, charOffsetUnderCursor + 1), baseline);
						drawCursor = false;
					}
					startOffset += chunkText.length();
				}
				if (drawCursor) {
					// A cursor at the end of the line is in a position past the end of the text.
					paintCursor(g, "", baseline);
				}
			}
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasHint);
		} finally {
			timer.stop();
		}
	}
	
	private List<StyledText> getLineStyledText(int line, int widthHintInChars) {
		List<StyledText> result = model.getTextLine(line).getStyledTextSegments(widthHintInChars);
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
	private void paintCursor(Graphics2D g, String characterUnderCursor, int baseline) {
		g.setColor(getCursorColor());
		Rectangle cursorRect = modelToView(cursorPosition);
		final int bottomY = cursorRect.y + cursorRect.height - 1;
		if (hasFocus) {
			TerminatorPreferences preferences = Terminator.getPreferences();
			// The CursorBlinker may have left blinkOn in either state if the user changed the cursor blink preference.
			// Ignore blinkOn if the cursor shouldn't be blinking right now.
			boolean cursorIsVisible = (preferences.getBoolean(TerminatorPreferences.BLINK_CURSOR) == false) || blinkOn;
			if (preferences.getBoolean(TerminatorPreferences.BLOCK_CURSOR)) {
				// Block.
				if (cursorIsVisible) {
					// Paint over the character underneath.
					g.fill(cursorRect);
					// Redraw the character in the
					// background color.
					g.setColor(getBackground());
					g.drawString(characterUnderCursor, cursorRect.x, baseline);
				}
			} else {
				// Underline.
				if (cursorIsVisible) {
					g.drawLine(cursorRect.x, bottomY, cursorRect.x + cursorRect.width - 1, bottomY);
				}
			}
		} else {
			// For some reason, terminals always seem to use an
			// empty block for the unfocused cursor, regardless
			// of what shape they're using for the focused cursor.
			// It's not obvious what else they could do that would
			// look better.
			g.drawRect(cursorRect.x, cursorRect.y, cursorRect.width - 1, cursorRect.height - 1);
		}
	}
	
	/**
	 * Paints the text. Returns how many pixels wide the text was.
	 */
	private int paintStyledText(Graphics2D g, FontMetrics metrics, StyledText text, int x, int y) {
		Stopwatch.Timer timer = paintStyledTextStopwatch.start();
		try {
			Style style = text.getStyle();
			Color foreground = style.getForeground();
			Color background = style.getBackground();
			
			if (style.isReverseVideo()) {
				Color oldForeground = foreground;
				foreground = background;
				background = oldForeground;
			}
			
			int textWidth = metrics.stringWidth(text.getText());
			if (background.equals(getBackground()) == false) {
				g.setColor(background);
				// Special continueToEnd flag used for drawing the backgrounds of Highlights which extend over the end of lines.
				// Used for multi-line selection.
				int backgroundWidth = text.continueToEnd() ? (getSize().width - x) : textWidth;
				g.fillRect(x, y - metrics.getMaxAscent() - metrics.getLeading(), backgroundWidth, metrics.getHeight());
			}
			if (style.isUnderlined()) {
				g.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 128));
				g.drawLine(x, y + 1, x + textWidth, y + 1);
			}
			g.setColor(foreground);
			g.drawString(text.getText(), x, y);
			if (style.isBold()) {
				// A font doesn't necessarily have a bold.
				// Mac OS X's "Monaco" font is an example.
				// The trouble is, you can't tell from the Font you get back from deriveFont.
				// isBold will always return true, and getting the WEIGHT attribute will give you WEIGHT_BOLD.
				// So we don't know how to test for a bold font.
				
				// Worse, if we actually get a bold font, it doesn't necessarily have metrics compatible with the plain variant.
				// ProggySquare (http://www.proggyfonts.com/) is an example: the bold variant is significantly wider.
				
				// The old-fashioned "overstrike" method of faking bold doesn't look too bad, and it works in these awkward cases.
				g.drawString(text.getText(), x + 1, y);
			}
			return textWidth;
		} finally {
			timer.stop();
		}
	}
	
	public boolean hasFocus() {
		return hasFocus;
	}
	
	//
	// FocusListener interface.
	//
	
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
			return 3 * getCharUnitSize().width;
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
		return false;
	}
	
	public boolean getScrollableTracksViewportHeight() {
		return false; // We want a vertical scroll-bar.
	}
}
