package e.toys.world;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

/**
 * Projects sunlight onto a world map.
 * 
 * Use this for comparison:
 * http://www.fourmilab.ch/cgi-bin/uncgi/Earth
 * (also http://www.cru.uea.ac.uk/~timo/sunclock.htm)
 * 
 * The KDE code on which this is all based is here:
 * http://webcvs.kde.org/cgi-bin/cvsweb.cgi/kdetoys/kworldwatch/
 * 
 * Original copyright notices preserved at the bottom of this file.
 * 
 * @author Elliott Hughes
 * @author John Mackin
 * @author Stephen Martin
 * @author John Walker
 */
public class SolarProjector {
    /* Circle segments for terminator */
    private static final int TERMINC = 100;
    
    private static Calendar utcCalendarForDate(Date date) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(date);
        return c;
    }
    
    public void paintIlluminatedArea(Graphics2D finalGraphics, final BufferedImage dayMap, final BufferedImage nightMap) {
        final int width = dayMap.getWidth();
        final int height = dayMap.getHeight();
        
        Date now = new Date();
        Calendar c = utcCalendarForDate(now);
        double jt = jtime(c);
        // FIXME: display this in the GUI.
        //System.err.println("Julian:" + jt);
        
        SunPosition sun = sunpos(jt, false);
        short[] widths = calculateIlluminatedArea(width, height, sun.dec);
        
        long startTime = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.drawImage(nightMap, 0, 0, null);
        int sec = c.get(Calendar.HOUR_OF_DAY)*60*60 + c.get(Calendar.MINUTE)*60 + c.get(Calendar.SECOND);
        int gmtPosition = width * sec / 86400; // note: greenwich is in the middle!
        int middle = width - gmtPosition;
        for (int y = 0; y < height; ++y) {
            if (widths[y] != -1) {
                final int start = middle - widths[y];
                final int stop = middle + widths[y];
                if (start < 0) {
                    copyHorizontalLine(image, dayMap, 0, stop, y);
                    copyHorizontalLine(image, dayMap, width + start, width, y);
                } else if (stop > width) {
                    copyHorizontalLine(image, dayMap, start, width, y);
                    copyHorizontalLine(image, dayMap, 0, stop - width, y);
                } else {
                    copyHorizontalLine(image, dayMap, start, stop, y);
                }
            }
        }
        //System.err.println("lines: " + lineCount);
        //System.err.println("time: " + (System.currentTimeMillis() - startTime));
        
        finalGraphics.drawImage(image, 0, 0, null);
    }
    
    private int lineCount = 0;
    
    private void copyHorizontalLine(BufferedImage dst, BufferedImage src, int fromX, int toX, int y) {
        final int width = toX - fromX;
        int[] pixels = src.getRGB(fromX, y, width, 1, null, 0, width);
        dst.setRGB(fromX, y, width, 1, pixels, 0, width);
        ++lineCount;
    }
    
    private short[] calculateIlluminatedArea(int xDots, int yDots, double declination) {
        /* Clear unoccupied cells in width table */
        short[] widths = new short[yDots];
        Arrays.fill(widths, (short) -1);
        
        /* Build transformation for declination */
        double s = Math.sin(-Math.toRadians(declination));
        double c = Math.cos(-Math.toRadians(declination));
        
        /* Increment over a semicircle of illumination */
        boolean firstTime = true;
        int ilat, ilon;
        int lilat = 0;
        int lilon = 0;
        for (double th = -(Math.PI / 2); th <= Math.PI / 2 + 0.001; th += Math.PI / TERMINC) {
            
            /* Transform the point through the declination rotation. */
            double x = -s * Math.sin(th);
            double y = Math.cos(th);
            double z = c * Math.sin(th);
            
            /* Transform the resulting co-ordinate through the map projection to obtain screen co-ordinates. */
            double lon = (y == 0 && x == 0) ? 0.0 : Math.toDegrees(Math.atan2(y, x));
            double lat = Math.toDegrees(Math.asin(z));
            
            ilat = (int) (yDots - (lat + 90) * (yDots / 180.0));
            ilon = (int) (lon * (xDots / 360.0));
            
            if (firstTime) {
                /* First time.  Just save start co-ordinate. */
                lilon = ilon;
                lilat = ilat;
                firstTime = false;
            } else {
                /* Trace out the line and set the width table. */
                if (lilat == ilat) {
                    widths[(yDots - 1) - ilat] = (short) (ilon == 0 ? 1 : ilon);
                } else {
                    double m = ((double) (ilon - lilon)) / (ilat - lilat);
                    for (int i = lilat; i != ilat; i += Math.signum(ilat - lilat)) {
                        int xt = lilon + (int) Math.floor((m * (i - lilat)) + 0.5);
                        widths[(yDots - 1) - i] = (short) (xt == 0 ? 1 : xt);
                    }
                }
                lilon = ilon;
                lilat = ilat;
            }
        }
        
        /* Now tweak the widths to generate full illumination for the correct pole. */
        if (declination < 0.0) {
            ilat = yDots - 1;
            lilat = -1;
        } else {
            ilat = 0;
            lilat = 1;
        }
        
        for (int i = ilat; i != yDots / 2; i += lilat) {
            if (widths[i] != -1) {
                while (true) {
                    widths[i] = (short) (xDots / 2);
                    if (i == ilat) {
                        break;
                    }
                    i -= lilat;
                }
                break;
            }
        }
        
        return widths;
    }
    
    /**
     * Calculates Greenwich Mean Siderial Time for a given
     * instant expressed as a Julian date and fraction.
     */
    private double greenwichMeanSiderialTime(double jd) {
        /* Time, in Julian centuries of 36525 ephemeris days,
        measured from the epoch 1900 January 0.5 ET. */
        
        double t = ((Math.floor(jd + 0.5) - 0.5) - 2415020.0) / 36525.0;
        double theta0 = 6.6460656 + 2400.051262 * t + 0.00002581 * t * t;
        t = (jd + 0.5) - (Math.floor(jd + 0.5));
        theta0 += (t * 24.0) * 1.002737908;
        theta0 = (theta0 - 24.0 * (Math.floor(theta0 / 24.0)));
        
        return theta0;
    }
    
    /** Solve the equation of Kepler.  */
    private double kepler(double m, double ecc) {
        final double EPSILON = 1E-6;
        double e = m = Math.toRadians(m);
        double delta;
        do {
            delta = e - ecc * Math.sin(e) - m;
            e -= delta / (1 - ecc * Math.cos(e));
        } while (Math.abs(delta) > EPSILON);
        return e;
    }
    
    static class SunPosition {
        double ra;
        double dec;
        double rv;
        double slong;
    }
    
    /**
     * Calculates the position of the Sun.  JD is the Julian  date
     * of  the  instant for which the position is desired and
     * Apparent should be true if  the  apparent  position
     * (corrected  for  nutation  and aberration) is desired.
     * 
     * The Sun's co-ordinates are returned  in  RA  and  DEC,
     * both  specified  in degrees (divide RA by 15 to obtain
     * hours).  The radius vector to the Sun in  astronomical
     * units  is returned in RV and the Sun's longitude (true
     * or apparent, as desired) is  returned  as  degrees  in
     * SLONG.
     */
    private SunPosition sunpos(double jd, boolean apparent) {
        double t, t2, t3, l, m, e, ea, v, theta, omega, eps;
        
        /* Time, in Julian centuries of 36525 ephemeris days,
        measured from the epoch 1900 January 0.5 ET. */
        
        t = (jd - 2415020.0) / 36525.0;
        t2 = t * t;
        t3 = t2 * t;
        
        /* Geometric mean longitude of the Sun, referred to the
        mean equinox of the date. */
        
        l = fixangle(279.69668 + 36000.76892 * t + 0.0003025 * t2);
        
        /* Sun's mean anomaly. */
        
        m = fixangle(358.47583 + 35999.04975*t - 0.000150*t2 - 0.0000033*t3);
        
        /* Eccentricity of the Earth's orbit. */
        
        e = 0.01675104 - 0.0000418 * t - 0.000000126 * t2;
        
        /* Eccentric anomaly. */
        
        ea = kepler(m, e);
        
        /* True anomaly */
        
        v = fixangle(2 * Math.toDegrees(Math.atan(Math.sqrt((1 + e) / (1 - e))  * Math.tan(ea / 2))));
        
        /* Sun's true longitude. */
        
        theta = l + v - m;
        
        /* Obliquity of the ecliptic. */
        
        eps = 23.452294 - 0.0130125 * t - 0.00000164 * t2 + 0.000000503 * t3;
        
        /* Corrections for Sun's apparent longitude, if desired. */
        
        if (apparent) {
            omega = fixangle(259.18 - 1934.142 * t);
            theta = theta - 0.00569 - 0.00479 * Math.sin(Math.toRadians(omega));
            eps += 0.00256 * Math.cos(Math.toRadians(omega));
        }
        
        /* Return Sun's longitude and radius vector */
        SunPosition result = new SunPosition();
        result.slong = theta;
        result.rv = (1.0000002 * (1 - e * e)) / (1 + e * Math.cos(Math.toRadians(v)));
        /* Determine solar co-ordinates. */
        result.ra = fixangle(Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(eps)) * Math.sin(Math.toRadians(theta)), Math.cos(Math.toRadians(theta)))));
        result.dec = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(eps)) * Math.sin(Math.toRadians(theta))));
        return result;
    }
    
    /** Converts internal GMT date and time to Julian day and fraction.  */
    private long jdate(Calendar c) {
        long y = c.get(Calendar.YEAR);
        long m = c.get(Calendar.MONTH) + 1;
        
        if (m > 2) {
            m = m - 3;
        } else {
            m = m + 9;
            y--;
        }
        long century = y / 100L;              /* Compute century */
        y -= 100L * century;
        return c.get(Calendar.DAY_OF_MONTH) + (century * 146097L) / 4 + (y * 1461L) / 4 + (m * 153L + 2) / 5 + 1721119L;
    }
    
    /**
     * Converts internal GMT  date  and  time  to  astronomical
     * Julian  time  (i.e.   Julian  date  plus  day fraction,
     * expressed as a double).
     */
    private double jtime(Calendar c) {
        return (jdate(c) - 0.5) + ((long) c.get(Calendar.SECOND) + 60L * (c.get(Calendar.MINUTE) + 60L * c.get(Calendar.HOUR_OF_DAY))) / 86400.0;
    }
    
    private static double fixangle(double a) {
        return ((a) - 360.0 * (Math.floor((a) / 360.0)));
    }
}

