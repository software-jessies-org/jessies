package e.tools;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

public class Xv extends MainFrame {
    
    public Xv(String filename) throws IOException {
        super(filename);
        fillWithContent(filename);
        setVisible(true);
    }
    
    private void fillWithContent(String filename) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filename));
            if (image == null) {
                setErrorContent("Couldn't open \"" + filename + "\"");
                return;
            }
        } catch (Exception ex) {
            setErrorContent("Error opening \"" + filename + "\" (" + ex.getMessage() + ")");
            return;
        }
        
        Log.warn("Opened '" + filename + "' (" + image.getWidth() + "x" + image.getHeight() + ").");
        
        setContentPane(new JScrollPane(new JLabel(new ImageIcon(image))));
        
        Dimension displaySize = getGraphicsConfiguration().getBounds().getSize();
        if (image.getWidth() > displaySize.width || image.getHeight() > displaySize.height) {
            setSize(displaySize.width / 2, displaySize.height / 2);
        } else {
            pack();
        }
    }
    
    private void setErrorContent(String text) {
        setContentPane(new JLabel(text, SwingConstants.CENTER));
        pack();
    }
    
    public static void main(String[] arguments) throws IOException {
        GuiUtilities.initLookAndFeel();
        for (String argument : arguments) {
            Xv xv = new Xv(argument);
        }
    }
}
