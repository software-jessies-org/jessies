package e.ptextarea;


import java.awt.event.*;

public class PMouseHandler extends MouseAdapter {
    private PTextArea textArea;
    
    public PMouseHandler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            return;
        }
        textArea.requestFocus();
        PCoordinates nearestChar = textArea.getNearestCoordinates(e.getPoint());
        textArea.setCaretLocation(textArea.getTextIndex(nearestChar));
    }
}
