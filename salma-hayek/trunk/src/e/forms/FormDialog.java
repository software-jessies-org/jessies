package e.forms;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import e.gui.*;
import e.util.*;

/**
 * Implements simple dialogs. You provide the content (probably with a FormPanel),
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
public class FormDialog extends JDialog {
    private static int componentSpacing = getComponentSpacing();
    
    private static HashMap dialogGeometries = new HashMap();
    
    private boolean wasAccepted;
    
    private static final boolean isWindows() {
        return (System.getProperty("os.name").indexOf("Windows") != -1);
    }
    
    public static int getComponentSpacing() {
        if (isWindows()) {
            return 4;
        }
        return 10;
    }
    
    /** Shows a non-modal dialog with just a Close button. */
    public static void showNonModal(Frame parent, String title, Container contentPane) {
        FormDialog formDialog = new FormDialog(parent, title, contentPane, "Close", null, false);
        formDialog.show();
    }
    
    /**
     * Shows a dialog with the usual OK and Cancel buttons.
     * See the note in the class documentation about "OK" buttons.
     */
    public static boolean show(Frame parent, String title, Container contentPane, String actionLabel) {
        return show(parent, title, contentPane, actionLabel, null);
    }
    
    /**
     * Shows a dialog with the usual OK and Cancel buttons plus an extra button
     * you provide.
     * See the note in the class documentation about "OK" buttons.
     */
    public static boolean show(Frame parent, String title, Container contentPane, String actionLabel, JButton extraButton) {
        FormDialog formDialog = new FormDialog(parent, title, contentPane, actionLabel, extraButton, true);
        formDialog.show();
        return formDialog.wasAccepted();
    }

    private FormDialog(Frame parent, String title, Container contentPane, String actionLabel, JButton extraButton, boolean modal) {
        super(parent, title, modal);
        init(parent, contentPane, actionLabel, extraButton);
    }
    
    private void init(Frame parent, Container contentPane, String actionLabel, JButton extraButton) {
        setResizable(true);
        
        JPanel internalContentPane = new JPanel(new BorderLayout(componentSpacing, componentSpacing));
        internalContentPane.setBorder(BorderFactory.createEmptyBorder(componentSpacing, componentSpacing, componentSpacing, componentSpacing));
        internalContentPane.add(contentPane, BorderLayout.CENTER);
        internalContentPane.add(makeButtonPanel(getRootPane(), actionLabel, extraButton), BorderLayout.SOUTH);
        
        setContentPane(internalContentPane);
        
        // Set sensible defaults for size and location.
        pack();
        setLocationRelativeTo(parent);
        
        // But if we've shown this dialog before, put it back where it last was.
        restorePreviousSize();
        
        initCloseBehavior();
        initFocus(contentPane);
    }
    
    private void initCloseBehavior() {
        initWindowManagerCloseBehavior();
        initKeyboardCloseBehavior();
    }

    private void initFocus(Container contentPane) {
        DialogFocusRedirector dialogFocusRedirector = new DialogFocusRedirector(contentPane);
        dialogFocusRedirector.redirectFocus();
    }
    
    private void initWindowManagerCloseBehavior() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
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
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", closeAction);
    }
    
    /**
     * Restores the size and location stored in the dialog geometries
     * configuration. This is invoked just after we've invoked pack, so we
     * refuse to allow the configuration to make us any smaller. (When JButton
     * grew vertically between 1.4.2 and 1.5.0, I spent some time wondering why
     * the LayoutManager was allowing my JButton to be clipped. It wasn't; this
     * code was forcing the clipping to occur. Hence the restriction now.)
     */
    public void restorePreviousSize() {
        Rectangle previousBounds = (Rectangle) dialogGeometries.get(getTitle());
        if (previousBounds != null) {
            setLocation(previousBounds.getLocation());
            Dimension newSize = previousBounds.getSize();
            newSize.height = Math.max(newSize.height, getHeight());
            newSize.width = Math.max(newSize.width, getWidth());
            setSize(newSize);
        }
    }
    
    public void acceptDialog() {
        processUserChoice(true);
    }

    public void cancelDialog() {
        processUserChoice(false);
    }

    public void processUserChoice(boolean isAcceptance) {
        dialogGeometries.put(getTitle(), getBounds());
        wasAccepted = isAcceptance;
        dispose();
    }

    public boolean wasAccepted() {
        return wasAccepted;
    }

    private JPanel makeButtonPanel(JRootPane rootPane, String actionLabel, JButton extraButton) {
        if (isModal() == false) {
            JButton closeButton = new JButton(actionLabel);
            closeButton.addActionListener(closeAction);
            rootPane.setDefaultButton(closeButton);
            return makeButtonPanel(closeButton, null, null);
        }
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(closeAction);

        JButton actionButton = new JButton(actionLabel);
        actionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                acceptDialog();
            }
        });
        
        Dimension cancelSize = cancelButton.getPreferredSize();
        Dimension actionSize = actionButton.getPreferredSize();
        final int width = (int) Math.max(actionSize.getWidth(), cancelSize.getWidth());
        final int height = (int) Math.max(actionSize.getHeight(), cancelSize.getHeight());
        Dimension buttonSize = new Dimension(width, height);
        actionButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        
        rootPane.setDefaultButton(actionButton);

        return makeButtonPanel(actionButton, cancelButton, extraButton);
    }
    
    private JPanel makeButtonPanel(JButton actionButton, JButton cancelButton, JButton extraButton) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, componentSpacing, componentSpacing, componentSpacing));
        //panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        
        if (isWindows()) {
            // Use the traditional Windows layout.
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
        StringBuffer content = new StringBuffer();
        Iterator it = dialogGeometries.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            Rectangle bounds = (Rectangle) dialogGeometries.get(name);
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
                String name = (String) lines[i++];
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
}
