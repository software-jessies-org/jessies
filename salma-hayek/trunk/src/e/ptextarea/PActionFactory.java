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
    
    public abstract static class PTextAction extends AbstractAction {
        public PTextAction(String name, KeyStroke keyStroke) {
            super(name);
            putValue(ACCELERATOR_KEY, keyStroke);
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
    
    private PActionFactory() {
    }
}
