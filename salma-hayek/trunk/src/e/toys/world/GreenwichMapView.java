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
    private JLabel label;
    
    public GreenwichMapView() {
        dayMap = getBufferedImage("land_ocean_ice_2048.jpg");
        nightMap = getBufferedImage("land_ocean_ice_lights_2048.jpg");
        Dimension size = new Dimension(dayMap.getWidth(), dayMap.getHeight());
        setPreferredSize(size);
        new Timer(5 * 60 * 1000, new Updater()).start();
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int x = e.getX();
                double y = (double) e.getY();
                double latitude = 90.0 - 180.0 * (y / (double) dayMap.getHeight());
                double longitude = 360.0 * ((double) x / (double) dayMap.getWidth()) - 180.0;
                if (label != null) {
                    label.setText("lat=" + fromDegrees(latitude, 'S', 'N') + " lon=" + fromDegrees(longitude, 'W', 'E'));
                }
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }
    
    public String fromDegrees(double angle, char negative, char positive) {
        String result = "";
        boolean usePositive = (angle >= 0);
        angle = Math.abs(angle);
        int degrees = (int) angle;
        result += degrees + "'";
        angle -= degrees;
        int minutes = (int) (60 * angle);
        result += minutes + "\"";
        result += usePositive ? positive : negative;
        return result;
    }
    
    public void setShowMeridian(boolean b) {
        this.showMeridian = b;
        repaint();
    }
    
    public void setLocationLabel(JLabel label) {
        this.label = label;
    }
    
    private BufferedImage getBufferedImage(String filename) {
        // Load the original image.
        ImageIcon icon = new ImageIcon(System.getProperty("user.home") + "/Desktop/" + filename);
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
    
    public void paintComponent(Graphics oldGraphics) {
        Graphics2D g = (Graphics2D) oldGraphics;
        new SolarProjector().paintIlluminatedArea(g, dayMap, nightMap);
        if (showMeridian) {
            final int greenwich = getWidth() / 2;
            g.setColor(new Color(0, 0, 0, 128));
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 2.0f, 3.0f }, 0.0f));
            g.drawLine(greenwich, 0, greenwich, getHeight());
        }
    }
}
