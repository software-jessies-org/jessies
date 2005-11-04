package e.debugger;

import java.awt.*;
import javax.swing.*;

import e.ptextarea.*;
import e.util.*;

/**
 * A text area that displays the output from a Process' output and error streams.
 */

public class ProcessMonitorPanel extends JPanel {
    
    private PTextArea textArea;
    
    public ProcessMonitorPanel() {
        setLayout(new BorderLayout());
        textArea = new PTextArea();
        textArea.setFont(new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12));
        textArea.showRightHandMarginAt(PTextArea.NO_MARGIN);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        add(textArea, BorderLayout.CENTER);
    }
    
    public void setProcess(final Process p) {
        textArea.setText("");
        new Thread(new Runnable() {
            public void run() {
                ProcessUtilities.readLinesFromStream(new ProcessUtilities.LineListener() {
                    public void processLine(String line) {
                        append(line);
                    }
                }, p.getInputStream());
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                ProcessUtilities.readLinesFromStream(new ProcessUtilities.LineListener() {
                    public void processLine(String line) {
                        append(line);
                    }
                }, p.getErrorStream());
            }
        }).start();
    }
    
    public void append(String line) {
        SwingUtilities.invokeLater(new AppendRunnable(line));
    }
    
    private class AppendRunnable implements Runnable {
        private String line;
        
        public AppendRunnable(String line) {
            this.line = line + "\n";
        }
        
        public void run() {
            textArea.append(line);
        }
    }
}
