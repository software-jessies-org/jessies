package e.ptextarea;


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
        int pressedLocation = getLocationOfMouse(e);
        if (e.getClickCount() == 1) {
            textArea.requestFocus();
        }
        dragHandler = getDragHandlerForClick(e);
        dragHandler.makeInitialSelection(pressedLocation);
    }
    
    private DragHandler getDragHandlerForClick(MouseEvent e) {
        switch (e.getClickCount() % 3) {
        case 0: return new TripleClickDragHandler();
        case 1: return new SingleClickDragHandler();
        case 2: return new DoubleClickDragHandler();
        default: throw new RuntimeException("Logically impossible");
        }
    }
    
    private int getLocationOfMouse(MouseEvent event) {
        PCoordinates nearestChar = textArea.getNearestCoordinates(event.getPoint());
        return textArea.getTextIndex(nearestChar);
    }
    
    public void mouseReleased(MouseEvent event) {
        if (dragHandler != null) {
            dragHandler.mouseReleased(event);
        }
        dragHandler = null;
    }
    
    public void mouseDragged(MouseEvent event) {
        if (dragHandler != null) {
            dragHandler.mouseDragged(event);
        }
    }
    
    public void mouseMoved(MouseEvent event) { }
    
    private interface DragHandler {
        public void makeInitialSelection(int pressedLocation);
        public void mouseDragged(MouseEvent event);
        public void mouseReleased(MouseEvent event);
    }
    
    private class SingleClickDragHandler implements DragHandler {
        private boolean dragHandlerWasCalled = false;
        private int pressedLocation;
        
        public void makeInitialSelection(int pressedLocation) {
            this.pressedLocation = pressedLocation;
            textArea.clearSelection();
            textArea.setCaretLocation(pressedLocation);
        }
        
        public void mouseDragged(MouseEvent event) {
            int location = getLocationOfMouse(event);
            textArea.select(Math.min(location, pressedLocation), Math.max(location, pressedLocation));
            textArea.setCaretLocation(location);
            dragHandlerWasCalled = true;
        }
        
        public void mouseReleased(MouseEvent event) {
            if (dragHandlerWasCalled == false) {
                textArea.clearSelection();
            }
        }
    }
    
    private class DoubleClickDragHandler implements DragHandler {
        private int pressedLocation;
        
        public void makeInitialSelection(int pressedLocation) {
            this.pressedLocation = pressedLocation;
        }
        
        public void mouseDragged(MouseEvent event) {
        }
        
        public void mouseReleased(MouseEvent event) {
        }
    }
    
    private class TripleClickDragHandler implements DragHandler {
        private int pressedLine;
        
        public void makeInitialSelection(int pressedLocation) {
            this.pressedLine = textArea.getLineOfOffset(pressedLocation);
            textArea.select(textArea.getLineStartOffset(pressedLine), getLineEndOffset(pressedLine));
            textArea.setCaretLocation(getLineEndOffset(pressedLine));
        }
        
        private int getLineEndOffset(int line) {
            if (line == textArea.getLineCount() - 1) {
                return textArea.getLineEndOffset(line);
            } else {
                return textArea.getLineStartOffset(line + 1);
            }
        }
        
        public void mouseDragged(MouseEvent event) {
            int currentLine = textArea.getLineOfOffset(getLocationOfMouse(event));
            int minLine = Math.min(currentLine, pressedLine);
            int maxLine = Math.max(currentLine, pressedLine);
            textArea.select(textArea.getLineStartOffset(minLine), getLineEndOffset(maxLine));
            if (currentLine == maxLine) {
                textArea.setCaretLocation(getLineEndOffset(currentLine));
            } else {
                textArea.setCaretLocation(textArea.getLineStartOffset(currentLine));
            }
        }
        
        public void mouseReleased(MouseEvent event) { }
    }
}
