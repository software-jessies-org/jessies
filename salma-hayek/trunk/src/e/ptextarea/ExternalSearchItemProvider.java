package e.ptextarea;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

class ExternalSearchItemProvider implements MenuItemProvider {
    private PTextArea textArea;
    
    public ExternalSearchItemProvider(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
        actions.add(new SearchInGoogleAction());
        actions.add(new LookUpInDictionaryAction());
    }
    
    private class SearchInGoogleAction extends AbstractAction {
        public SearchInGoogleAction() {
            super("Search in Google");
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME
        }
    }
    
    private class LookUpInDictionaryAction extends AbstractAction {
        public LookUpInDictionaryAction() {
            super("Look Up in Dictionary");
            setEnabled(GuiUtilities.isMacOs());
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME
        }
    }
}
