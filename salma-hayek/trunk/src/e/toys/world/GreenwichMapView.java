package e.toys.world;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.Timer;

public class GreenwichMapView extends JComponent {
    private BufferedImage dayMap;
    private BufferedImage nightMap;
    
    public GreenwichMapView() {
        dayMap = getBufferedImage("/Users/elliotth/Desktop/land_ocean_ice_2048.jpg");
        nightMap = getBufferedImage("/Users/elliotth/Desktop/land_ocean_ice_lights_2048.jpg");
        Dimension size = new Dimension(dayMap.getWidth(), dayMap.getHeight());
        setPreferredSize(size);
        new Timer(5 * 60 * 1000, new Updater()).start();
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                double y = (double) e.getY();
                double latitude = 90.0 - 180.0 * (y / (double) dayMap.getHeight());
                double longitude = 360.0 * ((double) x / (double) dayMap.getWidth()) - 180.0;
                System.err.println("lat="+latitude + " lon="+longitude);
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }
    
    private BufferedImage getBufferedImage(String filename) {
        // Load the original image.
        ImageIcon icon = new ImageIcon(filename);
        final int width = icon.getIconWidth();
        final int height = icon.getIconHeight();
        
        // Get a scaled copy.
        Image scaledMap = icon.getImage().getScaledInstance(width / 2, height / 2, Image.SCALE_AREA_AVERAGING);
        new ImageIcon(scaledMap); // Force the image to be prepared.
        
        // Draw the scaled copy into a BufferedImage so we can read/write it.
        BufferedImage image = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawImage(scaledMap, 0, 0, null);
        
        return image;
    }
    
    class Updater implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            repaint();
        }
    }
    
    public void paintComponent(Graphics g) {
        new SolarProjector().paintIlluminatedArea((Graphics2D) g, dayMap, nightMap);
    }
}
