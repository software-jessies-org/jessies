package e.testing;

import java.io.*;
import java.util.*;
import javax.swing.*;

import e.ptextarea.*;
import e.util.*;

/**
 * A PIndenterTester tests the indentation engines used for the PTextArea.
 * 
 * @author Phil Norman
 */

public class PIndenterTester {
    private String filename;
    private PTextArea textArea = new PTextArea(400, 400);
    private String originalContent;
    
    public PIndenterTester(String filename) {
        this.filename = filename;
        PTextBuffer buffer = textArea.getTextBuffer();
        buffer.readFromFile(new File(filename));
        originalContent = buffer.toString();
        buffer.putProperty(PTextBuffer.INDENTATION_PROPERTY, IndentationGuesser.guessIndentationFromFile(originalContent, "    "));
        FileType fileType = FileType.guessFileType(filename, originalContent);
        fileType.configureTextArea(textArea);
    }
    
    public int printDifferences(PrintWriter out) {
        int result = 0;
        String newContent = textArea.getTextBuffer().toString();
        TwoFileLineIterator iterator = new TwoFileLineIterator(originalContent, newContent);
        while (iterator.iterateToNext()) {
            if (iterator.areTheTwoLinesEqual()) {
                out.println(" " + iterator.getComparison());
            } else {
                out.println("!" + iterator.getComparison());
                result++;
            }
        }
        return result;
    }
    
    static class TwoFileLineIterator {
        private LineNumberReader baseline;
        private LineNumberReader comparison;
        private String currentBaseline;
        private String currentComparison;
        
        public TwoFileLineIterator(String baselineString, String comparisonString) {
            baseline = new LineNumberReader(new CharArrayReader(baselineString.toCharArray()));
            comparison = new LineNumberReader(new CharArrayReader(comparisonString.toCharArray()));
        }
        
        public int getLineIndex() {
            return baseline.getLineNumber();
        }
        
        public boolean iterateToNext() {
            try {
                currentBaseline = baseline.readLine();
                currentComparison = comparison.readLine();
            } catch (IOException ex) {
                // IOExceptions are impossible really...
                ex.printStackTrace();  // ...but prevent the compiler from warning.
            }
            return (currentBaseline != null) && (currentComparison != null);
        }
        
        public boolean areTheTwoLinesEqual() {
            return currentBaseline.equals(currentComparison);
        }
        
        public String getBaseline() {
            return currentBaseline;
        }
        
        public String getComparison() {
            return currentComparison;
        }
    }
    
    public void correctIndentation() {
        int textLength = textArea.getTextBuffer().length();
        textArea.getIndenter().fixIndentationBetween(0, textLength);
        int newLength = textArea.getTextBuffer().length();
    }
    
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int errorCounter = 0;
                for (String filename: args) {
                    PIndenterTester tester = new PIndenterTester(filename);
                    tester.correctIndentation();
                    PrintWriter out = new PrintWriter(System.out);
                    int differencesFound = tester.printDifferences(out);
                    out.flush();
                    errorCounter += differencesFound;
                    if (differencesFound == 0) {
                        System.out.println("No differences found in file " + filename);
                    }
                }
                if (args.length > 1) {
                    System.out.println("" + errorCounter + " differences found in all files");
                } else if (args.length == 0) {
                    System.err.println("Usage: indenter <files to test>");
                    System.exit(1);
                }
                if (errorCounter > 0) {
                    System.exit(1);
                }
            }
        });
    }
}
