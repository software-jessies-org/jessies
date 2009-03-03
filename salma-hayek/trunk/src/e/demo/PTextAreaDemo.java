package e.demo;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class PTextAreaDemo {
    private static final Font FIXED_FONT = new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12);
    private static final Font PROPORTIONAL_FONT = new Font("Verdana", Font.PLAIN, 12);
    
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
        textArea.setBorder(GuiUtilities.createEmptyBorder(4));
        textArea.setFont(FIXED_FONT);
        
        final File file = new File(filename);
        textArea.getTextBuffer().readFromFile(file);
        CharSequence content = textArea.getTextBuffer();
        
        FileType fileType = FileType.guessFileType(filename, content);
        fileType.configureTextArea(textArea);
        
        textArea.getTextBuffer().putProperty(PTextBuffer.INDENTATION_PROPERTY, IndentationGuesser.guessIndentationFromFile(content, "    "));
        
        JFrame frame = JFrameUtilities.makeScrollableContentWindow(file.getPath() + " - PTextAreaDemo", textArea);
        frame.setSize(new Dimension(600, 600));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // FIXME: stick these in the UI?
        System.err.println("indentationString='" + textArea.getIndentationString() + "'");
        System.err.println("fileType=" + textArea.getFileType().getName());
        System.err.println("indenter=" + textArea.getIndenter());
        System.err.println("styler=" + textArea.getTextStyler());
    }
    
    private static int countLinesChangedByIndenter(PTextArea textArea) {
        int changedLines = 0;
        for (int lineNumber = 0; lineNumber != textArea.getLineCount(); ++ lineNumber) {
            PIndenter indenter = textArea.getIndenter();
            String previousIndentation = indenter.getCurrentIndentationOfLine(lineNumber);
            indenter.fixIndentationOnLine(lineNumber);
            String newIndentation = indenter.getCurrentIndentationOfLine(lineNumber);
            if (newIndentation.equals(previousIndentation) == false) {
                ++ changedLines;
            }
        }
        return changedLines;
    }
}
