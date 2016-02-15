package terminator.view.highlight;

import e.util.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import terminator.*;
import terminator.model.*;
import terminator.view.*;

/**
 * Implements the feel (rather than the look) of the selection. The look is
 * deferred to the Highlighter. The feel is the usual sweep-selection, plus
 * double-click to highlight a word (the exact definition of which is biased
 * towards shell-like applications), triple-click for line selection, and
 * shift-click to extend a selection.
 * FIXME: a shift-click after a double- or triple-click line/word selection should not cause us to change the original anchor.
 */
public class SelectionHighlighter implements ClipboardOwner, MouseListener, MouseMotionListener {
        
    private TerminalView view;
    // Both null or both non-null.
    private Location highlightStart, highlightEnd;
    // Internally used while dragging. May be null or non-null independently of highlightStart/highlightEnd.
    private Point initialPoint;
    private DragHandler dragHandler;
    
    // Whether selections are handled as rectangular blocks instead of whole lines.
    private boolean blockMode;
    
    /** Creates a SelectionHighlighter for selecting text in the given view, and adds us as mouse listeners to that view. */
    public SelectionHighlighter(TerminalView view) {
        this.view = view;
        view.addMouseListener(this);
        view.addMouseMotionListener(this);
        view.setAutoscrolls(true);
    }
    
    public boolean hasSelection() {
        return highlightStart != null;
    }

    public Location getStart() {
        return highlightStart;
    }

    public Location getEnd() {
        return highlightEnd;
    }

    public void textChanged(Location start, Location end) {
        if (hasSelection()) {
            if (highlightEnd.compareTo(start) > 0 && highlightStart.compareTo(end) < 0) {
                highlightStart = null;
                highlightEnd = null;
                view.repaint();
            }
        }
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.isConsumed() || !SwingUtilities.isLeftMouseButton(e) || e.isPopupTrigger()) {
            return;
        }
        
        // Shift-click should move one end of the selection.
        if (e.isShiftDown() && initialPoint != null) {
            // My intent was that the dragHandler would remain, but apparently dragHandler is always null.
            if (dragHandler == null) {
                dragHandler = new SingleClickDragHandler();
            }
            mouseDragged(e);
            return;
        }
        
        updateSelectionMode(e);
        initialPoint = e.getPoint();
        Location loc = view.viewToModel(initialPoint, blockMode);
        highlightStart = null;
        highlightEnd = null;
        
        if (loc.getLineIndex() >= view.getModel().getLineCount()) {
            return;
        }
        dragHandler = getDragHandlerForClick(e);
        mouseDragged(e);

