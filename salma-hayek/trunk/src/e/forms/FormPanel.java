package e.forms;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * A panel that lays out form elements.
 */
public class FormPanel extends JPanel {
    private int nextRow = 0;

    private int componentSpacing;

    private ArrayList<JTextComponent> textComponents = new ArrayList<JTextComponent>();
    
    FormPanel() {
        setLayout(new GridBagLayout());
        componentSpacing = FormDialog.getComponentSpacing();
    }
    
    /**
     * Adds an invisible empty row, about the height of a normal row's descriptive text.
     */
    public void addEmptyRow() {
        addRow(" ", new JLabel(""));
    }
    
    /**
     * Adds a row consisting of a label and a corresponding component.
     */
    public void addRow(String text, Component component) {
        // Text components are automatically listened to for changes; see setTypingTimeoutAction.
        addTextComponentChildren(component);
        
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
        if (component instanceof JCheckBox) {
            // FIXME: we should have a better mechanism for component-specific spacing.
            componentConstraints.insets.top = 4;
        }
        if (component instanceof JScrollPane) {
            /*
             * A scrollable component in a form can likely make use
             * of additional vertical space.
             */
            componentConstraints.weighty = 1.0;
        }
        componentConstraints.anchor = GridBagConstraints.LINE_START;
        if (component instanceof JButton == false) {
            // Buttons shouldn't be stretched, but everything else may as well use all available space?
            componentConstraints.fill = GridBagConstraints.BOTH;
        }
        add(component, componentConstraints);

        nextRow++;
    }
    
    /**
     * Recursively add the JTextComponent children of the given component to our textComponents collection.
     */
    private void addTextComponentChildren(Component component) {
        addTextComponent(component);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                addTextComponent(child);
                addTextComponentChildren(child);
            }
        }
    }
    
    private void addTextComponent(Component component) {
        if (component instanceof JTextComponent) {
            textComponents.add((JTextComponent) component);
        }
    }
    
    ArrayList<JTextComponent> getTextComponents() {
        return new ArrayList<JTextComponent>(textComponents);
    }
}
