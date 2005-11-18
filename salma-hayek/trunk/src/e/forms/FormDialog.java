package e.forms;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;

import e.gui.*;
import e.util.*;

/**
 * Implements simple dialogs. You provide the content (using FormPanel),
 * and we'll provide the buttons and basic functionality. A non-modal dialog
 * gets just a Close button; a modal dialog gets OK and Cancel, and you can
 * also provide an extra button with any label.
 * 
 * Obviously you wouldn't dream of having an "OK" button, would you? Because
 * of this, you actually have to supply the label for the positive action
 * button; something like "Add", "Print", "Save" or whatever.
 * 
 * These dialogs automatically remember the location and size they had last time they were
 * shown, though this isn't automatically stored anywhere, so restart your program, and you'll
 * be back to defaults. If your program invokes writeGeometriesTo and readGeometriesFrom
 * on quit and initialization respectively, passing the same filename to each, your geometries
 * will be preserved.
 */
public class FormDialog {
    private static int componentSpacing = getComponentSpacing();
    
    private static HashMap<String, Rectangle> dialogGeometries = new HashMap<String, Rectangle>();
    
    private ArrayList<JTextComponent> listenedToTextFields;
    
    private DocumentListener documentListener;
    
    private Timer textChangeTimer;
    
    private boolean isListenerUpToDate;
    
    private boolean wasAccepted;
    
    private DialogFocusRedirector dialogFocusRedirector;
    
    private Runnable acceptRunnable = new NoOpRunnable();
    private Runnable cancelRunnable = new NoOpRunnable();
    
    private FormPanel formPanel;
    
    private String actionLabel;
    
    private JButton extraButton;
    
    private JDialog dialog;
    
    private boolean doNotRememberBounds = false;
    
    public static int getComponentSpacing() {
        if (GuiUtilities.isWindows()) {
            return 4;
        }
        if (GuiUtilities.isMacOs()) {
            return 10;
        }
        return 6;
    }
    
    /**
     * Shows a non-modal dialog with a Close button.
     */
    public void showNonModal() {
        this.actionLabel = "Close";
        configureDialog();
        dialog.setModal(false);
        dialog.setVisible(true);
    }
    
    /**
     * Shows a dialog with a Cancel button, and a button with the given label.
     * See the note in the class documentation about why you shouldn't label
     * a button "OK".
     */
    public boolean show(String actionLabel) {
        this.actionLabel = actionLabel;
        configureDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
        return wasAccepted;
    }

    FormDialog(Frame owner, String title, FormPanel formPanel) {
        this.dialog = new JDialog(owner, title);
        this.formPanel = formPanel;
        initAlwaysOnTopMonitoring();
    }
    
    // A dialog that doesn't track its owner's always-on-top state risks being
    // lost beneath its owner. The documentation for Window.setAlwaysOnTop
    // states that all owned windows will inherit the always-on-top state, but
    // this doesn't appear to apply to owned dialogs. I'm not sure if this is
    // a documentation bug or an implementation bug.
    private void initAlwaysOnTopMonitoring() {
        copyOwnerAlwaysOnTopState();
        dialog.getOwner().addPropertyChangeListener("alwaysOnTop", new PropertyChangeListener() {
            // One of the Java tutorial's example PropertyChangeListeners says
            // "propertyChanged", which is what you'd expect, but the
            // method appears to be "propertyChange" (as in the other example).
            public void propertyChange(PropertyChangeEvent e) {
                copyOwnerAlwaysOnTopState();
            }
        });
    }
    
    private void copyOwnerAlwaysOnTopState() {
        dialog.setAlwaysOnTop(dialog.getOwner().isAlwaysOnTop());
    }
    
    private void configureDialog() {
        dialog.setResizable(true);
        
        JComponent statusBar = formPanel.getStatusBar();
        if (extraButton != null && statusBar != null) {
            // It looks bad if we have a status bar and an
            // extra button together in the row of buttons.
            formPanel.addRow("", statusBar);
            statusBar = null;
        }
        
        JPanel internalContentPane = new JPanel(new BorderLayout(componentSpacing, componentSpacing));
        internalContentPane.setBorder(BorderFactory.createEmptyBorder(componentSpacing, componentSpacing, componentSpacing, componentSpacing));
        internalContentPane.add(formPanel, BorderLayout.CENTER);
        internalContentPane.add(makeButtonPanel(dialog.getRootPane(), actionLabel, statusBar), BorderLayout.SOUTH);
        
        dialog.setContentPane(internalContentPane);
        
        // Set sensible defaults for size and location.
        dialog.pack();
        dialog.setLocationRelativeTo(dialog.getOwner());
        
        // But if we've shown this dialog before, put it back where it last was.
        restorePreviousSize();
        
        initCloseBehavior();
        initFocus(formPanel);
        
        // Support for "as-you-type" interfaces that update whenever the user
        // pauses in their typing.
        ActionListener listener = formPanel.getTypingTimeoutActionListener();
        initComponentListener(listener);
        initTextChangeTimer(listener);
        initDocumentListener(formPanel);
    }
    
