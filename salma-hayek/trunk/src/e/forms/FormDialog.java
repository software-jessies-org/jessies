package e.forms;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;

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
    
    private ArrayList<JTextComponent> listenedToTextFields;
    
    private DocumentListener documentListener;
    
    private Timer textChangeTimer;
    
    private boolean isListenerUpToDate;
    
    private boolean wasAccepted;
    
    private DialogFocusRedirector dialogFocusRedirector;
    private boolean shouldRestoreFocus = true;
    
    // Our default accept callable just returns true, allowing the dialog to close.
    private Callable<Boolean> acceptCallable = new Callable<Boolean>() {
        public Boolean call() {
            return Boolean.TRUE;
        }
    };
    
    private Runnable cancelRunnable = new NoOpRunnable();
    
    private FormBuilder builder;
    
    private String actionLabel;
    
    private JButton extraButton;
    
    private JDialog dialog;
    private Frame owner;
    private boolean doNotSetLocationRelativeToOwner;
    
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
        showNonModal("Close");
    }
    
    /**
     * Shows a non-modal dialog with a default button label of your choosing.
     * You'll need to use setAcceptCallable to attach an action to the button.
     */
    public void showNonModal(String actionLabel) {
        // With a non-modal dialog, there's the possibility that the user just lost track of us.
        if (dialog.isShowing()) {
            dialog.toFront();
            return;
        }
        
        this.actionLabel = actionLabel;
        dialog.setModal(false);
        configureDialog();
        dialog.setVisible(true);
    }
    
    /**
     * Shows a dialog with a Cancel button, and a button with the given label.
     * See the note in the class documentation about why you shouldn't label
     * a button "OK".
     */
    public boolean show(String actionLabel) {
        this.actionLabel = actionLabel;
        dialog.setModal(true);
        configureDialog();
        dialog.setVisible(true);
        return wasAccepted;
    }

    FormDialog(FormBuilder builder, Frame ownerFrame, String title) {
        this.owner = initOwner(ownerFrame);
        
        this.builder = builder;
        this.dialog = new JDialog(owner, title);
        
        initAlwaysOnTopMonitoring();
    }
    
    private Frame initOwner(Frame f) {
        // The user can pass null to center a dialog and/or state that they don't have a specific parent...
        this.doNotSetLocationRelativeToOwner = (f == null);
        if (GuiUtilities.isMacOs() && f == null) {
            // ... but a null parent on Mac OS would cause the menu bar to disappear.
            Frame[] frames = Frame.getFrames();
            if (frames.length > 0) {
                f = frames[0];
            }
        }
        return f;
    }
    
    // A dialog that doesn't track its owner's always-on-top state risks being
    // lost beneath its owner. The documentation for Window.setAlwaysOnTop
    // states that all owned windows will inherit the always-on-top state, but
    // this doesn't appear to apply to owned dialogs. I'm not sure if this is
    // a documentation bug or an implementation bug.
    private void initAlwaysOnTopMonitoring() {
        if (owner == null) {
            return;
        }
        
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
        dialog.setAlwaysOnTop(owner.isAlwaysOnTop());
    }
    
    private void configureDialog() {
        dialog.setResizable(true);
        
        // If we've got multiple tabs to display, we need a JTabbedPane.
        // Otherwise we'll take the one and only FormPanel.
        JComponent centerpiece = null;
        List<FormPanel> formPanels = builder.getFormPanels();
        if (formPanels.size() > 1) {
            centerpiece = makeTabbedPaneForFormPanels(formPanels);
        } else {
            centerpiece = formPanels.get(0);
            centerpiece.setBorder(BorderFactory.createEmptyBorder(0, 0, componentSpacing, 0));
        }
        
        if (formPanels.size() == 1 && extraButton != null && builder.statusBar != null) {
            // It looks bad if we have a status bar and an extra button together in the row of buttons.
            formPanels.get(0).addRow("", builder.statusBar);
            builder.statusBar = null;
        }
        
        JPanel internalContentPane = new JPanel(new BorderLayout());
        internalContentPane.setBorder(BorderFactory.createEmptyBorder(componentSpacing, componentSpacing, componentSpacing, componentSpacing));
        internalContentPane.add(centerpiece, BorderLayout.CENTER);
        internalContentPane.add(makeButtonPanel(dialog.getRootPane(), actionLabel, builder.statusBar), BorderLayout.SOUTH);
        
        dialog.setContentPane(internalContentPane);
        
        // Set sensible defaults for size and location.
        dialog.pack();
        dialog.setLocationRelativeTo(doNotSetLocationRelativeToOwner ? null : owner);
        
        // But if we've shown this dialog before, put it back where it last was.
        restorePreviousSize();
        
        initCloseBehavior();
        initFocus(formPanels.get(0));
        
        // Support for "as-you-type" interfaces that update whenever the user pauses in their typing.
        ActionListener listener = builder.typingTimeoutActionListener;
        initComponentListener(listener);
        initTextChangeTimer(listener);
        initDocumentListener();
    }
    
    private JTabbedPane makeTabbedPaneForFormPanels(List<FormPanel> formPanels) {
        JTabbedPane tabbedPane = new JTabbedPane();
        for (int i = 0; i < formPanels.size(); ++i) {
            // Give each panel a border to push it away from the side of the tabbed pane.
            // We usual spacing will end up between the outside of the tabbed pane and the edge of the window/button panel.
            FormPanel panel = formPanels.get(i);
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 16, 10));
            
            tabbedPane.add(builder.tabTitles[i], panel);
        }
        
        for (FormPanel child : formPanels) {
            fixOpacityOfTabbedPaneChildren(child);
        }
        
        return tabbedPane;
    }
    
    /**
     * Mac OS tabbed panes' interiors have a darker background than normal panels.
     * Windows XP tabbed panes' interiors have a lighter (gradient) background than normal panels.
     * Sun's Metal tabbed panes' interiors use the tab ear color rather than the color of normal panels.
     * It may be possible (I don't know) for some GTK+ themes to use a similar effect.
     * For any of these effects to show through, the children need to be non-opaque (that is, transparent).
     */
    private static void fixOpacityOfTabbedPaneChildren(Component component) {
        // JTextField won't paint its background unless it's opaque, but almost everything else should be non-opaque.
        // The JButton documentation says not to make a JButton non-opaque.
        if (component instanceof JComponent && component instanceof JTextField == false && component instanceof JButton == false) {
            ((JComponent) component).setOpaque(false);
        }
        // Recurse for any children.
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                fixOpacityOfTabbedPaneChildren(child);
            }
        }
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
    private void initDocumentListener() {
        // Ensure we have a listener...
        if (documentListener == null) {
            this.documentListener = new DocumentListener() {
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
        }
        // ...and attach it to the text fields.
        addTextFieldListeners();
    }
    
    /**
     * Notifies the FormPanel's typing timeout listener if it's been more
     * than 0.5s since the user typed anything, on the assumption that it's
     * time to update the UI in response.
     */
    private void initTextChangeTimer(final ActionListener listener) {
        if (textChangeTimer == null) {
            textChangeTimer = new Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (dialog.isShowing()) {
                        listener.actionPerformed(null);
                        isListenerUpToDate = true;
                    }
                }
            });
            textChangeTimer.setRepeats(false);
        }
        // It doesn't matter what we initialize this to; our ComponentListener
        // will do the right thing either way. But "true" seems more natural.
        isListenerUpToDate = true;
    }
    
    /**
     * Adds listeners to all the text fields.
     */
    private void addTextFieldListeners() {
        // Collect up all the text fields on all the panels.
        listenedToTextFields = new ArrayList<JTextComponent>();
        for (FormPanel formPanel : builder.getFormPanels()) {
            listenedToTextFields.addAll(formPanel.getTextComponents());
        }
        // Add listeners.
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

    private void initFocus(FormPanel formPanel) {
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
        JFrameUtilities.restoreBounds(dialog.getTitle(), dialog);
    }
    
    /**
     * Invoked when the user chooses the dialog's "accept" action.
     * May be invoked programmatically to dismiss the dialog as if accepted.
     */
    public void acceptDialog() {
        try {
            Boolean okay = acceptCallable.call();
            if (okay == Boolean.TRUE) {
                processUserChoice(true);
            }
        } catch (Exception ex) {
            Log.warn("Accept callable for dialog \"" + dialog.getTitle() + "\" threw an exception", ex);
        }
    }
    
    /**
     * Invoked when the user chooses the dialog's "cancel" action.
     * May be invoked programmatically to dismiss the dialog as if canceled.
     */
    public void cancelDialog() {
        processUserChoice(false);
        EventQueue.invokeLater(cancelRunnable);
    }

    private void processUserChoice(boolean isAcceptance) {
        restoreFocus();
        if (doNotRememberBounds == false) {
            JFrameUtilities.storeBounds(dialog.getTitle(), dialog);
        }
        wasAccepted = isAcceptance;
        removeTextFieldListeners();
        dialog.dispose();
    }
    
    private void restoreFocus() {
        if (shouldRestoreFocus == false) {
            return;
        }
        // We want to restore focus to whichever component had it before we were shown, but we mustn't pass on focus until later because we may be here in response to a KeyEvent (such as the user hitting Return to accept the dialog), and it would be wrong for the original focus owner to process that event.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                dialogFocusRedirector.restoreFocus();
            }
        });
    }
    
    private JPanel makeButtonPanel(JRootPane rootPane, String actionLabel, JComponent statusBar) {
        JButton actionButton = new JButton(actionLabel);
        actionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                acceptDialog();
            }
        });
        GnomeStockIcon.configureButton(actionButton);
        rootPane.setDefaultButton(actionButton);
        
        JButton cancelButton = null;
        if (dialog.isModal()) {
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(closeAction);
            GnomeStockIcon.configureButton(cancelButton);
            ComponentUtilities.tieButtonSizes(actionButton, cancelButton);
        }
        
        if (extraButton != null) {
            GnomeStockIcon.configureButton(extraButton);
        }
        return makeButtonPanel(actionButton, cancelButton, statusBar);
    }
    
    private JPanel makeButtonPanel(JButton actionButton, JButton cancelButton, JComponent statusBar) {
        JPanel panel = new JPanel();
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
     * Sets an optional Callable that will be run if this dialog is accepted,
     * just before the dialog is removed from the display.
     * 
     * Any intended focus changes should be performed via invokeLater.
     * 
     * Return Boolean.TRUE from your Callable to allow the "accept"; Boolean.FALSE to leave the dialog up.
     */
    public void setAcceptCallable(Callable<Boolean> callable) {
        this.acceptCallable = callable;
    }
    
    /**
     * Sets an optional runnable that will be run if this dialog is canceled,
     * just before the dialog is removed from the display.
     * 
     * Any intended focus changes should be performed via invokeLater.
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
    
    /**
     * Enable or disable the automatic focus restoration.
     * Useful if your dialog has deliberately caused a change of focus, as with an open dialog, say.
     */
    public void setShouldRestoreFocus(boolean shouldRestoreFocus) {
        this.shouldRestoreFocus = shouldRestoreFocus;
    }
}
