package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import e.util.*;

public class TelnetAction extends AbstractAction {
    public TelnetAction() {
        super("Telnet...");
    }
    
    public void actionPerformed(ActionEvent e) {
        String defaultHost = Parameters.getParameter("telnet.defaultHost", "localhost");
        String hostname = JOptionPane.showInputDialog(Edit.getFrame(), "Hostname:", defaultHost);
        if (hostname != null) {
            Edit.openFile("telnet://" + hostname);
        }
    }
}
