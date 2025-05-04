package e.ptextarea;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import e.gui.*;
import e.util.*;

/**
 * A PTextArea is a replacement for JTextArea.
 * 
 * @author Phil Norman
 */
public class PTextArea extends JComponent implements PLineListener, Scrollable, ClipboardOwner {
    private static final int MIN_WIDTH = 50;
    
    public static final int NO_MARGIN = -1;
    
    public static final Color LINE_NUMBERS_BACKGROUND_COLOR = new Color(0x888888);
    public static final Color LINE_NUMBERS_FOREGROUND_COLOR = new Color(0xeeeeee);
    public static final Color MARGIN_BOUNDARY_COLOR = new Color(0.93f, 0.93f, 0.93f);
    public static final Color MARGIN_OUTSIDE_COLOR = new Color(0.97f, 0.97f, 0.97f);
    
    private SelectionHighlight selection;
    private boolean selectionEndIsAnchor;  // Otherwise, selection start is anchor.
    
    // When told to, we draw a big red arrow pointing at a specific location (eg the caret).
    // This field is non-null when we should be showing such a thing.
    // Typically this is used as a brief highlight, to show the user where the caret is.
    private Point bigRedArrowPoint;

    private PLineList lines;
    // TODO: experiment with java.util.ArrayDeque in Java 6.
    // But ArrayDeque wouldn't help bulk operations in the middle.
    private List<SplitLine> splitLines;
    
    // We cache the FontMetrics for readability rather than performance.
    private final FontMetrics[] metrics = new FontMetrics[3];
    private final PTabSegment SINGLE_TAB = new PTabSegment(this, 0, 1);
    
    private PHighlightManager highlights = new PHighlightManager();
    private PTextStyler textStyler = new PPlainTextStyler(this);
    private List<StyleApplicator> styleApplicators;
    private TabStyleApplicator tabStyleApplicator = new TabStyleApplicator(this);
    
    private boolean canShowRightHandMargin = false;
    private int rightHandMarginColumn = NO_MARGIN;
    
    private ArrayList<PCaretListener> caretListeners = new ArrayList<>();
    private ArrayList<PFindListener> findListeners = new ArrayList<>();
    private TreeMap<Integer, List<PLineSegment>> segmentCache = new TreeMap<>();
    
    private UnaryFunctor<String, String> pastedTextReformatter = new UnaryFunctor<String, String>() {
        public String evaluate(String s) {
            return s;
        }
    };
    
    private int rowCount;
    private int columnCount;
    
    private boolean editable;
    private boolean wordWrap;
    private boolean shouldHideMouseWhenTyping;
    
    private FileType fileType;
    private PIndenter indenter;
    private PTextAreaSpellingChecker spellingChecker;
    private EPopupMenu popupMenu;
    private PMouseHandler mouseHandler;
    
    public PTextArea() {
        this(0, 0);
    }
    
    public PTextArea(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.editable = true;
        this.wordWrap = false;
        this.shouldHideMouseWhenTyping = false;
        this.fileType = FileType.PLAIN_TEXT;
        this.lines = new PLineList(new PTextBuffer());
        this.selection = new SelectionHighlight(this, 0, 0);
        this.indenter = new PNoOpIndenter(this);
        
        initStyleApplicators();
        lines.addLineListener(this);
        revalidateLineWrappings();
        
        setAutoscrolls(true);
        setBackground(UIManager.getColor("EditorPane.background"));
        setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        setFont(UIManager.getFont("EditorPane.font"));
        setOpaque(true);
        setFocusTraversalKeysEnabled(false);
        
        requestFocusInWindow();
        
        initKeyBindings();
        initListeners();
        initPopupMenu();
    }
    
    private void initListeners() {
        this.mouseHandler = new PMouseHandler(this);
        addComponentListener(new Rewrapper(this));
        addCaretListener(new PMatchingBracketHighlighter());
        addKeyListener(new PKeyHandler(this, mouseHandler));
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        initFocusListening();
    }
    
    /**
     * Returns the lock object for this component's underlying buffer.  This should only be used within this package.
     */
    PLock getLock() {
        return getTextBuffer().getLock();
    }
    
    private void runWithoutMovingTheVisibleArea(Runnable runnable) {
        if (selection == null || isLineWrappingInvalid()) {
            runnable.run();
        } else {
            int charToKeepInPosition = getSelectionStart();
            int yPosition = -1;
            Rectangle visible = getVisibleRect();
            yPosition = getViewCoordinates(getCoordinates(charToKeepInPosition)).y - visible.y;
            if (yPosition < 0 || yPosition > visible.height) {
                yPosition = visible.y + visible.height / 2;
                charToKeepInPosition = getSplitLine(getNearestCoordinates(new Point(0, yPosition)).getLineIndex()).getTextIndex(this);
                yPosition = getViewCoordinates(getCoordinates(charToKeepInPosition)).y - visible.y;
            }
            runnable.run();
            visible = getVisibleRect();
            // If this text area isn't visible, the split lines won't have been generated, so the following code will
            // hit a null pointer exception. The simplest way to avoid this is just to skip the scrolling if we're not
            // visible.
            if (!isLineWrappingInvalid()) {
                int newYPosition = getViewCoordinates(getCoordinates(charToKeepInPosition)).y - visible.y;
                visible.y += newYPosition - yPosition;
                scrollRectToVisible(visible);
            }
        }
    }
    
