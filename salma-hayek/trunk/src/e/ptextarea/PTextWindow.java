package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final StopWatch stopWatch = new StopWatch(filename + ": ");
                
                final PTextArea area = new PTextArea();
                area.setFont(new Font(e.util.GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12));
                stopWatch.print("created text area");
                
                area.getPTextBuffer().readFromFile(new java.io.File(filename));
                stopWatch.print("read file");

                PTextStyler styler = getTextStyler(filename, area);
                if (styler != null) {
                    area.setTextStyler(styler);
                }
                stopWatch.print("added styler");
                
                JFrame frame = new JFrame("PTextArea Test: " + filename);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JScrollPane scroller = new JScrollPane(area);
                frame.getContentPane().add(scroller);
                frame.setJMenuBar(makeMenuBar());
                frame.setSize(new Dimension(600, 600));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        area.getPTextBuffer().writeToFile(new java.io.File(filename + ".bak"));
                    }
                });
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
                HashSet set = new HashSet();
                set.add("println");
                textArea.putClientProperty(PTextAreaSpellingChecker.KEYWORDS_JCOMPONENT_PROPERTY, set);
                return new PJavaTextStyler(textArea);
            } else if (extension.equals("rb")) {
                return new PRubyTextStyler(textArea);
            } else if (extension.equals("txt")) {
                return new PHyperlinkTextStyler(textArea, "\\bhttp://") {
                    public void hyperlinkClicked(CharSequence linkText) {
                        System.out.println("Hyperlink clicked: " + linkText);
                    }
                    public boolean isAcceptableMatch(java.util.regex.Matcher matcher) {
                        return true;
                    }
                };
            }
        }
        return null;
    }
    
    private static JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenAction());
        menuBar.add(fileMenu);
        
        return menuBar;
    }
    
    public static class OpenAction extends AbstractAction {
        public OpenAction() {
            super("Open...");
            putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("O", false));
        }
        
        public void actionPerformed(ActionEvent e) {
            System.out.println("OpenAction.actionPerformed");
        }
    }
}