    /**
     * Prods the typing timeout listener as soon as we're shown so that we're
     * showing up-to-date information before the user starts typing. Also prods
     * it if we're accepted without having notified the listener of the latest
     * typing.
     */
    private void initComponentListener(final ActionListener listener) {
        dialog.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                listener.actionPerformed(null);
            }
            
            public void componentHidden(ComponentEvent e) {
                if (wasAccepted && isListenerUpToDate == false) {
                    listener.actionPerformed(null);
                }
            }
        });
    }
    
    /**
     * Restarts our timer whenever the user types.
     */
    private void initDocumentListener(FormPanel formPanel) {
        // Create a listener...
        documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                textChanged();
            }
            
            public void insertUpdate(DocumentEvent e) {
                textChanged();
            }
            
            public void removeUpdate(DocumentEvent e) {
                textChanged();
            }
            
            private void textChanged() {
                textChangeTimer.restart();
                isListenerUpToDate = false;
            }
        };
        // ...and attach it to the text fields.
        this.listenedToTextFields = formPanel.getTextComponents();
        addTextFieldListeners();
    }
    
    /**
     * Notifies the FormPanel's typing timeout listener if it's been more
     * than 0.5s since the user typed anything, on the assumption that it's
     * time to update the UI in response.
     */
    private void initTextChangeTimer(final ActionListener listener) {
        textChangeTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (dialog.isShowing()) {
                    listener.actionPerformed(null);
                    isListenerUpToDate = true;
                }
            }
        });
        textChangeTimer.setRepeats(false);
        // It doesn't matter what we initialize this to; our ComponentListener
        // will do the right thing either way. But "true" seems more natural.
        isListenerUpToDate = true;
    }
    
    /**
     * Adds listeners to all the text fields.
     */
    private void addTextFieldListeners() {
        for (JTextComponent field : listenedToTextFields) {
            field.getDocument().addDocumentListener(documentListener);
        }
    }
    
    private void removeTextFieldListeners() {
        for (int i = listenedToTextFields.size() - 1; i >= 0; --i) {
            JTextComponent field = listenedToTextFields.remove(i);
            field.getDocument().removeDocumentListener(documentListener);
        }
    }
    
    private void initCloseBehavior() {
        initWindowManagerCloseBehavior();
        initKeyboardCloseBehavior();
    }

    private void initFocus(Container formPanel) {
        dialogFocusRedirector = new DialogFocusRedirector(formPanel);
        dialogFocusRedirector.redirectFocus();
    }
    
    private void initWindowManagerCloseBehavior() {
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            // windowClosing only applies to closes via the window manager.
            public void windowClosing(WindowEvent windowEvent) {
                cancelDialog();
            }
        });
    }
    
    private Action closeAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            cancelDialog();
        }
    };
    
    private void initKeyboardCloseBehavior() {
        closeOnKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false));
        if (GuiUtilities.isMacOs()) {
            closeOnKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_MASK, false));
        }
    }
    
    private void closeOnKeyStroke(KeyStroke keyStroke) {
        final String CLOSE_ACTION = "e.forms.FormDialog.CloseDialogIfEscapePressed";
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, CLOSE_ACTION);
        dialog.getRootPane().getActionMap().put(CLOSE_ACTION, closeAction);
    }
    
    /**
     * Restores the size and location stored in the dialog geometries
     * configuration. This is invoked just after we've invoked pack, so we
     * refuse to allow the configuration to make us any smaller. (When JButton
     * grew vertically between 1.4.2 and 1.5.0, I spent some time wondering why
     * the LayoutManager was allowing my JButton to be clipped. It wasn't; this
     * code was forcing the clipping to occur. Hence the restriction now.)
     */
    private void restorePreviousSize() {
        if (doNotRememberBounds) {
            return;
        }
        Rectangle previousBounds = dialogGeometries.get(dialog.getTitle());
        if (previousBounds != null) {
            Point newLocation = previousBounds.getLocation();
            Dimension newSize = previousBounds.getSize();
            
            // Don't shrink the dialog below its "pack" size.
            newSize.height = Math.max(newSize.height, dialog.getHeight());
            newSize.width = Math.max(newSize.width, dialog.getWidth());
            
            // Constrain the dialog to fit on the display.
            Rectangle displayBounds = dialog.getGraphicsConfiguration().getBounds();
            if (newLocation.x + newSize.width > displayBounds.width) {
                newSize.width = displayBounds.width - newLocation.x;
            }
            if (newLocation.y + newSize.height > displayBounds.height) {
                newSize.height = displayBounds.height - newLocation.y;
            }
            
            dialog.setLocation(newLocation);
            dialog.setSize(newSize);
        }
    }
    
    private void acceptDialog() {
        acceptRunnable.run();
        processUserChoice(true);
    }

    private void cancelDialog() {
        cancelRunnable.run();
        processUserChoice(false);
    }

    private void processUserChoice(boolean isAcceptance) {
        textChangeTimer.stop();
        if (doNotRememberBounds == false) {
            dialogGeometries.put(dialog.getTitle(), dialog.getBounds());
        }
        wasAccepted = isAcceptance;
        removeTextFieldListeners();
        dialog.dispose();
        dialogFocusRedirector.restoreFocus();
    }
    
    private JPanel makeButtonPanel(JRootPane rootPane, String actionLabel, JComponent statusBar) {
        JButton actionButton = new JButton(actionLabel);
        actionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                acceptDialog();
            }
        });
        rootPane.setDefaultButton(actionButton);
        
        JButton cancelButton = null;
        if (dialog.isModal()) {
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(closeAction);
            tieButtonSizes(actionButton, cancelButton);
        }
        return makeButtonPanel(actionButton, cancelButton, statusBar);
    }
    
    /**
     * Ensures that both buttons are the same size, and that the chosen size
     * is sufficient to contain the content of either.
     */
    private void tieButtonSizes(JButton actionButton, JButton cancelButton) {
        Dimension cancelSize = cancelButton.getPreferredSize();
        Dimension actionSize = actionButton.getPreferredSize();
        final int width = (int) Math.max(actionSize.getWidth(), cancelSize.getWidth());
        final int height = (int) Math.max(actionSize.getHeight(), cancelSize.getHeight());
        Dimension buttonSize = new Dimension(width, height);
        actionButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
    }
    
    private JPanel makeButtonPanel(JButton actionButton, JButton cancelButton, JComponent statusBar) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, componentSpacing, componentSpacing, componentSpacing));
        //panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        if (GuiUtilities.isWindows()) {
            // Use the traditional Windows layout.
            if (statusBar != null) {
                panel.add(statusBar);
            }
            panel.add(Box.createGlue());
            panel.add(actionButton);
            if (cancelButton != null) {
                panel.add(Box.createHorizontalStrut(componentSpacing));
                panel.add(cancelButton);
            }
            if (extraButton != null) {
                panel.add(Box.createHorizontalStrut(componentSpacing));
                panel.add(extraButton);
            }
        } else {
            // Use the Mac OS layout.
            if (extraButton != null) {
                panel.add(extraButton);
            }
            if (statusBar != null) {
                panel.add(statusBar);
            }
            panel.add(Box.createGlue());
            if (cancelButton != null) {
                panel.add(cancelButton);
                panel.add(Box.createHorizontalStrut(componentSpacing));
            }
            panel.add(actionButton);
        }
        
        return panel;
    }
    
    /**
     * Writes our dialog geometries to disk so we can preserve them across runs.
     * The format isn't very human-readable, because I couldn't get MessageFormat to work.
     */
    public static void writeGeometriesTo(String filename) {
        StringBuilder content = new StringBuilder();
        for (String name : dialogGeometries.keySet()) {
            Rectangle bounds = dialogGeometries.get(name);
            content.append(name + "\n");
            content.append(bounds.x + "\n");
            content.append(bounds.y + "\n");
            content.append(bounds.width + "\n");
            content.append(bounds.height + "\n");
        }
        StringUtilities.writeFile(new java.io.File(filename), content.toString());
    }
    
    /**
     * Reads stored dialog geometries back in from disk, so we can remember them across runs.
     */
    public static void readGeometriesFrom(String filename) {
        if (new java.io.File(filename).exists() == false) {
            return;
        }
        
        String[] lines = StringUtilities.readLinesFromFile(filename);
        try {
            for (int i = 0; i < lines.length;) {
                String name = lines[i++];
                int x = Integer.parseInt(lines[i++]);
                int y = Integer.parseInt(lines[i++]);
                int width = Integer.parseInt(lines[i++]);
                int height = Integer.parseInt(lines[i++]);
                Rectangle bounds = new Rectangle(x, y, width, height);
                dialogGeometries.put(name, bounds);
            }
        } catch (Exception ex) {
            Log.warn("Failed to read geometries from '" + filename + "'", ex);
        }
    }
    
    /**
     * Sets an optional runnable that will be run if this dialog is accepted,
     * just before the dialog is removed from the display.
     */
    public void setAcceptRunnable(Runnable runnable) {
        this.acceptRunnable = runnable;
    }
    
    /**
     * Sets an optional runnable that will be run if this dialog is canceled,
     * just before the dialog is removed from the display.
     */
    public void setCancelRunnable(Runnable runnable) {
        this.cancelRunnable = runnable;
    }
    
    public void setExtraButton(JButton button) {
        this.extraButton = button;
    }
    
    public void setRememberBounds(boolean rememberBounds) {
        this.doNotRememberBounds = !rememberBounds;
    }
}
