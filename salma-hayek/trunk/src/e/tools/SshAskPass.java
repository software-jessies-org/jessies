package e.tools;

import e.forms.*;
import javax.swing.*;

/**
 * A Java replacement for ssh-askpass. This lets something like SCM offer a
 * sensible login dialog rather than have a prompt appear on the terminal it
 * was started from. Unfortunately, I haven't been able to get Mac OS' ssh(1)
 * to use this. I'm checking it in so I can try it on Linux (where I've seen
 * other ssh-askpass replacements work).
 */
public class SshAskPass extends JFrame {
    private static final int EXIT_ACCEPT = 0;
    private static final int EXIT_CANCEL = 1;
    private static final int EXIT_ERROR = 3;
    private static final int EXIT_TIMEOUT = 4;
    
    private JPasswordField passwordField = new JPasswordField(20);
    
    public static void main(String[] args) {
        new SshAskPass();
    }
    
    private SshAskPass() {
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Password:", passwordField);
        boolean accepted = FormDialog.show(null, "SSH Password Dialog", formPanel, "OK");
        if (accepted) {
            System.out.println(new String(passwordField.getPassword()));
            System.exit(EXIT_ACCEPT);
        }
        System.exit(EXIT_CANCEL);
    }
}
