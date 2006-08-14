package e.gui;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JFrameUtilities {
    private static final Image FRAME_ICON = new ImageIcon(System.getProperty("org.jessies.frameIcon")).getImage();
    
    public static void setFrameIcon(JFrame frame) {
        frame.setIconImage(FRAME_ICON);
    }
    
    /**
     * Invoke this after calling pack but before setVisible.
     * The window's location and size will be adjusted to fit the display.
     */
    public static void constrainToScreen(Window window) {
        // Query the display information.
        Rectangle displayBounds = window.getGraphicsConfiguration().getBounds();
        Insets insets = window.getToolkit().getScreenInsets(window.getGraphicsConfiguration());
        
        // Query the window information.
        Point newLocation = window.getLocation();
        Dimension newSize = window.getSize();
        
        // Make sure the window's top left corner is on-screen.
        newLocation.x = Math.max(displayBounds.x + insets.left, newLocation.x);
        newLocation.y = Math.max(displayBounds.y + insets.top, newLocation.y);
        
        // Make sure the window's bottom right corner is on-screen.
        if (newLocation.x + newSize.width > displayBounds.x + displayBounds.width - insets.right) {
            newSize.width = (displayBounds.x + displayBounds.width - insets.right) - newLocation.x;
        }
        if (newLocation.y + newSize.height > displayBounds.y + displayBounds.height - insets.bottom) {
            newSize.height = (displayBounds.y + displayBounds.height - insets.bottom) - newLocation.y;
        }
        
        window.setLocation(newLocation);
        window.setSize(newSize);
    }
    
    public static JFrame showTextWindow(Component parent, String title, String content) {
        JFrame frame = makeScrollableContentWindow(title, makeTextArea(content));
        frame.setLocationRelativeTo(parent);
        frame.setVisible(true);
        return frame;
    }
    
    public static PTextArea makeTextArea(String content) {
        PTextArea textArea = new PTextArea(40, 80);
        textArea.setFont(new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 10));
        textArea.setText(content);
        textArea.setEditable(false);
        return textArea;
    }
    
    public static JFrame makeScrollableContentWindow(String title, JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        return makeSimpleWindow(title, scrollPane);
    }
    
    public static JFrame makeSimpleWindow(String title, JComponent content) {
        final JFrame frame = new JFrame(title);
        setFrameIcon(frame);
        frame.setContentPane(content);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Mac OS uses command-W to close a window using the keyboard. Unlike Linux and Windows' alt-f4, though, this isn't done by the window manager.
        if (GuiUtilities.isMacOs()) {
            KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_MASK, false);
            final String CLOSE_ACTION = "e.gui.JFrameUtilities.CloseWindowIfCommandWPressed";
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(commandW, CLOSE_ACTION);
            frame.getRootPane().getActionMap().put(CLOSE_ACTION, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            });
        }
        
        return frame;
    }
    
    private JFrameUtilities() {
    }
}
