package e.toys.world;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Shows a world map.
 */
public class WorldClock extends JFrame {
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

    private ImageIcon usIcon;
    private ImageIcon gbIcon;
    
    private GreenwichMapView mapView;

    private JLabel left;
    private JLabel right;

    public WorldClock() {
        super("World Clock");
        setContentPane(makeContentPane());
        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
//        final int x = screen.width - getWidth();
//        final int y = screen.height - getHeight();
//        setLocation(new Point(x, y));
        setVisible(true);
    }

    private ImageIcon makeIcon(String filename, String url) {
        ImageIcon icon = new ImageIcon(filename);
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            try {
                icon = new ImageIcon(new URL(url));
            } catch (java.io.IOException ex) {
                // Give up!
                ex = ex;
            }
        }
        final int preferredHeight = (int) new JLabel(" ").getPreferredSize().getHeight();
        if (icon.getIconHeight() > preferredHeight) {
            Image image = icon.getImage();
            icon = new ImageIcon(image.getScaledInstance(-1, preferredHeight, Image.SCALE_AREA_AVERAGING));
        }
        return icon;
    }

    private void initLabelsAndIcons() {
        usIcon = makeIcon("/usr/share/locale/l10n/us/flag.png",
            "http://www.cia.gov/cia/publications/factbook/flags/us-flag.gif");
        gbIcon = makeIcon("/usr/share/locale/l10n/gb/flag.png",
            "http://www.cia.gov/cia/publications/factbook/flags/uk-flag.gif");
        left = new JLabel("", usIcon, SwingConstants.LEFT);
        right = new JLabel("", gbIcon, SwingConstants.RIGHT);
        updateTimes();
    }

    private void updateTimes() {
        /*
        Date now = new Date();
        left.setText(LOCAL.format(now));
        right.setText(GB.format(now));
        */
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
        
        JLabel location = new JLabel(" ");
        mapView.setLocationLabel(location);
        controls.add(location, BorderLayout.EAST);
        
        return controls;
    }

    private JComponent makeContentPane() {
        JComponent content = new JPanel(new BorderLayout());
        mapView = new GreenwichMapView();
        content.setBorder(new EmptyBorder(0, 0, 10, 0));
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
