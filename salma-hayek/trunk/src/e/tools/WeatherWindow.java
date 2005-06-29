package e.tools;

import e.util.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

public class WeatherWindow extends JFrame {
    public static void main(String[] args) {
        new WeatherWindow().setVisible(true);
    }
    
    public WeatherWindow() {
        super("Weather");
        setBackground(new Color(0.3647f, 0.6941f, 0.8863f));
        setContentPane(makeUi());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
    }
    
    private JComponent makeUi() {
        String content;
        BufferedReader in = null;
        try {
            URL url = new URL("http://www.bbc.co.uk/weather/5day.shtml?id=1808");
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line);
                buffer.append('\n');
            }
            content = buffer.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            FileUtilities.close(in);
        }
        System.out.println(content);
        
        ArrayList<DayForecast> forecast = new ArrayList<DayForecast>();
        
        Pattern dayNamePattern = Pattern.compile("class=\"weatherday\".*strong>(\\S+)<br");
        Pattern imageUrlPattern = Pattern.compile("img src=\"(\\S+\\/fiveday_sym\\/\\d+\\S+)\"");
        Pattern dayTemperaturePattern = Pattern.compile("class=\"temptxt\"><strong>(\\d+)<.*Day ");
        Pattern nightTemperaturePattern = Pattern.compile("class=\"temptxt\">.*<strong>(\\d+)<.*Night ");
        Matcher matcher;
        String dayName = null;
        String imageUrl = null;
        int dayTemperatureC = 0;
        for (String line : content.split("\n")) {
            if ((matcher = dayNamePattern.matcher(line)).find()) {
                dayName = matcher.group(1);
            } else if ((matcher = imageUrlPattern.matcher(line)).find()) {
                imageUrl = "http://www.bbc.co.uk" + matcher.group(1);
            } else if ((matcher = dayTemperaturePattern.matcher(line)).find()) {
                dayTemperatureC = Integer.parseInt(matcher.group(1));
            } else if ((matcher = nightTemperaturePattern.matcher(line)).find()) {
                int nightTemperatureC = Integer.parseInt(matcher.group(1));
                DayForecast dayForecast = new DayForecast(dayName, imageUrl, dayTemperatureC, nightTemperatureC);
                System.out.println(dayForecast);
                forecast.add(dayForecast);
            }
        }
        
        /*
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, new String[] { "/Users/elliotth/bbc-forecast.rb" }, lines, errors);
        
        for (String line : lines) {
            Matcher matcher = Pattern.compile("(\\S+)\t(\\S+)\t(\\d+)\t(\\d+)").matcher(line);
            if (matcher.matches()) {
                String day = matcher.group(1);
                String imageUrl = matcher.group(2);
                int dayTemperatureC = Integer.parseInt(matcher.group(3));
                int nightTemperatureC = Integer.parseInt(matcher.group(4));
                forecast.add(new DayForecast(day, imageUrl, dayTemperatureC, nightTemperatureC));
            }
        }
        */
        
        JPanel result = new JPanel(new FlowLayout());
        for (DayForecast day : forecast) {
            result.add(day);
        }
        return result;
    }
    
    private static class DayForecast extends JPanel {
        private String day;
        private ImageIcon icon;
        private int dayTemperatureC;
        private int nightTemperatureC;
        
        public DayForecast(String day, String imageUrl, int dayTemperatureC, int nightTemperatureC) {
            super(new BorderLayout());
            this.day = day;
            try {
                this.icon = new ImageIcon(new URL(imageUrl));
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
            this.dayTemperatureC = dayTemperatureC;
            this.nightTemperatureC = nightTemperatureC;
            initUi();
        }
        
        private JLabel makeLabel(String text) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            //label.setBackground(new Color(0.3647f, 0.6941f, 0.8863f));
            //label.setOpaque(true);
            return label;
        }
        
        private void initUi() {
            add(makeLabel(day), BorderLayout.NORTH);
            add(new JLabel(icon), BorderLayout.CENTER);
            final String degreesC = "\u00b0C";
            add(makeLabel(dayTemperatureC + degreesC + " / " + nightTemperatureC + degreesC), BorderLayout.SOUTH);
        }
        
        public String toString() {
            return "DayForecast[day=" + day + ",icon=" + icon + ",dayTemperatureC=" + dayTemperatureC + ",nightTemperatureC=" + nightTemperatureC + "]";
        }
    }
}
