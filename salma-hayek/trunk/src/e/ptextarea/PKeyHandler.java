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
                    textArea.setCaretLocation(caret + insertion.length());
                }
                break;
            case KeyEvent.VK_D:
                {
                    int caret = textArea.getCaretLocation();
                    int delCount = Math.min(caret, 20);
                    textArea.getPTextBuffer().delete(caret - delCount, delCount);
                    textArea.setCaretLocation(caret - delCount);
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
        long startTime = System.currentTimeMillis();
        if (event.isControlDown()) {
            // Do nothing.
        } else {
            if (event.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                textArea.getPTextBuffer().insert(textArea.getCaretLocation(), new char[] { event.getKeyChar() });
                textArea.setCaretLocation(textArea.getCaretLocation() + 1);
            }
        }
        System.err.println("keyTyped took " + (System.currentTimeMillis() - startTime) + "ms.");
    }
    
    private boolean handleInvisibleKeyPressed(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_LEFT: moveCaretLeft(); break;
            case KeyEvent.VK_RIGHT: moveCaretRight(); break;
            case KeyEvent.VK_UP: moveCaretUp(); break;
            case KeyEvent.VK_DOWN: moveCaretDown(); break;
            case KeyEvent.VK_HOME: moveCaretToStartOfLine(); break;
            case KeyEvent.VK_END: moveCaretToEndOfLine(); break;

        default:
            return false;
        }
        return true;
    }
    
    private void moveCaretToStartOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        textArea.setCaretLocation(textArea.getLineStartOffset(lineIndex));
    }
    
    private void moveCaretToEndOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        textArea.setCaretLocation(textArea.getLineEndOffset(lineIndex));
    }
    
    private void moveCaretLeft() {
        if (textArea.getCaretLocation() > 0) {
            textArea.setCaretLocation(textArea.getCaretLocation() - 1);
        }
    }
    
    private void moveCaretRight() {
        if (textArea.getCaretLocation() < textArea.getPTextBuffer().length() - 1) {
            textArea.setCaretLocation(textArea.getCaretLocation() + 1);
        }
    }
    
    private void moveCaretUp() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        if (lineIndex == 0) {
            textArea.setCaretLocation(0);
        } else {
            int charOffset = textArea.getCaretLocation() - textArea.getLineStartOffset(lineIndex);
            lineIndex--;
            charOffset = Math.min(charOffset, textArea.getLineList().getLine(lineIndex).getLengthBeforeTerminator());
            textArea.setCaretLocation(textArea.getLineStartOffset(lineIndex) + charOffset);
        }
    }
    
    private void moveCaretDown() {
        int lineIndex = textArea.getLineOfOffset(textArea.getCaretLocation());
        if (lineIndex == textArea.getLineCount() - 1) {
            textArea.setCaretLocation(textArea.getPTextBuffer().length());
        } else {
            int charOffset = textArea.getCaretLocation() - textArea.getLineStartOffset(lineIndex);
            lineIndex++;
            charOffset = Math.min(charOffset, textArea.getLineList().getLine(lineIndex).getLengthBeforeTerminator());
            textArea.setCaretLocation(textArea.getLineStartOffset(lineIndex) + charOffset);
        }
    }
}
