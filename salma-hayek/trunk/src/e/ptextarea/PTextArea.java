package e.ptextarea;


import java.awt.*;
import java.awt.datatransfer.*;
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
    
    public static final int NO_MARGIN = -1;
    
    private static final Color MARGIN_BOUNDARY_COLOR = new Color(0.6f, 0.6f, 0.6f);
    private static final Color MARGIN_OUTSIDE_COLOR = new Color(0.96f, 0.96f, 0.96f);
    private static final Color SELECTION_COLOR = new Color(0.70f, 0.83f, 1.00f, 0.5f);
    private static final Color SELECTION_BOUNDARY_COLOR = new Color(0.5f, 0.55f, 0.7f, 0.75f);
    
    private static final Stroke WRAP_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.0f, 2.0f }, 0.0f);
    
    private SelectionHighlight selection;
    
    private PLineList lines;
    private List splitLines;  // TODO - Write a split buffer-style List implementation.
    
    // We cache the FontMetrics for readability rather than performance.
    private FontMetrics metrics;
    private int[] widthCache;
    
    private PAnchorSet anchorSet = new PAnchorSet();
    private ArrayList highlights = new ArrayList();
    private PTextStyler textStyler = PPlainTextStyler.INSTANCE;
    private int rightHandMarginColumn = NO_MARGIN;
    private ArrayList caretListeners = new ArrayList();
    
    private int rowCount;
    private int columnCount;
    
    private PTextAreaSpellingChecker spellingChecker;
    
    public PTextArea() {
        this(0, 0);
    }
    
    public PTextArea(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.selection = new SelectionHighlight(this, 0, 0);
        setFont(UIManager.getFont("TextArea.font"));
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
                rewrap();
            }
            
            private void rewrap() {
                if (getWidth() != lastWidth) {
                    lastWidth = getWidth();
                    invalidateLineWrappings();
                }
            }
        });
        setOpaque(true);
        setFocusTraversalKeysEnabled(false);
        requestFocus();
        initSpellingChecking();
    }
    
    public void addCaretListener(PCaretListener caretListener) {
        caretListeners.add(caretListener);
    }
    
    public void removeCaretListener(PCaretListener caretListener) {
        caretListeners.remove(caretListener);
    }
    
    private void fireCaretChangedEvent() {
        for (int i = 0; i < caretListeners.size(); i++) {
            ((PCaretListener) caretListeners.get(i)).caretMoved(getSelectionStart(), getSelectionEnd());
        }
    }
    
    public PTextStyler getPTextStyler() {
        return textStyler;
    }
    
    // Selection methods.
    public String getSelectedText() {
        int start = selection.getStartIndex();
        int end = selection.getEndIndex();
        if (start == end) {
            return "";
        } else {
            return getPTextBuffer().subSequence(start, end).toString();
        }
    }
    
    public int getSelectionStart() {
        return selection.getStartIndex();
    }
    
    public int getSelectionEnd() {
        return selection.getEndIndex();
    }
    
    public void setCaretPosition(int offset) {
        select(offset, offset);
    }
    
    public void select(int start, int end) {
        SelectionHighlight oldSelection = selection;
        repaintCaret();
        selection = new SelectionHighlight(this, start, end);
        repaintCaret();
        if (oldSelection.isEmpty() != selection.isEmpty()) {
            repaintAnchorRegion(selection.isEmpty() ? oldSelection : selection);
        } else if (oldSelection.isEmpty() == false && selection.isEmpty() == false) {
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
        
        int offsetToShow = getSelectionStart();
        if (oldSelection.getEndIndex() != getSelectionEnd()) {
            offsetToShow = getSelectionEnd();
        }
        Point point = getViewCoordinates(getCoordinates(offsetToShow));
        scrollRectToVisible(new Rectangle(point.x - 1, point.y - metrics.getMaxAscent(), 3, metrics.getHeight()));
        fireCaretChangedEvent();
    }
    
    public void insert(CharSequence chars) {
        SelectionSetter endCaret =new SelectionSetter(getSelectionStart() + chars.length());
        int length = getSelectionEnd() - getSelectionStart();
        getPTextBuffer().replace(new SelectionSetter(), getSelectionStart(), length, chars, endCaret);
    }
    
    public void replaceRange(CharSequence replacement, int start, int end) {
        SelectionSetter endCaret = new SelectionSetter(start + replacement.length());
        int length = end - start;
        PTextBuffer.SelectionSetter selectionPreserver = new PTextBuffer.SelectionSetter() {
            public void modifySelection() {
                // Replacing a range shouldn't involve any caret/selection changes.
            }
        };
        getPTextBuffer().replace(selectionPreserver, start, length, replacement, selectionPreserver);
    }
    
    public void replaceSelection(CharSequence replacement) {
        if (hasSelection()) {
            replaceRange(replacement, getSelectionStart(), getSelectionEnd());
        } else {
            insert(replacement);
        }
    }
    
    public void delete(int startFrom, int charCount) {
        SelectionSetter endCaret = new SelectionSetter(startFrom);
        getPTextBuffer().replace(new SelectionSetter(), startFrom, charCount, "", endCaret);
    }
    
    private class SelectionSetter implements PTextBuffer.SelectionSetter {
        private int start;
        private int end;
        
        public SelectionSetter() {
            this(getSelectionStart(), getSelectionEnd());
        }
        
        public SelectionSetter(int offset) {
            this(offset, offset);
        }
        
        public SelectionSetter(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public void modifySelection() {
            select(start, end);
        }
    }
    
    public boolean hasSelection() {
        return (getSelectionStart() != getSelectionEnd());
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
    
    /**
     * Sets the column number at which to draw the margin. Typically, this is
     * 80 when using a fixed-width font. Use the constant NO_MARGIN to suppress
     * the drawing of any margin.
     */
    public void showRightHandMarginAt(int rightHandMarginColumn) {
        this.rightHandMarginColumn = rightHandMarginColumn;
    }
    
    public void setTextStyler(PTextStyler textStyler) {
        this.textStyler = textStyler;
        repaint();
    }
    
    public void setFont(Font font) {
        super.setFont(font);
        cacheFontMetrics();
        showRightHandMarginAt(GuiUtilities.isFontFixedWidth(font) ? 80 : NO_MARGIN);
        invalidateLineWrappings();
        generateLineWrappings();
        repaint();
    }
    
    private void cacheFontMetrics() {
        metrics = getFontMetrics(getFont());
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
        final int lineIndex = getLineIndexAtLocation(point);
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
    
    /**
     * Returns the line index corresponding to the given point. To properly
     * cope with clicks past the end of the text, this method may update point
     * to have a huge x-coordinate. The line index will be that of the last
     * line in the document, because that's how other text components behave;
     * it's just that all clicks after the end of the text should correspond
     * to the last character in the document, and a huge x-coordinate is the
     * easiest way to ensure that code that goes on to work out which
     * character we're pointing to on the returned line will behave correctly.
     */
    private int getLineIndexAtLocation(Point point) {
        final int maxLineIndex = splitLines.size() - 1;
        int lineIndex = point.y / metrics.getHeight();
        if (lineIndex > maxLineIndex) {
            point.x = Integer.MAX_VALUE;
        }
        lineIndex = Math.max(0, Math.min(maxLineIndex, lineIndex));
        return lineIndex;
    }
    
    public PLineSegment getLineSegmentAtLocation(Point point) {
        generateLineWrappings();
        PLineSegment[] segments = getLineSegments(getLineIndexAtLocation(point));
        int x = 0;
        for (int i = 0; i < segments.length; i++) {
            int width = segments[i].getDisplayWidth(metrics, x);
            if (x + width > point.x) {
                return segments[i];
            }
            x += width;
        }
        return null;
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
        if (isLineWrappingInvalid()) {
            return new PCoordinates(-1, -1);
        }
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
        int baseline = getBaseline(coordinates.getLineIndex());
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
    
    private synchronized void repaintAnchorRegion(PAnchorRegion anchorRegion) {
        repaintIndexRange(anchorRegion.getStartIndex(), anchorRegion.getEndIndex());
    }
    
    private void repaintIndexRange(int startIndex, int endIndex) {
        if (isLineWrappingInvalid()) {
            return;
        }
        PCoordinates start = getCoordinates(startIndex);
        PCoordinates end = getCoordinates(endIndex);
        repaintLines(start.getLineIndex(), end.getLineIndex());
    }
    
    private void repaintCaret() {
        if (isLineWrappingInvalid()) {
            return;
        }
        Point point = getViewCoordinates(getCoordinates(getSelectionStart()));
        repaint(point.x - 1, point.y - metrics.getMaxAscent(), 3, metrics.getMaxAscent() + metrics.getMaxDescent());
    }
    
    public int getLineTop(int lineIndex) {
        return lineIndex * metrics.getHeight();
    }
    
    public int getLineHeight() {
        return metrics.getHeight();
    }
    
    private int getBaseline(int lineIndex) {
        return lineIndex * metrics.getHeight() + metrics.getMaxAscent();
    }
    
    public void paintComponent(Graphics oldGraphics) {
        //StopWatch watch = new StopWatch();
        initCharWidthCache();
        generateLineWrappings();
        Graphics2D graphics = (Graphics2D) oldGraphics;
        Rectangle bounds = graphics.getClipBounds();
        int whiteBackgroundWidth = paintRightHandMargin(graphics, bounds);
        graphics.setColor(getBackground());
        graphics.fillRect(bounds.x, bounds.y, whiteBackgroundWidth, bounds.height);
        int minLine = Math.min(splitLines.size() - 1, bounds.y / metrics.getHeight());
        int maxLine = Math.min(splitLines.size() - 1, (bounds.y + bounds.height) / metrics.getHeight());
        int baseline = getBaseline(minLine);
        PCoordinates caretCoords = getCaretCoordinates();
        paintHighlights(graphics, minLine, maxLine);
        for (int i = minLine; i <= maxLine; i++) {
            PLineSegment[] segments = getLineSegments(i);
            boolean drawCaret = (caretCoords.getLineIndex() == i);
            boolean showWrap = isNextLineAContinuationOf(i);
            paintSegments(graphics, segments, baseline, caretCoords, drawCaret, showWrap);
            baseline += metrics.getHeight();
        }
        //watch.print("Repaint");
    }
    
    private PCoordinates getCaretCoordinates() {
        if (hasSelection()) {
            // We don't render a caret if there's a non-empty selection.
            return new PCoordinates(-1, -1);
        }
        return getCoordinates(getSelectionStart());
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
    private int paintRightHandMargin(Graphics2D graphics, Rectangle bounds) {
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
        selection.paint(graphics);
        for (int i = 0; i < highlights.size(); i++) {
            PHighlight highlight = (PHighlight) highlights.get(i);
            if (highlight.getStart().getIndex() <= maxChar && highlight.getEnd().getIndex() > minChar) {
                highlight.paint(graphics);
            }
        }
    }
    
    private void applyColor(Graphics2D graphics, Color color) {
        graphics.setColor(isEnabled() ? color : Color.GRAY);
    }
    
    private void paintSegments(Graphics2D graphics, PLineSegment[] segments, int baseline, PCoordinates caretCoords, boolean drawCaret, boolean showWrap) {
        int x = 0;
        int charOffset = 0;
        for (int i = 0; i < segments.length; i++) {
            applyColor(graphics, textStyler.getColorForStyle(segments[i].getStyleIndex()));
            segments[i].paint(graphics, x, baseline);
            String text = segments[i].getText();
            if (drawCaret && caretCoords.getCharOffset() < charOffset + text.length()) {
                int caretX = x + segments[i].getDisplayWidth(metrics, x, caretCoords.getCharOffset() - charOffset);
                paintCaret(graphics, caretX, baseline);
                drawCaret = false;
            }
            charOffset += text.length();
            x += segments[i].getDisplayWidth(metrics, x);
        }
        if (showWrap) {
            paintWrapMark(graphics, x, baseline);
        }
        // If drawCaret is still set, the caret must be at the end of the line.
        if (drawCaret) {
            paintCaret(graphics, x, baseline);
        }
    }
    
    private void paintWrapMark(Graphics2D graphics, int x, int y) {
        graphics.setColor(Color.BLACK);
        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(WRAP_STROKE);
        int yMiddle = y - metrics.getMaxAscent() / 2;
        graphics.drawLine(x, yMiddle, getWidth(), yMiddle);
        graphics.setStroke(oldStroke);
    }
    
    private void paintCaret(Graphics2D graphics, int x, int y) {
        applyColor(graphics, Color.RED);
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
        for (int i = 0; i < MAX_CACHED_CHAR; i++) {
            widthCache[i] = metrics.charWidth(i);
        }
    }

    public void linesAdded(PLineEvent event) {
        if (isLineWrappingInvalid()) {
            return;
        }
        int lineIndex = event.getIndex();
        int splitIndex = getSplitLineIndex(lineIndex);
        int firstSplitIndex = splitIndex;
        changeLineIndices(lineIndex, event.getLength());
        for (int i = 0; i < event.getLength(); i++) {
            splitIndex += addSplitLines(lineIndex++, splitIndex);
        }
        updateHeight();
        repaintFromLine(firstSplitIndex);
    }
    
    public void linesRemoved(PLineEvent event) {
        if (isLineWrappingInvalid()) {
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
        if (isLineWrappingInvalid()) {
            return;
        }
        int lineCountChange = 0;
        int minLine = Integer.MAX_VALUE;
        int visibleLineCount = 0;
        for (int i = 0; i < event.getLength(); i++) {
            PLineList.Line line = lines.getLine(event.getIndex() + i);
            setLineWidth(line);
            int splitIndex = getSplitLineIndex(event.getIndex());
            if (i == 0) {
                minLine = splitIndex;
            }
            int removedCount = removeSplitLines(splitIndex);
            lineCountChange -= removedCount;
            int addedCount = addSplitLines(event.getIndex(), splitIndex);
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
        repaint(0, top, getWidth(), bottom - top);
    }
    
    private synchronized boolean isLineWrappingInvalid() {
        return (splitLines == null);
    }
    
    private synchronized void invalidateLineWrappings() {
        splitLines = null;
    }

    private synchronized void generateLineWrappings() {
        if (isLineWrappingInvalid() && isShowing()) {
            splitLines = new ArrayList();
            for (int i = 0; i < lines.size(); i++) {
                PLineList.Line line = lines.getLine(i);
                if (line.isWidthValid() == false) {
                    setLineWidth(line);
                }
                addSplitLines(i, splitLines.size());
            }
            updateHeight();
        }
    }
    
    private void updateHeight() {
        Dimension size = getSize();
        size.height = metrics.getHeight() * splitLines.size();
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
    
    public int getVisibleLineCount() {
        return splitLines.size();
    }
    
    private SplitLine getSplitLine(int index) {
        return (SplitLine) splitLines.get(index);
    }
    
    private int addSplitLines(int lineIndex, int index) {
        PLineList.Line line = lines.getLine(lineIndex);
        int addedCount = 0;
        int width = getWidth();
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
            return x + metrics.charWidth(ch);
        }
    }
    
    private void setLineWidth(PLineList.Line line) {
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
        generateLineWrappings();
    }
    
    /**
     * Replaces the entire contents of this text area with the given string.
     */
    public void setText(String newText) {
        getPTextBuffer().replace(new SelectionSetter(), 0, getPTextBuffer().length(), newText, new SelectionSetter(0));
    }
    
    /**
     * Appends the given string to the end of the text.
     */
    public void append(String newText) {
        PTextBuffer buffer = getPTextBuffer();
        synchronized (buffer) {
            buffer.replace(new SelectionSetter(), buffer.length(), 0, newText, new SelectionSetter(buffer.length() + newText.length()));
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
        return metrics.getHeight();
    }
    
    public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
        Insets insets = getInsets();
        if (columnCount != 0) {
            result.width = Math.max(result.width, columnCount * metrics.charWidth('m') + insets.left + insets.right);
        }
        if (rowCount != 0) {
            result.height = Math.max(result.height, rowCount * getLineHeight() + insets.top + insets.bottom);
        }
        return result;
    }
    
    /**
     * Pastes the clipboard contents or, if that's not available, the system
     * selection. The pasted content replaces the selection.
     */
    public void paste() {
        paste(false);
    }
    
    /**
     * Pastes X11's "selection", an activity usually associated with a
     * middle-button click. The pasted content replaces the selection.
     */
    public void pasteSystemSelection() {
        paste(true);
    }
    
    private void paste(boolean onlyPasteSystemSelection) {
        try {
            Toolkit toolkit = getToolkit();
            Transferable contents = toolkit.getSystemClipboard().getContents(this);
            if (onlyPasteSystemSelection || toolkit.getSystemSelection() != null) {
                contents = toolkit.getSystemSelection().getContents(this);
            }
            DataFlavor[] transferFlavors = contents.getTransferDataFlavors();
            String string = (String) contents.getTransferData(DataFlavor.stringFlavor);
            insert(string);
        } catch (Exception ex) {
            Log.warn("Couldn't paste.", ex);
        }
    }
    
    public void copy() {
        if (hasSelection() == false) {
            return;
        }
        StringSelection stringSelection = new StringSelection(getSelectedText());
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.getSystemClipboard().setContents(stringSelection, null);
        if (toolkit.getSystemSelection() != null) {
            toolkit.getSystemSelection().setContents(stringSelection, null);
        }
    }
    
    public void cut() {
        if (hasSelection()) {
            copy();
            replaceSelection("");
        }
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
        
        public boolean isEmpty() {
            return (getStartIndex() == getEndIndex());
        }
        
        public void paint(Graphics2D graphics, PCoordinates start, PCoordinates end) {
            if (isEmpty()) {
                return;
            }
            Point startPt = textArea.getViewCoordinates(start);
            Point endPt = textArea.getViewCoordinates(end);
            Color oldColor = graphics.getColor();
            int y = textArea.getLineTop(start.getLineIndex());
            int lineHeight = textArea.getLineHeight();
            for (int i = start.getLineIndex(); i <= end.getLineIndex(); i++) {
                int xStart = (i == start.getLineIndex()) ? startPt.x : 0;
                int xEnd = (i == end.getLineIndex()) ? endPt.x : textArea.getWidth();
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
                    if (xEnd < textArea.getWidth()) {
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
