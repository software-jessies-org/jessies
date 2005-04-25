package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PActionFactory {
    public static Action makeCopyAction() {
        return new CopyAction();
    }
    
    public static Action makeCutAction() {
        return new CutAction();
    }
    
    public static Action makePasteAction() {
        return new PasteAction();
    }
    
    public static Action makeRedoAction() {
        return new RedoAction();
    }
    
    public static Action makeSelectAllAction() {
        return new SelectAllAction();
    }
    
    public static Action makeUndoAction() {
        return new UndoAction();
    }
    
    public abstract static class PTextAction extends AbstractAction {
        /**
         * The parameter keyStroke can be null if you don't want to bind this
         * action to a key.
         */
        public PTextAction(String name, KeyStroke keyStroke) {
            super(name);
            if (keyStroke != null) {
                putValue(ACCELERATOR_KEY, keyStroke);
            }
        }
        
        public void actionPerformed(ActionEvent e) {
            PTextArea textArea = getTextArea();
            if (textArea != null) {
                performOn(textArea);
            }
        }
        
        public abstract void performOn(PTextArea textArea);
        
        public PTextArea getTextArea() {
            Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (component instanceof PTextArea) {
                return (PTextArea) component;
            }
            return null;
        }
    }
    
    public static class CopyAction extends PTextAction {
        public CopyAction() {
            super("Copy", e.util.GuiUtilities.makeKeyStroke("C", false));
        }
        
        public void performOn(PTextArea textArea) {
            textArea.copy();
        }
    }
    
    public static class CutAction extends PTextAction {
        public CutAction() {
            super("Cut", e.util.GuiUtilities.makeKeyStroke("X", false));
        }
        
        public void performOn(PTextArea textArea) {
            textArea.cut();
        }
    }
    
    public static class PasteAction extends PTextAction {
        public PasteAction() {
            super("Paste", e.util.GuiUtilities.makeKeyStroke("V", false));
        }
        
        public void performOn(PTextArea textArea) {
            textArea.paste();
        }
    }
    
    public static class RedoAction extends PTextAction {
        public RedoAction() {
            super("Redo", e.util.GuiUtilities.makeKeyStroke("Z", true));
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
        }
        
        public void performOn(PTextArea textArea) {
            textArea.selectAll();
        }
    }
    
    public static class UndoAction extends PTextAction {
        public UndoAction() {
            super("Undo", e.util.GuiUtilities.makeKeyStroke("Z", false));
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
