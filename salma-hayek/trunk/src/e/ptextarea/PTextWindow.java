package e.ptextarea;


import java.awt.*;
import javax.swing.*;

public class PTextWindow {
    private PTextWindow() { }
    
    public static void main(String[] filenames) {
        if (filenames.length == 0) {
            System.err.println("Syntax: PTextWindow <filename>...");
            System.exit(1);
        }
        for (int i = 0; i < filenames.length; ++i) {
            open(filenames[i]);
        }
    }
    
    private static void open(final String filename) {
        final StopWatch stopWatch = new StopWatch(filename + ": ");
        final PTextBuffer text = new PTextBuffer();
        text.setText(e.util.StringUtilities.readFile(filename).toCharArray());
        stopWatch.print("read file");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PTextArea area = new PTextArea(text);
                stopWatch.print("created text area");
                area.setFont(new Font(e.util.GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12));
                PTextStyler styler = getTextStyler(filename, area);
                if (styler != null) {
                    area.setTextStyler(styler);
                }
                stopWatch.print("added styler");
                //new PTextAreaSpellingChecker(area).checkSpelling();
                stopWatch.print("added spelling checker");
                JFrame frame = new JFrame("PTextArea Test: " + filename);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JScrollPane scroller = new JScrollPane(area);
                frame.getContentPane().add(scroller);
                frame.setSize(new Dimension(600, 600));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
    
    private static PTextStyler getTextStyler(String filename, PTextArea textArea) {
        if (filename.indexOf('.') != -1) {
            String extension = filename.substring(filename.lastIndexOf('.') + 1);
            if (extension.equals("cpp") || extension.equals("h")) {
                return new PCPPTextStyler(textArea);
            } else if (extension.equals("c")) {
                return new PCTextStyler(textArea);
            } else if (extension.equals("java")) {
                return new PJavaTextStyler(textArea);
            }
        }
        return null;
    }
}
