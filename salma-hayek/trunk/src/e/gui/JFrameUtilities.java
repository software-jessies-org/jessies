package e.gui;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import javax.swing.*;

public class JFrameUtilities {
    private static final Image FRAME_ICON;
    static {
        // Load the icon so that failures don't jeopardize users of this class; you don't want to hang in <clinit>!
        Image image = null;
        try {
            image = ImageIO.read(FileUtilities.fileFromString(System.getProperty("org.jessies.frameIcon")));
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            FRAME_ICON = image;
        }
    }
    
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
        Point topLeftBound = new Point(displayBounds.x + insets.left, displayBounds.y + insets.top);
        Point bottomRightBound = new Point(displayBounds.x + displayBounds.width - insets.right, displayBounds.y + displayBounds.height - insets.bottom);
        
        // Query the window information.
        Point newLocation = window.getLocation();
        Dimension newSize = window.getSize();
        
        // Make sure the window's bottom right corner is no further down and right than the bound.
        // Do this by moving the top left corner, keeping the size the same.
        if (newLocation.x + newSize.width > bottomRightBound.x) {
            newLocation.x = bottomRightBound.x - newSize.width;
        }
        if (newLocation.y + newSize.height > bottomRightBound.y) {
            newLocation.y = bottomRightBound.y - newSize.height;
        }
        
        // Make sure the window's top left corner is no further up and left than the bound.
        // If the size is non-negative, this is now sufficient to ensure that it is on-screen.
        newLocation.x = Math.max(topLeftBound.x, newLocation.x);
        newLocation.y = Math.max(topLeftBound.y, newLocation.y);
        
        // This may have moved the bottom right corner of the window off-screen again.
        // If so, we now know the window's too big for the screen, so shrink it to fit.
        if (newLocation.x + newSize.width > bottomRightBound.x) {
            newSize.width = bottomRightBound.x - newLocation.x;
        }
        if (newLocation.y + newSize.height > bottomRightBound.y) {
            newSize.height = bottomRightBound.y - newLocation.y;
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
        JComponent windowContent = content;
        
        // On Mac OS, the "grow box" used to resize the window takes up space *inside* the window, so we need to move out of its way.
        // See http://elliotth.blogspot.com/2004/07/dodging-macs-grow-box-in-java.html.
        if (GuiUtilities.isMacOs()) {
            final int size = (int) scrollPane.getVerticalScrollBar().getMinimumSize().getWidth();
            JPanel growBoxPanel = new JPanel();
            Dimension growBoxSize = new Dimension(size, size);
            growBoxPanel.setPreferredSize(growBoxSize);
            
            JPanel sidePanel = new JPanel(new BorderLayout());
            sidePanel.add(scrollPane.getVerticalScrollBar(), BorderLayout.CENTER);
            sidePanel.add(growBoxPanel, BorderLayout.SOUTH);
            
            windowContent = new JPanel(new BorderLayout());
            windowContent.add(scrollPane, BorderLayout.CENTER);
            windowContent.add(sidePanel, BorderLayout.EAST);
        }
        
        return makeSimpleWindow(title, windowContent);
    }
    
    public static JFrame makeSimpleWindow(String title, JComponent content) {
        JFrame frame = new JFrame(title);
        setFrameIcon(frame);
        frame.setContentPane(content);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Mac OS uses command-W to close a window using the keyboard. Unlike Linux and Windows' alt-f4, though, this isn't done by the window manager.
        if (GuiUtilities.isMacOs()) {
            KeyStroke commandW = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_MASK, false);
            closeOnKeyStroke(frame, commandW);
        }
        
        return frame;
    }
    
    private static final Action CLOSE_ACTION = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            Window window = SwingUtilities.getWindowAncestor((Component) e.getSource());
            // Dispatch an event so it's as if the window's close button was clicked.
            // The client has to set up the right behavior for that case.
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        }
    };
    
    public static void closeOnKeyStroke(JFrame frame, KeyStroke keyStroke) {
        final String CLOSE_ACTION_NAME = "e.gui.JFrameUtilities.CloseFrameOnKeyStroke";
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, CLOSE_ACTION_NAME);
        frame.getRootPane().getActionMap().put(CLOSE_ACTION_NAME, CLOSE_ACTION);
    }
    
    private JFrameUtilities() {
    }
}
