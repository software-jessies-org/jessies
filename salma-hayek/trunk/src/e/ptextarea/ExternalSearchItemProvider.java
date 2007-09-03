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
        actions.add(new ExternalSearchAction("Search in Google", "http://www.google.com/search?q=%s&ie=UTF-8&oe=UTF-8"));
        actions.add(GuiUtilities.isMacOs() ? new MacDictionaryAction() : new ExternalSearchAction("Look Up in Dictionary", "http://www.answers.com/%s"));
        actions.add(new ExternalSearchAction("Look Up in Wikipedia", "http://en.wikipedia.org/wiki/Special:Search?search=%s"));
    }
    
    private class ExternalSearchAction extends AbstractAction {
        private String urlTemplate;
        
        public ExternalSearchAction(String name, String urlTemplate) {
            super(name);
            this.urlTemplate = urlTemplate;
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                String encodedSelection = StringUtilities.urlEncode(textArea.getSelectedText().trim());
                BrowserLauncher.openURL(new Formatter().format(urlTemplate, encodedSelection).toString());
            } catch (Exception ex) {
                Log.warn("Exception launching browser", ex);
            }
        }
        
        // It only makes sense to search for a non-empty selection.
        public boolean isEnabled() {
            return (textArea.getSelectionStart() != textArea.getSelectionEnd());
        }
    }
    
    private class MacDictionaryAction extends AbstractAction {
        private MacDictionaryAction() {
            super("Look Up in Dictionary");
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                // We need to rewrite spaces as "%20" for them to find their way to Dictionary.app unmolested.
                // The usual url-encoded form ("+") doesn't work, for some reason.
                String encodedSelection = textArea.getSelectedText().trim().replaceAll("\\s+", "%20");
                
                // In Mac OS 10.4.10, a dict: URI that causes Dictionary.app to start doesn't actually cause the definition to be shown, so we need to ask twice.
                // Dictionary.app doesn't optimize the case where it's asked to look up what it's already displaying.
                // Checking whether Dictionary.app's already running and starting it if it isn't doesn't look any better, so just do the easiest thing.
                BrowserLauncher.openURL("dict:///");
                BrowserLauncher.openURL("dict:///" + encodedSelection);
            } catch (Exception ex) {
                Log.warn("Exception launching Dictionary.app", ex);
            }
        }
    }
}
