package terminator.view;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
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

	private TerminalPaneMaster controller;
	private TextBuffer model;
	private Location caretPosition = new Location(0, 0);
	private boolean hasFocus = false;
	private boolean displayCaret = true;
	private boolean blinkOn = true;
	private CursorBlinker cursorBlinker;
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
	
	public JTextBuffer(TerminalPaneMaster controller) {
		this.controller = controller;
		Options options = Options.getSharedInstance();
		model = new TextBuffer(this, options.getInitialColumnCount(), options.getInitialRowCount());
		TerminatorFrame.disableFocusTraversal(this);
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
		addHighlighter(new FindHighlighter());
		becomeDropTarget();
		cursorBlinker = new CursorBlinker(this);
		new Selector(this);
	}
	
	public static final Cursor INVISIBLE_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), new Point(0, 0), "invisible");
	
	public void userIsTyping() {
		blinkOn = true;
		redrawCaretPosition();
		setCursor(INVISIBLE_CURSOR);
	}
	
	public TerminalPaneMaster getTerminalPaneMaster() {
		return controller;
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
		try {
			Toolkit toolkit = getToolkit();
			Transferable contents = toolkit.getSystemClipboard().getContents(this);
			if (toolkit.getSystemSelection() != null) {
				contents = toolkit.getSystemSelection().getContents(this);
			}
			DataFlavor[] transferFlavors = contents.getTransferDataFlavors();
			String string = (String) contents.getTransferData(DataFlavor.stringFlavor);
			insertText(string);
		} catch (Exception ex) {
			Log.warn("Couldn't paste.", ex);
		}
	}
	
	private TerminalControl terminalControl;
	
	public void setTerminalControl(TerminalControl terminalControl) {
		this.terminalControl = terminalControl;
	}
	
	public void insertText(final String text) {
		new Thread() {
			public void run() {
				for (int i = 0; i < text.length(); i++) {
					terminalControl.sendChar(text.charAt(i));
				}
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
		BoundedRangeModel barModel = pane.getVerticalScrollBar().getModel();
		barModel.setValue(barModel.getMaximum() - barModel.getExtent());
		barModel = pane.getHorizontalScrollBar().getModel();
		barModel.setValue(barModel.getMinimum());
//		scrollRectToVisible(new Rectangle(0, getHeight() - 10, 10, 11));
	}
	
	public void scrollToTop() {
		scrollToLine(0);
	}
	
	public void scrollToLine(final int lineNumber) {
		Dimension character = getCharUnitSize();
		scrollRectToVisible(new Rectangle(0, lineNumber * character.height - 10, 10, character.height + 20));
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
	
	public boolean shouldShowCursor() {
		return displayCaret;
	}
	
	public Color getCursorColour() {
		if (blinkOn) {
			return Options.getSharedInstance().getColor("cursorColor");
		} else {
			return Options.getSharedInstance().getColor("background");
		}
	}
	
	public void blinkCursor() {
		blinkOn = !blinkOn;
		redrawCaretPosition();
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
	
	private Point getBaseline(Location charCoords) {
		Dimension character = getCharUnitSize();
		return new Point(charCoords.getCharOffset() * character.width, (charCoords.getLineIndex() + 1) * character.height - getFontMetrics(getFont()).getMaxDescent());
	}
	
	public Dimension getOptimalViewSize() {
		Dimension character = getCharUnitSize();
		return new Dimension(model.getMaxLineWidth() * character.width, model.getLineCount() * character.height);
	}
	
	public void clearScrollBuffer() {
		model.reset();
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
		return (Highlighter) highlighters.get(kind);
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
//		while (lineHighlights.size() > firstLineIndex) {
//			ArrayList list = (ArrayList) lineHighlights.remove(firstLineIndex);
//			if (list != null) {
//				Highlight[] highlights = (Highlight[]) list.toArray(new Highlight[list.size()]);
//				for (int j = 0; j < highlights.length; j++) {
//					repaintHighlight(highlights[j]);
//				}
//			}
//		}
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
			lineHighlights.set(lineIndex, new ArrayList());
		}
		ArrayList line = (ArrayList) lineHighlights.get(lineIndex);
		if (line.indexOf(highlight) == -1) {
			line.add(highlight);
		}
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
	
	public String getTabbedText(Highlight highlight) {
		Location start = highlight.getStart();
		Location end = highlight.getEnd();
		StringBuffer buf = new StringBuffer();
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
	
	private void redrawCaretPosition() {
		Point top = getLineTop(caretPosition);
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
		int lineNotToDraw = model.usingAlternativeBuffer() ? model.getFirstDisplayLine() - 1 : -1;
		for (int i = firstTextLine; i <= lastTextLine; i++) {
			if (i == lineNotToDraw) {
				continue;
			}
			boolean drawCaret = (shouldShowCursor() && i == caretPosition.getLineIndex());
			StyledText[] lineText = getLineText(i);
			int x = 0;
			int baseline = metrics.getHeight() * (i + 1) - metrics.getMaxDescent();
			for (int j = 0; j < lineText.length; j++) {
				x += paintStyledText(graphics, lineText[j], x, baseline, drawCaret);
			}
			if (lineText.length == 0 && drawCaret) {
				paintCaret(graphics, metrics);
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
		graphics.setColor(getCursorColour());
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
		
		// Sanity check for top weirdness.
		if (style.getForeground().equals(style.getBackground())) {
			Log.warn("Foreground and background colours are identical (text \"" + text.getText() + "\").");
		}
		
		int textWidth = metrics.stringWidth(text.getText());
		graphics.setColor(style.getBackground());
		// Special continueToEnd flag used for drawing the backgrounds of Highlights which extend
		// over the end of lines.  Used for multi-line selection.
		int backgroundWidth = text.continueToEnd() ? (getSize().width - x) : textWidth;
		graphics.fillRect(x, y - metrics.getMaxAscent(), backgroundWidth, metrics.getHeight());
		if (drawCaret) {
			paintCaret(graphics, metrics);
		}
		Color foreground = style.getForeground();
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
		redrawCaretPosition();
	}
	
	public void focusLost(FocusEvent event) {
		hasFocus = false;
		blinkOn = true;
		cursorBlinker.stop();
		redrawCaretPosition();
	}
	
	public boolean hasFocus() {
		return hasFocus;
	}
}
