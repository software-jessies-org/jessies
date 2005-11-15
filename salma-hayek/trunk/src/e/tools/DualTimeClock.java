package e.tools;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import javax.swing.Timer;

/**
 * Shows the time in two different time zones. Currently hard-wired for
 * (in effect) Bracknell and San Jose.
 */
public class DualTimeClock extends JWindow implements ActionListener {
    private static final DateFormat US = makeDateFormat();
    private static final DateFormat GB = makeDateFormat();
    static {
        /*
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            System.err.println(id);
        }
        */
        GB.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        US.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    private ImageIcon usIcon;
    private ImageIcon gbIcon;

    private JLabel left;
    private JLabel right;

    public DualTimeClock() {
        setContentPane(makeContentPane());
        pack();
        updateWindow();
        setVisible(true);
        new Timer(1000, this).start();
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
            icon = new ImageIcon(ImageUtilities.scale(icon.getImage(), -1, preferredHeight, ImageUtilities.InterpolationHint.BICUBIC));
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

    public void actionPerformed(ActionEvent e) {
        updateTimes();
        updateWindow();
    }

    private void updateTimes() {
        Date now = new Date();
        left.setText(US.format(now));
        right.setText(GB.format(now));
    }

    private void updateWindow() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = screen.width - getWidth();
        final int y = screen.height - getHeight();
        setLocation(new Point(x, y));
        toFront();
    }
    
    private JComponent makeContentPane() {
        initLabelsAndIcons();
        JComponent content = new JPanel(new BorderLayout());
        content.setBorder(new javax.swing.border.EmptyBorder(2, 2, 2, 2));
        content.add(left, BorderLayout.WEST);
        content.add(new JLabel("  "), BorderLayout.CENTER);
        content.add(right, BorderLayout.EAST);
        return content;
    }

    private static DateFormat makeDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm");
    }

    public static void main(String[] args) {
        new DualTimeClock();
    }
}
