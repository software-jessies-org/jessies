package e.demo;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.io.*;
import java.util.*;
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
        
        iterateOverSegments(textArea);
        //System.err.println("indenter changed lines=" + countLinesChangedByIndenter(textArea));
        
        JFrame frame = JFrameUtilities.makeScrollableContentWindow(file.getPath() + " - PTextAreaDemo", textArea);
        frame.setSize(new Dimension(600, 600));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }
    
    private static void iterateOverSegments(PTextArea textArea) {
        Iterator<PLineSegment> segments = textArea.getLogicalSegmentIterator(0);
        int segmentCount = 0;
        while (segments.hasNext()) {
            PLineSegment segment = segments.next();
            ++ segmentCount;
            String segmentDump = segment.toString();
            //System.err.println("segment=" + segmentDump);
        }
        System.err.println("segmentCount=" + segmentCount);
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
