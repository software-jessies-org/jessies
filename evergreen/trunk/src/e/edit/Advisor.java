package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.Timer;
import e.ptextarea.*;
import e.gui.*;
import e.util.*;

public class Advisor extends JPanel {
    private static Advisor instance;
    
    private ArrayList<WorkspaceResearcher> researchers = new ArrayList<WorkspaceResearcher>();
    
    /** The advice window. */
    private AdvisorHtmlPane advicePane = new AdvisorHtmlPane();
    
    private JFrame frame;
    
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
    
    private synchronized JFrame getFrame() {
        if (frame == null) {
            // FIXME: should have at least status line/progress indicator and back button. Perhaps as part of AdvisorHtmlPane rather than here?
            frame = JFrameUtilities.makeSimpleWindow("Evergreen Documentation Browser", advicePane);
            frame.setSize(new Dimension(400, 500));
        }
        return frame;
    }
    
    public synchronized void showDocumentation() {
        advicePane.setText("");
        getFrame().setVisible(true);
        research(getLookupString());
    }
    
    public synchronized void showDocumentation(String content) {
        if (content.startsWith("<html>") == false) {
            content = "<html><head><title></title></head><body bgcolor=#FFFFFF>" + content + "</body></html>";
        }
        advicePane.setText(content);
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
        ETextWindow textWindow = ETextAction.getFocusedTextWindow();
        StringBuilder newText = new StringBuilder();
        for (WorkspaceResearcher researcher : researchers) {
            if (textWindow == null || researcher.isSuitable(textWindow)) {
                String result = researcher.research(text);
                if (result != null && result.length() > 0) {
                    newText.append(result);
                }
            }
        }
        
        if (newText.length() == 0) {
            newText.append("No documentation found for \"" + text + "\".");
        }
        
        showDocumentation(newText.toString());
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
