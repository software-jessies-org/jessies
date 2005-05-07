package e.tools;

import javax.swing.*;
import e.util.*;

public class Xv extends JFrame {
    
    public Xv(String filename) {
        super(filename);
        Log.warn("Opening '" + filename + "'.");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setContentPane(new JLabel(new ImageIcon(filename)));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] arguments) {
        Log.setApplicationName("Xv");
        GuiUtilities.initLookAndFeel();
        for (int i = 0; i < arguments.length; ++i) {
            Xv xv = new Xv(arguments[i]);
        }
    }
}
