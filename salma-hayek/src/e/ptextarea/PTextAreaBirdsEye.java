package e.ptextarea;

import e.gui.*;

public class PTextAreaBirdsEye implements BirdsEye {
    private final PTextArea textArea;
    private int currentLineInTextArea = -1;
    
    public PTextAreaBirdsEye(PTextArea textArea) {
        this.textArea = textArea;
        initCaretListener();
    }
    
    private void initCaretListener() {
        PCaretListener listener = new PCaretListener() {
            public void caretMoved(PTextArea textArea, int selectionStart, int selectionEnd) {
                currentLineInTextArea = textArea.getLineOfOffset(selectionStart);
            }
        };
        textArea.addCaretListener(listener);
    }
    
    public int getVisibleLineCount() {
        return textArea.getSplitLineCount();
    }
    
    public int getVisibleLineIndex(int logicalLineIndex) {
        return textArea.getSplitLineIndex(logicalLineIndex);
    }
    
    public int getLogicalLineIndex(int visibleLineIndex) {
        int charIndex = textArea.getTextIndex(new PCoordinates(visibleLineIndex, 0));
        return textArea.getLineOfOffset(charIndex);
    }
    
    public boolean isCurrentLineIndex(int logicalLineIndex) {
        return (logicalLineIndex == currentLineInTextArea);
    }
    
    public void goToLineAtIndex(int logicalLineIndex) {
        textArea.goToLine(logicalLineIndex + 1);
    }
}
