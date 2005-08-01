package e.tools;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * A Java equivalent of Apple's Pixie.app magnifying glass utility. This is
 * useful for checking layout, graphics, and colors in your own programs and
 * others.
 */
public class FatBits extends JFrame {
    private Robot robot;
    private Timer timer;
    private ScaledImagePanel scaledImagePanel;
    private int scaleFactor;
    
    private JSlider scaleSlider;
    private JCheckBox showGridCheckBox;
    private JCheckBox keepOnTopCheckBox;
    
    private JLabel positionLabel;
    
    private JLabel colorLabel;
    private ColorSwatchIcon colorSwatch;
    
    public FatBits() {
        super("FatBits");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            Log.warn("failed to create a Robot", ex);
        }
        timer = new Timer(50, new MouseTracker());
        setSize(new Dimension(250, 300));
        setContentPane(makeUi());
        
        //
        // FIXME: support keyboard operation:
        //
        //   '+': increase scale (also with command, like Preview?)
        //   '-': decrease scale
        //   ' ': play/pause following pointer position (also C-L like Pixie?)
        //   arrow keys: move Robot for finer positioning than with the mouse
        //
        
        timer.start();
    }
    
    private JComponent makeUi() {
        initColorLabel();
        initKeepOnTopCheckBox();
        initPositionLabel();
        initScaledImagePanel();
        initScaleSlider();
        initShowGridCheckBox();
        
        JPanel result = new JPanel(new BorderLayout());
        result.add(scaledImagePanel, BorderLayout.CENTER);
        result.add(makeInfoPanel(), BorderLayout.SOUTH);
        return result;
    }
    
    private JPanel makeInfoPanel() {
        CircularButton infoButton = new CircularButton();
        infoButton.setActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FormBuilder form = new FormBuilder(FatBits.this, "FatBits Preferences");
                FormPanel formPanel = form.getFormPanel();
                formPanel.addRow("Scale:", scaleSlider);
                formPanel.addRow("", showGridCheckBox);
                formPanel.addRow("", keepOnTopCheckBox);
                form.showNonModal();
                // FIXME: show a dialog with the various controls in it.
                // also: [x] refresh only when mouse moves
                //       [ ] show mouse hot-spot
                // alternative grid colors?
            }
        });
        JPanel result = new JPanel(new BorderLayout(8, 0));
        // FIXME: the right border should only be large on Mac OS, and should
        // correspond more closely to the grow box inset.
        result.setBorder(new javax.swing.border.EmptyBorder(4, 4, 4, 24));
        result.add(colorLabel, BorderLayout.WEST);
        result.add(positionLabel, BorderLayout.CENTER);
        result.add(infoButton, BorderLayout.EAST);
        return result;
    }
    
    private void initScaledImagePanel() {
        this.scaledImagePanel = new ScaledImagePanel();
    }
    
    private void initPositionLabel() {
        this.positionLabel = new JLabel(" ");
        positionLabel.setFont(colorLabel.getFont());
    }
    
    private void updatePosition(Point p) {
        if (scaledImagePanel.isShowing() == false) {
            return;
        }
        
        positionLabel.setText("(" + p.x + "," + p.y + ")");
        
        Rectangle screenCaptureBounds = getScreenCaptureBounds(p);
        BufferedImage capturedImage = robot.createScreenCapture(screenCaptureBounds);
        updateCenterColor(capturedImage.getRGB(capturedImage.getWidth() / 2, capturedImage.getHeight() / 2));
        
        Image scaledImage = capturedImage.getScaledInstance(roundLengthDown(scaledImagePanel.getWidth()), roundLengthDown(scaledImagePanel.getHeight()), Image.SCALE_REPLICATE);
        scaledImagePanel.setImage(scaledImage);
    }
    
    private void initColorLabel() {
        this.colorLabel = new JLabel(" ");
        Font font = colorLabel.getFont();
        colorLabel.setFont(new Font(GuiUtilities.getMonospacedFontName(), font.getStyle(), font.getSize()));
        int height = colorLabel.getPreferredSize().height - 2;
        this.colorSwatch = new ColorSwatchIcon(null, new Dimension(20, height));
        colorLabel.setIcon(colorSwatch);
    }
    
    private void updateCenterColor(int argb) {
        colorLabel.setText(String.format("%06x", argb & 0xffffff));
        colorSwatch.setColor(new Color(argb));
    }
    
    private void initShowGridCheckBox() {
        this.showGridCheckBox = new JCheckBox("Show grid");
        showGridCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                scaledImagePanel.setShowGrid(showGridCheckBox.isSelected());
            }
        });
        showGridCheckBox.setSelected(false);
    }
    
    private void initKeepOnTopCheckBox() {
        this.keepOnTopCheckBox = new JCheckBox("Keep window on top");
        keepOnTopCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                FatBits.this.setAlwaysOnTop(keepOnTopCheckBox.isSelected());
            }
        });
        keepOnTopCheckBox.setSelected(false);
    }
    
    private void initScaleSlider() {
        this.scaleSlider = new JSlider(1, 4);
        scaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                scaleFactor = (1 << scaleSlider.getValue());
                updatePosition(getPointerLocation());
            }
        });
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        for (int i = scaleSlider.getMinimum(); i <= scaleSlider.getMaximum(); ++i) {
            labels.put(i, new JLabel(Integer.toString(1 << i) + "x"));
        }
        scaleSlider.setLabelTable(labels);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setValue(1);
    }
    
    private int roundLengthDown(int length) {
        return (length - (length % scaleFactor));
    }
    
    private Rectangle getScreenCaptureBounds(Point center) {
        Point topLeft = new Point(center.x - scaledImagePanel.getWidth() / (2 * scaleFactor), center.y - scaledImagePanel.getHeight() / (2 * scaleFactor));
        Rectangle result = new Rectangle(topLeft, scaledImagePanel.getSize());
        result.width /= scaleFactor;
        result.height /= scaleFactor;
        
        // Constrain the capture to the display.
        // Apple's 1.5 VM crashes if you don't.
        result.x = Math.max(result.x, 0);
        result.y = Math.max(result.y, 0);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
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
        private boolean showGrid;
        
        public void setImage(Image image) {
            this.image = image;
            repaint();
        }
        
        public void setShowGrid(boolean showGrid) {
            this.showGrid = showGrid;
            repaint();
        }
        
        public void paintComponent(Graphics g) {
            int xOrigin = xOrigin();
            int yOrigin = yOrigin();
            g.drawImage(image, xOrigin, yOrigin, null);
            paintGridLines(g, xOrigin, yOrigin);
        }
        
        private void paintGridLines(Graphics g, int xOrigin, int yOrigin) {
            if (showGrid == false) {
                return;
            }
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
            Point p = getPointerLocation();
            if (lastPosition != null && lastPosition.equals(p)) {
                return;
            }
            lastPosition = p;
            updatePosition(lastPosition);
        }
    }
    
    public static void main(String[] args) {
        new FatBits().setVisible(true);
    }
}
