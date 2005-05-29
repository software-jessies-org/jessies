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
            try {
                // We need to rewrite spaces as "%20" for them to find their
                // way to Dictionary.app unmolested. The usual url-encoded
                // form ("+") doesn't work, for some reason.
                String encodedSelection = textArea.getSelectedText().trim().replaceAll("\\s+", "%20");
                // In Mac OS 10.4.1, a dict: URI that causes Dictionary.app to
                // start doesn't actually cause the definition to be shown, so
                // we need to ask twice. If we knew the dictionary was already
                // open, we could avoid the flicker. But we may as well wait
                // for Apple to fix the underlying problem.
                BrowserLauncher.openURL("dict:///");
                BrowserLauncher.openURL("dict:///" + encodedSelection);
            } catch (Exception ex) {
                Log.warn("Exception launching browser", ex);
            }
        }
    }
}