/*
 * Sun clock.  X11 version by John Mackin.
 *
 * This program was derived from, and is still in part identical with, the
 * Suntools Sun clock program whose author's comment appears immediately
 * below.  Please preserve both notices.
 *
 * The X11R3/4 version of this program was written by John Mackin, at the
 * Basser Department of Computer Science, University of Sydney, Sydney,
 * New South Wales, Australia; <john@cs.su.oz.AU>.  This program, like
 * the one it was derived from, is in the public domain: `Love is the
 * law, love under will.'
 */

/*

Sun clock

Designed and implemented by John Walker in November of 1988.

Version for the Sun Workstation.

The algorithm used to calculate the position of the Sun is given in
Chapter 18 of:

"Astronomical  Formulae for Calculators" by Jean Meeus, Third Edition,
Richmond: Willmann-Bell, 1985.  This book can be obtained from:

Willmann-Bell
P.O. Box 35025
Richmond, VA  23235
USA
Phone: (804) 320-7016

This program was written by:

John Walker
Autodesk, Inc.
2320 Marinship Way
Sausalito, CA  94965
USA
Fax:   (415) 389-9418
Voice: (415) 332-2344 Ext. 2829
Usenet: {sun,well,uunet}!acad!kelvin
or: kelvin@acad.uu.net

modified for interactive maps by

Stephen Martin
Fujitsu Systems Business of Canada
smartin@fujitsu.ca

This  program is in the public domain: "Do what thou wilt shall be the
whole of the law".  I'd appreciate  receiving  any  bug  fixes  and/or
enhancements,  which  I'll  incorporate  in  future  versions  of  the
program.  Please leave the original attribution information intact  so
that credit and blame may be properly apportioned.

Revision history:

1.0  12/21/89  Initial version.
8/24/89  Finally got around to submitting.

1.1   8/31/94  Version with interactive map.
1.2  10/12/94  Fixes for HP and Solaris, new icon bitmap
1.3  11/01/94  Timezone now shown in icon
1.4  03/29/98  Fixed city drawing, added icon animation

*/
