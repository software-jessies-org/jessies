/*
 * Copyright (C) 2006, Elliott Hughes.
 * 
 * This file is part of KnowAll.
 * 
 * KnowAll is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * KnowAll is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with KnowAll; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * Shows the time in a particular time zones.
 */
public class WorldClock extends JPanel {
    private static final Color DAY_SKY_TOP = new Color(0x4b66a6);
    private static final Color DAY_SKY_BOTTOM = new Color(0x80aae0);
    private static final Color NIGHT_SKY_TOP = new Color(0x0);
    private static final Color NIGHT_SKY_BOTTOM = new Color(0x2b1b2b);
    
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final Calendar calendar;
    
    private final JLabel label = new JLabel(" ");
    
    /**
     * All valid arguments can be queried with TimeZone.getAvailableIDs.
     */
    public WorldClock(String timeZoneName) {
        super(new BorderLayout());
        add(label, BorderLayout.CENTER);
        
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneName);
        dateFormat.setTimeZone(timeZone);
        calendar = Calendar.getInstance(timeZone);
        
        if (timeZoneName.contains("America")) {
            label.setIcon(makeIcon("/usr/share/locale/l10n/us/flag.png", "http://www.cia.gov/cia/publications/factbook/flags/us-flag.gif"));
        } else {
            label.setIcon(makeIcon("/usr/share/locale/l10n/gb/flag.png", "http://www.cia.gov/cia/publications/factbook/flags/uk-flag.gif"));
        }
        
        label.setOpaque(false);
        label.setBorder(new javax.swing.border.EmptyBorder(0, 2, 0, 0));
        
        updateTime();
        new RepeatingComponentTimer(this, 1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTime();
            }
        });
    }
    
    @Override
    public void paint(Graphics g) {
        paintSky((Graphics2D) g);
        g.fillRect(0, 0, getWidth(), getHeight());
        paintChildren(g);
    }
    
    private void paintSky(Graphics2D g) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean isDaytime = (hour >= 7 && hour <= 18);
        Color topColor = isDaytime ? DAY_SKY_TOP : NIGHT_SKY_TOP;
        Color bottomColor = isDaytime ? DAY_SKY_BOTTOM : NIGHT_SKY_BOTTOM;
        g.setPaint(new GradientPaint(0, 0, topColor, getWidth(), getHeight(), bottomColor));
        label.setForeground(isDaytime ? Color.WHITE : Color.GRAY);
    }
    
    private void updateTime() {
        Date now = new Date();
        calendar.setTime(now);
        label.setText(dateFormat.format(now));
    }
    
    private static ImageIcon makeIcon(String filename, String url) {
        ImageIcon icon = new ImageIcon(filename);
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            try {
                icon = new ImageIcon(new URL(url));
            } catch (java.io.IOException ex) {
                // Give up!
                ex = ex;
            }
        }
        int preferredHeight = (int) new JLabel(" ").getPreferredSize().getHeight();
        preferredHeight = (2*preferredHeight)/3;
        if (icon.getIconHeight() > preferredHeight) {
            icon = new ImageIcon(ImageUtilities.scale(icon.getImage(), -1, preferredHeight, ImageUtilities.InterpolationHint.BICUBIC));
        }
        return icon;
    }
}
