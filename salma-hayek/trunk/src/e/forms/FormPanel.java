package e.forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A panel that lays out form elements.
 */
public class FormPanel extends JPanel {
    private int nextRow = 0;

    private int componentSpacing;

    private JComponent statusBar;
    
    private ActionListener typingTimeoutActionListener;
    
    public FormPanel() {
        setLayout(new GridBagLayout());
        componentSpacing = FormDialog.getComponentSpacing();
    }

    /**
     * Adds a row consisting of a label and a corresponding component.
     */
    public void addRow(String text, Component component) {
        JLabel label = new JLabel(text);
        label.setLabelFor(component);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = nextRow;
        labelConstraints.insets = new Insets(componentSpacing, componentSpacing, 0, 0);
        labelConstraints.anchor = (component instanceof JScrollPane) ? GridBagConstraints.NORTHEAST : GridBagConstraints.EAST;
        labelConstraints.fill = GridBagConstraints.NONE;
        add(label, labelConstraints);

        // Add the component with its constraints
        GridBagConstraints componentConstraints = new GridBagConstraints();
        componentConstraints.gridx = 1;
        componentConstraints.gridy = nextRow;
        componentConstraints.gridwidth = GridBagConstraints.REMAINDER;
        componentConstraints.insets = new Insets(componentSpacing, componentSpacing, 0, componentSpacing);
        componentConstraints.weightx = 1.0;
        if (component instanceof JScrollPane) {
            /*
             * A scrollable component in a form can likely make use
             * of additional vertical space.
             */
            componentConstraints.weighty = 1.0;
        }
        componentConstraints.anchor = GridBagConstraints.CENTER;
        componentConstraints.fill = GridBagConstraints.BOTH;
        add(component, componentConstraints);

        nextRow++;
    }

    /**
     * Sets the status bar that any dialog using this panel
     * should include. Using this in preference to adding
     * a row will give us a chance to lay out the status bar
     * in a more appropriate manner.
     */
    public void setStatusBar(JComponent component) {
        this.statusBar = component;
    }

    JComponent getStatusBar() {
        return this.statusBar;
    }
    
    public void setTypingTimeoutActionListener(ActionListener listener) {
        this.typingTimeoutActionListener = listener;
    }
    
    ActionListener getTypingTimeoutActionListener() {
        return this.typingTimeoutActionListener;
    }
}
