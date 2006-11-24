package e.gui;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A clickable link, similar to those seen in web browsers.
 */
public class JHyperlinkButton extends JPanel implements ActionListener {
    private JButton button;
    private String url;
    
    public JHyperlinkButton(String text, String targetUrl) {
        // We're a panel with a FlowLayout rather than being a JButton directly.
        // This lets us be added to arbitrary layouts without expanding our active area past the width of our text.
        super(new FlowLayout());
        this.url = targetUrl;
        this.button = new JButton("<html><body><u>" + text + "</u>");
        configureButton();
        add(button);
    }
    
    private void configureButton() {
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setForeground(Color.BLUE);
        button.setRequestFocusEnabled(false);
        button.setToolTipText(url);
        button.addActionListener(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher.openURL(url);
        } catch (Throwable th) {
            SimpleDialog.showDetails(this, "Problem opening URL", th);
        }
    }
}
