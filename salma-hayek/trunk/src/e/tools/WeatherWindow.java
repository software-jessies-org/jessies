package e.tools;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.swing.*;

public class WeatherWindow extends MainFrame {
    private static final Color BACKGROUND_COLOR = new Color(0.3647f, 0.6941f, 0.8863f);
    private static final Font DAY_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font TEMPERATURE_FONT = new Font("SansSerif", Font.PLAIN, 10);
    
    public static void main(String[] args) {
        new WeatherWindow().setVisible(true);
    }
    
    public WeatherWindow() {
        super("Weather");
        setBackground(BACKGROUND_COLOR);
        setContentPane(makeUi());
        pack();
    }
    
    private JComponent makeUi() {
        // "http://www.bbc.co.uk/weather/5day.shtml?world=4079" // Basel, CH.
        ArrayList<DayForecast> forecast = collectForecastFor("http://www.bbc.co.uk/weather/5day.shtml?world=0306"); // San Jose, CA, US.
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
        result.setBackground(BACKGROUND_COLOR);
        for (DayForecast day : forecast) {
            result.add(day);
        }
        return result;
    }
    
    private ArrayList<DayForecast> collectForecastFor(String location) {
        ArrayList<DayForecast> forecast = new ArrayList<DayForecast>();
        
        String content;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new URL(location).openStream()));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line);
                buffer.append('\n');
            }
            content = buffer.toString();
        } catch (IOException ex) {
            Log.warn("Failed to read forecast.", ex);
            return null;
        } finally {
            FileUtilities.close(in);
        }
        
        Pattern cityNamePattern = Pattern.compile("^\\s+<title>BBC.* Forecast in .* for (\\S+),");
        Pattern dayNamePattern = Pattern.compile("class=\"weatherday\".*strong>(\\S+)<br");
        Pattern imageUrlPattern = Pattern.compile("img src=\"(\\S+\\/fiveday_sym\\/\\d+\\S+)\"");
        Pattern dayTemperaturePattern = Pattern.compile("class=\"temptxt\"><strong>(\\d+)<.*Day ");
        Pattern nightTemperaturePattern = Pattern.compile("class=\"temptxt\">.*<strong>(\\d+)<.*Night ");
        String dayName = null;
        String imageUrl = null;
        int dayTemperatureC = 0;
        Matcher matcher;
        for (String line : content.split("\n")) {
            if ((matcher = cityNamePattern.matcher(line)).find()) {
                setTitle(matcher.group(1) + " 5 Day Forecast");
            } else if ((matcher = dayNamePattern.matcher(line)).find()) {
                dayName = matcher.group(1);
            } else if ((matcher = imageUrlPattern.matcher(line)).find()) {
                imageUrl = "http://www.bbc.co.uk" + matcher.group(1);
            } else if ((matcher = dayTemperaturePattern.matcher(line)).find()) {
                dayTemperatureC = Integer.parseInt(matcher.group(1));
            } else if ((matcher = nightTemperaturePattern.matcher(line)).find()) {
                int nightTemperatureC = Integer.parseInt(matcher.group(1));
                DayForecast dayForecast = new DayForecast(dayName, imageUrl, dayTemperatureC, nightTemperatureC);
                forecast.add(dayForecast);
            }
        }
        
        if (forecast.size() != 5) {
            System.out.println(content);
            for (DayForecast dayForecast : forecast) {
                System.out.println(dayForecast);
            }
        }
        
        return forecast;
    }
    
    private static class DayForecast extends JPanel {
        private String day;
        private ImageIcon icon;
        private int dayTemperatureC;
        private int nightTemperatureC;
        
        public DayForecast(String day, String imageUrl, int dayTemperatureC, int nightTemperatureC) {
            super(new BorderLayout());
            setBackground(BACKGROUND_COLOR);
            this.day = day;
            try {
                BufferedImage image = ImageIO.read(new URL(imageUrl));
                BufferedImage scaledImage = new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5), AffineTransformOp.TYPE_BILINEAR).filter(image, null);
                this.icon = new ImageIcon(scaledImage);
            } catch (IOException ex) {
                Log.warn("Failed to read image \"" + imageUrl + "\".", ex);
            }
            this.dayTemperatureC = dayTemperatureC;
            this.nightTemperatureC = nightTemperatureC;
            initUi();
        }
        
        private JLabel makeLabel(String text, Font font) {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setFont(font);
            return label;
        }
        
        private void initUi() {
            add(makeLabel(day, DAY_FONT), BorderLayout.NORTH);
            add(new JLabel(icon), BorderLayout.CENTER);
            final String degreesC = "\u00b0C";
            add(makeLabel(dayTemperatureC + degreesC + " / " + nightTemperatureC + degreesC, TEMPERATURE_FONT), BorderLayout.SOUTH);
        }
        
        public String toString() {
            return "DayForecast[day=" + day + ",icon=" + icon + ",dayTemperatureC=" + dayTemperatureC + ",nightTemperatureC=" + nightTemperatureC + "]";
        }
    }
}
