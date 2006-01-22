
package e.gui;

import e.util.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * Opens a web browser at the given URL. Also takes a name for use in menus
 * or on buttons.
 */
public class WebLinkAction extends AbstractAction {
    private String url;
    
    public WebLinkAction(String name, String url) {
        super(name);
        this.url = url;
    }
    
    public WebLinkAction(String name, File file) {
        this(name, urlFromFile(file));
    }
    
    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher.openURL(url);
        } catch (IOException ex) {
            SimpleDialog.showDetails(null, (String) getValue(NAME), ex);
        }
    }
    
    private static String urlFromFile(File file) {
        return file.toURI().toString();
    }
}
