package e.ptextarea;


import java.awt.event.*;

public class PMouseHandler extends MouseAdapter {
    private PTextArea textArea;
    
    public PMouseHandler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void mouseClicked(MouseEvent event) {
        textArea.requestFocus();
        PCoordinates nearestChar = textArea.getNearestCoordinates(event.getPoint());
        textArea.setCaretLocation(textArea.getTextIndex(nearestChar));
    }
}
