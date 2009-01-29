package e.ptextarea;

import e.gui.*;
import java.awt.*;
import java.awt.event.*;

public class PActionFactory {
    public static PTextAction makeCopyAction() {
        return new CopyAction();
    }
    
    public static PTextAction makeCutAction() {
        return new CutAction();
    }
    
    public static PTextAction makeFindAction() {
        return new PFind.FindAction();
    }
    
    public static PTextAction makeFindMatchingBracketAction() {
        return new FindMatchingBracketAction();
    }
    
    public static PTextAction makeFindNextAction() {
        return new FindNextAction();
    }
    
    public static PTextAction makeFindPreviousAction() {
        return new FindPreviousAction();
    }
    
    public static PTextAction makePasteAction() {
        return new PasteAction();
    }
    
    public static PTextAction makeRedoAction() {
        return new RedoAction();
    }
    
    public static PTextAction makeSelectAllAction() {
        return new SelectAllAction();
    }
    
    public static PTextAction makeUndoAction() {
        return new UndoAction();
    }
    
    public static class CopyAction extends PTextAction {
        public CopyAction() {
            super("_Copy", "C", false);
            GnomeStockIcon.configureAction(this);
        }
        
        @Override public boolean isEnabled() {
            final PTextArea textArea = getTextArea();
            return (textArea != null && textArea.hasSelection());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.copy();
        }
    }
    
    public static class CutAction extends PTextAction {
        public CutAction() {
            super("Cu_t", "X", false);
            GnomeStockIcon.configureAction(this);
        }
        
        @Override public boolean isEnabled() {
            final PTextArea textArea = getTextArea();
            return (textArea != null && textArea.isEditable() && textArea.hasSelection());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.cut();
        }
    }
    
    public static class FindMatchingBracketAction extends PTextAction {
        public FindMatchingBracketAction() {
            super("Find _Matching Bracket", "5", false);
        }
        
        
        public void performOn(PTextArea textArea) {
            // Find Matching Bracket inhabits a gray area between actions and caret movements.
            // It's high level enough to be thought of as the former, but most convenient as the latter (both for the user and the implementer).
            // Unfortunately, it means we end up here, faking a control-5 key event to send to the text area.
            KeyEvent control5 = new KeyEvent(textArea, KeyEvent.KEY_PRESSED, EventQueue.getMostRecentEventTime(), InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_5, KeyEvent.CHAR_UNDEFINED);
            textArea.dispatchEvent(control5);
        }
    }
    
    public static class FindNextAction extends PTextAction {
        public FindNextAction() {
            super("Find _Next", "G", false);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.findNext();
        }
    }
    
    public static class FindPreviousAction extends PTextAction {
        public FindPreviousAction() {
            super("Find _Previous", "D", false);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.findPrevious();
        }
    }
    
    public static class PasteAction extends PTextAction {
        public PasteAction() {
            super("_Paste", "V", false);
            GnomeStockIcon.configureAction(this);
        }
        
        @Override public boolean isEnabled() {
            final PTextArea textArea = getTextArea();
            return (textArea != null && textArea.isEditable());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.paste();
        }
    }
    
    public static class RedoAction extends PTextAction {
        public RedoAction() {
            super("_Redo", "Z", true);
            GnomeStockIcon.configureAction(this);
        }
        
        @Override public boolean isEnabled() {
            final PTextArea textArea = getTextArea();
            return (textArea != null && textArea.getTextBuffer().getUndoBuffer().canRedo());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.getTextBuffer().getUndoBuffer().redo();
        }
    }
    
    public static class SelectAllAction extends PTextAction {
        public SelectAllAction() {
            super("Select _All", "A", false);
            GnomeStockIcon.configureAction(this);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.selectAll();
        }
    }
    
    public static class UndoAction extends PTextAction {
        public UndoAction() {
            super("_Undo", "Z", false);
            GnomeStockIcon.configureAction(this);
        }
        
        @Override public boolean isEnabled() {
            final PTextArea textArea = getTextArea();
            return (textArea != null && textArea.getTextBuffer().getUndoBuffer().canUndo());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.getTextBuffer().getUndoBuffer().undo();
        }
    }
    
    private PActionFactory() {
    }
}
