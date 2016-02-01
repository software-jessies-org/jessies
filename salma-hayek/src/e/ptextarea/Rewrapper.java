package e.ptextarea;

import java.awt.event.*;

/**
 * Causes the text in a given PTextArea to be re-wrapped if the text area's
 * width should change.
 */
class Rewrapper extends ComponentAdapter {
    private PTextArea textArea;
    private int lastWidth;
    
    Rewrapper(PTextArea textArea) {
        this.textArea = textArea;
        this.lastWidth = 0;
    }
    
    @Override
    public void componentResized(ComponentEvent event) {
        rewrap();
    }
    
    private void rewrap() {
        PLock lock = textArea.getLock();
        lock.getReadLock();
        try {
            if (getWidth() != lastWidth) {
                textArea.rewrap();
                lastWidth = getWidth();
                textArea.repaint();
            }
        } finally {
            lock.relinquishReadLock();
        }
    }
    
    private int getWidth() {
        return textArea.getWidth();
    }
}
