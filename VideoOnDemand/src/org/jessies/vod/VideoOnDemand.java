package org.jessies.vod;

import e.util.*;
import quicktime.*;
import quicktime.app.view.*;
import quicktime.io.*;
import quicktime.qd.*;
import quicktime.std.*;
import quicktime.std.clocks.*;
import quicktime.std.movies.*;
import quicktime.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;

public class VideoOnDemand extends JFrame {
    private Movie movie;
    private MovieController movieController;
    private double fps;
    private String[] subtitles;
    
    public VideoOnDemand(String movieName) {
        // FIXME: read the "box" title from a .title text file?
        super(FileUtilities.getUserFriendlyName(movieName));
        setLayout(new BorderLayout());
        
        try {
            this.movie = movieFromFilename(movieName);
            this.movieController = new MovieController(movie);
            
            initFramesPerSecond();
            initSubtitles(movieName);
            
            Component movieView = QTFactory.makeQTComponent(movie).asComponent();
            getContentPane().add(movieView, BorderLayout.CENTER);
            // FIXME: this should be a dialog.
            getContentPane().add(makeSearchField(), BorderLayout.SOUTH);
            
            QDRect movieRect = movie.getNaturalBoundsRect();
            Dimension movieSize = new Dimension(movieRect.getWidth(), movieRect.getHeight());
            setSize(movieSize);
            setLocationRelativeTo(null);
            setVisible(true);
            
            movie.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private JComponent makeSearchField() {
        final JTextField field = new JTextField(80);
        field.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String searchTerm = field.getText().toLowerCase();
                // FIXME: this is way too primitive! Searching shouldn't be line-based.
                for (String line : subtitles) {
                    // FIXME: this is way too primitive! Searching should allow regular expressions and be case-insensitive (since subtitles are often ALL CAPS).
                    if (line.toLowerCase().contains(searchTerm)) {
                        Matcher matcher = Pattern.compile("^\\{(\\d+)\\}\\{\\d+\\}(.*)$").matcher(line);
                        if (matcher.matches()) {
                            showCurrentTime();
                            int startFrame = Integer.parseInt(matcher.group(1));
                            String text = matcher.group(2);
                            goToFrame(startFrame);
                            showCurrentTime();
                            field.selectAll();
                            return;
                        } else {
                            System.err.println("Couldn't parse .sub line:");
                            System.err.println(line);
                        }
                    }
                }
            }
        });
        return field;
    }
    
    private void goToFrame(int frame) {
        try {
            movieController.goToTime(new TimeRecord(1, (int) ((double) frame / fps)));
        } catch (QTException ex) {
            ex.printStackTrace();
        }
    }
    
    private void showCurrentTime() {
        try {
            int timeInSeconds = movie.getTime()/movie.getTimeScale();
            System.out.println("Currently at time " + timeInSeconds + "s (" + TimeUtilities.msToIsoString(timeInSeconds * 1000) + ").");
        } catch (QTException ex) {
            ex.printStackTrace();
        }
    }
    
    private Movie movieFromFilename(String movieName) throws QTException {
        // FIXME: support other formats than just .mp4.
        return Movie.fromFile(OpenMovieFile.asRead(new QTFile(FileUtilities.fileFromString(movieName + ".mp4"))));
    }
    
    private void initSubtitles(String movieName) {
        // FIXME: support .srt as well as .sub, or write a conversion utility.
        this.subtitles = StringUtilities.readLinesFromFile(movieName + ".sub");
    }
    
    private void initFramesPerSecond() throws QTException {
        // Copied from "QuickTime Amateur".
        Track videoTrack = movie.getIndTrackType(1, StdQTConstants.visualMediaCharacteristic, StdQTConstants.movieTrackCharacteristic);
        double units = videoTrack.getMedia().getDuration();
        double frames = videoTrack.getMedia().getSampleCount();
        double unitsPerSecond = videoTrack.getMedia().getTimeScale();
        double expectedRate = unitsPerSecond * frames / units;
        // FIXME: the movie doesn't necessarily play at a constant frame rate.
        this.fps = expectedRate;
    }
    
    public static void main(String[] arguments) throws Exception {
        QTSession.open();
        Log.warn("QuickTime version " + QTSession.getMajorVersion() + "." + QTSession.getMinorVersion());
        // FIXME: scan ~/Movies for .sub and .srt files; only load the .mp4s on demand.
        for (String filename : arguments) {
            VideoOnDemand videoOnDemand = new VideoOnDemand(filename);
            videoOnDemand.setVisible(true);
        }
        // FIXME: call QTSession.close();
    }
}
