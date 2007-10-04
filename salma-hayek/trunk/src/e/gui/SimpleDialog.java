package e.gui;

import e.forms.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import javax.swing.*;

public class SimpleDialog {
    /**
     * CSS for Mac OS to get Mac-like dialog text.
     * FIXME: GNOME uses different fonts and sizes, but I don't know how to get them.
     */
    private static final String MAC_CSS = "<head><style type=\"text/css\">b { font: 13pt \"Lucida Grande\" } p { font: 11pt \"Lucida Grande\"; margin-top: 8px }</style></head>";
    
    /**
     * How many characters to aim for when breaking a message
     * into reasonable-length lines for a dialog.
     */
    private static final int MAX_LINE_LENGTH = 72;
    
    /**
     * Adds line breaks to a text/plain or text/html string.
     * 
     * Existing newline characters or HTML tags are taken as explicit breaks.
     * 
     * On Mac OS, the "P" and "B" tags' appearance is modified via CSS to resemble the Mac UI guidelines for dialog text.
     * See http://www.randelshofer.ch/quaqua/guide/joptionpane.html for the inspiration.
     */
    public static String breakLongMessageLines(String message) {
        if (message.length() < MAX_LINE_LENGTH) {
            return message;
        }
        
        boolean html = message.matches("^(?i)<html>.*");
        if (html) {
            message = message.substring(6);
        }
        
        StringBuilder brokenMessage = new StringBuilder(message);
        int chunkLength = 0;
        for (int i = 0; i < brokenMessage.length(); ++i) {
            if (brokenMessage.charAt(i) == '\n' || (html && brokenMessage.charAt(i) == '<')) {
                // An explicit newline is taken as a manual split.
                // Similarly, an HTML tag is assumed to be "<body>", "<p>", or "<br>", all of which can be considered manual line breaks.
                // FIXME: work better with tags such as "<b>" or "<i>".
                chunkLength = 0;
            } else {
                chunkLength++;
                if (chunkLength > MAX_LINE_LENGTH && brokenMessage.charAt(i) == ' ') {
                    brokenMessage.insert(i + 1, '\n');
                }
            }
        }
        
        String result = brokenMessage.toString();
        if (html) {
            // On Mac OS, make CSS available to make Mac-like formatting easier.
            if (GuiUtilities.isMacOs()) {
                result = MAC_CSS + result;
            }
            // Put back the leading "<html>" that we stripped earlier.
            result = "<html>" + result;
            
            // If we pass an HTML message to JOptionPane, it must not contain
            // newlines. If it does, only the first line is treated as HTML.
            // (Tested on Mac OS' Java 5.)
            result = result.replaceAll("\n", "<br>");
        }
        return result;
    }
    
    /**
     * Wraps JOptionPane.showMessageDialog using breakLongMessageLines to improve readability.
     */
    public static void showAlert(Component owner, String title, String message) {
        JOptionPane.showMessageDialog(owner, breakLongMessageLines(makeDialogText(title, message)), "", JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Apple and GNOME both say that alert dialogs should have no title, and that -- using the GNOME terminology -- the primary text should be bold and separated from (but in the same area as) the secondary text.
     * 
     * http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/XHIGWindows/chapter_17_section_6.html
     * http://developer.gnome.org/projects/gup/hig/2.0/windows-alert.html
     */
    private static String makeDialogText(String title, String message) {
        // On Mac OS, the primary text should end in punctuation.
        if (GuiUtilities.isMacOs()) {
            if (title.endsWith(".") == false && title.endsWith("?") == false) {
                title += ".";
            }
        }
        // On GNOME, the primary text shouldn't end with ".".
        if (GuiUtilities.isGtk() && title.endsWith(".")) {
            title = title.substring(0, title.length() - 1);
        }
        return "<html><body><b>" + title + "</b><p>" + message;
    }
    
    /**
     * Intended for longer messages than showAlert, especially ones from other
     * programs. Swing's message dialog doesn't let you copy text (regardless
     * of whether or not the native system's dialog would); this uses a proper
     * text area, so you can select and copy as you'd expect.
     */
    public static FormDialog showDetails(Component owner, String title, String details) {
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, owner);
        PTextArea textArea = new PTextArea(10, 40);
        textArea.setEditable(false);
        textArea.setText(details);
        textArea.setWrapStyleWord(true);
        FormBuilder form = new FormBuilder(frame, title);
        form.getFormPanel().addRow("Details:", new JScrollPane(textArea));
        form.showNonModal();
        return form.getFormDialog();
    }
    
    /**
     * Shows a message in a similar manner to the other method of this name,
     * but in this case the message is the text of the Throwable's stack trace.
     * This is currently most useful for things that couldn't possibly happen.
     * It's not very friendly, but it's probably more friendly than just
     * writing output to the console/log.
     */
    public static FormDialog showDetails(Component owner, String title, Throwable throwable) {
        return showDetails(owner, title, StringUtilities.stackTraceFromThrowable(throwable));
    }
    
    public static boolean askQuestion(Component owner, String title, String message, String continueText) {
        int option = askQuestionHelper(owner, title, message, new Object[] { continueText, "Cancel" });
        return (option == JOptionPane.YES_OPTION);
    }
    
    public static String askQuestion(Component owner, String title, String message, String continueTextYes, String continueTextNo) {
        int option = askQuestionHelper(owner, title, message, new Object[] { continueTextYes, continueTextNo, "Cancel" });
        if (option == JOptionPane.YES_OPTION) {
            return continueTextYes;
        } else if (option == JOptionPane.NO_OPTION) {
            return continueTextNo;
        } else {
            return "Cancel";
        }
    }
    
    /**
     * The beginnings of a work-around for Sun bug 6372048.
     */
    private static int askQuestionHelper(Component owner, String title, String message, Object[] options) {
        JOptionPane pane = new JOptionPane(breakLongMessageLines(makeDialogText(title, message)), JOptionPane.QUESTION_MESSAGE, (options.length == 3) ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION);
        pane.setOptions(options);
        pane.setInitialValue(options[0]);
        JDialog dialog = pane.createDialog(owner, title);
        
        // FIXME: requires Java 6. We may need to let the user choose other modalities.
        //dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        
        dialog.setVisible(true);
        dialog.dispose();
        
        Object selectedValue = pane.getValue();
        if (selectedValue == null) {
            return JOptionPane.CLOSED_OPTION;
        }
        if (selectedValue instanceof Integer) {
            return ((Integer) selectedValue).intValue();
        }
        for (int i = 0; i < options.length; ++i) {
            if (options[i].equals(selectedValue)) {
                return i;
            }
        }
        return JOptionPane.CLOSED_OPTION;
    }
    
    /**
     * This class exports static methods.
     */
    private SimpleDialog() {
    }
}
