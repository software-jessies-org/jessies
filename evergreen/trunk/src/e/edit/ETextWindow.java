package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import e.gui.*;
import e.util.*;

/**
A text-editing component.
*/
public class ETextWindow extends ETextComponent implements ChangeListener, DocumentListener {
    protected String filename;
    protected File file;
    private long lastModifiedTime;
    protected ETextArea text;
    private boolean isDirty;
    private BirdView birdView;
    
    private final static String UNKNOWN = "Unknown";
    private final static String C_PLUS_PLUS = "C++";
    private final static String JAVA = "Java";
    private final static String RUBY = "Ruby";
    private final static String PERL = "Perl";
    
    private String fileType = UNKNOWN;
    
    private static final Hashtable KEYWORDS_MAP = new Hashtable();
    
    static {
        initKeywordsFor(C_PLUS_PLUS);
        initKeywordsFor(JAVA);
        initKeywordsFor(RUBY);
    }
    
    private static void initKeywordsFor(String language) {
        HashSet keywords = new HashSet();
        String keywordsFileName = System.getProperty("env.EDIT_HOME") + File.separatorChar + "keywords-" + language;
        String[] keywordArray = StringUtilities.readLinesFromFile(keywordsFileName);
        for (int i = 0; i < keywordArray.length; i++) {
            if (keywordArray[i].startsWith("#")) {
                continue; // Ignore comments.
            }
            keywords.add(keywordArray[i]);
        }
        KEYWORDS_MAP.put(language, keywords);
    }
    
    public ETextWindow(String filename) {
        super(filename);
        this.filename = filename;
        this.file = FileUtilities.fileFromString(filename);
        this.text = new ETextArea();
        attachPopupMenuTo(text);
        JScrollPane scrollPane = new JScrollPane(text);
        this.birdView = new BirdView(text, scrollPane.getVerticalScrollBar());
        add(scrollPane, BorderLayout.CENTER);
        add(birdView, BorderLayout.EAST);
        text.addFocusListener(this);
        fillWithContent();
    }
    
    public BirdView getBirdView() {
        return birdView;
    }
    
    public String getFilename() {
        return filename;
    }
    
