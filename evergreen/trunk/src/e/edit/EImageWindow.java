package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.*;

import e.gui.*;

public class EImageWindow extends EWindow {
    private String filename;
    private Image image = null;
    private ImageIcon icon = null;
    private ImageRenderer imageRenderer;
    private Collection menuItems;
    
    public EImageWindow(String filename) {
        super(filename);
        this.filename = filename;
        this.image = loadImage(filename);
        this.imageRenderer = new ImageRenderer(image);
        JScrollPane scrollPane = new JScrollPane(imageRenderer);
        initPopupMenuItems();
        attachPopupMenuTo(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
        retitle();
    }
    
    public Collection getPopupMenuItems() {
        return menuItems;
    }
    
    public void initPopupMenuItems() {
        menuItems = new ArrayList();
        menuItems.add(new ZoomInAction());
        menuItems.add(new ZoomOutAction());
        menuItems.add(new ActualSizeAction());
    }
    
    public class ZoomInAction extends AbstractAction {
        public ZoomInAction() {
            super("Zoom In");
        }
        public void actionPerformed(ActionEvent e) {
            imageRenderer.setScaleFactor(imageRenderer.getScaleFactor() * 2.0);
            retitle();
        }
    }
    
    public class ZoomOutAction extends AbstractAction {
        public ZoomOutAction() {
            super("Zoom Out");
        }
        public void actionPerformed(ActionEvent e) {
            imageRenderer.setScaleFactor(imageRenderer.getScaleFactor() / 2.0);
            retitle();
        }
    }
    
    public class ActualSizeAction extends AbstractAction {
        public ActualSizeAction() {
            super("Actual Size");
        }
        public void actionPerformed(ActionEvent e) {
            imageRenderer.setScaleFactor(1.0);
            retitle();
        }
    }
    
    public Image loadImage(String filename) {
        Image newImage = Toolkit.getDefaultToolkit().createImage(filename);
        waitForImageToLoad(newImage);
        return newImage;
    }
    
    public void waitForImageToLoad(Image image) {
        try {
            MediaTracker mediaTracker = new MediaTracker(this);
            mediaTracker.addImage(image, 0);
            mediaTracker.waitForAll();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    /** Image windows show the image's dimensions after the filename. */
    public void retitle() {
        setTitle(filename + " - " + getImageDimensions());
    }
    
    public String getImageDimensions() {
        String result = image.getWidth(null) + "x" + image.getHeight(null);
        double scaleFactor = imageRenderer.getScaleFactor();
        if ((int) scaleFactor != 1) {
            result += " (" + scaleFactor + "x)";
        }
        return result;
    }
    
    public class ImageRenderer extends JComponent {
        private Image image;
        private double scaleFactor;
        private AffineTransform transform;
        
        public ImageRenderer(final Image image) {
            this.image = image;
            //this.addMouseListener(new MouseAdapter() {
                //public void mouseClicked(MouseEvent e) {
                    //int rgb = image.getRGB(e.getX() / scaleFactor, e.getY() / scaleFactor);
                    //Color c = new Color(rgb);
                    //colorLabel.setText(c.getRed() + ", " + c.getGreen() + ", " + c.getBlue());
                //}
            //});
            setScaleFactor(1);
        }
        public void setScaleFactor(double scaleFactor) {
            this.scaleFactor = scaleFactor;
            this.transform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
            JComponent parent = (JComponent) this.getParent();
            if (parent != null) {
                this.invalidate();
                parent.revalidate();
            }
            this.repaint();
        }
        public double getScaleFactor() {
            return this.scaleFactor;
        }
        public void paint(Graphics g) {
            ((Graphics2D) g).drawImage(image, transform, null);
        }
        public Dimension getPreferredSize() {
            int width = (int) (scaleFactor * image.getWidth(null));
            int height = (int) (scaleFactor * image.getHeight(null));
            return new Dimension(width, height);
        }
    }
}
