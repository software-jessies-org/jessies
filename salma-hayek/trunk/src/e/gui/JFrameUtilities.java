package e.gui;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import javax.swing.*;

public class JFrameUtilities {
    private static final Image FRAME_ICON = new ImageIcon(System.getProperty("org.jessies.frameIcon")).getImage();
    
    public static void setFrameIcon(JFrame frame) {
        frame.setIconImage(FRAME_ICON);
    }
    
    public static void showTextWindow(String title, String content) {
        PTextArea textArea = new PTextArea(40, 80);
        textArea.setFont(new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 10));
        textArea.setText(content);
        showScrollableContentWindow(title, textArea);
    }
    
    public static void showScrollableContentWindow(String title, JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        
        JFrame frame = new JFrame(title);
        setFrameIcon(frame);
        frame.setContentPane(scrollPane);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }
    
    private JFrameUtilities() {
    }
}
