package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SearchField extends JTextField {
    public SearchField(String placeholderText) {
        super(15);
        addFocusListener(new PlaceholderText(placeholderText));
    }

    public SearchField() {
        this("Search");
    }
    
    class PlaceholderText implements FocusListener {
        private String placeholderText;
        private String previousText = "";
        private Color previousColor;

        PlaceholderText(String placeholderText) {
            this.placeholderText = placeholderText;
            focusLost(null);
        }

        public void focusGained(FocusEvent e) {
            setForeground(previousColor);
            setText(previousText);
        }

        public void focusLost(FocusEvent e) {
            previousText = getText();
            previousColor = getForeground();
            setForeground(Color.GRAY);
            setText(placeholderText);
        }
    }
}
