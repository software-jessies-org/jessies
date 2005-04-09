package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import e.util.*;

public class PKeyHandler extends KeyAdapter {
    private PTextArea textArea;
    private UpDownMovementHandler movementHandler = new UpDownMovementHandler();
    
    public PKeyHandler(PTextArea textArea) {
        this.textArea = textArea;
        textArea.addCaretListener(movementHandler);
    }
    
    public void keyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getKeyCode()) {
            case KeyEvent.VK_T: textArea.printLineInfo(); break;
            case KeyEvent.VK_L: textArea.getLineList().printLineInfo(); break;
            case KeyEvent.VK_R: textArea.repaint(); break;
            case KeyEvent.VK_Z: undoRedo(event.isShiftDown()); break;
            }
        } else {
            if (handleInvisibleKeyPressed(event)) {
                event.consume();
            }
        }
    }
    
    private void undoRedo(boolean isShifted) {
        PUndoBuffer undoer = textArea.getPTextBuffer().getUndoBuffer();
        if (isShifted) {
            undoer.redo();
        } else {
            undoer.undo();
        }
    }
    
    public class PColoredHighlightMatcher implements PHighlightMatcher {
        private Color color;
        
        public PColoredHighlightMatcher(Color color) {
            this.color = color;
        }
        
        public boolean matches(PHighlight highlight) {
            if (highlight instanceof PColoredHighlight) {
                return ((PColoredHighlight) highlight).getColor().equals(color);
            } else {
                return false;
            }
        }
    }
    
    public void keyTyped(KeyEvent event) {
        if (isInsertableCharacter(event)) {
            insertCharacter(event.getKeyChar());
            event.consume();
        }
    }
    
    private boolean isInsertableCharacter(KeyEvent e) {
        if (e.isAltDown() || e.isAltGraphDown() || e.isControlDown() || e.isMetaDown()) {
            return false;
        }
        
        switch (e.getKeyChar()) {
        case KeyEvent.CHAR_UNDEFINED:
        case '\010':  // backspace
        case '\177':  // delete
            return false;
            
        default:
            return true;
        }
    }
    
    private boolean handleInvisibleKeyPressed(KeyEvent event) {
        boolean shiftDown = event.isShiftDown();
        if (movementHandler.handleMovementKeys(event)) {
            return true;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.VK_LEFT: moveCaret(shiftDown, caretLeft(shiftDown)); break;
            case KeyEvent.VK_RIGHT: moveCaret(shiftDown, caretRight(shiftDown)); break;
            case KeyEvent.VK_HOME: moveCaret(shiftDown, caretToStartOfLine()); break;
            case KeyEvent.VK_END: moveCaret(shiftDown, caretToEndOfLine()); break;
            case KeyEvent.VK_BACK_SPACE: backspace(); break;
            case KeyEvent.VK_DELETE: delete(); break;
            
        default:
            return false;
        }
        return true;
    }
    
    private void insertCharacter(char ch) {
        textArea.insert(new CharArrayCharSequence(new char[] { ch }));
    }
    
    private void backspace() {
        int start = textArea.getSelectionStart();
        int end = textArea.getSelectionEnd();
        if (start == end && start > 0) {
            --start;
        }
        if (start != end) {
            textArea.delete(start, end - start);
        }
    }
    
    private void delete() {
        int start = textArea.getSelectionStart();
        int end = textArea.getSelectionEnd();
        if (start == end && end < textArea.getPTextBuffer().length() - 1) {
            ++end;
        }
        if (start != end) {
            textArea.delete(start, end - start);
        }
    }
    
    private void moveCaret(boolean shiftDown, int newOffset) {
        int start = newOffset;
        int end = newOffset;
        if (shiftDown) {
            start = Math.min(textArea.getSelectionStart(), start);
            end = Math.max(textArea.getSelectionEnd(), end);
        }
        textArea.select(start, end);
    }
    
    private int caretToStartOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getSelectionStart());
        return textArea.getLineStartOffset(lineIndex);
    }
    
    private int caretToEndOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getSelectionEnd());
        return textArea.getLineEndOffset(lineIndex);
    }
    
    private int caretLeft(boolean shiftDown) {
        // FIXME: we need to remember a bias for keyboard shift+arrow movement.
        if (shiftDown || textArea.getSelectionStart() == textArea.getSelectionEnd()) {
            return Math.max(0, textArea.getSelectionStart() - 1);
        } else {
            return textArea.getSelectionStart();
        }
    }
    
    private int caretRight(boolean shiftDown) {
        // FIXME: we need to remember a bias for keyboard shift+arrow movement.
        if (shiftDown || textArea.getSelectionStart() == textArea.getSelectionEnd()) {
            return Math.min(textArea.getSelectionEnd() + 1, textArea.getPTextBuffer().length());
        } else {
            return textArea.getSelectionEnd();
        }
    }
    
    private class UpDownMovementHandler implements PCaretListener {
        private boolean isEntered = false;
        private int xPixelLocation = -1;
        
        public void caretMoved(int selectionStart, int selectionEnd) {
            if (isEntered == false) {
                xPixelLocation = -1;
            }
        }
        
        public boolean handleMovementKeys(KeyEvent event) {
            isEntered = true;
            if (xPixelLocation == -1) {
                xPixelLocation = getCurrentXPixelLocation();
            }
            boolean shiftDown = event.isShiftDown();
            try {
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_UP: moveCaret(shiftDown, caretUp()); return true;
                    case KeyEvent.VK_DOWN: moveCaret(shiftDown, caretDown()); return true;
                    default: return false;
                }
            } finally {
                isEntered = false;
            }
        }
        
        private int getCurrentXPixelLocation() {
            PCoordinates coords = textArea.getCoordinates(textArea.getSelectionStart());
            return textArea.getViewCoordinates(coords).x;
        }
        
        private int caretUp() {
            PCoordinates coords = textArea.getCoordinates(textArea.getSelectionStart());
            int lineIndex = coords.getLineIndex();
            if (lineIndex == 0) {
                return 0;
            } else {
                int y = textArea.getViewCoordinates(new PCoordinates(lineIndex - 1, 0)).y;
                return textArea.getTextIndex(textArea.getNearestCoordinates(new Point(xPixelLocation, y)));
            }
        }
        
        private int caretDown() {
            PCoordinates coords = textArea.getCoordinates(textArea.getSelectionStart());
            int lineIndex = coords.getLineIndex();
            if (lineIndex == textArea.getVisibleLineCount() - 1) {
                return textArea.getPTextBuffer().length();
            } else {
                int y = textArea.getViewCoordinates(new PCoordinates(lineIndex + 1, 0)).y;
                return textArea.getTextIndex(textArea.getNearestCoordinates(new Point(xPixelLocation, y)));
            }
        }
    }
}
