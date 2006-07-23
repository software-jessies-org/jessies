package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.Timer;
import e.ptextarea.*;
import e.util.*;

public class Advisor extends JPanel {
    private static Advisor instance;
    
    private ArrayList<WorkspaceResearcher> researchers = new ArrayList<WorkspaceResearcher>();
    
    /** The advice window. */
    private AdvisorHtmlPane advicePane = new AdvisorHtmlPane();
    
    public static synchronized Advisor getInstance() {
        if (instance == null) {
            instance = new Advisor();
        }
        return instance;
    }
    
    private Advisor() {
        setLayout(new BorderLayout());
        add(advicePane, BorderLayout.CENTER);
        //FIXME
        /*
        final JTextField textField = new JTextField();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // If we select the text, the researchers will be invoked.
                textField.selectAll();
            }
        });
        add(textField, BorderLayout.SOUTH);
        */
        new Thread(new Runnable() {
            public void run() {
                addResearcher(JavaResearcher.getSharedInstance());
                addResearcher(ManPageResearcher.getSharedInstance());
                addResearcher(new NumberResearcher());
                addResearcher(new RubyDocumentationResearcher());
            }
        }).start();
    }
    
    public String getLookupString() {
        ETextArea textArea = ETextAction.getFocusedTextArea();
        if (textArea == null) {
            return "";
        }
        
        // We give the researchers the selection, if there is one.
        // If there isn't, we give them the current line up to the caret.
        String string = textArea.getSelectedText();
        if (string == null) {
            string = "";
        }
        if (string.length() == 0) {
            int dot = textArea.getSelectionStart();
            int lineNumber = textArea.getLineOfOffset(dot);
            int lineStart = textArea.getLineStartOffset(lineNumber);
            string = textArea.getTextBuffer().subSequence(lineStart, dot).toString();
        }
        
        // If the user's selected more than a line, don't tell the researchers.
        if (string.matches(".*\n.*\n")) {
            return "";
        }
        
        // Remove whitespace, because no researcher is going to tell us anything about whitespace.
        return string.trim();
    }
    
    public void research(String text) {
        // If there's nothing to look at, don't wake the researchers.
        if (text.length() == 0) {
            return;
        }
        
        ETextWindow textWindow = ETextAction.getFocusedTextWindow();
        StringBuilder newText = new StringBuilder("<html><head><title></title></head><body bgcolor=#FFFFFF>");
        for (WorkspaceResearcher researcher : researchers) {
            if (textWindow == null || researcher.isSuitable(textWindow)) {
                String result = researcher.research(text);
                if (result != null && result.length() > 0) {
                    newText.append(result);
                }
            }
        }
        newText.append("</body></html>");
        String result = newText.toString();

        // Deliberately ignore the advisors if they're just babbling.
        int lineCount = 0;
        for (int i = 0; i < result.length(); ++i) {
            if (result.charAt(i) == '\n') {
                ++lineCount;
            }
        }
        if (lineCount > 100) {
            result = "(Too much output.)";
        }

        advicePane.setText(result);
    }
    
    private void addResearcher(WorkspaceResearcher researcher) {
        researchers.add(researcher);
    }
    
    public void linkClicked(String link) {
        // Offer the link to each researcher.
        for (WorkspaceResearcher researcher : researchers) {
            if (researcher.handleLink(link)) {
                return;
            }
        }
        // Hand it on to the file-opening code to work out what to do with it.
        Evergreen.getInstance().openFile(link);
    }
}
