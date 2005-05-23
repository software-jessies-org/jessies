package e.toys.world;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.Timer;

public class GreenwichMapView extends JComponent {
    private BufferedImage dayMap;
    private BufferedImage nightMap;
    private boolean showMeridian = false;
    private LocationListener listener;
    
    public GreenwichMapView() {
        dayMap = getBufferedImage("land_ocean_ice_2048.jpg");
        nightMap = getBufferedImage("land_ocean_ice_lights_2048.jpg");
        Dimension size = new Dimension(dayMap.getWidth(), dayMap.getHeight());
        setPreferredSize(size);
        new Timer(5 * 60 * 1000, new Updater()).start();
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                if (listener != null) {
                    int x = e.getX();
                    double y = (double) e.getY();
                    double latitude = 90.0 - 180.0 * (y / (double) dayMap.getHeight());
                    double longitude = 360.0 * ((double) x / (double) dayMap.getWidth()) - 180.0;
                    listener.locationChanged(latitude, longitude);
                }
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }
    
    public void setShowMeridian(boolean b) {
        this.showMeridian = b;
        repaint();
    }
    
    public void setLocationListener(LocationListener listener) {
        this.listener = listener;
    }
    
    private BufferedImage getBufferedImage(String filename) {
        // Load the original image.
        ImageIcon icon = new ImageIcon(System.getProperty("user.home") + "/Desktop/" + filename);
        final int width = icon.getIconWidth() / 2;
        final int height = icon.getIconHeight() / 2;
        
        // Get a scaled copy.
        Image scaledMap = icon.getImage().getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        new ImageIcon(scaledMap); // Force the image to be prepared.
        
        // Draw the scaled copy into a BufferedImage so we can read/write it.
        BufferedImage image = new BufferedImage(scaledMap.getWidth(null), scaledMap.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawImage(scaledMap, 0, 0, null);
        
        return image;
    }
    
    class Updater implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            repaint();
        }
    }
    
    public void paintComponent(Graphics oldGraphics) {
        Graphics2D g = (Graphics2D) oldGraphics;
        new SolarProjector().paintIlluminatedArea(g, dayMap, nightMap);
        if (showMeridian) {
            final int greenwich = getWidth() / 2;
            g.setColor(new Color(0, 0, 0, 128));
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 2.0f, 3.0f }, 0.0f));
            g.drawLine(greenwich, 0, greenwich, getHeight());
        }
        g.setColor(Color.RED);
        Gazetteer gazetteer = Gazetteer.getInstance();
        for (int i = 0; i < gazetteer.size(); ++i) {
            Place place = gazetteer.get(i);
            if (true || place.name.contains("Turkey")) {
                double x = (place.longitude + 180.0) * (double) dayMap.getWidth() / 360.0;
                double y = (90.0 - place.latitude) * (double) dayMap.getHeight() / 180.0;
                g.fillRect((int) x, (int) y, 1, 1);
            }
        }
    }
}