        view.repaint();
    }
    
    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON2) {
            view.middleButtonPaste();
        }
    }
    
    public void mouseReleased(MouseEvent event) {
        if (SwingUtilities.isLeftMouseButton(event) && (dragHandler != null)) {
            // The user may release Ctrl before the mouse button.
            selectionChanged();
            dragHandler = null;
        }
    }
    
    public void mouseDragged(MouseEvent event) {
        if (SwingUtilities.isLeftMouseButton(event) && (dragHandler != null)) {
            updateSelectionMode(event);
            Location oldLocation = view.viewToModel(initialPoint, blockMode);
            Location newLocation = view.viewToModel(event.getPoint(), blockMode);
            Location start = Location.min(oldLocation, newLocation);
            Location end = Location.max(oldLocation, newLocation);
            dragHandler.mouseDragged(start, end);
            view.scrollRectToVisible(new Rectangle(event.getX(), event.getY(), 10, 10));
        }
    }
    
    private void updateSelectionMode(MouseEvent event) {

        if(GuiUtilities.isMacOs()) 
            blockMode = event.isAltDown();
        else 
            blockMode = event.isControlDown();
    }
    
    private DragHandler getDragHandlerForClick(MouseEvent e) {
        if (e.getClickCount() == 1) {
            return new SingleClickDragHandler();
        } else if (e.getClickCount() == 2) {
            return new DoubleClickDragHandler();
        } else {
            return new TripleClickDragHandler();
        }
    }
    
    public void mouseMoved(MouseEvent event) { }
    
    public void mouseEntered(MouseEvent event) { }
    
    public void mouseExited(MouseEvent event) { }
    
    public interface DragHandler {
        public void mouseDragged(Location start, Location end);
    }
    
    public class SingleClickDragHandler implements DragHandler {
        public void mouseDragged(Location start, Location end) {
            setHighlight(start, end);
        }
    }
    
    public class DoubleClickDragHandler implements DragHandler {
        public void mouseDragged(Location start, Location end) {
            setHighlight(getWordStart(start), getWordEnd(end));
        }
        
        private Location getWordStart(Location location) {
            final int lineNumber = location.getLineIndex();
            String line = view.getModel().getTextLine(lineNumber).getString();
            if (location.getCharOffset() >= line.length()) {
                return location;
            }
            
            int start = location.getCharOffset();
            while (start > 0 && isWordChar(line.charAt(start - 1))) {
                --start;
            }
            return new Location(lineNumber, start);
        }
        
        private Location getWordEnd(Location location) {
            final int lineNumber = location.getLineIndex();
            String line = view.getModel().getTextLine(lineNumber).getString();
            if (location.getCharOffset() >= line.length()) {
                return location;
            }
            
            int end = location.getCharOffset();
            while (end < line.length() && isWordChar(line.charAt(end))) {
                ++end;
            }
            return new Location(lineNumber, end);
        }
        
        private boolean isWordChar(char ch) {
            // Space marks the end of a word by any reasonable definition.
            // Bracket characters usually mark the end of what you're interested in.
            // Likewise quote characters.
            // Colon would break selection of URLs (Matt Hillsdon tried it, didn't like it).
            // It's also useful to copy grep-style file:line "addresses" for eg Evergreen.
            // mydarus wanted double-click to select "Slot" from:
            // Equipment=1,Subrack=1,Slot=3,PlugInUnit=1,ExchangeTerminal=1,E1PhysPathTerm=pp1
            return " <>(){}[]`'\",=".indexOf(ch) == -1;
        }
    }
    
    public class TripleClickDragHandler implements DragHandler {
        public void mouseDragged(Location start, Location end) {
            selectLines(start, end);
        }
        
        private void selectLines(Location start, Location end) {
            start = new Location(start.getLineIndex(), 0);
            end = new Location(end.getLineIndex() + 1, 0);
            setHighlight(start, end);
        }
    }
    
    private void clearSelection() {
        initialPoint = null;
        highlightStart = null;
        highlightEnd = null;

        view.repaint();
    }
    
    public void selectAll() {
        Location start = new Location(0, 0);
        Location end = new Location(view.getModel().getLineCount(), 0);
        setHighlight(start, end);
        selectionChanged();
    }
    
    /**
     * Copies the selected text to the clipboard.
     */
    public void copyToSystemClipboard() {
        copyToClipboard(view.getToolkit().getSystemClipboard());
    }
    
    /**
     * Copies the selected text to X11's selection.
     * Does nothing on other platforms.
     */
    public void updateSystemSelection() {
        if (!hasSelection()) {
            // Almost all X11 applications leave the selection alone in this case.
            return;
        }
        Clipboard systemSelection = view.getToolkit().getSystemSelection();
        if (systemSelection != null) {
            systemSelection.setContents(new LazyStringSelection() {
                @Override public String reallyGetText() {
                    return getTabbedString();
                }
            }, this);
        }
    }
    
    /**
     * Copies the selected text to X11's selection (like XTerm and friends) or Windows's clipboard (like PuTTY).
     */
    private void selectionChanged() {
        if (e.util.GuiUtilities.isWindows()) {
            copyToSystemClipboard();
        } else {
            updateSystemSelection();
        }
    }
    
    private void copyToClipboard(Clipboard clipboard) {
        if (!hasSelection()) {
            return;
        }
        String newContents = getTabbedString();
        if (newContents.length() == 0) {
            // Copying the empty string to the clipboard is bizarre, and caused one user trouble (because we didn't cope with zero-length pastes).
            return;
        }
        StringSelection selection = new StringSelection(newContents);
        clipboard.setContents(selection, this);
    }
    
    public String getTabbedString() {
        return hasSelection() ? view.getTabbedString(highlightStart, highlightEnd, blockMode) : "";
    }
    
    /**
     * Invoked to notify us that we no longer own the clipboard; we use
     * this to clear the selection, so we're not misrepresenting the
     * situation.
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        clearSelection();
    }
    
    private void setHighlight(Location start, Location end) {
        TextLine startLine = view.getModel().getTextLine(start.getLineIndex());
        start = new Location(start.getLineIndex(), startLine.getEffectiveCharStartOffset(start.getCharOffset()));
        if (end.getLineIndex() < view.getModel().getLineCount()) {
            TextLine endLine = view.getModel().getTextLine(end.getLineIndex());
            end = new Location(end.getLineIndex(), endLine.getEffectiveCharEndOffset(end.getCharOffset()));
        }
        if (start.equals(end)) {
            highlightStart = null;
            highlightEnd = null;
        } else {
            highlightStart = start;
            highlightEnd = end;
        }
        view.repaint();
    }

    public boolean isBlockMode() {
        return blockMode;
    }
    
    private boolean isValidLocation(TerminalView view, Location location) {
        TerminalModel model = view.getModel();
        if (location.getLineIndex() >= model.getLineCount()) {
            return false;
        }
        TextLine line = model.getTextLine(location.getLineIndex());
        // It is common for a selection to start after the last visible character on a line.
        if (location.getCharOffset() > line.length()) {
            return false;
        }
        return true;
    }

}
