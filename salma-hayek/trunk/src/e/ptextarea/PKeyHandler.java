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
            case KeyEvent.VK_T:
                textArea.printLineInfo();
                return;
            case KeyEvent.VK_L:
                textArea.getLineList().printLineInfo();
                return;
            case KeyEvent.VK_R:
                textArea.repaint();
                return;
            }
        }
        if (handleInvisibleKeyPressed(event)) {
            event.consume();
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
        
        char ch = e.getKeyChar();
        return (ch != KeyEvent.CHAR_UNDEFINED && (ch == '\n' || ch == '\t' || ch >= ' ') && ch != Ascii.DEL);
    }
    
    private boolean handleInvisibleKeyPressed(KeyEvent event) {
        boolean byWord = GuiUtilities.isMacOs() ? event.isAltDown() : event.isControlDown();
        boolean extendingSelection = event.isShiftDown();
        int key = event.getKeyCode();
        if (isStartOfTextKey(event)) {
            moveCaret(extendingSelection, 0);
        } else if (isEndOfTextKey(event)) {
            moveCaret(extendingSelection, textArea.getTextBuffer().length());
        } else if (isStartOfLineKey(event)) {
            moveCaret(extendingSelection, caretToStartOfLine());
        } else if (isEndOfLineKey(event)) {
            moveCaret(extendingSelection, caretToEndOfLine());
        } else if (GuiUtilities.isMacOs() && (key == KeyEvent.VK_HOME || key == KeyEvent.VK_END)) {
            textArea.ensureVisibilityOfOffset((key == KeyEvent.VK_HOME) ? 0 : textArea.getTextBuffer().length());
        } else if (key == KeyEvent.VK_LEFT) {
            moveLeft(byWord, extendingSelection);
        } else if (key == KeyEvent.VK_RIGHT) {
            moveRight(byWord, extendingSelection);
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            backspace();
        } else if (key == KeyEvent.VK_DELETE) {
            if (event.isShiftDown()) {
                textArea.cut();
            } else {
                delete();
            }
        } else if (key == KeyEvent.VK_INSERT && event.isShiftDown()) {
            textArea.paste();
        } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            movementHandler.handleMovementKeys(event);
        } else {
            return false;
        }
        return true;
    }
    
    private boolean isStartOfLineKey(KeyEvent e) {
        if (GuiUtilities.isMacOs()) {
            return ((e.isControlDown() || e.isMetaDown()) && e.getKeyCode() == KeyEvent.VK_LEFT);
        } else {
            return (e.getKeyCode() == KeyEvent.VK_HOME);
        }
    }
    
    private boolean isEndOfLineKey(KeyEvent e) {
        if (GuiUtilities.isMacOs()) {
            return ((e.isControlDown() || e.isMetaDown()) && e.getKeyCode() == KeyEvent.VK_RIGHT);
        } else {
            return (e.getKeyCode() == KeyEvent.VK_END);
        }
    }
    
    private boolean isStartOfTextKey(KeyEvent e) {
        if (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_UP) {
            return true;
        }
        return (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_HOME);
    }
    
    private boolean isEndOfTextKey(KeyEvent e) {
        if (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_DOWN) {
            return true;
        }
        return (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_END);
    }
    
    private void insertCharacter(char ch) {
        if (ch == '\t') {
            textArea.insertTab();
        } else if (ch == '\n') {
            textArea.insertNewline();
        } else {
            CharSequence content = new CharArrayCharSequence(new char[] { ch });
            if (textArea.getIndenter().isElectric(ch)) {
                textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
                try {
                    textArea.replaceSelection(content);
                    textArea.getIndenter().fixIndentation();
                } finally {
                    textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
                }
            } else {
                textArea.replaceSelection(content);
            }
        }
    }
    
    private void backspace() {
        Range range = textArea.getIndenter().getRangeToRemove();
        if (range.isNonEmpty()) {
            textArea.delete(range.getStart(), range.length());
        }
    }
    
    private void delete() {
        int start = textArea.getSelectionStart();
        int end = textArea.getSelectionEnd();
        if (start == end && end < textArea.getTextBuffer().length()) {
            ++end;
        }
        if (start != end) {
            textArea.delete(start, end - start);
        }
    }
    
    private void moveCaret(boolean extendingSelection, int newOffset) {
        if (extendingSelection) {
            textArea.changeUnanchoredSelectionExtreme(newOffset);
        } else {
            textArea.setCaretPosition(newOffset);
        }
    }
    
    private int caretToStartOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getSelectionStart());
        return textArea.getLineStartOffset(lineIndex);
    }
    
    private int caretToEndOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getSelectionEnd());
        return textArea.getLineEndOffsetBeforeTerminator(lineIndex);
    }
    
    private void moveLeft(boolean byWord, boolean extendingSelection) {
        int newOffset = byWord ? caretToPreviousWord() : caretLeft(extendingSelection);
        moveCaret(extendingSelection, newOffset);
    }
    
    private void moveRight(boolean byWord, boolean extendingSelection) {
        int newOffset = byWord ? caretToNextWord() : caretRight(extendingSelection);
        moveCaret(extendingSelection, newOffset);
    }
    
    private int caretLeft(boolean extendingSelection) {
        int newOffset = textArea.getSelectionStart();
        if (extendingSelection) {
            newOffset = textArea.getUnanchoredSelectionExtreme() - 1;
        } else if (textArea.hasSelection() == false) {
            --newOffset;
        }
        return Math.max(0, newOffset);
    }
    
    private int caretRight(boolean extendingSelection) {
        int newOffset = textArea.getSelectionEnd();
        if (extendingSelection) {
            newOffset = textArea.getUnanchoredSelectionExtreme() + 1;
        } else if (textArea.hasSelection() == false) {
            ++newOffset;
        }
        return Math.min(newOffset, textArea.getTextBuffer().length());
    }
    
    private int caretToPreviousWord() {
        CharSequence chars = textArea.getTextBuffer();
        String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
        int offset = textArea.getUnanchoredSelectionExtreme();
        
        // If we're at the start of the document, we're not going far.
        if (offset == 0) {
            return 0;
        }
        
        // If we're at the start of a word, go to the start of the word before.
        if (PWordUtilities.isInWord(chars, offset - 1, stopChars) == false) {
            return PWordUtilities.getWordStart(chars, PWordUtilities.getNonWordStart(chars, offset - 1, stopChars), stopChars);
        }
        
        // Otherwise go to the start of the current word.
        return PWordUtilities.getWordStart(chars, offset, stopChars);
    }
    
    private int caretToNextWord() {
        CharSequence chars = textArea.getTextBuffer();
        String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
        int offset = textArea.getUnanchoredSelectionExtreme();
        
        // If we're at the end of the document, we're not going far.
        if (offset == chars.length()) {
            return offset;
        }
        
        // If we're in a word, go to the end of this word.
        if (PWordUtilities.isInWord(chars, offset, stopChars)) {
            return PWordUtilities.getWordEnd(chars, offset, stopChars);
        }
        
        // Otherwise go to the start of the next word.
        return PWordUtilities.getWordEnd(chars, PWordUtilities.getNonWordEnd(chars, PWordUtilities.getWordEnd(chars, offset, stopChars), stopChars), stopChars);
    }
    
    private class UpDownMovementHandler implements PCaretListener {
        private boolean isEntered = false;
        private int xPixelLocation = -1;
        
        public void caretMoved(PTextArea textArea, int selectionStart, int selectionEnd) {
            if (isEntered == false) {
                xPixelLocation = -1;
            }
        }
        
        public void handleMovementKeys(KeyEvent event) {
            isEntered = true;
            if (xPixelLocation == -1) {
                xPixelLocation = getCurrentXPixelLocation();
            }
            boolean extendingSelection = event.isShiftDown();
            try {
                moveCaret(extendingSelection, event.getKeyCode() == KeyEvent.VK_UP ? caretUp() : caretDown());
            } finally {
                isEntered = false;
            }
        }
        
        private int getCurrentXPixelLocation() {
            PCoordinates coords = textArea.getCoordinates(textArea.getUnanchoredSelectionExtreme());
            return textArea.getViewCoordinates(coords).x;
        }
        
        private int caretUp() {
            PCoordinates coords = textArea.getCoordinates(textArea.getUnanchoredSelectionExtreme());
            int lineIndex = coords.getLineIndex();
            if (lineIndex == 0) {
                return 0;
            } else {
                int y = textArea.getViewCoordinates(new PCoordinates(lineIndex - 1, 0)).y;
                return textArea.getTextIndex(textArea.getNearestCoordinates(new Point(xPixelLocation, y)));
            }
        }
        
        private int caretDown() {
            PCoordinates coords = textArea.getCoordinates(textArea.getUnanchoredSelectionExtreme());
            int lineIndex = coords.getLineIndex();
            if (lineIndex == textArea.getVisibleLineCount() - 1) {
                return textArea.getTextBuffer().length();
            } else {
                int y = textArea.getViewCoordinates(new PCoordinates(lineIndex + 1, 0)).y;
                return textArea.getTextIndex(textArea.getNearestCoordinates(new Point(xPixelLocation, y)));
            }
        }
    }
}
