package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import e.util.*;

public class PKeyHandler extends KeyAdapter {
    private PTextArea textArea;
    
    public PKeyHandler(PTextArea textArea) {
        this.textArea = textArea;
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
            if (undoer.canRedo()) {
                undoer.redo();
            }
        } else {
            if (undoer.canUndo()) {
                undoer.undo();
            }
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
        switch (event.getKeyCode()) {
            case KeyEvent.VK_LEFT: moveCaret(event, caretLeft()); break;
            case KeyEvent.VK_RIGHT: moveCaret(event, caretRight()); break;
            case KeyEvent.VK_UP: moveCaret(event, caretUp()); break;
            case KeyEvent.VK_DOWN: moveCaret(event, caretDown()); break;
            case KeyEvent.VK_HOME: moveCaret(event, caretToStartOfLine()); break;
            case KeyEvent.VK_END: moveCaret(event, caretToEndOfLine()); break;
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
        if (textArea.hasSelection()) {
            textArea.deleteSelection();
        } else {
            int caret = textArea.getCaretLocation();
            if (caret > 0) {
                textArea.delete(caret - 1, 1);
            }
        }
    }
    
    private void delete() {
        if (textArea.hasSelection()) {
            textArea.deleteSelection();
        } else {
            int caret = textArea.getCaretLocation();
            if (caret < textArea.getPTextBuffer().length() - 1) {
                textArea.delete(caret, 1);
            }
        }
    }
    
    private void moveCaret(KeyEvent event, int newCaretLocation) {
        if (event.isShiftDown() == false) {
            textArea.clearSelection();
        }
        
        if (textArea.getCaretLocation() != newCaretLocation) {
            if (event.isShiftDown()) {
                int otherExtreme;
                if (textArea.getCaretLocation() == textArea.getSelectionStart()) {
                    otherExtreme = textArea.getSelectionEnd();
                } else {
                    otherExtreme = textArea.getSelectionStart();
                }
                textArea.select(Math.min(otherExtreme, newCaretLocation), Math.max(otherExtreme, newCaretLocation));
            }
            textArea.setCaretPosition(newCaretLocation);
        }
    }
    
    private int caretToStartOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        return textArea.getLineStartOffset(lineIndex);
    }
    
    private int caretToEndOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        return textArea.getLineEndOffset(lineIndex);
    }
    
    private int caretLeft() {
        if (textArea.getCaretLocation() > 0) {
            return textArea.getCaretLocation() - 1;
        } else {
            return textArea.getCaretLocation();
        }
    }
    
    private int caretRight() {
        if (textArea.getCaretLocation() < textArea.getPTextBuffer().length()) {
            return textArea.getCaretLocation() + 1;
        } else {
            return textArea.getCaretLocation();
        }
    }
    
    private int caretUp() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        if (lineIndex == 0) {
            return 0;
        } else {
            int charOffset = textArea.getCaretLocation() - textArea.getLineStartOffset(lineIndex);
            lineIndex--;
            charOffset = Math.min(charOffset, textArea.getLineList().getLine(lineIndex).getLengthBeforeTerminator());
            return textArea.getLineStartOffset(lineIndex) + charOffset;
        }
    }
    
    private int caretDown() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        if (lineIndex == textArea.getLineCount() - 1) {
            return textArea.getPTextBuffer().length();
        } else {
            int charOffset = textArea.getCaretLocation() - textArea.getLineStartOffset(lineIndex);
            lineIndex++;
            charOffset = Math.min(charOffset, textArea.getLineList().getLine(lineIndex).getLengthBeforeTerminator());
            return textArea.getLineStartOffset(lineIndex) + charOffset;
        }
    }
}
