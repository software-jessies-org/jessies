package org.jessies.vod;

import e.util.*;
import quicktime.*;
import quicktime.app.view.*;
import quicktime.io.*;
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
    private double fps;
    private String[] subtitles;
    
    public VideoOnDemand(String movieName) {
        // FIXME: read the "box" title from a .title text file?
        super(FileUtilities.getUserFriendlyName(movieName));
        setLayout(new BorderLayout());
        
        try {
            this.movie = movieFromFilename(movieName);
            
            initFramesPerSecond();
            initSubtitles(movieName);
            
            QTComponent movieView = QTFactory.makeQTComponent(movie);
            getContentPane().add(movieView.asComponent(), BorderLayout.CENTER);
            getContentPane().add(makeSearchField(), BorderLayout.SOUTH);
            pack();
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
                String searchTerm = field.getText();
                // FIXME: this is way too primitive! Searching shouldn't be line-based.
                for (String line : subtitles) {
                    // FIXME: this is way too primitive! Searching should allow regular expressions and be case-insensitive (since subtitles are ALL CAPS).
                    if (line.contains(searchTerm)) {
                        Matcher matcher = Pattern.compile("^\\{(\\d+)\\}\\{\\d+\\}(.*)$").matcher(line);
                        if (matcher.matches()) {
                            showCurrentTime();
                            long startFrame = Long.parseLong(matcher.group(1));
                            String text = matcher.group(2);
                            int newTime = (int) ((double) startFrame / fps);
                            System.out.println(startFrame + " (" + newTime + ") : " + text);
                            try {
                                MovieController movieController = new MovieController(movie);
                                movieController.goToTime(new TimeRecord(1, (int) ((double) startFrame / fps)));
                                showCurrentTime();
                                field.selectAll();
                                return;
                            } catch (QTException ex) {
                                ex.printStackTrace();
                            }
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
    
    private void showCurrentTime() {
        try {
            int timeInSeconds = movie.getTime()/movie.getTimeScale();
            System.out.println("Currently at time " + timeInSeconds + "s (" + TimeUtilities.durationToIsoString(timeInSeconds * 1000) + ").");
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
