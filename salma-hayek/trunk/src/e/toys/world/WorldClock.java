package e.toys.world;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.swing.*;

/**
 * Shows a world map.
 * 
 * TODO:
 * search on place name.
 * search on country code.
 * search on TLD (not always same as country code).
 * link to CIA world factbook.
 * show times, or remove "Clock" from the name?
 */
public class WorldClock extends JFrame implements LocationListener {
    private static final DateFormat LOCAL = makeDateFormat();
    private static final DateFormat GB = makeDateFormat();
    static {
        /*
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            System.err.println(id);
        }
        */
        GB.setTimeZone(TimeZone.getTimeZone("Europe/London"));
    }

    private GreenwichMapView mapView;
    private JLabel location;

    public WorldClock() {
        super("World Clock");
        setContentPane(makeContentPane());
        pack();
        centerFrameOnScreen();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
    
    public void locationChanged(double latitude, double longitude) {
        Gazetteer gazetteer = Gazetteer.getInstance();
        String bestName = "(middle of nowhere)";
        Place bestPlace = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < gazetteer.size(); ++i) {
            Place town = gazetteer.get(i);
            double distance = town.distanceFrom(latitude, longitude);
            if (distance < bestDistance && distance < 3.0) {
                bestName = town.name;
                bestPlace = town;
                bestDistance = distance;
            }
        }
        String text = bestName + " " + bestDistance;
        if (bestPlace != null) {
            text += " " + bestPlace.latitude + " " + bestPlace.longitude;
        }
        text += " lat=" + latitude + " lon=" + longitude;
        location.setText(text);
        //location.setText(bestName + " " + bestDistance + " " + bestPlace.latitude + " " + bestPlace.longitude + " lat=" + fromDegrees(latitude, 'S', 'N') + " lon=" + fromDegrees(longitude, 'W', 'E'));
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
    
    private void centerFrameOnScreen() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screen.width - getWidth()) / 2;
        final int y = (screen.height - getHeight()) / 2;
        setLocation(new Point(x, y));
    }

    private JComponent makeControls() {
        JPanel controls = new JPanel(new BorderLayout());
        
        JCheckBox showMeridian = new JCheckBox("Show Greenwich Meridian");
        showMeridian.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                mapView.setShowMeridian(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        controls.add(showMeridian, BorderLayout.WEST);
        
        location = new JLabel(" ");
        controls.add(location, BorderLayout.EAST);
        
        return controls;
    }

    private JComponent makeContentPane() {
        JComponent content = new JPanel(new BorderLayout());
        mapView = new GreenwichMapView();
        mapView.setLocationListener(this);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        content.add(mapView, BorderLayout.CENTER);
        content.add(makeControls(), BorderLayout.SOUTH);
        return content;
    }

    private static DateFormat makeDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm");
    }

    public static void main(String[] args) {
        new WorldClock();
    }
}
