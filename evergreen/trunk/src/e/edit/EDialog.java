package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class EDialog extends JDialog {
    private ActionListener listener;
    
    private JComponent ui;
    
    public EDialog(Frame frame) {
        super(frame);
    }
    
    public EDialog(Component parent) {
        super((Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent));
    }
    
    private boolean initialized = false;
    public void initialize() {
        if (initialized) {
            return;
        }
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(ui = createUI(), BorderLayout.CENTER);
        addFocusListener(new DialogFocusRedirector(ui));
        pack();
        initialized = true;
    }
    
    /**
     * Called when the dialog is about to be shown. Override this to set
     * the dialog's initial state if you're reusing a dialog.
     */
    public void aboutToBeShown() {
        /* Does nothing. */
    }
    
    /** Called when the dialog is about to be hidden. */
    public void aboutToBeHidden() {
        /* Does nothing. */
    }
    
    public abstract JComponent createUI();
    
    public void superSetVisible(boolean visible) {
        super.setVisible(visible);
    }
    
    public void setVisible(boolean visible) {
        if (visible == true) {
            initialize();
            aboutToBeShown();
            setSize(getPreferredSize());
            superSetVisible(true);
            validate();
            requestFocus();
        } else {
            aboutToBeHidden();
            superSetVisible(false);
        }
    }
    
    public void fireActionPerformed() {
        listener.actionPerformed(null);
    }
    
    public void addActionListener(ActionListener listener) {
        this.listener = listener;
    }
}
