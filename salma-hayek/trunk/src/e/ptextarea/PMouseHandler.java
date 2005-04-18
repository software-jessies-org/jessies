package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PMouseHandler extends MouseAdapter implements MouseMotionListener {
    private PTextArea textArea;
    private PDragHandler dragHandler;
    
    public PMouseHandler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void mousePressed(MouseEvent e) {
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
        int pressedOffset = getOffsetAtMouse(e);
        textArea.getPTextStyler().mouseClicked(e, pressedOffset);
        if (e.isConsumed() == false) {
            dragHandler = getDragHandlerForClick(e);
            dragHandler.makeInitialSelection(pressedOffset);
        }
    }
    
    private PDragHandler getDragHandlerForClick(MouseEvent e) {
        if (e.getClickCount() == 1) {
            return new SingleClickDragHandler();
        } else if (e.getClickCount() == 2) {
            PDragHandler result = textArea.getPTextStyler().getDoubleClickDragHandler(getOffsetAtMouse(e));
            return (result == null) ? new DoubleClickDragHandler() : result;
        } else {
            return new TripleClickDragHandler();
        }
    }
    
    private int getOffsetAtMouse(MouseEvent event) {
        PCoordinates nearestChar = textArea.getNearestCoordinates(event.getPoint());
        return textArea.getTextIndex(nearestChar);
    }
    
    public void mouseReleased(MouseEvent event) {
        dragHandler = null;
    }
    
    public void mouseDragged(MouseEvent event) {
        if (dragHandler != null) {
            int newOffset = getOffsetAtMouse(event);
            dragHandler.mouseDragged(newOffset);
            textArea.ensureVisibilityOfOffset(Math.min(Math.max(0, newOffset), textArea.getPTextBuffer().length()));
        }
    }
    
    public void mouseMoved(MouseEvent event) {
        Cursor newCursor = textArea.getPTextStyler().getCursorForLocation(event.getPoint());
        textArea.setCursor(newCursor);
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
            
            int bracketOffset = PBracketUtilities.findMatchingBracket(textArea.getPTextBuffer(), pressedOffset);
            if (bracketOffset != -1) {
                int start = Math.min(pressedOffset, bracketOffset);
                int end = Math.max(pressedOffset, bracketOffset);
                textArea.setSelection(start, end, false);
                return;
            }
            
            selectByWord(pressedOffset, pressedOffset, false);
        }
        
        private void selectByWord(int startOffset, int endOffset, boolean endIsAnchored) {
            CharSequence chars = textArea.getPTextBuffer();
            String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
            textArea.setSelection(PWordUtilities.getWordStart(chars, startOffset, stopChars), PWordUtilities.getWordEnd(chars, endOffset, stopChars), endIsAnchored);
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
            textArea.select(textArea.getLineStartOffset(pressedLine), getLineEndOffset(pressedLine));
        }
        
        private int getLineEndOffset(int line) {
            if (line == textArea.getLineCount() - 1) {
                return textArea.getLineEndOffsetBeforeTerminator(line);
            } else {
                return textArea.getLineStartOffset(line + 1);
            }
        }
        
        public void mouseDragged(int newOffset) {
            int currentLine = textArea.getLineOfOffset(newOffset);
            int minLine = Math.min(currentLine, pressedLine);
            int maxLine = Math.max(currentLine, pressedLine);
            textArea.setSelection(textArea.getLineStartOffset(minLine), getLineEndOffset(maxLine), (pressedLine == minLine));
        }
    }
}
