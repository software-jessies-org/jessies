package e.ptextarea;


import java.awt.*;
import java.awt.event.*;

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
            case KeyEvent.VK_I:
                {
                    int caret = textArea.getCaretLocation();
                    String insertion = "Insertion\nof some\ntext";
                    textArea.getPTextBuffer().insert(caret, insertion.toCharArray());
                    textArea.setCaretPosition(caret + insertion.length());
                }
                break;
            case KeyEvent.VK_D:
                {
                    int caret = textArea.getCaretLocation();
                    int delCount = Math.min(caret, 20);
                    textArea.getPTextBuffer().remove(caret - delCount, delCount);
                    textArea.setCaretPosition(caret - delCount);
                }
                break;
                
            case KeyEvent.VK_H:
                {
                    int caret = textArea.getCaretLocation();
                    int end = Math.min(caret + 20, textArea.getPTextBuffer().length());
                    textArea.addHighlight(new PColoredHighlight(textArea, caret, end, Color.YELLOW));
                }
                break;
                
            case KeyEvent.VK_A:
                {
                    if (event.isShiftDown()) {
                        textArea.removeHighlights(new PColoredHighlightMatcher(Color.CYAN));
                    } else {
                        for (int i = 0; i < textArea.getPTextBuffer().length() - 4; i += 4) {
                            textArea.addHighlight(new PColoredHighlight(textArea, i, i + 2, Color.CYAN));
                        }
                    }
                }
            }
        } else {
            if (handleInvisibleKeyPressed(event)) {
                event.consume();
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
        if (textArea.hasSelection()) {
            deleteSelection();
        }
        textArea.getPTextBuffer().insert(textArea.getCaretLocation(), new char[] { ch });
        textArea.setCaretPosition(textArea.getCaretLocation() + 1);
    }
    
    private void deleteSelection() {
        int selectionStart = textArea.getSelectionStart();
        int selectionLength = textArea.getSelectionEnd() - selectionStart;
        textArea.clearSelection();
        textArea.getPTextBuffer().remove(selectionStart, selectionLength);
        textArea.setCaretPosition(selectionStart);
    }
    
    private void backspace() {
        if (textArea.hasSelection()) {
            deleteSelection();
        } else {
            int caret = textArea.getCaretLocation();
            if (caret > 0) {
                caret--;
                textArea.setCaretPosition(caret);
                textArea.getPTextBuffer().remove(caret, 1);
            }
        }
    }
    
    private void delete() {
        if (textArea.hasSelection()) {
            deleteSelection();
        } else {
            int caret = textArea.getCaretLocation();
            PTextBuffer buffer = textArea.getPTextBuffer();
            if (caret < buffer.length() - 1) {
                buffer.remove(caret, 1);
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
