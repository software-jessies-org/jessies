package e.ptextarea;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.util.List;
import e.util.*;

/**
 * A PTextArea is a replacement for JTextArea.
 * 
 * @author Phil Norman
 */

public class PTextArea extends JComponent implements PLineListener, Scrollable {
    private static final int MIN_WIDTH = 50;
    private static final int MAX_CACHED_CHAR = 128;
    private static final int NO_MARGIN = -1;
    
    private static final Color MARGIN_BOUNDARY_COLOR = new Color(0.6f, 0.6f, 0.6f);
    private static final Color MARGIN_OUTSIDE_COLOR = new Color(0.96f, 0.96f, 0.96f);
    private static final Color SELECTION_COLOR = new Color(0.70f, 0.83f, 1.00f, 0.5f);
    private static final Color SELECTION_BOUNDARY_COLOR = new Color(0.5f, 0.55f, 0.7f, 0.75f);
    
    private static final Stroke WRAP_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.0f, 2.0f }, 0.0f);
    
    private PLineList lines;
    private PHighlight selection;
    private List splitLines;  // TODO - Write a split buffer-style List implementation.
    private int[] widthCache;
    private int caretLocation;
    private PAnchorSet anchorSet = new PAnchorSet();
    private ArrayList highlights = new ArrayList();
    private PTextStyler textStyler = PPlainTextStyler.INSTANCE;
    private int rightHandMarginColumn = NO_MARGIN;
    
    private int rowCount;
    private int columnCount;
    
    private PTextAreaSpellingChecker spellingChecker;
    
    public PTextArea() {
        this(0, 0);
    }
    
    public PTextArea(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        setAutoscrolls(true);
        setBackground(Color.WHITE);
        setText(new PTextBuffer());
        PMouseHandler mouseHandler = new PMouseHandler(this);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addKeyListener(new PKeyHandler(this));
        addComponentListener(new ComponentAdapter() {
            private int lastWidth;
            
            public void componentResized(ComponentEvent event) {
                if (getSize().width != lastWidth) {
                    lastWidth = getSize().width;
                    invalidateLineWrappings();
                }
            }
        });
        setOpaque(true);
        setFocusTraversalKeysEnabled(false);
        requestFocus();
        initSpellingChecking();
    }
    
    // Selection methods.
    public String getSelectedText() {
        if (selection == null) {
            return "";
        } else {
            return getPTextBuffer().subSequence(selection.getStartIndex(), selection.getEndIndex()).toString();
        }
    }
    
    public int getSelectionStart() {
        return (selection == null) ? getCaretLocation() : selection.getStartIndex();
    }
    
    public int getSelectionEnd() {
        return (selection == null) ? getCaretLocation() : selection.getEndIndex();
    }
    
    public void replaceSelection(String newContent) {
        throw new UnsupportedOperationException("Can't do this yet.");
    }
    
    public void clearSelection() {
        select(0, 0);
    }
    
    public void select(int start, int end) {
        PHighlight oldSelection = selection;
        if (start == end) {
            selection = null;
        } else {
            selection = new SelectionHighlight(this, start, end);
        }
        if ((oldSelection == null) != (selection == null)) {
            repaintAnchorRegion(selection == null ? oldSelection : selection);
        } else if (oldSelection != null && selection != null) {
            int minStart = Math.min(oldSelection.getStartIndex(), selection.getStartIndex());
            int maxStart = Math.max(oldSelection.getStartIndex(), selection.getStartIndex());
            if (minStart != maxStart) {
                repaintLines(getCoordinates(minStart).getLineIndex(), getCoordinates(maxStart).getLineIndex() + 1);
            }
            int minEnd = Math.min(oldSelection.getEndIndex(), selection.getEndIndex());
            int maxEnd = Math.max(oldSelection.getEndIndex(), selection.getEndIndex());
            if (minEnd != maxEnd) {
                repaintLines(getCoordinates(minEnd).getLineIndex() - 1, getCoordinates(maxEnd).getLineIndex());
            }
        }
    }
    
    public boolean hasSelection() {
        return (selection != null);
    }
    
    public void selectAll() {
        select(0, getPTextBuffer().length());
    }
    
    private void initSpellingChecking() {
        spellingChecker = new PTextAreaSpellingChecker(this);
        spellingChecker.checkSpelling();
    }
    
    // Utility methods.
    public int getLineCount() {
        return lines.size();
    }
    
    public int getLineStartOffset(int line) {
        return lines.getLine(line).getStart();
    }
    
    public int getLineEndOffset(int line) {
        return lines.getLine(line).getEndOffsetBeforeTerminator();
    }
    
    public int getLineOfOffset(int offset) {
        return getSplitLine(getCoordinates(offset).getLineIndex()).getLineIndex();
    }
    
    public void showRightHandMarginAt(int rightHandMarginColumn) {
        this.rightHandMarginColumn = rightHandMarginColumn;
    }
    
    public void setTextStyler(PTextStyler textStyler) {
        this.textStyler = textStyler;
        repaint();
    }
    
    public void setFont(Font font) {
        super.setFont(font);
        showRightHandMarginAt(GuiUtilities.isFontFixedWidth(font) ? 80 : NO_MARGIN);
        invalidateLineWrappings();
        generateLineWrappings();
        repaint();
    }
    
    public void addHighlight(PHighlight highlight) {
        highlights.add(highlight);
        repaintAnchorRegion(highlight);
    }
    
    public void removeHighlight(PHighlight highlight) {
        highlights.remove(highlight);
        repaintAnchorRegion(highlight);
    }
    
    public List getHighlights() {
        return Collections.unmodifiableList(highlights);
    }
    
    public void removeHighlights(PHighlightMatcher matcher) {
        PHighlight match = null;
        boolean isOnlyOneMatch = true;
        for (int i = 0; i < highlights.size(); i++) {
            PHighlight highlight = (PHighlight) highlights.get(i);
            if (matcher.matches(highlight)) {
                if (match != null) {
                    isOnlyOneMatch = false;
                }
                match = highlight;
                highlights.remove(i);
                i--;
            }
        }
        if (isOnlyOneMatch) {
            if (match != null) {
                repaintAnchorRegion(match);
            }
            // If isOnlyOneMatch is true, but match is null, nothing was removed and so we don't repaint at all.
        } else {
            repaint();
        }
    }
    
    public PAnchorSet getAnchorSet() {
        return anchorSet;
    }
    
    public PLineList getLineList() {
        return lines;
    }
    
    public PTextBuffer getPTextBuffer() {
        return lines.getPTextBuffer();
    }
    
    public PCoordinates getNearestCoordinates(Point point) {
        if (point.y < 0) {
            return new PCoordinates(0, 0);
        }
        generateLineWrappings();
        final int maxLineIndex = splitLines.size() - 1;
        FontMetrics metrics = getFontMetrics(getFont());
        int lineIndex = point.y / metrics.getHeight();
        if (lineIndex > maxLineIndex) {
            point.x = Integer.MAX_VALUE;
        }
        lineIndex = Math.max(0, Math.min(maxLineIndex, lineIndex));
        PLineSegment[] segments = getLineSegments(lineIndex);
        int charOffset = 0;
        int x = 0;
        for (int i = 0; i < segments.length; i++) {
            int width = segments[i].getDisplayWidth(metrics, x);
            if (x + width > point.x) {
                charOffset += segments[i].getCharOffset(metrics, x, point.x);
                return new PCoordinates(lineIndex, charOffset);
            }
            charOffset += segments[i].getText().length();
            x += width;
        }
        return new PCoordinates(lineIndex, charOffset);
    }
    
    private PLineSegment[] getLineSegments(int lineIndex) {
        SplitLine splitLine = getSplitLine(lineIndex);
        PTextSegment[] text = textStyler.getLineSegments(splitLine);
        return getTabbedSegments(text);
    }
    
    private PLineSegment[] getTabbedSegments(PTextSegment[] segments) {
        ArrayList result = new ArrayList();
        for (int i = 0; i < segments.length; i++) {
            addTabbedSegments(segments[i], result);
        }
        return (PLineSegment[]) result.toArray(new PLineSegment[result.size()]);
    }
    
    private void addTabbedSegments(PTextSegment segment, ArrayList target) {
        while (true) {
            String text = segment.getText();
            int tabIndex = text.indexOf('\t');
            if (tabIndex == -1) {
                target.add(segment);
                return;
            }
            target.add(segment.subSegment(0, tabIndex));
            int tabEnd = tabIndex;
            while (tabEnd < text.length() && text.charAt(tabEnd) == '\t') {
                tabEnd++;
            }
            target.add(new PTabSegment(text.substring(tabIndex, tabEnd)));
            segment = segment.subSegment(tabEnd);
            if (segment.getLength() == 0) {
                return;
            }
        }
    }
    
    public PCoordinates getCoordinates(int location) {
        int min = 0;
        int max = splitLines.size();
        while (max - min > 1) {
            int mid = (min + max) / 2;
            SplitLine line = getSplitLine(mid);
            if (line.containsIndex(location)) {
                return new PCoordinates(mid, location - line.getTextIndex());
            } else if (location < line.getTextIndex()) {
                max = mid;
            } else {
                min = mid;
            }
        }
        return new PCoordinates(min, location - getSplitLine(min).getTextIndex());
    }
    
    public Point getViewCoordinates(PCoordinates coordinates) {
        FontMetrics metrics = getFontMetrics(getFont());
        int baseline = getBaseline(metrics, coordinates.getLineIndex());
        PLineSegment[] segments = getLineSegments(coordinates.getLineIndex());
        int x = 0;
        int charOffset = 0;
        for (int i = 0; i < segments.length; i++) {
            String text = segments[i].getText();
            if (coordinates.getCharOffset() < charOffset + text.length()) {
                x += segments[i].getDisplayWidth(metrics, x, coordinates.getCharOffset() - charOffset);
                return new Point(x, baseline);
            }
            charOffset += text.length();
            x += segments[i].getDisplayWidth(metrics, x);
        }
        return new Point(x, baseline);
    }
    
    public int getTextIndex(PCoordinates coordinates) {
        return getSplitLine(coordinates.getLineIndex()).getTextIndex() + coordinates.getCharOffset();
    }
    
    public int getCaretLocation() {
        return caretLocation;
    }
    
    public void setCaretPosition(int newLocation) {
        if (newLocation != caretLocation) {
            repaintCaret();
            caretLocation = newLocation;
            repaintCaret();
            Point point = getViewCoordinates(getCoordinates(caretLocation));
            FontMetrics metrics = getFontMetrics(getFont());
            scrollRectToVisible(new Rectangle(point.x - 1, point.y - metrics.getMaxAscent(), 3, metrics.getHeight()));
        }
    }
    
    private synchronized void repaintAnchorRegion(PAnchorRegion anchorRegion) {
        repaintIndexRange(anchorRegion.getStartIndex(), anchorRegion.getEndIndex());
    }
    
    private void repaintIndexRange(int startIndex, int endIndex) {
        if (splitLines == null) {
            return;
        }
        PCoordinates start = getCoordinates(startIndex);
        PCoordinates end = getCoordinates(endIndex);
        repaintLines(start.getLineIndex(), end.getLineIndex());
    }
    
    private void repaintCaret() {
        Point point = getViewCoordinates(getCoordinates(caretLocation));
        FontMetrics metrics = getFontMetrics(getFont());
        repaint(point.x - 1, point.y - metrics.getMaxAscent(), 3, metrics.getMaxAscent() + metrics.getMaxDescent());
    }
    
    public int getLineTop(int lineIndex) {
        return lineIndex * getFontMetrics(getFont()).getHeight();
    }
    
    public int getLineHeight() {
        return getFontMetrics(getFont()).getHeight();
    }
    
    private int getBaseline(FontMetrics metrics, int lineIndex) {
        return (lineIndex + 1) * metrics.getHeight() - metrics.getMaxDescent();
    }
    
    public void paintComponent(Graphics oldGraphics) {
        //StopWatch watch = new StopWatch();
        initCharWidthCache();
        generateLineWrappings();
        Graphics2D graphics = (Graphics2D) oldGraphics;
        Rectangle bounds = graphics.getClipBounds();
        FontMetrics metrics = graphics.getFontMetrics();
        int whiteBackgroundWidth = paintRightHandMargin(graphics, bounds, metrics);
        graphics.setColor(getBackground());
        graphics.fillRect(bounds.x, bounds.y, whiteBackgroundWidth, bounds.height);
        int minLine = Math.min(splitLines.size() - 1, bounds.y / metrics.getHeight());
        int maxLine = Math.min(splitLines.size() - 1, (bounds.y + bounds.height) / metrics.getHeight());
        int baseline = getBaseline(metrics, minLine);
        PCoordinates caretCoords = getCoordinates(caretLocation);
        paintHighlights(graphics, minLine, maxLine);
        for (int i = minLine; i <= maxLine; i++) {
            PLineSegment[] segments = getLineSegments(i);
            boolean drawCaret = (caretCoords.getLineIndex() == i);
            boolean showWrap = isNextLineAContinuationOf(i);
            paintSegments(graphics, metrics, segments, baseline, caretCoords, drawCaret, showWrap);
            baseline += metrics.getHeight();
        }
        //watch.print("Repaint");
    }
    
    private boolean isNextLineAContinuationOf(int index) {
        final int nextIndex = index + 1;
        return (nextIndex < splitLines.size()) && (getSplitLine(index).getLineIndex() == getSplitLine(nextIndex).getLineIndex());
    }
    
    /**
     * Draws the right-hand margin, and returns the width of the rectangle
     * from bounds.x that should be filled with the non-margin background color.
     * Using this in paintComponent lets us avoid unnecessary flicker caused
     * by filling the area twice.
     */
    private int paintRightHandMargin(Graphics2D graphics, Rectangle bounds, FontMetrics metrics) {
        int whiteBackgroundWidth = bounds.width;
        if (rightHandMarginColumn != NO_MARGIN) {
            int offset = metrics.stringWidth("n") * rightHandMarginColumn;
            graphics.setColor(MARGIN_BOUNDARY_COLOR);
            graphics.drawLine(offset, bounds.y, offset, bounds.y + bounds.height);
            graphics.setColor(MARGIN_OUTSIDE_COLOR);
            graphics.fillRect(offset + 1, bounds.y, bounds.x + bounds.width - offset - 1, bounds.height);
            whiteBackgroundWidth = (offset - bounds.x);
        }
        return whiteBackgroundWidth;
    }
    
    private void paintHighlights(Graphics2D graphics, int minLine, int maxLine) {
        int minChar = getSplitLine(minLine).getTextIndex();
        SplitLine max = getSplitLine(maxLine);
        int maxChar = max.getTextIndex() + max.getLength();
        if (selection != null) {
            selection.paint(graphics);
        }
        for (int i = 0; i < highlights.size(); i++) {
            PHighlight highlight = (PHighlight) highlights.get(i);
            if (highlight.getStart().getIndex() <= maxChar && highlight.getEnd().getIndex() > minChar) {
                highlight.paint(graphics);
            }
        }
    }
    
    private void paintSegments(Graphics2D graphics, FontMetrics metrics, PLineSegment[] segments, int baseline, PCoordinates caretCoords, boolean drawCaret, boolean showWrap) {
        int x = 0;
        int charOffset = 0;
        for (int i = 0; i < segments.length; i++) {
            graphics.setColor(textStyler.getColorForStyle(segments[i].getStyleIndex()));
            String text = segments[i].getText();
            if (segments[i].isVisible()) {
                graphics.drawString(text, x, baseline);
            }
            if (drawCaret && caretCoords.getCharOffset() < charOffset + text.length()) {
                int caretX = x + segments[i].getDisplayWidth(metrics, x, caretCoords.getCharOffset() - charOffset);
                paintCaret(graphics, metrics, caretX, baseline);
                drawCaret = false;
            }
            charOffset += text.length();
            x += segments[i].getDisplayWidth(metrics, x);
        }
        if (showWrap) {
            paintWrapMark(graphics, metrics, x, baseline);
        }
        // If drawCaret is still set, the caret must be at the end of the line.
        if (drawCaret) {
            paintCaret(graphics, metrics, x, baseline);
        }
    }
    
    private void paintWrapMark(Graphics2D graphics, FontMetrics metrics, int x, int y) {
        graphics.setColor(Color.BLACK);
        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(WRAP_STROKE);
        int yMiddle = y - metrics.getMaxAscent() / 2;
        graphics.drawLine(x, yMiddle, getWidth(), yMiddle);
        graphics.setStroke(oldStroke);
    }
    
    private void paintCaret(Graphics2D graphics, FontMetrics metrics, int x, int y) {
        graphics.setColor(Color.RED);
        int yTop = y - metrics.getMaxAscent();
        int yBottom = y + metrics.getMaxDescent() - 1;
        graphics.drawLine(x, yTop + 1, x, yBottom - 1);
        graphics.drawLine(x, yTop + 1, x + 1, yTop);
        graphics.drawLine(x, yTop + 1, x - 1, yTop);
        graphics.drawLine(x, yBottom - 1, x + 1, yBottom);
        graphics.drawLine(x, yBottom - 1, x - 1, yBottom);
    }
    
    private void initCharWidthCache() {
        widthCache = new int[MAX_CACHED_CHAR];
        FontMetrics metrics = getFontMetrics(getFont());
        for (int i = 0; i < MAX_CACHED_CHAR; i++) {
            widthCache[i] = metrics.charWidth(i);
        }
    }

    public void linesAdded(PLineEvent event) {
        if (splitLines == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont());
        int lineIndex = event.getIndex();
        int splitIndex = getSplitLineIndex(lineIndex);
        int firstSplitIndex = splitIndex;
        changeLineIndices(lineIndex, event.getLength());
        for (int i = 0; i < event.getLength(); i++) {
            splitIndex += addSplitLines(lineIndex++, metrics, splitIndex);
        }
        updateHeight();
        repaintFromLine(firstSplitIndex);
    }
    
    public void linesRemoved(PLineEvent event) {
        if (splitLines == null) {
            return;
        }
        int splitIndex = getSplitLineIndex(event.getIndex());
        for (int i = 0; i < event.getLength(); i++) {
            removeSplitLines(splitIndex);
        }
        changeLineIndices(event.getIndex() + event.getLength(), -event.getLength());
        updateHeight();
        repaintFromLine(splitIndex);
    }
    
    private void changeLineIndices(int lineIndex, int change) {
        for (int i = getSplitLineIndex(lineIndex); i < splitLines.size(); i++) {
            SplitLine line = getSplitLine(i);
            line.setLineIndex(line.getLineIndex() + change);
        }
    }
    
    public void linesChanged(PLineEvent event) {
        if (splitLines == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont());
        int lineCountChange = 0;
        int minLine = Integer.MAX_VALUE;
        int visibleLineCount = 0;
        for (int i = 0; i < event.getLength(); i++) {
            PLineList.Line line = lines.getLine(event.getIndex() + i);
            setLineWidth(line, metrics);
            int splitIndex = getSplitLineIndex(event.getIndex());
            if (i == 0) {
                minLine = splitIndex;
            }
            int removedCount = removeSplitLines(splitIndex);
            lineCountChange -= removedCount;
            int addedCount = addSplitLines(event.getIndex(), metrics, splitIndex);
            lineCountChange += addedCount;
            visibleLineCount += addedCount;
        }
        if (lineCountChange != 0) {
            updateHeight();
            repaintFromLine(getSplitLineIndex(event.getIndex()));
        } else {
            repaintLines(minLine, minLine + visibleLineCount);
        }
    }
    
    public void repaintFromLine(int splitIndex) {
        int lineTop = getLineTop(splitIndex);
        Dimension size = getSize();
        repaint(0, lineTop, size.width, size.height - lineTop);
    }
    
    public void repaintLines(int minSplitIndex, int maxSplitIndex) {
        int top = getLineTop(minSplitIndex);
        int bottom = getLineTop(maxSplitIndex + 1);
        repaint(0, top, getSize().width, bottom - top);
    }
    
    private synchronized void invalidateLineWrappings() {
        splitLines = null;
    }

    private synchronized void generateLineWrappings() {
        if (splitLines == null) {
            splitLines = new ArrayList();
            FontMetrics metrics = getFontMetrics(getFont());
            for (int i = 0; i < lines.size(); i++) {
                PLineList.Line line = lines.getLine(i);
                if (line.isWidthValid() == false) {
                    setLineWidth(line, metrics);
                }
                addSplitLines(i, metrics, splitLines.size());
            }
            updateHeight();
        }
    }
    
    private void updateHeight() {
        Dimension size = getSize();
        size.height = getFontMetrics(getFont()).getHeight() * splitLines.size();
        setSize(size);
        setPreferredSize(size);
    }
    
    private int removeSplitLines(int splitIndex) {
        int lineIndex = getSplitLine(splitIndex).getLineIndex();
        int removedLineCount = 0;
        while (splitIndex < splitLines.size() && getSplitLine(splitIndex).getLineIndex() == lineIndex) {
            SplitLine line = getSplitLine(splitIndex);
            splitLines.remove(splitIndex);
            removedLineCount++;
        }
        return removedLineCount;
    }
    
    public int getSplitLineIndex(int lineIndex) {
        if (lineIndex > getSplitLine(splitLines.size() - 1).getLineIndex()) {
            return splitLines.size();
        }
        int min = 0;
        int max = splitLines.size();
        while (max - min > 1) {
            int mid = (min + max) / 2;
            int midIndex = getSplitLine(mid).getLineIndex();
            if (midIndex == lineIndex) {
                return backtrackToLineStart(mid);
            } else if (midIndex > lineIndex) {
                max = mid;
            } else {
                min = mid;
            }
        }
        return backtrackToLineStart(min);
    }
    
    private int backtrackToLineStart(int splitIndex) {
        while (getSplitLine(splitIndex).getOffset() > 0) {
            splitIndex--;
        }
        return splitIndex;
    }
    
    public void printLineInfo() {
        for (int i = 0; i < splitLines.size(); i++) {
            SplitLine line = getSplitLine(i);
            System.err.println(i + ": line " + line.getLineIndex() + ", offset " + line.getOffset() + ", length " + line.getLength());
        }
    }
    
    private SplitLine getSplitLine(int index) {
        return (SplitLine) splitLines.get(index);
    }
    
    private int addSplitLines(int lineIndex, FontMetrics metrics, int index) {
        PLineList.Line line = lines.getLine(lineIndex);
        int addedCount = 0;
        int width = getSize().width;
        if (width == 0) {
            width = Integer.MAX_VALUE;  // Don't wrap if we don't have any size.
        }
        width = Math.max(width, MIN_WIDTH);  // Ensure we're at least a sensible width.
        if (line.getWidth() <= width) {
            splitLines.add(index, new SplitLine(lineIndex, 0, line.getLength()));
            addedCount++;
        } else {
            int x = 0;
            CharSequence chars = line.getContents();
            int offset = 0;
            for (int i = 0; i < chars.length(); i++) {
                char ch = chars.charAt(i);
                x = addCharWidth(x, ch);
                if (x >= width) {
                    splitLines.add(index++, new SplitLine(lineIndex, offset, i - offset));
                    addedCount++;
                    offset = i;
                    x = addCharWidth(0, ch);
                }
            }
            if (x > 0) {
                splitLines.add(index++, new SplitLine(lineIndex, offset, chars.length() - offset));
                addedCount++;
            }
        }
        return addedCount;
    }
    
    private int addCharWidth(int x, char ch) {
        if (ch == '\t') {
            x += PTabSegment.MIN_TAB_WIDTH;  // A tab's at least as wide as this.
            x += PTabSegment.TAB_WIDTH;
            return x - x % PTabSegment.TAB_WIDTH;
        } else if (ch < MAX_CACHED_CHAR) {
            return x + widthCache[(int) ch];
        } else {
            return x + getFontMetrics(getFont()).charWidth(ch);
        }
    }
    
    private void setLineWidth(PLineList.Line line, FontMetrics metrics) {
        line.setWidth(metrics.stringWidth(line.getContents().toString()));
    }
    
    private void setText(PTextBuffer text) {
        if (lines != null) {
            lines.removeLineListener(this);
        }
        lines = new PLineList(text);
        lines.addLineListener(this);
        text.addTextListener(anchorSet);
        invalidateLineWrappings();
    }
    
    /**
     * Replaces the entire contents of this text area with the given string.
     * FIXME: should use PTextBuffer.replace, when it's available.
     */
    public void setText(String newText) {
        getPTextBuffer().remove(0, getPTextBuffer().length());
        getPTextBuffer().insert(0, newText.toCharArray());
    }
    
    /**
     * Appends the given string to the end of the text.
     */
    public void append(String newText) {
        PTextBuffer buffer = getPTextBuffer();
        synchronized (buffer) {
            buffer.insert(buffer.length(), newText.toCharArray());
            setCaretPosition(buffer.length());
        }
    }
    
    /**
     * Returns a copy of the text in this text area.
     */
    public String getText() {
        return getPTextBuffer().toString();
    }
    
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
    
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return visible.height;  // We should never be asked for orientation=H.
    }
    
    public boolean getScrollableTracksViewportHeight() {
        // If our parent is larger than we are, expand to fill the space.
        return getParent().getHeight() > getPreferredSize().height;
    }
    
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }
    
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return getFontMetrics(getFont()).getHeight();
    }
    
    public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
        Insets insets = getInsets();
        if (columnCount != 0) {
            result.width = Math.max(result.width, columnCount * getFontMetrics(getFont()).charWidth('m') + insets.left + insets.right);
        }
        if (rowCount != 0) {
            result.height = Math.max(result.height, rowCount * getLineHeight() + insets.top + insets.bottom);
        }
        return result;
    }
    
    public class SplitLine {
        private int lineIndex;
        private int offset;
        private int length;
        
        public SplitLine(int lineIndex, int offset, int length) {
            this.lineIndex = lineIndex;
            this.offset = offset;
            this.length = length;
        }
        
        public int getLineIndex() {
            return lineIndex;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public int getLength() {
            return length;
        }
        
        public void setLineIndex(int lineIndex) {
            this.lineIndex = lineIndex;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        public void setLength(int length) {
            this.length = length;
        }
        
        public int getTextIndex() {
            return lines.getLine(lineIndex).getStart() + offset;
        }
        
        public boolean containsIndex(int charIndex) {
            int startIndex = getTextIndex();
            return (charIndex >= startIndex) && (charIndex < startIndex + length);
        }
        
        public CharSequence getContents() {
            CharSequence parent = lines.getLine(lineIndex).getContents();
            int end = offset + length;
            if (length > 0 && parent.charAt(end - 1) == '\n') {
                end -= 1;
            }
            return parent.subSequence(offset, end);
        }
    }
    
    private class SelectionHighlight extends PHighlight {
        public SelectionHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex);
        }
    
        public void paint(Graphics2D graphics, PCoordinates start, PCoordinates end) {
            Point startPt = textArea.getViewCoordinates(start);
            Point endPt = textArea.getViewCoordinates(end);
            Color oldColor = graphics.getColor();
            int y = textArea.getLineTop(start.getLineIndex());
            int lineHeight = textArea.getLineHeight();
            for (int i = start.getLineIndex(); i <= end.getLineIndex(); i++) {
                int xStart = (i == start.getLineIndex()) ? startPt.x : 0;
                int xEnd = (i == end.getLineIndex()) ? endPt.x : textArea.getSize().width;
                graphics.setColor(SELECTION_COLOR);
                paintRectangleContents(graphics, new Rectangle(xStart, y, xEnd - xStart, lineHeight));
                int yBottom = y + lineHeight - 1;
                graphics.setColor(SELECTION_BOUNDARY_COLOR);
                if (i == start.getLineIndex()) {
                    if (xStart > 0) {
                        graphics.drawLine(xStart, y, xStart, yBottom);
                    }
                    graphics.drawLine(xStart, y, xEnd, y);
                } else if (i == start.getLineIndex() + 1) {
                    graphics.drawLine(0, y, Math.min(xEnd, startPt.x), y);
                }
                if (i == end.getLineIndex()) {
                    if (xEnd < textArea.getSize().width) {
                        graphics.drawLine(xEnd, y, xEnd, yBottom);
                    }
                    graphics.drawLine(xStart, yBottom, xEnd, yBottom);
                } else if (i == end.getLineIndex() - 1) {
                    graphics.drawLine(Math.max(endPt.x, xStart), yBottom, xEnd, yBottom);
                }
                y += lineHeight;
            }
            graphics.setColor(oldColor);
        }
        
        public void paintRectangleContents(Graphics2D graphics, Rectangle rectangle) {
            graphics.fill(rectangle);
        }
    }
}
