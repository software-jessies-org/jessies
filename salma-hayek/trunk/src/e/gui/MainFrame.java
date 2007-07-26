package e.gui;

import e.util.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        this("");
    }
    
    public MainFrame(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }
    
    @Override
    public void setVisible(boolean newVisibility) {
        if (newVisibility) {
            setLocationRelativeTo(null);
            JFrameUtilities.constrainToScreen(this);
            JFrameUtilities.setFrameIcon(this);
            super.setVisible(true);
            GuiUtilities.finishGnomeStartup();
        } else {
            super.setVisible(false);
            dispose();
        }
    }
}
