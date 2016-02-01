package e.tools;

import com.apple.eawt.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * A Java equivalent of Apple's Pixie.app magnifying glass utility. This is
 * useful for checking layout, graphics, and colors in your own programs and
 * those of others.
 */
public class FatBits extends MainFrame {
    private Preferences preferences;
    private Robot robot;
    private RepeatingComponentTimer timer;
    private ScaledImagePanel scaledImagePanel;
    
    private ELabel positionLabel;
    
    private ELabel colorLabel;
    private ELabel colorSwatchLabel;
    private ColorSwatchIcon colorSwatch;
    
    public FatBits() {
        super("FatBits");
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            Log.warn("failed to create a Robot", ex);
        }
        setSize(new Dimension(280, 300));
        setContentPane(makeUi());
        setJMenuBar(new FatBitsMenuBar());
        timer = new RepeatingComponentTimer(this, 50, new MouseTracker());
        timer.start();
    }
    
    private JComponent makeUi() {
        this.preferences = new FatBitsPreferences();
        preferences.addPreferencesListener(new Preferences.Listener() {
            public void preferencesChanged() {
                FatBits.this.setAlwaysOnTop(preferences.getBoolean(FatBitsPreferences.KEEP_ON_TOP));
                if (scaledImagePanel != null) {
                    scaledImagePanel.repaint();
                }
            }
        });
        preferences.readFromDisk();
        
        initAboutBox();
        initMacOsEventHandlers();
        
        initColorLabel();
        initPositionLabel();
        initScaledImagePanel();
        initScaleSlider();
        
        JPanel result = new JPanel(new BorderLayout());
        result.add(scaledImagePanel, BorderLayout.CENTER);
        result.add(makeInfoPanel(), BorderLayout.SOUTH);
        return result;
    }
    
    private void initAboutBox() {
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setWebSiteAddress("https://code.google.com/p/jessies/wiki/SalmaHayek");
        aboutBox.addCopyright("Copyright (C) 2005-2008 software.jessies.org team.");
        aboutBox.addCopyright("All Rights Reserved.");
        aboutBox.setLicense(AboutBox.License.GPL_2_OR_LATER);
    }
    
    private void initMacOsEventHandlers() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        
        Application.getApplication().addApplicationListener(new ApplicationAdapter() {
            @Override
            public void handleQuit(ApplicationEvent e) {
                e.setHandled(true);
            }
        });
    }
    
    private JPanel makeInfoPanel() {
        JPanel infoPanel = new JPanel(new BorderLayout(8, 0));
        infoPanel.setBorder(GuiUtilities.createEmptyBorder(4));
        infoPanel.add(colorSwatchLabel, BorderLayout.WEST);
        if (GuiUtilities.isGtk()) {
            JButton infoButton = new JButton(preferences.makeShowPreferencesAction());
            infoButton.setText("");
            infoPanel.add(infoButton, BorderLayout.EAST);
        }
        
        JPanel textLines = new JPanel(new GridLayout(2,1));
        textLines.add(colorLabel);
        textLines.add(positionLabel);
        
        infoPanel.add(textLines, BorderLayout.CENTER);
        return infoPanel;
    }
    
    private void initScaledImagePanel() {
        this.scaledImagePanel = new ScaledImagePanel();
    }
    
    private void initPositionLabel() {
        this.positionLabel = new ELabel();
        positionLabel.setFont(colorLabel.getFont());
    }
    
    private boolean updatePosition(Point p) {
        if (scaledImagePanel.isShowing() == false) {
            return false;
        }
        
        positionLabel.setText("X:" + p.x + " Y:" + p.y);
        
        Rectangle screenCaptureBounds = getScreenCaptureBounds(p);
        BufferedImage capturedImage = robot.createScreenCapture(screenCaptureBounds);
        updateCenterColor(capturedImage.getRGB(capturedImage.getWidth() / 2, capturedImage.getHeight() / 2));
        
        Image scaledImage = scaleImage(capturedImage, roundLengthDown(scaledImagePanel.getWidth()), roundLengthDown(scaledImagePanel.getHeight()));
        scaledImagePanel.setImage(scaledImage);
        
        return true;
    }
    
    private static Image scaleImage(Image sourceImage, int width, int height) {
        return ImageUtilities.scale(sourceImage, width, height, ImageUtilities.InterpolationHint.REPLICATE);
    }
    
    private void initColorLabel() {
        this.colorLabel = new ELabel();
        Font font = colorLabel.getFont();
        colorLabel.setFont(new Font(GuiUtilities.getMonospacedFontName(), font.getStyle(), font.getSize()));
        int height = colorLabel.getPreferredSize().height - 2;
        this.colorSwatch = new ColorSwatchIcon(null, new Dimension(20, height));
        this.colorSwatchLabel = new ELabel(colorSwatch);
    }
    
    private void updateCenterColor(int argb) {
        Color color = new Color(argb);
        colorLabel.setText(String.format("#%06x RGB:%d,%d,%d", argb & 0xffffff, color.getRed(), color.getGreen(), color.getBlue()));
        colorSwatch.setColor(color);
        colorSwatchLabel.repaint();
    }
    
    private void initScaleSlider() {
        final JSlider scaleSlider = new JSlider(1, 4);
        scaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                preferences.put(FatBitsPreferences.SCALE_FACTOR, Integer.valueOf(1 << scaleSlider.getValue()));
                updatePosition(getPointerLocation());
            }
        });
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        for (int i = scaleSlider.getMinimum(); i <= scaleSlider.getMaximum(); ++i) {
            labels.put(i, new ELabel(Integer.toString(1 << i) + "x"));
        }
        scaleSlider.setLabelTable(labels);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setSnapToTicks(true);
        preferences.setCustomUiForKey(FatBitsPreferences.SCALE_FACTOR, scaleSlider);
        scaleSlider.setValue(preferences.getInt(FatBitsPreferences.SCALE_FACTOR) >> 1);
    }
    
    private int roundLengthDown(int length) {
        int scaleFactor = preferences.getInt(FatBitsPreferences.SCALE_FACTOR);
        return (length - (length % scaleFactor));
    }
    
    private Rectangle getScreenCaptureBounds(Point center) {
        int scaleFactor = preferences.getInt(FatBitsPreferences.SCALE_FACTOR);
        Point topLeft = new Point(center.x - scaledImagePanel.getWidth() / (2 * scaleFactor), center.y - scaledImagePanel.getHeight() / (2 * scaleFactor));
        Rectangle result = new Rectangle(topLeft, scaledImagePanel.getSize());
        result.width /= scaleFactor;
        result.height /= scaleFactor;
        
        // Constrain the capture to the display.
        // Apple's 1.5 VM crashes if you don't.
        result.x = Math.max(result.x, 0);
        result.y = Math.max(result.y, 0);
        Dimension screenSize = getToolkit().getScreenSize();
        if (result.x + result.width > screenSize.width) {
            result.x = screenSize.width - result.width;
        }
        if (result.y + result.height > screenSize.height) {
            result.y = screenSize.height - result.height;
        }
        return result;
    }
    
    private Point getPointerLocation() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        return pointerInfo.getLocation();
    }
    
    private class ScaledImagePanel extends JComponent {
        private Image image;
        
        public ScaledImagePanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (timer.isRunning() == false) {
                        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                        paintImage(image.getGraphics());
                        updateCenterColor(image.getRGB(e.getX(), e.getY()));
                    }
                }
            });
        }
        
        public void setImage(Image image) {
            this.image = image;
            repaint();
        }
        
        public Image getImage() {
            return image;
        }
        
        @Override
        public void paintComponent(Graphics g) {
            paintImage(g);
            paintGridLines(g, xOrigin(), yOrigin());
            paintCrosshair(g);
        }
        
        private void paintImage(Graphics g) {
            g.drawImage(image, xOrigin(), yOrigin(), null);
        }
        
        private void paintCrosshair(Graphics g) {
            if (preferences.getBoolean(FatBitsPreferences.SHOW_CROSSHAIR) == false) {
                return;
            }
            g.setColor(preferences.getBoolean(FatBitsPreferences.SHOW_GRID) ? Color.RED : Color.BLACK);
            g.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
            g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
        }
        
        private void paintGridLines(Graphics g, int xOrigin, int yOrigin) {
            if (preferences.getBoolean(FatBitsPreferences.SHOW_GRID) == false) {
                return;
            }
            int scaleFactor = preferences.getInt(FatBitsPreferences.SCALE_FACTOR);
            g.setColor(Color.BLACK);
            final int maxX = xOrigin + roundLengthDown(getWidth());
            final int maxY = yOrigin + roundLengthDown(getHeight());
            for (int x = xOrigin + scaleFactor; x < maxX; x += scaleFactor) {
                g.drawLine(x, yOrigin, x, maxY - 1);
            }
            for (int y = yOrigin + scaleFactor; y < maxY; y += scaleFactor) {
                g.drawLine(xOrigin, y, maxX - 1, y);
            }
        }
        
        private int xOrigin() {
            return origin(getWidth());
        }
        
        private int yOrigin() {
            return origin(getHeight());
        }
        
        private int origin(int dimension) {
            return (dimension - roundLengthDown(dimension)) / 2;
        }
    }
    
    private class MouseTracker implements ActionListener {
        private Point lastPosition = null;

        public void actionPerformed(ActionEvent e) {
            // Has the pointer moved?
            Point p = getPointerLocation();
            if (scaledImagePanel.isShowing() && lastPosition != null && lastPosition.equals(p)) {
                return;
            }
            
            // Update.
            boolean updated = updatePosition(p);
            if (updated) {
                lastPosition = p;
            }
        }
    }
    
    private static class FatBitsPreferences extends Preferences {
        private static String SCALE_FACTOR = "scaleFactor";
        private static String SHOW_CROSSHAIR = "showCrosshair";
        private static String SHOW_GRID = "showGrid";
        private static String KEEP_ON_TOP = "keepOnTop";
        // also: [x] refresh only when mouse moves
        //       [ ] show mouse hot-spot
        // alternative grid colors?
        
        protected String getPreferencesFilename() {
            return "~/.org.jessies.FatBits";
        }
        
        protected void initPreferences() {
            addPreference(SCALE_FACTOR, Integer.valueOf(1), "Scale");
            addPreference(SHOW_CROSSHAIR, Boolean.TRUE, "Show crosshair");
            addPreference(SHOW_GRID, Boolean.FALSE, "Show grid");
            addPreference(KEEP_ON_TOP, Boolean.FALSE, "Keep on top");
        }
    }
    
    private class FatBitsMenuBar extends JMenuBar {
        private FatBitsMenuBar() {
            if (GuiUtilities.isMacOs() == false) {
                add(makeFileMenu());
            }
            add(makeEditMenu());
            add(makeImageMenu());
            add(makeHelpMenu());
        }
        
        private JMenu makeFileMenu() {
            JMenu menu = GuiUtilities.makeMenu("File", 'F');
            menu.add(new QuitAction());
            return menu;
        }
        
        private JMenu makeEditMenu() {
            JMenu menu = GuiUtilities.makeMenu("Edit", 'E');
            menu.add(new CopyImageAction());
            preferences.initPreferencesMenuItem(menu);
            return menu;
        }
        
        private JMenu makeImageMenu() {
            JMenu menu = GuiUtilities.makeMenu("Image", 'I');
            menu.add(new MouseMotionAction("Left", -1, 0));
            menu.add(new MouseMotionAction("Right", +1, 0));
            menu.add(new MouseMotionAction("Up", 0, -1));
            menu.add(new MouseMotionAction("Down", 0, +1));
            menu.addSeparator();
            menu.add(new JCheckBoxMenuItem(new LockImageAction()));
            return menu;
        }
        
        private JMenu makeHelpMenu() {
            HelpMenu helpMenu = new HelpMenu();
            return helpMenu.makeJMenu();
        }
    }
    
    private class MouseMotionAction extends AbstractAction {
        private int dx;
        private int dy;
        
        private MouseMotionAction(String direction, int dx, int dy) {
            GuiUtilities.configureAction(this, "Move _" + direction, GuiUtilities.makeKeyStroke(direction.toUpperCase(), false));
            this.dx = dx;
            this.dy = dy;
        }
        
        public void actionPerformed(ActionEvent e) {
            Point p = getPointerLocation();
            robot.mouseMove(p.x + dx, p.y + dy);
        }
    }
    
    private class QuitAction extends AbstractAction {
        private QuitAction() {
            GuiUtilities.configureAction(this, "_Quit", GuiUtilities.makeKeyStroke("Q", false));
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
    
    private class CopyImageAction extends AbstractAction {
        private CopyImageAction() {
            GuiUtilities.configureAction(this, "_Copy Image", GuiUtilities.makeKeyStroke("C", false));
        }
        
        public void actionPerformed(ActionEvent e) {
            ImageSelection.copyImageToClipboard(scaledImagePanel.getImage());
        }
    }
    
    private class LockImageAction extends AbstractAction {
        private LockImageAction() {
            GuiUtilities.configureAction(this, "Loc_k Image", GuiUtilities.makeKeyStroke("L", false));
        }
        
        public void actionPerformed(ActionEvent e) {
            JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
            boolean lock = menuItem.getState();
            if (lock) {
                timer.stop();
            } else {
                timer.start();
            }
        }
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                new FatBits().setVisible(true);
            }
        });
    }
}