    private void initKeyBindings() {
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeCopyAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeCutAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeFindAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeFindNextAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeFindPreviousAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makePasteAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeRedoAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeSelectAllAction());
        ComponentUtilities.initKeyBinding(this, PActionFactory.makeUndoAction());
    }
    
    public void addCaretListener(PCaretListener caretListener) {
        caretListeners.add(caretListener);
    }
    
    public void removeCaretListener(PCaretListener caretListener) {
        caretListeners.remove(caretListener);
    }
    
    private void fireCaretChangedEvent() {
        for (PCaretListener caretListener : caretListeners) {
            caretListener.caretMoved(this, getSelectionStart(), getSelectionEnd());
        }
    }
    
    public PTextStyler getTextStyler() {
        return textStyler;
    }
    
    private void initStyleApplicators() {
        styleApplicators = new ArrayList<StyleApplicator>();
        addStyleApplicator(new UnprintableCharacterStyleApplicator(this));
        addStyleApplicator(new HyperlinkStyleApplicator(this));
        textStyler.initStyleApplicators();
    }
    
    public void addStyleApplicator(StyleApplicator styleApplicator) {
        styleApplicators.add(styleApplicator);
    }
    
    public void addStyleApplicatorFirst(StyleApplicator styleApplicator) {
        styleApplicators.add(0, styleApplicator);
    }
    
    // Selection methods.
    public String getSelectedText() {
        getLock().getReadLock();
        try {
            int start = selection.getStartIndex();
            int end = selection.getEndIndex();
            if (start == end) {
                return "";
            } else {
                return getTextBuffer().subSequence(start, end).toString();
            }
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    PHighlightManager getHighlightManager() {
        return highlights;
    }
    
    SelectionHighlight getSelection() {
        return selection;
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
    
    public int getUnanchoredSelectionExtreme() {
        return selectionEndIsAnchor ? getSelectionStart() : getSelectionEnd();
    }
    
    public void changeUnanchoredSelectionExtreme(int newPosition) {
        int anchorPosition = selectionEndIsAnchor ? getSelectionEnd() : getSelectionStart();
        int minPosition = Math.min(newPosition, anchorPosition);
        int maxPosition = Math.max(newPosition, anchorPosition);
        boolean endIsAnchor = (maxPosition == anchorPosition);
        setSelection(minPosition, maxPosition, endIsAnchor);
    }
    
    public void select(int start, int end) {
        setSelection(start, end, false);
    }
    
    public void setSelection(int start, int end, boolean selectionEndIsAnchor) {
        getLock().getWriteLock();
        try {
            setSelectionWithoutScrolling(start, end, selectionEndIsAnchor);
            ensureVisibilityOfOffset(getUnanchoredSelectionExtreme());
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public void setSelectionWithoutScrolling(int start, int end, boolean selectionEndIsAnchor) {
        getLock().getWriteLock();
        try {
            this.selectionEndIsAnchor = selectionEndIsAnchor;
            SelectionHighlight oldSelection = selection;
            selection = new SelectionHighlight(this, start, end);
            updateSystemSelection();
            oldSelection.detachAnchors();
            if (oldSelection.isEmpty() != selection.isEmpty()) {
                repaintHighlight(selection.isEmpty() ? oldSelection : selection);
                repaintCaret(selection.isEmpty() ? selection : oldSelection);
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
            } else {
                repaintCaret(oldSelection);
                repaintCaret(selection);
            }
            fireCaretChangedEvent();
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    /**
     * Copies the selected text to X11's selection.
     * Does nothing on other platforms.
     */
    private void updateSystemSelection() {
        if (hasSelection() == false) {
            // Almost all X11 applications leave the selection alone in this case.
            return;
        }
        Clipboard systemSelection = getToolkit().getSystemSelection();
        if (systemSelection != null) {
            systemSelection.setContents(new LazyStringSelection() {
                public String reallyGetText() {
                    return getSelectedText();
                }
            }, this);
        }
    }
    
    private void copyToClipboard(Clipboard clipboard) {
        String newContents = getSelectedText();
        if (newContents.length() == 0) {
            return;
        }
        StringSelection selection = new StringSelection(newContents);
        clipboard.setContents(selection, this);
    }
    
    /**
     * Invoked to notify us that we no longer own the clipboard.
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // Firefox doesn't clear its selection when someone else claims the
        // X11 selection and I imagine it could be quite annoying in Evergreen.
        // So deliberately do nothing.
        
        // If we already have the read lock and ask for the write lock in
        // setSelectionWithoutScrolling, that would cause deadlock if
        // another thread has the read lock and is waiting for this thread.
    }
    
    /**
     * Avoids NullPointerExceptions when we try to use the splitLines before they're available.
     * I think the only way we can fix this properly is to remember what we've been asked to do, and do it as soon as we're able.
     * Hence the name.
     * 
     * FIXME: lose this and do the right thing instead..
     */
    private boolean weAreTooBrokenToWaitUntilWeAreAbleToCarryThisOut() {
        return (isLineWrappingInvalid() || isShowing() == false);
    }
    
    public void centerOffsetInDisplay(int offset) {
        if (weAreTooBrokenToWaitUntilWeAreAbleToCarryThisOut()) {
            return;
        }
        
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport == null) {
            return;
        }
        
        Point point = getViewCoordinates(getCoordinates(offset));
        final int height = viewport.getExtentSize().height;
        int y = point.y - height/2;
        y = Math.max(0, y);
        y = Math.min(y, getHeight() - height);
        viewport.setViewPosition(new Point(0, y));
    }

    public void brieflyShowBigRedArrowPointingAt(int offset) {
        if (weAreTooBrokenToWaitUntilWeAreAbleToCarryThisOut()) {
            return;
        }
        bigRedArrowPoint = getViewCoordinates(getCoordinates(offset));
        repaint();
        // Show the big red arrow for 1 second. It's big and red, so that should be enough for it to be
        // noticed, without sticking around beyond its welcome.
        javax.swing.Timer disappearTimer = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bigRedArrowPoint = null;
                repaint();
            }
        });
        disappearTimer.setRepeats(false);
        disappearTimer.start();
    }

    public void ensureVisibilityOfOffset(int offset) {
        if (weAreTooBrokenToWaitUntilWeAreAbleToCarryThisOut()) {
            return;
        }
        
        Point point = getViewCoordinates(getCoordinates(offset));
        scrollRectToVisible(new Rectangle(point.x - 1, point.y - metrics[Font.PLAIN].getMaxAscent(), 3, getLineHeight()));
    }
    
    /**
     * Returns the indenter responsible for auto-indent (and other aspects of
     * indentation correction) in this text area.
     */
    public PIndenter getIndenter() {
        return indenter;
    }
    
    /**
     * Sets the indenter responsible for this text area. Typically useful when
     * you know more about the language of the content than PTextArea does.
     */
    public void setIndenter(PIndenter newIndenter) {
        this.indenter = newIndenter;
    }
    
    /**
     * Returns the text of the given line (without the newline).
     */
    public String getLineText(int lineNumber) {
        getLock().getReadLock();
        try {
            int start = getLineStartOffset(lineNumber);
            int end = getLineEndOffsetBeforeTerminator(lineNumber);
            return (start == end) ? "" : getTextBuffer().subSequence(start, end).toString();
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    /**
     * Returns the string to use as a single indent level in this text area.
     */
    public String getIndentationString() {
        String result = (String) getTextBuffer().getProperty(PTextBuffer.INDENTATION_PROPERTY);
        if (result == null) {
            result = "\t";
        }
        return result;
    }
    
    /**
     * Appends the given string to the end of the text. This is meant for
     * programmatic use, and so does not pay attention to or modify the
     * selection.
     */
    public void insertPreservingSelection(String newText, int offset) {
        getLock().getWriteLock();
        try {
            SelectionSetter noChange = new SelectionSetter(SelectionSetter.DO_NOT_CHANGE);
            PTextBuffer buffer = getTextBuffer();
            buffer.replace(noChange, offset, 0, newText, noChange);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public void replaceRange(CharSequence replacement, int start, int end) {
        getLock().getWriteLock();
        try {
            SelectionSetter endCaret = new SelectionSetter(start + replacement.length());
            getTextBuffer().replace(new SelectionSetter(), start, end - start, replacement, endCaret);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    /**
     * Replaces the selection with 'replacement', or inserts 'replacement' at the caret if there is no selection.
     */
    public void replaceSelection(CharSequence replacement) {
        getLock().getWriteLock();
        try {
            replaceRange(replacement, getSelectionStart(), getSelectionEnd());
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public void delete(int startFrom, int charCount) {
        getLock().getWriteLock();
        try {
            SelectionSetter endCaret = new SelectionSetter(startFrom);
            getTextBuffer().replace(new SelectionSetter(), startFrom, charCount, "", endCaret);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    private class SelectionSetter implements PTextBuffer.SelectionSetter {
        private static final int DO_NOT_CHANGE = -1;
        
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
            if (start != DO_NOT_CHANGE && end != DO_NOT_CHANGE) {
                // We bounds-check the length for the case when we're using a SelectionSetter
                // during 'revert to saved', where we use the selection in the pre-reverted
                // file on the newly-loaded contents (which might be shorter).
                int len = getTextBuffer().length();
                select(Math.min(start, len), Math.min(end, len));
            }
        }
    }
    
    public boolean hasSelection() {
        return (getSelectionStart() != getSelectionEnd());
    }
    
    public void selectAll() {
        getLock().getReadLock();
        try {
            select(0, getTextBuffer().length());
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    /**
     * Repaints us when we gain/lose focus, so we can re-color the selection,
     * like a native text component.
     */
    private void initFocusListening() {
        addFocusListener(new FocusListener() {
            private boolean firstFocusGain = true;

            public void focusGained(FocusEvent e) {
                repaint();
                if (firstFocusGain) {
                    initSpellingChecking();
                    firstFocusGain = false;
                }
            }
            
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }
    
    private void initSpellingChecking() {
        spellingChecker = new PTextAreaSpellingChecker(this);
        spellingChecker.checkSpelling();
    }
    
    public PTextAreaSpellingChecker getSpellingChecker() {
        return spellingChecker;
    }
    
    // Utility methods.
    public int getLineCount() {
        return lines.size();
    }
    
    /**
     * Returns a CharSequence providing access to all the characters in the given line up to but not
     * including the line terminator.
     */
    public CharSequence getLineContents(int line) {
        getLock().getReadLock();
        try {
            return lines.getLineContents(line);
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public int getLineStartOffset(int line) {
        getLock().getReadLock();
        try {
            return lines.getLine(line).getStart();
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    /**
     * Returns the offset at the end of the given line, but before the
     * newline. This differs from JTextArea's getLineEndOffset, where the
     * line end offset is taken to include the newline.
     */
    public int getLineEndOffsetBeforeTerminator(int line) {
        getLock().getReadLock();
        try {
            return lines.getLine(line).getEndOffsetBeforeTerminator(getTextBuffer());
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public int getLineOfOffset(int offset) {
        getLock().getReadLock();
        try {
            return lines.getLineIndex(offset);
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public void setWrapStyleWord(boolean newWordWrapState) {
        if (wordWrap != newWordWrapState) {
            wordWrap = newWordWrapState;
            revalidateLineWrappings();
        }
    }
    
    /**
     * Sets the column number at which to draw the margin. Typically, this is
     * 80 when using a fixed-width font. Use the constant NO_MARGIN (the
     * default) to suppress the drawing of any margin.
     */
    public void showRightHandMarginAt(int rightHandMarginColumn) {
        this.rightHandMarginColumn = rightHandMarginColumn;
    }
    
    /**
     * Returns the column number at which the margin is drawn.
     * Test against the constant NO_MARGIN to see if no margin is being drawn.
     */
    public int getRightHandMarginColumn() {
        return canShowRightHandMargin ? rightHandMarginColumn : NO_MARGIN;
    }
    
    public void setTextStyler(PTextStyler textStyler) {
        this.textStyler = textStyler;
        initStyleApplicators();
        clearSegmentCache();
        repaint();
    }
    
    public void setFont(Font oldFont) {
        setScaledFont(GuiUtilities.applyFontScaling(oldFont));
    }
    
    public void setScaledFont(final Font font) {
        runWithoutMovingTheVisibleArea(() -> {
            PTextArea.super.setFont(font);
            getLock().getWriteLock();
            try {
                cacheFontMetrics();
                canShowRightHandMargin = GuiUtilities.isFontFixedWidth(font);
                lines.invalidateWidths();
                revalidateLineWrappings();
            } finally {
                getLock().relinquishWriteLock();
            }
        });
        repaint();
    }
    
    private void cacheFontMetrics() {
        metrics[Font.PLAIN] = getFontMetrics(getFont());
        metrics[Font.BOLD] = getFontMetrics(getFont().deriveFont(Font.BOLD));
        metrics[Font.ITALIC] = getFontMetrics(getFont().deriveFont(Font.ITALIC));
    }
    
    public void addHighlight(PHighlight highlight) {
        getLock().getWriteLock();
        try {
            highlights.add(highlight);
            repaintHighlight(highlight);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public List<PHighlight> getNamedHighlights(String highlighterName) {
        return getNamedHighlightsOverlapping(highlighterName, 0, getTextBuffer().length() + 1);
    }
    
    /**
     * Returns all highlights matching highlighterName overlapping the range [beginOffset, endOffset).
     */
    public List<PHighlight> getNamedHighlightsOverlapping(String highlighterName, int beginOffset, int endOffset) {
        getLock().getReadLock();
        try {
            return highlights.getNamedHighlightsOverlapping(highlighterName, beginOffset, endOffset);
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    /**
     * Selects the given highlight.
     */
    public void selectHighlight(PHighlight highlight) {
        getLock().getReadLock();
        try {
            centerOffsetInDisplay(highlight.getStartIndex());
            select(highlight.getStartIndex(), highlight.getEndIndex());
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public void removeHighlights(String highlightManager) {
        removeHighlights(highlightManager, 0, getTextBuffer().length() + 1);
    }
    
    /**
     * Removes highlights matching "highlighterName" in the range [beginOffset, endOffset).
     */
    public void removeHighlights(String highlighterName, int beginOffset, int endOffset) {
        getLock().getWriteLock();
        try {
            List<PHighlight> removeList = highlights.getNamedHighlightsOverlapping(highlighterName, beginOffset, endOffset);
            IdentityHashMap<PAnchor, Object> deadAnchors = new IdentityHashMap<>();
            for (PHighlight highlight : removeList) {
                highlight.collectAnchors(deadAnchors);
                highlights.remove(highlight);
            }
            getTextBuffer().getAnchorSet().removeAll(deadAnchors);
            if (removeList.size() == 1) {
                repaintHighlight(removeList.get(0));
            } else if (removeList.size() > 1) {
                repaint();
            }
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public void removeHighlight(PHighlight highlight) {
        getLock().getWriteLock();
        try {
            highlights.remove(highlight);
            highlight.detachAnchors();
            repaintHighlight(highlight);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public PLineList getLineList() {
        return lines;
    }
    
    public PTextBuffer getTextBuffer() {
        return lines.getTextBuffer();
    }
    
    public PCoordinates getNearestCoordinates(Point point) {
        getLock().getReadLock();
        try {
            Insets insets = getInsets();
            if (point.y < insets.top) {
                return new PCoordinates(0, 0);
            }
            generateLineWrappings();
            final int lineIndex = getLineIndexAtLocation(point);
            int charOffset = 0;
            int x = insets.left;
            for (PLineSegment segment : getLineSegmentsForSplitLine(lineIndex)) {
                int width = segment.getDisplayWidth(x);
                if (x + width > point.x) {
                    charOffset += segment.getCharOffset(x, point.x);
                    return new PCoordinates(lineIndex, charOffset);
                }
                charOffset += segment.getModelTextLength();
                x += width;
            }
            return new PCoordinates(lineIndex, charOffset);
        } finally {
            getLock().relinquishReadLock();
        }
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
        int lineIndex = (point.y - getInsets().top) / getLineHeight();
        if (lineIndex > maxLineIndex) {
            point.x = Integer.MAX_VALUE;
        }
        lineIndex = Math.max(0, Math.min(maxLineIndex, lineIndex));
        return lineIndex;
    }
    
    public PLineSegment getLineSegmentAtLocation(Point point) {
        getLock().getReadLock();
        try {
            generateLineWrappings();
            int x = getInsets().left;
            for (PLineSegment segment : getLineSegmentsForSplitLine(getLineIndexAtLocation(point))) {
                int width = segment.getDisplayWidth(x);
                if (x + width > point.x) {
                    return segment;
                }
                x += width;
            }
            return null;
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public Iterator<PLineSegment> getLogicalSegmentIterator(int offset) {
        return new PLogicalSegmentIterator(this, offset);
    }
    
    public Iterator<PLineSegment> getWrappedSegmentIterator(int offset) {
        return new PWrappedSegmentIterator(this, offset);
    }
    
    private List<PLineSegment> getLineSegmentsForSplitLine(int splitLineIndex) {
        return getLineSegmentsForSplitLine(getSplitLine(splitLineIndex));
    }
    
    /**
     * Returns a series of segments of text describing how to render each part of the
     * specified line.
     * FIXME - delete once all this is sorted out properly.
     * FIXME - this is moved straight out of PAbstractTextStyler.  It needs major work.
     * 
     * FIXME: when should you call getLineSegments, and when should you call getLineSegmentsForSplitLine?
     */
    private final List<PLineSegment> getLineSegmentsForSplitLine(SplitLine splitLine) {
        int lineIndex = splitLine.getLineIndex();
        List<PLineSegment> segments = getLineSegments(lineIndex);
        int index = 0;
        ArrayList<PLineSegment> result = new ArrayList<>();
        int start = splitLine.getOffset();
        int end = start + splitLine.getLength();
        
        for (int i = 0; index < end && i < segments.size(); ++i) {
            PLineSegment segment = segments.get(i);
            if (start >= index + segment.getModelTextLength()) {
                index += segment.getModelTextLength();
                continue;
            }
            if (start > index) {
                int skip = start - index;
                segment = segment.subSegment(skip);
                index += skip;
            }
            if (end < index + segment.getModelTextLength()) {
                segment = segment.subSegment(0, end - index);
            }
            result.add(segment);
            index += segment.getModelTextLength();
        }
        return result;
    }
    
    // FIXME: when should you call getLineSegments, and when should you call getLineSegmentsForSplitLine?
    public List<PLineSegment> getLineSegments(int lineIndex) {
        getLock().getReadLock();
        try {
            // Return it straight away if we've already cached it.
            synchronized (segmentCache) {
                if (segmentCache.containsKey(lineIndex)) {
                    return segmentCache.get(lineIndex);
                }
            }
            
            // Let the styler have the first go.
            List<PLineSegment> segments = textStyler.getTextSegments(lineIndex);
            
            // Then let the style applicators add their finishing touches.
            String line = getLineContents(lineIndex).toString();
            for (StyleApplicator styleApplicator : styleApplicators) {
                segments = applyStyleApplicator(styleApplicator, line, segments);
            }
            
            // Finally, deal with tabs.
            segments = applyStyleApplicator(tabStyleApplicator, line, segments);
            synchronized (segmentCache) {
                segmentCache.put(lineIndex, segments);
            }
            return segments;
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    private void clearSegmentCacheFrom(int lineIndex) {
        synchronized (segmentCache) {
            // Take a copy of the indices.  Otherwise, if we try removing stuff from the segment cache
            // based on values taken from the sub-tree from the line index onwards, we incur a
            // concurrent modification exception.
            // FIXME: http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html#keySet() implies that we can use Iterator.remove to avoid this copy.
            Set<Integer> keySet = segmentCache.tailMap(lineIndex).keySet();
            Integer[] indices = keySet.toArray(new Integer[keySet.size()]);
            for (int index : indices) {
                segmentCache.remove(index);
            }
        }
    }
    
    private void clearSegmentCache() {
        synchronized (segmentCache) {
            segmentCache.clear();
        }
    }
    
    private List<PLineSegment> applyStyleApplicator(StyleApplicator styleApplicator, String line, List<PLineSegment> inputSegments) {
        List<PLineSegment> result = new ArrayList<>();
        for (PLineSegment segment : inputSegments) {
            if (styleApplicator.canApplyStylingTo(segment.getStyle())) {
                result.addAll(styleApplicator.applyStylingTo(line, segment));
            } else {
                result.add(segment);
            }
        }
        return result;
    }
    
    private void addTabbedSegments(PLineSegment segment, ArrayList<PLineSegment> target) {
        while (true) {
            String text = segment.getViewText();
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
            int offset = segment.getOffset();
            target.add(new PTabSegment(this, offset + tabIndex, offset + tabEnd));
            segment = segment.subSegment(tabEnd);
            if (segment.getModelTextLength() == 0) {
                return;
            }
        }
    }
    
    public PCoordinates getLogicalCoordinates(int offset) {
        final int lineIndex = getLineOfOffset(offset);
        return new PCoordinates(lineIndex, offset - getLineStartOffset(lineIndex));
    }
    
    // FIXME: I'm never quite sure this should be used in all the places it's used. I'm pretty sure the name is misleading, especially given that PCoordinates is nothing more than a pair of ints.
    public PCoordinates getCoordinates(int location) {
        getLock().getReadLock();
        try {
            if (isLineWrappingInvalid()) {
                return new PCoordinates(-1, -1);
            }
            int min = 0;
            int max = splitLines.size();
            while (max - min > 1) {
                int mid = (min + max) / 2;
                SplitLine line = getSplitLine(mid);
                if (line.containsIndex(this, location)) {
                    return new PCoordinates(mid, location - line.getTextIndex(this));
                } else if (location < line.getTextIndex(this)) {
                    max = mid;
                } else {
                    min = mid;
                }
            }
            return new PCoordinates(min, location - getSplitLine(min).getTextIndex(this));
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public Point getViewCoordinates(PCoordinates coordinates) {
        getLock().getReadLock();
        try {
            int baseline = getBaseline(coordinates.getLineIndex());
            int x = getInsets().left;
            int charOffset = 0;
            for (PLineSegment segment : getLineSegmentsForSplitLine(coordinates.getLineIndex())) {
                if (coordinates.getCharOffset() <= charOffset + segment.getModelTextLength()) {
                    x += segment.getDisplayWidth(x, coordinates.getCharOffset() - charOffset);
                    return new Point(x, baseline);
                }
                charOffset += segment.getModelTextLength();
                x += segment.getDisplayWidth(x);
            }
            return new Point(x, baseline);
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public int getTextIndex(PCoordinates coordinates) {
        return getSplitLine(coordinates.getLineIndex()).getTextIndex(this) + coordinates.getCharOffset();
    }
    
    private void repaintHighlight(PHighlight highlight) {
        repaintIndexRange(highlight.getStartIndex(), highlight.getEndIndex());
    }
    
    private void repaintIndexRange(int startIndex, int endIndex) {
        if (isLineWrappingInvalid()) {
            return;
        }
        PCoordinates start = getCoordinates(startIndex);
        PCoordinates end = getCoordinates(endIndex);
        repaintLines(start.getLineIndex(), end.getLineIndex());
    }
    
    private void repaintCaret(SelectionHighlight caret) {
        if (isLineWrappingInvalid()) {
            return;
        }
        Point point = getViewCoordinates(getCoordinates(caret.getStartIndex()));
        repaint(point.x - 1, point.y - metrics[Font.PLAIN].getMaxAscent(), 3, metrics[Font.PLAIN].getMaxAscent() + metrics[Font.PLAIN].getMaxDescent());
    }
    
    public int getLineTop(int lineIndex) {
        return lineIndex * getLineHeight() + getInsets().top;
    }
    
    public int getLineHeight() {
        return metrics[Font.PLAIN].getHeight();
    }
    
    public FontMetrics getFontMetrics(int fontFlags) {
        return metrics[fontFlags];
    }
    
    public int getBaseline(int lineIndex) {
        return lineIndex * getLineHeight() + metrics[Font.PLAIN].getMaxAscent() + getInsets().top;
    }
    
    public void paintComponent(Graphics oldGraphics) {
        getLock().getReadLock();
        try {
            generateLineWrappings();
            
            PTextAreaRenderer renderer = new PTextAreaRenderer(this, (Graphics2D) oldGraphics);
            renderer.render();
            if (bigRedArrowPoint != null) {
                renderer.drawBigRedArrowPointingAt(bigRedArrowPoint);
            }
        } catch (Throwable th) {
            Log.warn("PTextArea paint failed", th);
        } finally {
            getLock().relinquishReadLock();
        }
    }

    public void linesAdded(PLineEvent event) {
        if (isLineWrappingInvalid()) {
            return;
        }
        int lineIndex = event.getLineIndex();
        clearSegmentCacheFrom(lineIndex);
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
        clearSegmentCacheFrom(event.getLineIndex());
        int beginSplitIndex = getSplitLineIndex(event.getLineIndex());
        int endSplitIndex = getSplitLineIndex(event.getLineIndex() + event.getLength());
        removeSplitLines(beginSplitIndex, endSplitIndex);
        changeLineIndices(event.getLineIndex() + event.getLength(), -event.getLength());
        updateHeight();
        repaintFromLine(beginSplitIndex);
    }
    
    public void linesCompletelyReplaced(PLineEvent event) {
        clearSegmentCache();
        revalidateLineWrappings();
    }
    
    private void changeLineIndices(int lineIndex, int change) {
        for (int i = getSplitLineIndex(lineIndex); i < splitLines.size(); i++) {
            SplitLine line = getSplitLine(i);
            line.setLineIndex(line.getLineIndex() + change);
        }
    }
    
    public void linesChanged(PLineEvent event) {
        clearSegmentCacheFrom(event.getLineIndex());
        if (isLineWrappingInvalid()) {
            return;
        }
        int lineCountChange = 0;
        int minLine = Integer.MAX_VALUE;
        int visibleLineCount = 0;
        for (int i = 0; i < event.getLength(); i++) {
            final int lineIndex = event.getLineIndex() + i;
            setLineWidth(lineIndex);
            int beginSplitIndex = getSplitLineIndex(event.getLineIndex()); // FIXME: shouldn't this be "+ i" too?
            int endSplitIndex = getSplitLineIndex(event.getLineIndex() + 1);
            if (i == 0) {
                minLine = beginSplitIndex;
            }
            int removedCount = removeSplitLines(beginSplitIndex, endSplitIndex);
            lineCountChange -= removedCount;
            int addedCount = addSplitLines(event.getLineIndex(), beginSplitIndex);
            lineCountChange += addedCount;
            visibleLineCount += addedCount;
        }
        if (lineCountChange != 0) {
            updateHeight();
            repaintFromLine(getSplitLineIndex(event.getLineIndex()));
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
    
    public boolean isLineWrappingInvalid() {
        return (splitLines == null);
    }
    
    private void revalidateLineWrappings() {
        getLock().getWriteLock();
        try {
            splitLines = null;
            generateLineWrappings();
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    /** Only for use by class Rewrapper. */
    void rewrap() {
        if (isShowing()) {
            runWithoutMovingTheVisibleArea(() -> { revalidateLineWrappings(); });
        } else {
            // We're not showing, so dump the splitLines to force regeneration next
            // time we turn visible.
            getLock().getWriteLock();
            splitLines = null;
            getLock().relinquishWriteLock();
        }
    }
    
    /**
     * Calls generateLineWrappings so we're ready to use as soon as we're
     * added to a parent, rather than having to wait until we're first
     * painted. This means that we can be created on the Event Dispatch Thread
     * and manipulated immediately (centerOffsetInDisplay being a common use
     * case).
     * 
     * FIXME: this sounds like a hack. what's the real problem?
     * FIXME: we call generateLineWrappings twice for each new file; first on addNotify and then on componentResized.
     */
    public void addNotify() {
        super.addNotify();
        generateLineWrappings();
    }
    
    private void generateLineWrappings() {
        getLock().getWriteLock();
        try {
            if (isLineWrappingInvalid() && isShowing()) {
                splitLines = new ArrayList<SplitLine>();
                for (int i = 0; i < lines.size(); ++i) {
                    addSplitLines(i, splitLines.size());
                }
                updateHeight();
            }
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    private void updateHeight() {
        Dimension size = getSize();
        Insets insets = getInsets();
        size.height = getLineHeight() * splitLines.size() + insets.top + insets.bottom;
        setSize(size);
        setPreferredSize(size);
    }
    
    private int removeSplitLines(int beginSplitIndex, int endSplitIndex) {
        splitLines.subList(beginSplitIndex, endSplitIndex).clear();
        return endSplitIndex - beginSplitIndex;
    }
    
    public int getSplitLineIndex(int lineIndex) {
        getLock().getReadLock();
        // Ensure that the splitLines are not null before proceeding. This function can be called
        // by the BirdView while splitLines is null, which causes really nasty painting defects
        // if we don't do this.
        generateLineWrappings();
        try {
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
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    private int backtrackToLineStart(int splitIndex) {
        while (getSplitLine(splitIndex).getOffset() > 0) {
            splitIndex--;
        }
        return splitIndex;
    }
    
    public void logLineInfo() {
        Log.warn("Dumping PTextArea SplitLine info:");
        for (int i = 0; i < splitLines.size(); i++) {
            SplitLine line = getSplitLine(i);
            Log.warn("SplitLine " + i + ": line " + line.getLineIndex() + ", offset " + line.getOffset() + ", length " + line.getLength());
        }
    }
    
    public int getSplitLineCount() {
        return (splitLines != null) ? splitLines.size() : getLineCount();
    }
    
    public SplitLine getSplitLineOfOffset(int offset) {
        return getSplitLine(getCoordinates(offset).getLineIndex());
    }
    
    public SplitLine getSplitLine(int index) {
        return splitLines.get(index);
    }
    
    private int addSplitLines(int lineIndex, int index) {
        final int initialSplitLineCount = splitLines.size();
        if (lines.isWidthValid(lineIndex) == false) {
            setLineWidth(lineIndex);
        }
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        if (width <= 0) {
            width = Integer.MAX_VALUE;  // Don't wrap if we don't have any size.
        }
        width = Math.max(width, MIN_WIDTH);  // Ensure we're at least a sensible width.
        if (lines.getWidth(lineIndex) <= width) {
            // The whole line fits.
            splitLines.add(index, new SplitLine(lineIndex, 0, lines.getLineContents(lineIndex).length()));
        } else {
            // The line's too long, so break it into SplitLines.
            int x = 0;
            CharSequence chars = lines.getLineContents(lineIndex);
            int lastSplitOffset = 0;
            for (int i = 0; i < chars.length(); i++) {
                char ch = chars.charAt(i);
                x = addCharWidth(x, ch);
                if (x >= width - getMinimumWrapMarkWidth()) {
                    if (wordWrap) {
                        // Try to find a break before the last break.
                        for (int splitOffset = i; splitOffset >= lastSplitOffset; --splitOffset) {
                            if (chars.charAt(splitOffset) == ' ' && splitOffset < chars.length() - 1) {
                                // Break so that the word goes to the next line
                                // but the inter-word character stays where it
                                // was.
                                i = splitOffset + 1;
                                ch = chars.charAt(i);
                                break;
                            }
                        }
                    }
                    splitLines.add(index++, new SplitLine(lineIndex, lastSplitOffset, i - lastSplitOffset));
                    lastSplitOffset = i;
                    x = addCharWidth(0, ch);
                }
            }
            if (x > 0) {
                splitLines.add(index++, new SplitLine(lineIndex, lastSplitOffset, chars.length() - lastSplitOffset));
            }
        }
        return (splitLines.size() - initialSplitLineCount);
    }
    
    /**
     * Returns the amount of space that must remain to the right of a character
     * for that character not to cause a line wrap. We use this to ensure that
     * there's at least this much space for the wrap mark.
     */
    private int getMinimumWrapMarkWidth() {
        return metrics[Font.PLAIN].charWidth('W');
    }
    
    private int addCharWidth(int x, char ch) {
        // FIXME: this is a hack, and doesn't generalize to arbitrary PTextSegments for which getViewText and getCharSequence (that is, the model text) return different strings. I tried to rewrite the wrapping code to use getLineSegments. setLineWidth is easy, but addSplitLines is pretty difficult because you need to keep track of the two strings and the correspondence between offsets in them, or rewrite it completely to work on the text segments itself. This code has been known broken since at least 2005-06, so another special case is better than nothing.
        if (ch == '\t') {
            return x + SINGLE_TAB.getDisplayWidth(x);
        } else if (ch < ' ' || ch == '\u007f') {
            // FIXME: we could cache these, since there are so few.
            StringBuilder chars = new StringBuilder(6);
            StringUtilities.appendUnicodeEscape(chars, ch);
            return x + metrics[Font.PLAIN].stringWidth(chars.toString());
        } else {
            return x + metrics[Font.PLAIN].charWidth(ch);
        }
    }
    
    private void setLineWidth(int lineIndex) {
        CharSequence chars = lines.getLineContents(lineIndex);
        int width = 0;
        for (int i = 0; i < chars.length(); ++i) {
            width = addCharWidth(width, chars.charAt(i));
        }
        lines.getLine(lineIndex).setWidth(width);
    }
    
    /**
     * Replaces the entire contents of this text area with the given CharSequence.
     */
    public void setText(CharSequence newText) {
        getTextBuffer().replace(new SelectionSetter(), 0, getTextBuffer().length(), newText, new SelectionSetter(guessTargetCaretPos(newText)));
    }
    
    /**
     * Replaces the contents of the buffer with whatever's in the given file.
     * If the undo buffer is empty, this is treated as the initial load of a file, and so the
     * action will not be undoable (otherwise, the initial load of a file would have an undo
     * action that would present the user with an empty file, which would be considered identical
     * to what's on disk - clearly wrong). If we can undo something, however, then we execute
     * this as an undoable action, so that a 'revert to file' can be undone.
     */
    public void readFromFile(File file) {
        SelectionSetter oldSel = null;
        SelectionSetter newSel = null;
        if (getTextBuffer().getUndoBuffer().canUndo()) {
            oldSel = new SelectionSetter();
            // Try to maintain the same selection as before, so we're in roughly the same area
            // of the file. And trust the SelectionSetter to check the bounds of the file to
            // ensure we're not exceeding them.
            // We could do a better job of this by using the 'guessTargetCaretPos' function below,
            // although we'd have to do that lazily as at this point we don't know what the new
            // contents is going to be.
            // Alternatively we could refactor the whole kaboodle so that the file handling isn't
            // done within the PTextBuffer (which is really the wrong place for it, after all).
            newSel = new SelectionSetter();
        }
        getTextBuffer().readFromFile(file, oldSel, newSel);
    }
    
    /**
     * Looks at the new text, compares it with the existing text, and guesses where best to place the caret in the new text.
     *
     * If the processing seems to be purely reformatting, we try to keep the caret where it was by looking at the non-whitespace
     * characters. Otherwise, we just try to stay roughly the same distance into the file.
     */
    public int guessTargetCaretPos(CharSequence newText) {
        // We delegate to guessTargetCaretPos, which does the actual processing, but we actually want to find the
        // location based on the *reversed* char sequences. This is because some processing tools will not only
        // reformat, but may also modify imports (there are tools in golang which do this). As import statements
        // are always at the start of the file, they will cause us to fall back to proportional caret positioning
        // when they're modified.
        // By doing the calculation based on reversed CharSequences, we side-step this problem.
        CharSequence oldReversed = new ReversedCharSequence(getTextBuffer());
        CharSequence newReversed = new ReversedCharSequence(newText);
        int oldPosReversed = oldReversed.length() - getSelectionStart();
        int newPosReversed = guessTargetCaretPos(oldReversed, oldPosReversed, newReversed);
        return newText.length() - newPosReversed;
    }
    
    // ReversedCharSequence implements only the minimal interface of CharSequence we need for guessTargetCaretPos, namely
    // length() and charAt(int). The subSequence and toString() are not implemented properly.
    private static class ReversedCharSequence implements CharSequence {
        private CharSequence seq;
        
        public ReversedCharSequence(CharSequence seq) {
            this.seq = seq;
        }
        
        public int length() {
            return seq.length();
        }
        
        public char charAt(int i) {
            return seq.charAt(seq.length() - 1 - i);
        }
        
        public CharSequence subSequence(int s, int e) {
            throw new UnsupportedOperationException("ReversedCharSequence doesn't implement subSequence");
        }
        
        public String toString() {
            throw new UnsupportedOperationException("ReversedCharSequence doesn't implement toString");
        }
    }
    
    private int guessTargetCaretPos(CharSequence oldText, int oldPos, CharSequence newText) {
        if (oldText.length() == 0) {
            // This also prevents a divide by zero if we fall back to keeping the same proportional distance into the file.
            return 0;
        }
        int newPos = 0;
        for (int i = 0; i < oldText.length(); i++) {
            if (i == oldPos) {
                // We've reached the caret location in the original text. Whatever equivalent index
                // we've found in the new text is our best guess at where the caret should end up.
                return newPos;
            }
            char oldCh = oldText.charAt(i);
            if (ignoreCharForCaretGuessing(oldCh)) {
                // If the same character is present in the new text, try to skip past it too.
                // This allows us to more accurately keep the caret position around skippable characters,
                // for example when reformatting code when the caret is positioned after a space.
                if (newPos < newText.length() && oldCh == newText.charAt(newPos)) {
                    newPos++;
                }
                continue;
            }
            char newCh = ' ';
            while (newPos < newText.length() && ignoreCharForCaretGuessing(newCh)) {
                newCh = newText.charAt(newPos++);
            }
            if (oldCh != newCh) {
                // If we see non-indent-affected chars differ in the old vs new content, we assume this is not
                // a simple case of reformatting. Just try to keep roughly the same distance into the file.
                break;
            }
        }
        return (int) ((long) oldPos * newText.length() / oldText.length());
    }
    
    private boolean ignoreCharForCaretGuessing(char ch) {
        // For caret position guessing, ignore all whitespace, as that's precisely what's going to be
        // changed by code formatting tools. In addition, we ignore common comment demarking characters,
        // as some formatters will splurge comments across multiple lines, thus adding to the set of
        // comment markers.
        return Character.isWhitespace(ch) || ch == '*' || ch == '/' || ch == '#';
    }
    
    /**
     * Appends the given string to the end of the text. This is meant for
     * programmatic use, and so does not pay attention to or modify the
     * selection.
     */
    public void append(String newText) {
        getLock().getWriteLock();
        try {
            SelectionSetter noChange = new SelectionSetter(SelectionSetter.DO_NOT_CHANGE);
            PTextBuffer buffer = getTextBuffer();
            buffer.replace(noChange, buffer.length(), 0, newText, noChange);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    /**
     * Returns a copy of the text in this text area.
     */
    public String getText() {
        getLock().getReadLock();
        try {
            return getTextBuffer().toString();
        } finally {
            getLock().relinquishReadLock();
        }
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
        return getLineHeight();
    }
    
    public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
        Insets insets = getInsets();
        if (columnCount != 0) {
            result.width = Math.max(result.width, columnCount * metrics[Font.PLAIN].charWidth('m') + insets.left + insets.right);
        }
        if (rowCount != 0) {
            result.height = Math.max(result.height, rowCount * getLineHeight() + insets.top + insets.bottom);
        }
        return result;
    }
    
    /**
     * Sets the functor that reformats pasted text before it replaces the selection.
     */
    public void setPastedTextReformatter(UnaryFunctor<String, String> pastedTextReformatter) {
        this.pastedTextReformatter = pastedTextReformatter;
    }
    
    /**
     * Pastes the clipboard contents. The pasted content replaces the selection.
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
        if (isEditable() == false) {
            return;
        }
        getLock().getReadLock();
        try {
            Transferable contents = clipboard.getContents(this);
            String string = pastedTextReformatter.evaluate((String) contents.getTransferData(DataFlavor.stringFlavor));
            pasteAndReIndent(string);
        } catch (Exception ex) {
            Log.warn("Couldn't paste.", ex);
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    public void pasteAndReIndent(String string) {
        getLock().getWriteLock();
        try {
            final int startOffsetOfReplacement = getSelectionStart();
            final int endOffsetOfReplacement = startOffsetOfReplacement + string.length();
            getTextBuffer().getUndoBuffer().startCompoundEdit();
            try {
                replaceSelection(string);
                getIndenter().fixIndentationBetween(startOffsetOfReplacement, endOffsetOfReplacement);
            } finally {
                getTextBuffer().getUndoBuffer().finishCompoundEdit();
            }
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public void copy() {
        copyToClipboard(getToolkit().getSystemClipboard());
    }
    
    public void cut() {
        getLock().getWriteLock();
        try {
            if (hasSelection() && isEditable()) {
                copy();
                replaceSelection("");
            }
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    public boolean isEditable() {
        return editable;
    }
    
    /**
     * Sets whether or not this text component should be editable by the user.
     * A PropertyChange event ("editable") is fired when the state is changed.
     */
    public void setEditable(boolean newState) {
        if (editable == newState) {
            return;
        }
        boolean oldState = this.editable;
        this.editable = newState;
        firePropertyChange("editable", Boolean.valueOf(oldState), Boolean.valueOf(newState));
    }
    
    //
    // Find functionality.
    //
    
    /**
     * Highlights all matches of the given regular expression.
     * The given BirdView (which can be null) will be updated to correspond to the new matches.
     */
    public int findAllMatches(String regularExpression, BirdView birdView) {
        getLock().getWriteLock();
        try {
            return findAllMatchesWithWriteLockAlreadyHeld(regularExpression, birdView);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    // Return value is total match count.
    private int findAllMatchesWithWriteLockAlreadyHeld(String regularExpression, BirdView birdView) {
        // Anything to search for?
        if (regularExpression == null || regularExpression.length() == 0) {
            removeHighlights(PFind.MatchHighlight.HIGHLIGHTER_NAME);
            if (birdView != null) {
                birdView.clearMatchingLines();
            }
            return 0;
        }
        
        List<PHighlight> old = getHighlightManager().getNamedHighlightsOverlapping(PFind.MatchHighlight.HIGHLIGHTER_NAME, 0, getTextBuffer().length());
        
        // Find all the matches, and update the set of highlights in the text area.
        // We used to do this the brute-force way, by dropping all highlights and repopulating,
        // but that's really very slow, particularly when the display area is large, as in involves
        // repainting the entire text window.
        // We could further improve the speed by only checking the area which was modified for changes
        // in the regexp matches, rather than running regexp across the whole file. My timings suggest
        // it takes around 1-6ms to update highlights when the regexp is "void" in this file. This
        // seems to be OK, but in other cases it may not be. I guess for now it's not worthwhile.
        if (birdView != null) {
            birdView.setValueIsAdjusting(true);
        }
        try {
            // To correctly update the birdview, we need to know which lines used to be highlighted
            // but aren't any more. We can't just remove the matched line when we drop an old highlight,
            // as there may be other highlights on the same line.
            HashSet<Integer> birdViewKeepLines = new HashSet<Integer>();
            HashSet<Integer> birdViewDeleteLines = new HashSet<Integer>();
            ListIterator<PHighlight> oldIt = old.listIterator();
            PHighlight oldHighlight = null;
            int matchCount = 0;
            Matcher matcher = PatternUtilities.smartCaseCompile(regularExpression).matcher(getTextBuffer());
            while (matcher.find()) {
                ++matchCount;
                boolean done = false;
                PHighlight highlight = new PFind.MatchHighlight(this, matcher.start(), matcher.end());
                while (oldHighlight != null || (oldHighlight == null && oldIt.hasNext())) {
                    if (oldHighlight == null) {
                        oldHighlight = oldIt.next();
                    }
                    if (oldHighlight.getStartIndex() >= highlight.getStartIndex()) {
                        // The new highlight is indeed new, so we break out to go deal with it.
                        // The oldHighlight is later on in the file, so we'll keep it around
                        // for later processing.
                        break;
                    }
                    // This oldHighlight needs to be removed, as it's no longer valid.
                    removeHighlight(oldHighlight);
                    birdViewDeleteLines.add(getLineOfOffset(oldHighlight.getEndIndex()));
                    oldHighlight = null;
                }
                int highlightLine = getLineOfOffset(matcher.end());
                birdViewKeepLines.add(highlightLine);
                if (!highlight.equals(oldHighlight)) {
                    if (oldHighlight != null && oldHighlight.getStartIndex() == highlight.getStartIndex()) {
                        // Old highlight starts at the same position, but ends elsewhere. This can happen while we're
                        // writing the find string. In this case we must remove the old highlight and add the new one.
                        removeHighlight(oldHighlight);
                        oldHighlight = null;
                        // No need to ask for the removal from birdview here, as the new highlight will be at the same position.
                    }
                    if (birdView != null) {
                        birdView.addMatchingLine(highlightLine);
                    }
                    addHighlight(highlight);
                } else {
                    // We've dealt with this old highlight, as it's the same as the new one, so
                    // drop it so that next time around the loop, we fetch the next one.
                    oldHighlight = null;
                }
            }
            // If we've finished with the matches, we must check if there are old highlights still, which appear
            // after the last new highlight. They must all be removed.
            while (oldHighlight != null || oldIt.hasNext()) {
                if (oldHighlight == null) {
                    oldHighlight = oldIt.next();
                }
                removeHighlight(oldHighlight);
                birdViewDeleteLines.add(getLineOfOffset(oldHighlight.getEndIndex()));
                oldHighlight = null;
            }
            // Clear up lines in the birdview that have had highlights removed, but none kept or added.
            if (birdView != null) {
                for (Integer line : birdViewDeleteLines) {
                    if (!birdViewKeepLines.contains(line)) {
                        birdView.removeMatchingLine(line);
                    }
                }
            }
            return matchCount;
        } finally {
            if (birdView != null) {
                birdView.setValueIsAdjusting(false);
            }
        }
    }
    
    public void findNext() {
        findNextOrPrevious(true);
    }
    
    public void findPrevious() {
        findNextOrPrevious(false);
    }
    
    private void findNextOrPrevious(boolean next) {
        fireAboutToFindEvent();
        PHighlight nextHighlight = highlights.getNextOrPreviousHighlight(PFind.MatchHighlight.HIGHLIGHTER_NAME, next, next ? getSelectionEnd() : getSelectionStart());
        if (nextHighlight != null) {
            selectHighlight(nextHighlight);
        }
    }
    
    public int getFindMatchCount() {
        return highlights.countHighlightsOfType(PFind.MatchHighlight.HIGHLIGHTER_NAME);
    }
    
    public void fireAboutToFindEvent() {
        for (PFindListener findListener : findListeners) {
            findListener.aboutToFind();
        }
    }
    
    public void addFindListener(PFindListener findListener) {
        findListeners.add(findListener);
    }
    
    public void removeFindListener(PFindListener findListener) {
        findListeners.remove(findListener);
    }
    
    public EPopupMenu getPopupMenu() {
        return popupMenu;
    }
    
    private void initPopupMenu() {
        popupMenu =  new EPopupMenu(this);
    }
    
    /**
     * Selects the given line, and ensures that the selection is visible.
     * For this method, intended to be directly connected to the UI, line numbers are 1-based.
     */
    public void goToLine(int line) {
        // Humans number lines from 1, the rest of PTextArea from 0.
        --line;
        final int start = getLineStartOffset(line);
        final int end = getLineEndOffsetBeforeTerminator(line);
        centerOnNewSelection(start, end);
    }
    
    /**
     * Changes the selection, and centers the selection on the display.
     */
    public void centerOnNewSelection(final int start, final int end) {
        // Center first, to avoid flicker because PTextArea.select may
        // have caused some scrolling to ensure that the selection is
        // visible, but probably won't have to scrolled such that our
        // offset is centered.
        centerOffsetInDisplay(start);
        select(start, end);
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType newFileType) {
        fileType = newFileType;
    }
    
    public boolean shouldHideMouseWhenTyping() {
        return shouldHideMouseWhenTyping;
    }
    
    public void setShouldHideMouseWhenTyping(boolean newState) {
        shouldHideMouseWhenTyping = newState;
    }
}
