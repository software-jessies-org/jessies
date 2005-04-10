package e.ptextarea;


import java.awt.*;
import java.awt.event.*;

public class PMouseHandler extends MouseAdapter implements MouseMotionListener {
    private PTextArea textArea;
    private DragHandler dragHandler;
    
    public PMouseHandler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            return;
        }
        int pressedOffset = getOffsetAtMouse(e);
        if (e.getClickCount() == 1) {
            textArea.requestFocus();
        }
        textArea.getPTextStyler().mouseClicked(e, pressedOffset);
        if (e.isConsumed() == false) {
            dragHandler = getDragHandlerForClick(e);
            dragHandler.makeInitialSelection(pressedOffset);
        }
    }
    
    private DragHandler getDragHandlerForClick(MouseEvent e) {
        switch (e.getClickCount() % 3) {
        case 0: return new TripleClickDragHandler();
        case 1: return new SingleClickDragHandler();
        case 2: return new DoubleClickDragHandler();
        default: throw new RuntimeException("Logically impossible");
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
            dragHandler.mouseDragged(event);
        }
    }
    
    public void mouseMoved(MouseEvent event) {
        Cursor newCursor = textArea.getPTextStyler().getCursorForLocation(event.getPoint());
        textArea.setCursor(newCursor);
    }
    
    private interface DragHandler {
        public void makeInitialSelection(int pressedOffset);
        public void mouseDragged(MouseEvent event);
    }
    
    private class SingleClickDragHandler implements DragHandler {
        private int pressedOffset;
        
        public void makeInitialSelection(int pressedOffset) {
            this.pressedOffset = pressedOffset;
            textArea.select(pressedOffset, pressedOffset);
        }
        
        public void mouseDragged(MouseEvent event) {
            int offset = getOffsetAtMouse(event);
            textArea.select(Math.min(offset, pressedOffset), Math.max(offset, pressedOffset));
        }
    }
    
    private class DoubleClickDragHandler implements DragHandler {
        private int pressedOffset;
        
        public void makeInitialSelection(int pressedOffset) {
            this.pressedOffset = pressedOffset;
            selectByWord(pressedOffset, pressedOffset);
        }
        
        private void selectByWord(int startOffset, int endOffset) {
            CharSequence chars = textArea.getPTextBuffer();
            String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
            textArea.select(PWordUtilities.getWordStart(chars, startOffset, stopChars), PWordUtilities.getWordEnd(chars, endOffset, stopChars));
        }
        
        public void mouseDragged(MouseEvent event) {
            int offset = getOffsetAtMouse(event);
            selectByWord(Math.min(offset, pressedOffset), Math.max(offset, pressedOffset));
        }
    }
    
    private class TripleClickDragHandler implements DragHandler {
        private int pressedLine;
        
        public void makeInitialSelection(int pressedOffset) {
            this.pressedLine = textArea.getLineOfOffset(pressedOffset);
            textArea.select(textArea.getLineStartOffset(pressedLine), getLineEndOffset(pressedLine));
        }
        
        private int getLineEndOffset(int line) {
            if (line == textArea.getLineCount() - 1) {
                return textArea.getLineEndOffset(line);
            } else {
                return textArea.getLineStartOffset(line + 1);
            }
        }
        
        public void mouseDragged(MouseEvent event) {
            int currentLine = textArea.getLineOfOffset(getOffsetAtMouse(event));
            int minLine = Math.min(currentLine, pressedLine);
            int maxLine = Math.max(currentLine, pressedLine);
            textArea.select(textArea.getLineStartOffset(minLine), getLineEndOffset(maxLine));
        }
    }
}
