package e.gui;

import e.util.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class WebLinkAction extends AbstractAction {
    private String url;
    
    public WebLinkAction(String name, String url) {
        super(name);
        this.url = url;
    }
    
    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher.openURL(url);
        } catch (IOException ex) {
            SimpleDialog.showDetails(null, (String) getValue(NAME), ex);
        }
    }
}