    /** Returns the grep-style ":<line>:<column>" address for the caret position. */
    public String getAddress() {
        try {
            int caretPosition = text.getCaretPosition();
            int lineNumber = 1 + text.getLineOfOffset(caretPosition);
            int lineStart = text.getLineStartOffset(lineNumber - 1);
            int columnNumber = 1 + emacsDistance(caretPosition, lineStart);
            return ":" + lineNumber + ":" + columnNumber;
        } catch (BadLocationException ex) {
            return "";
        }
    }
    
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        if (e.isTemporary() == false) {
            Edit.getTagsPanel().ensureTagsCorrespondTo(this);
        }
    }
    
    public void requestFocus() {
        text.requestFocus();
    }
    
    //
    // DocumentListener interface.
    //

    public void changedUpdate(DocumentEvent e) {
        /*
         * This represents a style change, which should not be
         * considered as a dirtying because styles are meaningless
         * in a programmer's editor, and don't get saved.
         */
    }
    
    public void insertUpdate(DocumentEvent e) {
        markAsDirty();
    }
    
    public void removeUpdate(DocumentEvent e) {
        markAsDirty();
    }
    
    public UndoManager getUndoManager() {
        return text.getUndoManager();
    }
    
    /** Tests whether the 'content' looks like a Unix shell script. */
    private static boolean isInterpretedContent(String content, String interpreter) {
        return Pattern.compile("^#![^\\n]*" + interpreter).matcher(content).find();
    }
    
    /** Tests whether the 'content' looks like a Unix shell script, of the Ruby variety. */
    private static boolean isRubyContent(String content) {
        return isInterpretedContent(content, "ruby");
    }
    
    /** Tests whether the 'content' looks like a Unix shell script, of the Perl variety. */
    private static boolean isPerlContent(String content) {
        return isInterpretedContent(content, "perl");
    }
    
    /**
     * Tests whether the 'content' looks like a C++ file.
     *
     * A standard C++ header file (such as <string>) might not have any extension,
     * though it's likely in that case start with #ifndef.
     *
     * GNU headers tend to have an emacs mode hint, so let's obey those too (I think
     * emacs scans the whole file, but GNU headers seem to use the first line).
     */
    private static boolean isCPlusPlusContent(String content) {
        return content.startsWith("#ifndef") || content.matches(".*" + StringUtilities.regularExpressionFromLiteral("-*- C++ -*-") + ".*");
    }
    
    /** Attaches a set of keywords to a Document, tipping the spelling checker off that some words are okay. */
    private void initKeywordsForDocument() {
        HashSet keywords = (HashSet) KEYWORDS_MAP.get(fileType);
        if (keywords != null) {
            text.getDocument().putProperty(JTextComponentSpellingChecker.KEYWORDS_DOCUMENT_PROPERTY, keywords);
        }
    }
    
    /**
     * Invoked during construction. Override this if you don't want to read from a file with
     * the same name as the window's title.
     */
    public void fillWithContent() {
        try {
            String content  = StringUtilities.readFile(file.getAbsolutePath());
            lastModifiedTime = file.lastModified();
            
            if (filename.endsWith(".java")) {
                fileType = JAVA;
                text.setIndenter(new JavaIndenter());
            } else if (filename.endsWith(".rb") || isRubyContent(content)) {
                fileType = RUBY;
                text.setIndenter(new RubyIndenter());
            } else if (filename.endsWith(".cpp") || filename.endsWith(".hpp") || filename.endsWith(".c") || filename.endsWith(".h") || filename.endsWith(".m") || filename.endsWith(".mm") || content.startsWith("#ifndef") || isCPlusPlusContent(content)) {
                fileType = C_PLUS_PLUS;
                text.setIndenter(new JavaIndenter());
            } else if (filename.endsWith(".pl") || isPerlContent(content)) {
                fileType = PERL;
                text.setIndenter(new JavaIndenter());
            }
            initKeywordsForDocument();
            
            text.setText(content);
            text.getUndoManager().discardAllEdits();
            text.getDocument().addDocumentListener(this);
            markAsClean();
            getTitleBar().checkForCounterpart(); // If we don't do this, we don't get the icon until we get focus.
        } catch (Throwable th) {
            Log.warn("in ContentLoader exception handler", th);
            Edit.showAlert("Open", "Couldn't open file '" + th.getMessage() + "'.");
            throw new RuntimeException("don't open this window");
        }
    }
    
    public void revertToSaved() {
        boolean revert = Edit.askQuestion("Revert", "Revert to saved version of '" + file.getName() + "'?\nReverting will lose your current changes.", "Revert");
        if (revert) {
            int originalCaretPosition = text.getCaretPosition();
            fillWithContent();
            text.setCaretPosition(originalCaretPosition);
            Edit.showStatus("Reverted to saved version of " + FileUtilities.getUserFriendlyName(filename));
        }
    }
    
    public ETextArea getText() {
        return text;
    }
    
    public void windowClosing() {
        Edit.showStatus("Closed " + FileUtilities.getUserFriendlyName(filename));
        this.file = null;
        getWorkspace().unregisterTextComponent(getText());
        // FIXME: what else needs doing to ensure that we give back memory?
    }
    
    /**
     * Closes this text window if the text isn't dirty.
     */
    public void closeWindow() {
        if (isDirty()) {
            boolean discard = Edit.askQuestion("Close", "Do you want to discard the changes you made to the file\n'" + filename + "'?", "Discard");
            if (discard == false) {
                return;
            }
        }
        Edit.getTagsPanel().ensureTagsAreHidden();
        super.closeWindow();
    }
    
    public Collection getPopupMenuItems() {
        ArrayList items = new ArrayList();
        items.add(new OpenSelectionAction());
        items.add(new FindFilesContainingSelectionAction());
        items.add(new ExecuteAction());
        addContextSpecificMenuItems(items);
        addWindowSpecificMenuItems(items);
        addExternalToolMenuItems(items);
        addSpellingSuggestions(items);
        return items;
    }

    public void addExternalToolMenuItems(final Collection items) {
        ExternalToolsParser toolsParser = new ExternalToolsParser() {
            private boolean needSeparator = true;
            
            public void addItem(Action action) {
                addAction(action);
            }

            public void addItem(Action action, char keyboardEquivalent) {
                addAction(action);
            }

            public void addAction(Action action) {
                if (needSeparator) {
                    addSeparator();
                    needSeparator = false;
                }
                /* Ignore ExternalToolActions that aren't context-sensitive. */
                if (action instanceof ExternalToolAction) {
                    if (((ExternalToolAction) action).isContextSensitive() == false) {
                        return;
                    }
                }
                items.add(action);
            }

            public void addSeparator() {
                items.add(null);
            }
        };
        toolsParser.parse();
    }

    /**
     * Returns the filename of the counterpart to this file, or null.
     * A Java .java file, for example, has no counterpart. A C++ .cpp
     * file, on the other hand, may have a .h counterpart (and vice
     * versa).
     */
    public String getCounterpartFilename() {
        // Work out what the counterpart would be called.
        String basename = file.getName().replaceAll("\\..*$", "");
        String counterpartFilename = null;
        
        if (filename.endsWith(".cpp")) {
            counterpartFilename = basename + ".h";
        } else if (filename.endsWith(".h")) {
            counterpartFilename = basename + ".cpp";
        } else {
            return null;
        }

        // See if the counterpart exists.
        File counterpartFile = new File(file.getParentFile(), counterpartFilename);
        return (counterpartFile.exists() ? counterpartFilename : null);
    }

    public void switchToCounterpart() {
        String counterpartFilename = getCounterpartFilename();
        if (counterpartFilename != null) {
            Edit.openFile(getContext() + File.separator + counterpartFilename);
        } else {
            Edit.showAlert("Switch To Counterpart", "File '" + filename + "' has no counterpart.");
        }
    }

    public boolean isCPlusPlus() {
        return (fileType == C_PLUS_PLUS);
    }
    
    public boolean isJava() {
        return (fileType == JAVA);
    }
    
    public boolean isRuby() {
        return (fileType == RUBY);
    }
    
    public void addContextSpecificMenuItems(Collection items) {
        boolean needSeparator = true;
    }
    
    public void addWindowSpecificMenuItems(Collection items) {
        items.add(null);
        items.add(new SaveAction());
        items.add(null);
        items.add(new UndoAction());
        items.add(new RedoAction());
        
        if (isCPlusPlus()) {
            //items.add(null);
            //items.add(new OpenIncludeFileAction());
        }
    }
    
    public void addSpellingSuggestions(Collection items) {
        try {
            final JTextComponentSpellingChecker.Range actualRange = new JTextComponentSpellingChecker.Range();
            if (text.getSpellingChecker().isMisspelledWordBetween(text.getSelectionStart(), text.getSelectionEnd(), actualRange)) {
                String misspelling = text.getText(actualRange.start, actualRange.end - actualRange.start);
                String[] suggestions = SpellingChecker.getSharedSpellingCheckerInstance().getSuggestionsFor(misspelling);
                for (int i = 0; i < suggestions.length; i++) {
                    items.add(new CorrectSpellingAction(this, suggestions[i], actualRange.start, actualRange.end));
                }
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    public class ExecuteAction extends AbstractAction {
        public ExecuteAction() {
            super("Execute");
        }
        public void actionPerformed(ActionEvent e) {
            execute(text.getSelectedText());
        }
        public boolean isEnabled() {
            if (super.isEnabled() == false) {
                return false;
            }
            String selection = text.getSelectedText();
            if (selection == null || selection.length() == 0) {
                return false;
            }
            return true;
        }
    }
    
    private void doGoToSelection(int startOffset, int endOffset) {
        text.ensureVisibilityOfOffset(startOffset);
        text.select(startOffset, endOffset);
    }
    
    public void goToSelection(final int startOffset, final int endOffset) {
        if (EventQueue.isDispatchThread()) {
            doGoToSelection(startOffset, endOffset);
        } else {
            try {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        doGoToSelection(startOffset, endOffset);
                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void goToLine(int line) {
        try {
            // Humans number lines from 1, JTextComponent from 0.
            line--;
            final int start = text.getLineStartOffset(line);
            final int end = text.getLineEndOffset(line);
            goToSelection(start, end);
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    public int getCurrentLineNumber() {
        try {
            // Humans number lines from 1, JTextComponent from 0.
            return 1 + text.getLineOfOffset(text.getCaretPosition());
        } catch (BadLocationException ex) {
            return 0;
        }
    }
    
    public boolean isDirty() {
        return isDirty;
    }
    
    public void markAsDirty() {
        //findDialog.removeCurrentHighlights();
        isDirty = true;
        repaint();
    }
    
    public void markAsClean() {
        isDirty = false;
        repaint();
    }
    
    public void clear() {
        text.setText("");
    }
    
    /**
    * Used to mark the matches in the text as if they'd been gone over with a highlighter pen. We use
    * full yellow with half-alpha so you can see the selection through, as a dirty smudge, just like a real
    * highlighter pen might do.
    */
    private Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 0, 128));
    
    /** Returns the highlight painter that the find code should use to highlight matches. */
    public Highlighter.HighlightPainter getMatchHighlightPainter() {
        return painter;
    }
    
    public void findNext() {
        findHighlight(true);
    }
    
    public void findPrevious() {
        findHighlight(false);
    }
    
    private void findHighlight(boolean forwards) {
        Highlighter highlighter = getText().getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        int start = forwards ? 0 : highlights.length - 1;
        int stop = forwards ? highlights.length : -1;
        int step = forwards ? 1 : -1;
        for (int i = start; i != stop; i += step) {
            if (highlights[i].getPainter() == painter) {
                if (highlighterIsNext(forwards, highlights[i])) {
                    goToSelection(highlights[i].getStartOffset(), highlights[i].getEndOffset());
                    return;
                }
            }
        }
    }
    
    private boolean highlighterIsNext(boolean forwards, Highlighter.Highlight highlight) {
        final int minOffset = Math.min(highlight.getStartOffset(), highlight.getEndOffset());
        final int maxOffset = Math.max(highlight.getStartOffset(), highlight.getEndOffset());
        if (forwards) {
            return minOffset > text.getSelectionEnd();
        } else {
            return maxOffset < text.getSelectionStart();
        }
    }
    
    public boolean isFocusCycleRoot() {
        return true;
    }
    
    /**
     * Returns the offset corresponding to an actual start offset and an emacs line-relative offset.
     * Emacs, it seems, although it allows the user to alter the tab size, talks to the outside world
     * in terms of character cells rather than characters, and takes a tab to be eight cells (and all
     * other characters to be a single cell). So we, who use characters throughout, need to
     * translate emacs offsets into real offsets.
     */
    private int emacsWalk(int offset, int howFar) {
        try {
            while (--howFar > 0) {
                char c = text.getCharAt(offset);
                if (c == '\t') howFar -= 7;
                offset++;
            }
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
        return offset;
    }
    
    /**
     * Returns the distance between the given offset and the given line-start offset
     * in emacs' corrupted tabs-are-eight-spaces-and-not-characters-in-their-own-right
     * terms. Damn that program to hell.
     */
    private int emacsDistance(int pureCaretOffset, int lineStart) {
        int result = 0;
        try {
            for (int offset = lineStart; offset < pureCaretOffset; offset++) {
                char c = text.getCharAt(offset);
                if (c == '\t') result += 7;
                result++;
            }
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
        return result;
    }
    
    public void jumpToAddress(String address) {
        try {
            StringTokenizer st = new StringTokenizer(address, ":");
            int line = Integer.parseInt(st.nextToken()) - 1;
            int offset = text.getLineStartOffset(line);
            int maxOffset = text.getLineEndOffset(line) - 1;
            if (st.hasMoreTokens()) {
                try {
                    offset = emacsWalk(offset, Integer.parseInt(st.nextToken()));
                } catch (NumberFormatException ex) {}
            }
            
            // We interpret address ending with a ":" (from grep and compilers) as requiring
            // the line to be selected. Other addresses (such as from our open-file-list) just
            // mean "position the caret".
            int endOffset = (address.endsWith(":")) ? (maxOffset + 1) : offset;
            
            if (st.hasMoreTokens()) {
                try {
                    endOffset = text.getLineStartOffset(Integer.parseInt(st.nextToken()) - 1);
                } catch (NumberFormatException ex) {}
            }
            if (st.hasMoreTokens()) {
                try {
                    // emacs end offsets seem to include the character following.
                    endOffset = emacsWalk(endOffset, Integer.parseInt(st.nextToken())) + 1;
                } catch (NumberFormatException ex) {}
            }
            offset = Math.min(offset, maxOffset);
            endOffset = Math.min(endOffset, maxOffset);
            goToSelection(offset, endOffset);
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
    * Returns the name of the context for this window. In this case, the directory the file's in.
    */
    public String getContext() {
        return file.getParent();
    }
    
    public void reportError(String error) {
        getWorkspace().reportError(getContext(), error);
    }
    
    public boolean isOutOfDateWithRespectToDisk() {
        return file.exists() && file.lastModified() != lastModifiedTime;
    }
    
    /** Saves the text. Returns true if the file was saved okay. */
    public boolean save() {
        if (isOutOfDateWithRespectToDisk()) {
            //TODO: report when the file was modified & when your copy dates from.
            boolean replace = Edit.askQuestion("Save", "The file '" + this.filename + "' has been modified since it was read in. Do you want to replace it with the version you are saving?", "Replace");
            if (replace == false) {
                return false;
            }
        }
        
        if (file.exists() && copyFile(this.filename, this.filename + ".bak") == false) {
            Edit.showAlert("Save", "File '" + this.filename + "' wasn't saved! Couldn't create backup file.");
            return false;
        }
        
        try {
            Edit.showStatus("Saving " + FileUtilities.getUserFriendlyName(filename) + "...");
            writeCopyTo(file);
            Edit.showStatus("Saved " + FileUtilities.getUserFriendlyName(filename));
            markAsClean();
            this.lastModifiedTime = file.lastModified();
            return true;
        } catch (IOException ex) {
            Edit.showStatus("");
            Edit.showAlert("Save", "Couldn't save file '" + FileUtilities.getUserFriendlyName(filename) + "' (" + ex.getMessage() + ").");
            ex.printStackTrace();
        }
        return false;
    }
    
    /** Saves the text to a file with the given name. Returns true if the file was saved okay. */
    public boolean saveAs(String newFilename) {
        try {
            File newFile = FileUtilities.fileFromString(newFilename);
            if (newFile.exists()) {
                boolean replace = Edit.askQuestion("Save As", "An item named '" + newFilename + "' already exists in this location. Do you want to replace it with the one you are saving?", "Replace");
                if (replace == false) {
                    return false;
                }
            }
            writeCopyTo(newFile);
            return true;
        } catch (Exception ex) {
            Edit.showAlert("Save As", "Couldn't save file '" + newFilename + "' (" + ex.getMessage() + ").");
            ex.printStackTrace();
        }
        return false;
    }
    
    public void writeCopyTo(File file) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(file));
        text.write(writer);
        writer.close();
    }
    
    /** FIXME: replace with use of File.renameTo */
    public boolean copyFile(String fromName, String toName) {
        try {
            FileInputStream from = new FileInputStream(fromName);
            FileOutputStream to = new FileOutputStream(toName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
            to.close();
            from.close();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    /** Executes the given command. */
    public void execute(String command) {
        invokeShellCommand(command.trim());
    }
    
    public void invokeShellCommand(String command) {
        invokeShellCommand(getContext(), command);
    }
    
    public void invokeShellCommand(String context, String command) {
        try {
            new ShellCommand(filename, getCurrentLineNumber(), getWorkspace(), context, command);
        } catch (IOException ex) {
            Edit.showAlert("Run", "Can't start task (" + ex.getMessage() + ").");
        }
    }
    
    public void stateChanged(ChangeEvent e) {
        repaintTitle();
    }
}
