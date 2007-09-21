package e.gui;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

public class JFrameUtilities {
    private static final Image FRAME_ICON;
    static {
        // Load the icon carefully so that failures don't jeopardize users of this class.
        // You don't want to hang in <clinit>!
        String filename = System.getProperty("org.jessies.frameIcon");
        Image image = null;
        try {
            // Load the icon if it looks like a real attempt was made to specify one.
            if (filename.length() > 0) {
                image = ImageIO.read(FileUtilities.fileFromString(filename));
            }
        } catch (Throwable th) {
            Log.warn("Failed to load icon \"" + filename + "\".", th);
        } finally {
            FRAME_ICON = image;
        }
    }
    
    private static final KeyStroke ESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
    
    private static HashMap<String, Rectangle> dialogGeometries = new HashMap<String, Rectangle>();
    
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
        JComponent windowContent = scrollPane;
        
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
        MainFrame frame = new MainFrame(title);
        frame.setContentPane(content);
        frame.pack();
        
        // Most systems let you close a dialog with Esc.
        closeOnEsc(frame);
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
    
    public static void closeOnEsc(JDialog dialog) {
        closeOnKeyStroke(dialog.getRootPane(), ESC);
    }
    
    public static void closeOnEsc(JFrame frame) {
        closeOnKeyStroke(frame.getRootPane(), ESC);
    }
    
    public static void closeOnKeyStroke(JFrame frame, KeyStroke keyStroke) {
        closeOnKeyStroke(frame.getRootPane(), keyStroke);
    }
    
    private static void closeOnKeyStroke(JRootPane rootPane, KeyStroke keyStroke) {
        final String CLOSE_ACTION_NAME = "e.gui.JFrameUtilities.CloseFrameOnKeyStroke";
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, CLOSE_ACTION_NAME);
        rootPane.getActionMap().put(CLOSE_ACTION_NAME, CLOSE_ACTION);
    }
    
    /**
     * Writes our dialog geometries to disk so we can preserve them across runs.
     * The format isn't very human-readable, but we're stuck with it now.
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
     * Call this before setVisible to make the window/dialog corresponding to the given name appear in the position last known to storeBounds.
     * It's a good idea to call setLocationRelativeTo first, as a default.
     * You have to pass a name because Window doesn't have a title (even though its subclasses do).
     * Feel free to treat "name" as a tag rather than literally the window title.
     */
    public static void restoreBounds(String name, Window window) {
        Rectangle previousBounds = dialogGeometries.get(name);
        if (previousBounds != null) {
            Point newLocation = previousBounds.getLocation();
            Dimension newSize = previousBounds.getSize();
            
            // Don't shrink the dialog below its "pack" size.
            newSize.height = Math.max(newSize.height, window.getHeight());
            newSize.width = Math.max(newSize.width, window.getWidth());
            
            window.setLocation(newLocation);
            window.setSize(newSize);
            
            JFrameUtilities.constrainToScreen(window);
        }
    }
    
    /**
     * Call this to record the fact that the window/dialog with the given name has moved.
     */
    public static void storeBounds(String name, Window window) {
        dialogGeometries.put(name, window.getBounds());
    }
    
    private JFrameUtilities() {
    }
}
