package e.gui;

import java.awt.*;
import javax.swing.*;

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
        StringBuffer result = new StringBuffer(message);
        int chunkLength = 0;
        for (int i = 0; i < result.length(); i++) {
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
