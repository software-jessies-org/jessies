package e.demo;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

public class PTextAreaDemo {
    private PTextAreaDemo() {
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Syntax: PTextAreaDemo <filename...>");
            System.exit(1);
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
            }
        });
        
        for (final String filename : args) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    makeTextViewer(filename);
                }
            });
        }
    }
    
    private static void makeTextViewer(String filename) {
        PTextArea textArea = new PTextArea();
        textArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        
        final File file = new File(filename);
        textArea.getTextBuffer().readFromFile(file);
        CharSequence content = textArea.getTextBuffer();
        
        FileType fileType = FileType.guessFileType(filename, content);
        fileType.configureTextArea(textArea);
        System.err.println("indenter=" + textArea.getIndenter());
        System.err.println("styler=" + textArea.getTextStyler());
        
        textArea.getTextBuffer().putProperty(PTextBuffer.INDENTATION_PROPERTY, IndentationGuesser.guessIndentationFromFile(content));
        
        JFrame frame = new JFrame(file.getPath() + " - PTextAreaDemo");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JScrollPane scroller = new JScrollPane(textArea);
        frame.getContentPane().add(scroller);
        frame.setSize(new Dimension(600, 600));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }
    
    private static char[] getFileText(File file) {
        try {
            CharArrayWriter chars = new CharArrayWriter();
            PrintWriter writer = new PrintWriter(chars);
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                writer.println(line);
            }
            in.close();
            writer.close();
            return chars.toCharArray();
        } catch (IOException ex) {
            ex.printStackTrace();
            return new char[0];
        }
    }
}
