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
        
        // FIXME: is this what we want, or do we want a main menu item that brings up a dialog? The latter might be more direct for most practical uses.
        /*
        final JTextField textField = new JTextField();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                research(textField.getText());
            }
        });
        add(textField, BorderLayout.NORTH);
        */
        
        new Thread(new Runnable() {
            public void run() {
                addResearcher(JavaResearcher.getSharedInstance());
                addResearcher(ManPageResearcher.getSharedInstance());
                addResearcher(new NumberResearcher());
                addResearcher(new PerlDocumentationResearcher());
                addResearcher(new RubyDocumentationResearcher());
                addResearcher(new StlDocumentationResearcher());
            }
        }).start();
    }
    
    private synchronized JFrame getFrame() {
        if (frame == null) {
            frame = JFrameUtilities.makeSimpleWindow("Evergreen Documentation Browser", this);
            frame.setSize(new Dimension(600, 500));
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
            synchronized (researchers) {
                for (WorkspaceResearcher researcher : researchers) {
                    if (textWindow == null || researcher.isSuitable(textWindow)) {
                        String result = researcher.research(searchTerm);
                        if (result != null && result.length() > 0) {
                            newText.append(result);
                        }
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
        synchronized (researchers) {
            researchers.add(researcher);
        }
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
            // Anything off the web or local HTML should be displayed in the documentation browser, rather than handed off to the platform's web browser.
            // Non-HTML files, though, need to be handed off so they're opened for editing.
            if (link.startsWith("http:") || link.matches("file:.*\\.html")) {
                advicePane.setPage(link);
                return null;
            }
            // Offer the link to each researcher.
            synchronized (researchers) {
                for (WorkspaceResearcher researcher : researchers) {
                    if (researcher.handleLink(link)) {
                        return null;
                    }
                }
            }
            // Hand it on to the file-opening code to work out what to do with it.
            Evergreen.getInstance().openFile(link);
            return null;
        }
    }
    
    public static String findToolOnPath(String tool) {
        ArrayList<String> availableTools = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, new String[] { "which", tool }, availableTools, errors);
        if (status != 0 || availableTools.size() == 0) {
            return null;
        }
        return availableTools.get(0);
    }
}
