package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.Timer;
import org.jdesktop.swingworker.SwingWorker;

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
        research(getSearchTerm());
    }
    
    private class ResearchRunner extends SwingWorker<String, Object> {
        private String searchTerm;
        private ETextWindow textWindow;
        
        private ResearchRunner(String searchTerm) {
            this.searchTerm = searchTerm;
            this.textWindow = ETextAction.getFocusedTextWindow();
            advicePane.setText("Searching for documentation on \"" + searchTerm + "\"...");
        }
        
        @Override
        protected String doInBackground() {
            StringBuilder newText = new StringBuilder();
            for (WorkspaceResearcher researcher : researchers) {
                if (textWindow == null || researcher.isSuitable(textWindow)) {
                    String result = researcher.research(searchTerm);
                    if (result != null && result.length() > 0) {
                        newText.append(result);
                    }
                }
            }
            
            if (newText.length() == 0) {
                newText.append("No documentation found for \"" + searchTerm + "\".");
            }
            
            return newText.toString();
        }
        
        @Override
        public void done() {
            try {
                showDocumentation(get());
            } catch (Exception ex) {
                Log.warn("ResearchRunner failed", ex);
            }
        }
    }
    
    public void showDocumentation(String content) {
        if (content.startsWith("<html>") == false) {
            content = "<html><head><title></title></head><body bgcolor=#FFFFFF>" + content + "</body></html>";
        }
        // JEditorPane.setText is thread-safe.
        advicePane.setText(content);
    }
    
    private String getSearchTerm() {
        ETextArea textArea = ETextAction.getFocusedTextArea();
        if (textArea == null) {
            return "";
        }
        
        // We use the selection, if there is one.
        String selection = textArea.getSelectedText();
        if (selection.length() > 0) {
            return selection.trim();
        }
        
        // Otherwise, we use the word at the caret.
        CharSequence chars = textArea.getTextBuffer();
        int caretPosition = textArea.getSelectionStart();
        String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
        int start = PWordUtilities.getWordStart(chars, caretPosition, stopChars);
        int end = PWordUtilities.getWordEnd(chars, caretPosition, stopChars);
        return chars.subSequence(start, end).toString();
    }
    
    public void research(String text) {
        new ResearchRunner(text).execute();
    }
    
    private void addResearcher(WorkspaceResearcher researcher) {
        researchers.add(researcher);
    }
    
    public void linkClicked(String link) {
        new LinkClickRunner(link).execute();
    }
    
    private class LinkClickRunner extends SwingWorker<Object, Object> {
        private String link;
        
        private LinkClickRunner(String link) {
            this.link = link;
        }
        
        @Override
        protected Object doInBackground() {
            // Offer the link to each researcher.
            for (WorkspaceResearcher researcher : researchers) {
                if (researcher.handleLink(link)) {
                    return null;
                }
            }
            // Hand it on to the file-opening code to work out what to do with it.
            Evergreen.getInstance().openFile(link);
            return null;
        }
    }
}
