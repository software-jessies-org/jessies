package e.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import e.forms.*;
import e.ptextarea.*;

public class SimpleDialog {
    /**
     * How many characters to aim for when breaking a message
     * into reasonable-length lines for a dialog.
     */
    private static final int MAX_LINE_LENGTH = 72;
    
    public static String breakLongMessageLines(String message) {
        if (message.length() < MAX_LINE_LENGTH) {
            return message;
        }
        StringBuilder result = new StringBuilder(message);
        int chunkLength = 0;
        for (int i = 0; i < result.length(); ++i) {
            if (result.charAt(i) == '\n') {
                chunkLength = 0;
            } else {
                chunkLength++;
                if (chunkLength > MAX_LINE_LENGTH && result.charAt(i) == ' ') {
                    result.insert(i + 1, '\n');
                }
            }
        }
        return result.toString();
    }
    
    public static void showAlert(Frame frame, String title, String message) {
        JOptionPane.showMessageDialog(frame, breakLongMessageLines(message), title, JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Intended for longer messages than showAlert, especially ones from other
     * programs. Swing's message dialog doesn't let you copy text (regardless
     * of whether or not the native system's dialog would); this uses a proper
     * text area, so you can select and copy as you'd expect.
     */
    public static void showDetails(Frame frame, String title, String details) {
        PTextArea textArea = new PTextArea(10, 40);
        textArea.setEditable(false);
        textArea.setText(details);
        textArea.setWrapStyleWord(true);
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Details:", new JScrollPane(textArea));
        FormDialog.showNonModal(frame, title, formPanel);
    }
    
    /**
     * Shows a message in a similar manner to the other method of this name,
     * but in this case the message is the text of the Throwable's stack trace.
     * This is currently most useful for things that couldn't possibly happen.
     * It's not very friendly, but it's probably more friendly than just
     * writing output to the console/log.
     */
    public static void showDetails(Frame frame, String title, Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        String exceptionDetails = stringWriter.toString();
        showDetails(frame, title, exceptionDetails);
    }
    
    public static boolean askQuestion(Frame frame, String title, String message, String continueText) {
        Object[] options = { continueText, "Cancel" };
        int option = JOptionPane.showOptionDialog(frame, breakLongMessageLines(message), title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return (option == JOptionPane.YES_OPTION);
    }
    
    public static String askQuestion(Frame frame, String title, String message, String continueTextYes, String continueTextNo) {
        Object[] options = { continueTextYes, continueTextNo, "Cancel" };
        int option = JOptionPane.showOptionDialog(frame, breakLongMessageLines(message), title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (option == JOptionPane.YES_OPTION) {
            return continueTextYes;
        } else if (option == JOptionPane.NO_OPTION) {
            return continueTextNo;
        } else {
            return "Cancel";
        }
    }
    
    /**
     * This class exports static methods.
     */
    private SimpleDialog() {
    }
}
