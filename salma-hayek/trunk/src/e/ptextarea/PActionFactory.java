package e.ptextarea;

import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

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
            super("Copy", e.util.GuiUtilities.makeKeyStroke("C", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.copy();
        }
    }
    
    public static class CutAction extends PTextAction {
        public CutAction() {
            super("Cut", e.util.GuiUtilities.makeKeyStroke("X", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.cut();
        }
    }
    
    public static class FindNextAction extends PTextAction {
        public FindNextAction() {
            super("Find Next", e.util.GuiUtilities.makeKeyStroke("G", false));
        }
        
        public void performOn(PTextArea textArea) {
            textArea.findNext();
        }
    }
    
    public static class FindPreviousAction extends PTextAction {
        public FindPreviousAction() {
            super("Find Previous", e.util.GuiUtilities.makeKeyStroke("D", false));
        }
        
        public void performOn(PTextArea textArea) {
            textArea.findPrevious();
        }
    }
    
    public static class PasteAction extends PTextAction {
        public PasteAction() {
            super("Paste", e.util.GuiUtilities.makeKeyStroke("V", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.paste();
        }
    }
    
    public static class RedoAction extends PTextAction {
        public RedoAction() {
            super("Redo", e.util.GuiUtilities.makeKeyStroke("Z", true));
            GnomeStockIcon.configureAction(this);
        }
        
        public boolean isEnabled() {
            PTextArea textArea = getTextArea();
            return (textArea != null && textArea.getTextBuffer().getUndoBuffer().canRedo());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.getTextBuffer().getUndoBuffer().redo();
        }
    }
    
    public static class SelectAllAction extends PTextAction {
        public SelectAllAction() {
            super("Select All", e.util.GuiUtilities.makeKeyStroke("A", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public void performOn(PTextArea textArea) {
            textArea.selectAll();
        }
    }
    
    public static class UndoAction extends PTextAction {
        public UndoAction() {
            super("Undo", e.util.GuiUtilities.makeKeyStroke("Z", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public boolean isEnabled() {
            PTextArea textArea = getTextArea();
            return (textArea != null && textArea.getTextBuffer().getUndoBuffer().canUndo());
        }
        
        public void performOn(PTextArea textArea) {
            textArea.getTextBuffer().getUndoBuffer().undo();
        }
    }
    
    private PActionFactory() {
    }
}
