package e.ptextarea;


import java.awt.event.*;

public class PMouseHandler extends MouseAdapter implements MouseMotionListener {
    private PTextArea textArea;
    private boolean isDraggingSelection = false;
    private boolean dragHandlerWasCalled = false;
    private int pressedLocation = -1;
    
    public PMouseHandler(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            return;
        }
        textArea.requestFocus();
        pressedLocation = getLocationOfMouse(e);
        textArea.setCaretLocation(pressedLocation);
        isDraggingSelection = true;
        dragHandlerWasCalled = false;
    }
    
    private int getLocationOfMouse(MouseEvent event) {
        PCoordinates nearestChar = textArea.getNearestCoordinates(event.getPoint());
        return textArea.getTextIndex(nearestChar);
    }
    
    public void mouseReleased(MouseEvent event) {
        isDraggingSelection = false;
        if (dragHandlerWasCalled == false) {
            textArea.clearSelection();
        }
    }
    
    public void mouseDragged(MouseEvent event) {
        if (isDraggingSelection) {
            int location = getLocationOfMouse(event);
            textArea.select(Math.min(location, pressedLocation), Math.max(location, pressedLocation));
            textArea.setCaretLocation(location);
            dragHandlerWasCalled = true;
        }
    }
    
    public void mouseMoved(MouseEvent event) { }
}
