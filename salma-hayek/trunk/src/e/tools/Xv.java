package e.tools;

import e.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

public class Xv extends JFrame {
    
    public Xv(String filename) throws IOException {
        super(filename);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        BufferedImage image = ImageIO.read(new File(filename));
        Log.warn("Opened '" + filename + "' (" + image.getWidth() + "x" + image.getHeight() + ").");
        
        setContentPane(new JScrollPane(new JLabel(new ImageIcon(image))));
        
        Dimension displaySize = getGraphicsConfiguration().getBounds().getSize();
        if (image.getWidth() > displaySize.width || image.getHeight() > displaySize.height) {
            setSize(displaySize.width / 2, displaySize.height / 2);
        } else {
            pack();
        }
        setLocationByPlatform(true);
        setVisible(true);
    }

    public static void main(String[] arguments) throws IOException {
        Log.setApplicationName("Xv");
        GuiUtilities.initLookAndFeel();
        for (String argument : arguments) {
            Xv xv = new Xv(argument);
        }
    }
}
