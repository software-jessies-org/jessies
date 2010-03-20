package e.forms;

import e.util.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * A panel that lays out form elements.
 */
public class FormPanel extends JPanel {
    private int nextRow = 0;

    private ArrayList<JTextComponent> textComponents = new ArrayList<JTextComponent>();
    
    public FormPanel() {
        setLayout(new GridBagLayout());
    }
    
    /**
     * Adds an invisible empty row, about the height of a normal row's descriptive text.
     */
    public void addEmptyRow() {
        addRow(" ", new JLabel(""));
    }
    
    /**
     * Adds a row consisting of just a component.
     * This component will take up the entire row, including the space usually reserved for a label.
     */
    public void addWideRow(Component component) {
        addRow(null, component);
    }
    
    /**
     * Adds a row consisting of a label and a corresponding component.
     * If you want an empty label, use "". (This is the more common case.)
     * If you don't want even a space for a label, and want 'component' to take up the entire row, use null.
     * FIXME: it would probably be better to refactor so that the basic dialog stuff takes a JComponent content pane, and FormPanel is just one possible content pane.
     */
    public void addRow(String text, Component component) {
        // Text components are automatically listened to for changes; see setTypingTimeoutAction.
        addTextComponentChildren(component);
        
        int gridx = 0;
        
        // Add the label, unless the caller doesn't want one.
        if (text != null) {
            JLabel label = new JLabel(text);
            label.setLabelFor(component);
            
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = gridx++;
            labelConstraints.gridy = nextRow;
            labelConstraints.insets = new Insets(GuiUtilities.getComponentSpacing(), GuiUtilities.getComponentSpacing(), 0, 0);
            labelConstraints.anchor = (component instanceof JScrollPane) ? GridBagConstraints.NORTHEAST : GridBagConstraints.EAST;
            labelConstraints.fill = GridBagConstraints.NONE;
            add(label, labelConstraints);
        }

        // Add the component with its constraints
        GridBagConstraints componentConstraints = new GridBagConstraints();
        componentConstraints.gridx = gridx++;
        componentConstraints.gridy = nextRow;
        componentConstraints.gridwidth = GridBagConstraints.REMAINDER;
        componentConstraints.insets = new Insets(GuiUtilities.getComponentSpacing(), GuiUtilities.getComponentSpacing(), 0, GuiUtilities.getComponentSpacing());
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
