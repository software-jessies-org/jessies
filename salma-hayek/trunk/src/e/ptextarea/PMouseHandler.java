package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class PMouseHandler implements MouseInputListener {
    private PTextArea textArea;
    private PDragHandler dragHandler;
    private Point lastKnownPosition;
    
    public PMouseHandler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void mousePressed(MouseEvent e) {
        trackMouse(e);
        if (e.isPopupTrigger()) {
            return;
        }
        if (e.getClickCount() == 1) {
            textArea.requestFocus();
            if (SwingUtilities.isMiddleMouseButton(e)) {
                textArea.pasteSystemSelection();
                return;
            }
        }
        handleHyperlinks(e);
        if (e.isConsumed() == false) {
            dragHandler = getDragHandlerForClick(e);
            dragHandler.makeInitialSelection(getOffsetAtMouse());
        }
    }
    
    private PDragHandler getDragHandlerForClick(MouseEvent e) {
        if (e.getClickCount() == 1) {
            return new SingleClickDragHandler();
        } else if (e.getClickCount() == 2) {
            return new DoubleClickDragHandler();
        } else {
            return new TripleClickDragHandler();
        }
    }
    
    private int getOffsetAtMouse() {
        PCoordinates nearestChar = textArea.getNearestCoordinates(lastKnownPosition);
        return textArea.getTextIndex(nearestChar);
    }
    
    public void mouseReleased(MouseEvent e) {
        trackMouse(e);
        dragHandler = null;
    }
    
    public void mouseClicked(MouseEvent e) {
        trackMouse(e);
        // No explicit action.
    }
    
    public void mouseDragged(MouseEvent e) {
        trackMouse(e);
        if (dragHandler != null) {
            int newOffset = getOffsetAtMouse();
            dragHandler.mouseDragged(newOffset);
            textArea.ensureVisibilityOfOffset(Math.min(Math.max(0, newOffset), textArea.getTextBuffer().length()));
        }
    }
    
    public void mouseMoved(MouseEvent e) {
        trackMouse(e);
        updateCursorAndToolTip();
    }
    
    public void mouseEntered(MouseEvent e) {
        trackMouse(e);
        updateCursorAndToolTip();
    }
    
    public void mouseExited(MouseEvent e) {
        trackMouse(e);
        updateCursorAndToolTip();
    }
    
    private void trackMouse(MouseEvent e) {
        lastKnownPosition = e.getPoint();
    }
    
    public void updateCursorAndToolTip() {
        Cursor newCursor = null;
        String newToolTip = null;
        PLineSegment segment = textArea.getLineSegmentAtLocation(lastKnownPosition);
        if (segment != null && segment.getStyle() == PStyle.HYPERLINK) {
            newCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            newToolTip = PTextSegment.class.cast(segment).getToolTip();
        }
        textArea.setCursor(newCursor);
        textArea.setToolTipText(newToolTip);
    }
    
    private void handleHyperlinks(MouseEvent e) {
        PLineSegment segment = textArea.getLineSegmentAtLocation(lastKnownPosition);
        if (segment != null && segment.getStyle() == PStyle.HYPERLINK) {
            PTextSegment.class.cast(segment).linkClicked();
            e.consume();
        }
    }
    
    private class SingleClickDragHandler implements PDragHandler {
        public void makeInitialSelection(int pressedOffset) {
            textArea.select(pressedOffset, pressedOffset);
        }
        
        public void mouseDragged(int newOffset) {
            textArea.changeUnanchoredSelectionExtreme(newOffset);
        }
    }
    
    private class DoubleClickDragHandler implements PDragHandler {
        private int pressedOffset;
        
        // FIXME: doesn't work the same as native components.  The initial selection start should remain the
        // anchor in all cases.
        public void makeInitialSelection(int pressedOffset) {
            this.pressedOffset = pressedOffset;
            
            // 1. a string literal probably starts and ends with a word, so
            // check for string literals first.
            if (selectStringLiteral()) {
                return;
            }
            
            // 2. then other bracket characters.
            if (PBracketUtilities.isNextToBracket(textArea.getTextBuffer(), pressedOffset)) {
                int bracketOffset = PBracketUtilities.findMatchingBracketInSameStyle(textArea, pressedOffset);
                if (bracketOffset != -1) {
                    if (pressedOffset <= bracketOffset) {
                        textArea.setSelection(pressedOffset, bracketOffset, false);
                    } else {
                        // Selecting the region inside the brackets means starting one beyond the bracket.
                        textArea.setSelection(bracketOffset + 1, pressedOffset, false);
                    }
                    return;
                }
            }
            
            // 3. we'll have dealt with braces at the ends of lines by now, so
            // a double-click after the end of the line should select the whole
            // line. I don't know of any platform whose native behavior this is,
            // but it's how acme used to work, and hence wily, and hence Edit,
            // and I'm really used to it.
            PTextBuffer textBuffer = textArea.getTextBuffer();
            if (pressedOffset < textBuffer.length() && textBuffer.charAt(pressedOffset) == '\n') {
                selectLine(textArea.getLineOfOffset(pressedOffset));
                return;
            }
            
            // 4. our last resort is to select the word under the mouse.
            selectByWord(pressedOffset, pressedOffset, false);
        }
        
        private boolean selectStringLiteral() {
            // Are we in a string literal?
            Iterator<PLineSegment> it = textArea.getLogicalSegmentIterator(pressedOffset);
            if (it.hasNext() == false) {
                return false;
            }
            PLineSegment segment = it.next();
            if (segment.getStyle() != PStyle.STRING) {
                return false;
            }
            
            // If we're inside the " at either end, select the whole literal.
            boolean atStart = (segment.getOffset() == pressedOffset - 1);
            boolean atEnd = (segment.getEnd() == pressedOffset + 1);
            if (atStart || atEnd) {
                textArea.setSelection(segment.getOffset() + 1, segment.getEnd() - 1, false);
                return true;
            }
            return false;
        }
        
        private void selectByWord(int startOffset, int endOffset, boolean endIsAnchored) {
            CharSequence chars = textArea.getTextBuffer();
            String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
            textArea.setSelectionWithoutScrolling(PWordUtilities.getWordStart(chars, startOffset, stopChars), PWordUtilities.getWordEnd(chars, endOffset, stopChars), endIsAnchored);
        }
        
        public void mouseDragged(int newOffset) {
            int start = Math.min(newOffset, pressedOffset);
            int end = Math.max(newOffset, pressedOffset);
            selectByWord(start, end, (pressedOffset == start));
        }
    }
    
    private class TripleClickDragHandler implements PDragHandler {
        private int pressedLine;
        
        public void makeInitialSelection(int pressedOffset) {
            this.pressedLine = textArea.getLineOfOffset(pressedOffset);
            selectLine(pressedLine);
        }
        
        public void mouseDragged(int newOffset) {
            int currentLine = textArea.getLineOfOffset(newOffset);
            int minLine = Math.min(currentLine, pressedLine);
            int maxLine = Math.max(currentLine, pressedLine);
            textArea.setSelectionWithoutScrolling(textArea.getLineStartOffset(minLine), getLineEndOffset(maxLine), (pressedLine == minLine));
        }
    }
    
    private void selectLine(int line) {
        textArea.select(textArea.getLineStartOffset(line), getLineEndOffset(line));
    }
    
    private int getLineEndOffset(int line) {
        if (line == textArea.getLineCount() - 1) {
            return textArea.getLineEndOffsetBeforeTerminator(line);
        } else {
            return textArea.getLineStartOffset(line + 1);
        }
    }
}
