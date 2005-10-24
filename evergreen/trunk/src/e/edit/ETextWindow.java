package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.Timer;
import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;

/**
 * A text-editing component.
 */
public class ETextWindow extends EWindow implements PTextListener {
    /**
     * Ensures that the TagsPanel is empty if the focused window isn't an ETextWindow.
     */
    static {
        new KeyboardFocusMonitor() {
            public void focusChanged(Component oldOwner, Component newOwner) {
                if (newOwner instanceof EWindow && newOwner instanceof ETextWindow == false) {
                    Edit.getInstance().getTagsPanel().ensureTagsAreHidden();
                }
            }
        };
    }
    
    protected String filename;
    protected File file;
    private long lastModifiedTime;
    protected ETextArea text;
    private BirdView birdView;
    private TagsUpdater tagsUpdater;
    
    // Used to display a watermark to indicate such things as a read-only file.
    private WatermarkViewPort watermarkViewPort;
    // Used to update the watermark without creating and destroying an excessive number of threads.
    private final ExecutorService watermarkUpdateExecutor = Executors.newSingleThreadExecutor();
    
    private static final Color FOCUSED_SELECTION_COLOR = new Color(0.70f, 0.83f, 1.00f);
    private static final Color UNFOCUSED_SELECTION_COLOR = new Color(0.83f, 0.83f, 0.83f);
    
    public static final String UNKNOWN = "Unknown";
    public static final String BASH = "Bash";
    public static final String C_PLUS_PLUS = "C++";
    public static final String JAVA = "Java";
    public static final String RUBY = "Ruby";
    public static final String PERL = "Perl";
    
    private String fileType = UNKNOWN;
    
    private static final HashMap<String, HashSet<String>> SPELLING_EXCEPTIONS_MAP = new HashMap<String, HashSet<String>>();

    private Timer findResultsUpdater;
    
    private HashSet<String> initSpellingExceptionsFor(String language) {
        HashSet<String> result = new HashSet<String>();
        
        // The text styler knows all the language's keywords.
        text.getTextStyler().addKeywordsTo(result);
        
        // The JavaResearcher knows all the words used in JDK identifiers.
        if (language == JAVA) {
            JavaResearcher.addJavaWordsTo(result);
        }
        
        // And there may be a file of extra spelling exceptions for this language.
        String exceptionsFileName = Edit.getInstance().getResourceFilename("spelling-exceptions-" + language);
        if (FileUtilities.exists(exceptionsFileName)) {
            for (String exception : StringUtilities.readLinesFromFile(exceptionsFileName)) {
                if (exception.startsWith("#")) {
                    continue; // Ignore comments.
                }
                result.add(exception);
            }
        }
        SPELLING_EXCEPTIONS_MAP.put(language, result);
        return result;
    }
    
    public ETextWindow(String filename) {
        super(filename);
        this.filename = filename;
        this.file = FileUtilities.fileFromString(filename);
        this.text = new ETextArea();
        text.getTextBuffer().addTextListener(this);
        initBugDatabaseLinks();
        initTextAreaPopupMenu();
        
        this.watermarkViewPort = new WatermarkViewPort();
        watermarkViewPort.setView(text);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewport(watermarkViewPort);
        
        initFocusListener();
        this.birdView = new BirdView(this, scrollPane.getVerticalScrollBar());
        add(scrollPane, BorderLayout.CENTER);
        add(birdView, BorderLayout.EAST);
        fillWithContent();
        tagsUpdater = new TagsUpdater(this);
        initFindResultsUpdater();
    }
    
    private void initBugDatabaseLinks() {
        try {
            // See if SCM's bug database link code is available.
            Class klass = Class.forName("e.scm.BugDatabaseHighlighter");
            // Use it.
            java.lang.reflect.Constructor constructor = klass.getConstructor(PTextArea.class);
            text.addStyleApplicator(StyleApplicator.class.cast(constructor.newInstance(text)));
        } catch (Exception ex) {
            // We can survive without this functionality, but there's probably
            // something odd going on if we can't find it.
            Log.warn("Couldn't initialize bug database links.", ex);
        }
    }
    
    private void initFindResultsUpdater() {
        findResultsUpdater = new Timer(150, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                text.updateFindResults();
            }
        });
        findResultsUpdater.setRepeats(false);
    }

    private void initFocusListener() {
        text.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                rememberWeHadFocusLast();
                updateWatermarkAndTitleBar();
            }
            
            public void focusLost(FocusEvent e) {
            }
            
            private void rememberWeHadFocusLast() {
                Workspace workspace = (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, ETextWindow.this);
                workspace.rememberFocusedTextWindow(ETextWindow.this);
            }
        });
    }
    
    public BirdView getBirdView() {
        return birdView;
    }
    
    public String getFilename() {
        return filename;
    }
    
    /** Returns the grep-style ":<line>:<column>" address for the caret position. */
    public String getAddress() {
        String result = addressFromOffset(text.getSelectionStart());
        if (text.hasSelection()) {
            // emacs end offsets seem to include the character following.
            result += addressFromOffset(text.getSelectionEnd() - 1);
        }
        return result;
    }
    
    private String addressFromOffset(int offset) {
        int lineNumber = 1 + text.getLineOfOffset(offset);
        int lineStart = text.getLineStartOffset(lineNumber - 1);
        int columnNumber = 1 + emacsDistance(text.getTextBuffer(), offset, lineStart);
        return ":" + lineNumber + ":" + columnNumber;
    }
    
    public void requestFocus() {
        text.requestFocus();
    }
    
    //
    // PTextListener interface.
    //
    
    public void textCompletelyReplaced(PTextEvent e) {
        textBecameDirty();
    }
    
    public void textRemoved(PTextEvent e) {
        textBecameDirty();
    }
    
    public void textInserted(PTextEvent e) {
        textBecameDirty();
    }
    
    /** Tests whether the 'content' looks like a Unix shell script. */
    private static boolean isInterpretedContent(CharSequence content, String interpreter) {
        return Pattern.compile("^#![^\\n]*" + interpreter).matcher(content).find();
    }
    
    /** Tests whether the 'content' looks like a Unix shell script, of the Ruby variety. */
    private static boolean isRubyContent(CharSequence content) {
        return isInterpretedContent(content, "/ruby");
    }
    
    /** Tests whether the 'content' looks like a Unix shell script, of the Perl variety. */
    private static boolean isPerlContent(CharSequence content) {
        return isInterpretedContent(content, "/perl");
    }
    
    private static boolean isBashContent(CharSequence content) {
        return isInterpretedContent(content, "/(bash|sh)");
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
    private static boolean isCPlusPlusContent(CharSequence content) {
        return Pattern.compile("(#ifndef|" + StringUtilities.regularExpressionFromLiteral("-*- C++ -*-") + ")").matcher(content).find();
    }
    
    private HashSet<String> getSpellingExceptionsForLanguage(String language) {
        HashSet<String> exceptions = SPELLING_EXCEPTIONS_MAP.get(language);
        if (exceptions == null) {
            exceptions = initSpellingExceptionsFor(language);
        }
        return exceptions;
    }
    
    /**
     * Attaches an appropriate set of spelling exceptions to our document.
     */
    private void initSpellingExceptionsForDocument() {
        text.putClientProperty(PTextAreaSpellingChecker.SPELLING_EXCEPTIONS_PROPERTY, getSpellingExceptionsForLanguage(fileType));
    }
    
    public void updateWatermarkAndTitleBar() {
        watermarkUpdateExecutor.execute(new Runnable() {
            // Fields initialized in "run" so not even that work is done on the EDT.
            private ArrayList<String> items;
            private boolean isSerious;
            private boolean hasCounterpart;
            
            private void init() {
                items = new ArrayList<String>();
                isSerious = false;
                hasCounterpart = false;
            }
            
            public void run() {
                init();
                doFileChecks();
                updateUiOnEventThread();
            }
            
            private void doFileChecks() {
                if (file.exists() == false) {
                    items.add("(deleted)");
                    isSerious = true;
                }
                if (file.exists() && file.canWrite() == false) {
                    items.add("(read-only)");
                }
                if (file.exists() && isOutOfDateWithRespectToDisk()) {
                    items.add("(out-of-date)");
                    isSerious = true;
                }
                hasCounterpart = (getCounterpartFilename() != null);
            }
            
            private void updateUiOnEventThread() {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        watermarkViewPort.setSerious(isSerious);
                        watermarkViewPort.setWatermark(items.size() > 0 ? StringUtilities.join(items, " ") : null);
                        getTitleBar().setShowSwitchButton(hasCounterpart);
                    }
                });
            }
        });
    }
    
    private void fillWithContent() {
        try {
            lastModifiedTime = file.lastModified();
            text.getTextBuffer().readFromFile(file);
            
            configureForGuessedFileType();
            updateWatermarkAndTitleBar();
            text.getTextBuffer().getUndoBuffer().resetUndoBuffer();
            text.getTextBuffer().getUndoBuffer().setCurrentStateClean();
            getTitleBar().repaint();
        } catch (Throwable th) {
            Log.warn("in ContentLoader exception handler", th);
            Edit.getInstance().showAlert("Open", "Couldn't open file '" + FileUtilities.getUserFriendlyName(file) + "' (" + th.getMessage() + ")");
            throw new RuntimeException("can't open " + FileUtilities.getUserFriendlyName(file));
        }
    }
    
    private void configureForGuessedFileType() {
        String originalFileType = fileType;
        CharSequence content  = text.getTextBuffer();
        if (filename.endsWith(".java")) {
            fileType = JAVA;
            text.setIndenter(new PJavaIndenter(text));
            text.setTextStyler(new PJavaTextStyler(text));
        } else if (isRubyContent(content)) {
            fileType = RUBY;
            text.setIndenter(new PRubyIndenter(text));
            text.setTextStyler(new PRubyTextStyler(text));
        } else if (filename.endsWith(".cpp") || filename.endsWith(".hpp") || filename.endsWith(".c") || filename.endsWith(".h") || filename.endsWith(".m") || filename.endsWith(".mm") || filename.endsWith(".hh") || filename.endsWith(".cc") || filename.endsWith(".strings") || isCPlusPlusContent(content)) {
            fileType = C_PLUS_PLUS;
            text.setIndenter(new PCppIndenter(text));
            text.setTextStyler(new PCPPTextStyler(text));
        } else if (filename.endsWith(".pl") || filename.endsWith(".pm") || isPerlContent(content)) {
            fileType = PERL;
            text.setIndenter(new PPerlIndenter(text));
            text.setTextStyler(new PPerlTextStyler(text));
        } else if (isBashContent(content)) {
            fileType = BASH;
            text.setTextStyler(new PBashTextStyler(text));
        } else {
            // Plain text.
            text.setWrapStyleWord(true);
        }
        if (fileType != originalFileType) {
            initSpellingExceptionsForDocument();
        }
        text.setFont(ChangeFontAction.getAppropriateFontForContent(content));
        text.getIndenter().setIndentationPropertyBasedOnContent(content);
    }
    
    private void uncheckedRevertToSaved() {
        // FIXME - work with non-empty selection
        int originalCaretPosition = text.getSelectionStart();
        fillWithContent();
        text.setCaretPosition(originalCaretPosition);
        Edit.getInstance().showStatus("Reverted to saved version of " + filename);
    }
    
    private boolean showPatchAndAskForConfirmation(String verb, String question, boolean fromDiskToMemory) {
        String diskLabel = "disk at " + FileUtilities.getLastModifiedTime(file);
        String memoryLabel = "memory";
        
        String diskContent;
        try {
            diskContent = StringUtilities.readFile(file);
        } catch (Exception ex) {
            Log.warn("Couldn't read file for patch", ex);
            // Pretend that the file is empty. The most likely reason for being
            // here is that the file has been deleted.
            diskContent = "";
            diskLabel = "(couldn't read file)";
        }
        String memoryContent = text.getTextBuffer().toString();
        
        String fromLabel = fromDiskToMemory ? diskLabel : memoryLabel;
        String toLabel = fromDiskToMemory ? memoryLabel : diskLabel;
        
        String fromContent = fromDiskToMemory ? diskContent : memoryContent;
        String toContent = fromDiskToMemory ? memoryContent : diskContent;
        
        JComponent patchView = SimplePatchDialog.makeScrollablePatchView(fromLabel, fromContent, toLabel, toContent);
        
        String title = verb;
        String buttonLabel = verb;
        FormBuilder form = new FormBuilder(Edit.getInstance().getFrame(), title);
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("", new JLabel(question));
        formPanel.addRow("Patch:", patchView);
        return form.show(buttonLabel);
    }
    
    public void revertToSaved() {
        if (file.exists() == false) {
            Edit.getInstance().showAlert("Revert to Saved", "'" + getFilename() + "' does not exist.");
            return;
        }
        if (isDirty() == false && file.exists() && isOutOfDateWithRespectToDisk() == false) {
            Edit.getInstance().showAlert("Revert to Saved", "'" + getFilename() + "' is the same on disk as in the editor.");
            return;
        }
        if (showPatchAndAskForConfirmation("Revert to Saved", "Revert to on-disk version of '" + file.getName() + "'? (Equivalent to applying the following patch.)", true)) {
            uncheckedRevertToSaved();
        }
    }
    
    public ETextArea getText() {
        return text;
    }
    
    public void windowWillClose() {
        if (findResultsUpdater != null) {
            findResultsUpdater.stop();
            findResultsUpdater = null;
        }
        Edit.getInstance().showStatus("Closed " + filename);
        getWorkspace().unregisterTextComponent(getText());
        // FIXME: what else needs doing to ensure that we give back memory?
    }
    
    /**
     * Closes this text window if the text isn't dirty.
     */
    public void closeWindow() {
        if (isDirty()) {
            if (showPatchAndAskForConfirmation("Discard", "Discard changes to '" + file.getName() + "'? (Equivalent to applying the following patch.)", true) == false) {
                return;
            }
        }
        Edit.getInstance().getTagsPanel().ensureTagsAreHidden();
        super.closeWindow();
    }
    
    public void initTextAreaPopupMenu() {
        text.getPopupMenu().addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                actions.add(new OpenQuicklyAction());
                actions.add(new FindFilesContainingSelectionAction());
                actions.add(new RevertToSavedAction());
                actions.add(null);
                actions.add(new CheckInChangesAction());
                actions.add(new ShowHistoryAction());
                // FIXME: this could be an additional provider. Would there be any advantage?
                addExternalToolMenuItems(actions);
            }
        });
    }

    public void addExternalToolMenuItems(final Collection<Action> items) {
        ExternalToolsParser toolsParser = new ExternalToolsParser() {
            public void addItem(ExternalToolAction action) {
                addAction(action);
            }

            public void addItem(ExternalToolAction action, char keyboardEquivalent) {
                addAction(action);
            }

            private void addAction(ExternalToolAction action) {
                /* Ignore ExternalToolActions that aren't context-sensitive. */
                if (action.isContextSensitive() == false) {
                        return;
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
        
        String parent = file.getParent();
        if (filename.endsWith(".cpp") || filename.endsWith(".cc") || filename.endsWith(".c")) {
            if (FileUtilities.exists(parent, basename + ".hpp")) {
                return basename + ".hpp";
            } else if (FileUtilities.exists(parent, basename + ".hh")) {
                return basename + ".hh";
            } else if (FileUtilities.exists(parent, basename + ".h")) {
                return basename + ".h";
            }
        } else if (filename.endsWith(".hpp") || filename.endsWith(".hh") || filename.endsWith(".h")) {
            if (FileUtilities.exists(parent, basename + ".cpp")) {
                return basename + ".cpp";
            } else if (FileUtilities.exists(parent, basename + ".cc")) {
                    return basename + ".cc";
            } else if (FileUtilities.exists(parent, basename + ".c")) {
                return basename + ".c";
            }
        }
        return null;
    }

    public void switchToCounterpart() {
        String counterpartFilename = getCounterpartFilename();
        if (counterpartFilename != null) {
            Edit.getInstance().openFile(getContext() + File.separator + counterpartFilename);
        } else {
            Edit.getInstance().showAlert("Switch To Counterpart", "File '" + filename + "' has no counterpart.");
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
    
    /**
     * FIXME: this should be replaced with a proper system of Mode,
     * incorporating Indenter and all the other stuff currently done
     * by asking isRuby and isJava et cetera.
     */
    public String getFileType() {
        return fileType;
    }
    
    public void goToLine(int line) {
        // Humans number lines from 1, JTextComponent from 0.
        line--;
        final int start = text.getLineStartOffset(line);
        final int end = text.getLineEndOffsetBeforeTerminator(line);
        centerOnNewSelection(start, end);
    }
    
    /**
     * Changes the selection, and centers the selection on the display.
     */
    private void centerOnNewSelection(final int start, final int end) {
        // Center first, to avoid flicker because PTextArea.select may
        // have caused some scrolling to ensure that the selection is
        // visible, but probably won't have to scrolled such that our
        // offset is centered.
        text.centerOffsetInDisplay(start);
        text.select(start, end);
    }
    
    public int getCurrentLineNumber() {
        // Humans number lines from 1, JTextComponent from 0.
        // FIXME - work with non-empty selections
        return 1 + text.getLineOfOffset(text.getSelectionStart());
    }
    
    public boolean isDirty() {
        return ! text.getTextBuffer().getUndoBuffer().isClean();
    }
    
    public void textBecameDirty() {
        if (findResultsUpdater != null) {
            findResultsUpdater.restart();
        }
        getTitleBar().repaint();
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
    private int emacsWalk(CharSequence chars, int offset, int howFar) {
        while (--howFar > 0) {
            char c = chars.charAt(offset);
            if (c == '\t') howFar -= 7;
            offset++;
        }
        return offset;
    }
    
    /**
     * Returns the distance between the given offset and the given line-start offset
     * in emacs' corrupted tabs-are-eight-spaces-and-not-characters-in-their-own-right
     * terms. Damn that program to hell.
     */
    private int emacsDistance(CharSequence chars, int pureCaretOffset, int lineStart) {
        int result = 0;
        for (int offset = lineStart; offset < pureCaretOffset; offset++) {
            char c = chars.charAt(offset);
            if (c == '\t') result += 7;
            result++;
        }
        return result;
    }
    
    public void jumpToAddress(String address) {
        CharSequence chars = text.getTextBuffer();
        StringTokenizer st = new StringTokenizer(address, ":");
        if (st.hasMoreTokens() == false) {
            return;
        }
        int line = Integer.parseInt(st.nextToken()) - 1;
        int offset = text.getLineStartOffset(line);
        int maxOffset = text.getLineEndOffsetBeforeTerminator(line);
        if (st.hasMoreTokens()) {
            try {
                offset = emacsWalk(chars, offset, Integer.parseInt(st.nextToken()));
            } catch (NumberFormatException ex) {
                ex = ex;
            }
        }
        offset = Math.min(offset, maxOffset);
        
        // We interpret address ending with a ":" (from grep and compilers) as requiring
        // the line to be selected. Other addresses (such as from our open-file-list) just
        // mean "position the caret".
        int endOffset = (address.endsWith(":")) ? (maxOffset + 1) : offset;
        endOffset = Math.min(endOffset, maxOffset);
        
        if (st.hasMoreTokens()) {
            try {
                endOffset = text.getLineStartOffset(Integer.parseInt(st.nextToken()) - 1);
            } catch (NumberFormatException ex) {
                ex = ex;
            }
        }
        if (st.hasMoreTokens()) {
            try {
                // emacs end offsets seem to include the character following.
                endOffset = emacsWalk(chars, endOffset, Integer.parseInt(st.nextToken())) + 1;
            } catch (NumberFormatException ex) {
                ex = ex;
            }
        }
        centerOnNewSelection(offset, endOffset);
    }
    
    /**
    * Returns the name of the context for this window. In this case, the directory the file's in.
    */
    public String getContext() {
        return FileUtilities.getUserFriendlyName(file.getParent());
    }
    
    public void reportError(final String error) {
        getWorkspace().reportError(error);
    }
    
    private boolean isOutOfDateWithRespectToDisk() {
        // If the time stamp on disk is the same as it was when we last read
        // or wrote the file, assume it hasn't changed.
        if (file.lastModified() == lastModifiedTime) {
            return false;
        }
        
        // If the on-disk content is the same as what we have in memory, then
        // the fact that the time stamp is different isn't significant.
        try {
            String currentContentInMemory = getText().getText();
            String currentContentOnDisk = StringUtilities.readFile(file);
            if (currentContentInMemory.equals(currentContentOnDisk)) {
                lastModifiedTime = file.lastModified();
                return false;
            }
        } catch (Exception ex) {
            Log.warn("Couldn't compare with on-disk copy.", ex);
        }
        
        return true;
    }
    
    /** Saves the text. Returns true if the file was saved okay. */
    public boolean save() {
        if (file.exists() && isOutOfDateWithRespectToDisk()) {
            if (showPatchAndAskForConfirmation("Overwrite", "Overwrite the currently saved version of '" + file.getName() + "'? (Equivalent to applying the following patch.)", false) == false) {
                return false;
            }
        }
        
        Edit edit = Edit.getInstance();
        
        // Try to save a backup copy first. Ideally, we should do this from
        // a timer and always have a recent backup around.
        File backupFile = FileUtilities.fileFromString(this.filename + ".bak");
        if (file.exists()) {
            try {
                writeToFile(backupFile);
            } catch (Exception ex) {
                edit.showAlert("Save", "File '" + this.filename + "' wasn't saved! Couldn't create backup file.");
                return false;
            }
        }
        
        try {
            edit.showStatus("Saving " + filename + "...");
            // The file may be a symlink on a cifs server.
            // In this case, it's important that we write into the original file rather than creating a new one.
            writeToFile(file);
            text.getTextBuffer().getUndoBuffer().setCurrentStateClean();
            getTitleBar().repaint();
            edit.showStatus("Saved " + filename);
            backupFile.delete();
            this.lastModifiedTime = file.lastModified();
            configureForGuessedFileType();
            updateWatermarkAndTitleBar();
            tagsUpdater.updateTags();
            SaveMonitor.getInstance().fireSaveListeners();
            return true;
        } catch (Exception ex) {
            edit.showStatus("");
            edit.showAlert("Save", "Couldn't save file '" + filename + "' (" + ex.getMessage() + ").");
            Log.warn("Problem saving \"" + filename + "\"", ex);
        }
        return false;
    }
    
    /** Saves the text to a file with the given name. Returns true if the file was saved okay. */
    public boolean saveAs(String newFilename) {
        try {
            File newFile = FileUtilities.fileFromString(newFilename);
            if (newFile.exists()) {
                boolean replace = Edit.getInstance().askQuestion("Save As", "An item named '" + newFilename + "' already exists in this location. Do you want to replace it with the one you are saving?", "Replace");
                if (replace == false) {
                    return false;
                }
            }
            writeToFile(newFile);
            return true;
        } catch (Exception ex) {
            Edit.getInstance().showAlert("Save As", "Couldn't save file '" + newFilename + "' (" + ex.getMessage() + ").");
            Log.warn("Problem saving as \"" + newFilename + "\"", ex);
        }
        return false;
    }
    
    private void writeToFile(File file) {
        ensureBufferEndsInNewline();
        text.getTextBuffer().writeToFile(file);
    }
    
    /**
     * JLS3 3.4 implies that all files must end with a line terminator. It's
     * pointless requiring a round trip for the user if this isn't the case,
     * so we silently fix their files.
     */
    private void ensureBufferEndsInNewline() {
        if (isCPlusPlus() || isJava()) {
            PTextBuffer buffer = text.getTextBuffer();
            if (buffer.length() == 0 || buffer.charAt(buffer.length() - 1) != '\n') {
                // '\n' is always correct; the buffer will translate if needed.
                text.append("\n");
            }
        }
    }
    
    /**
     * Implements the Comparable interface so windows can be sorted
     * into alphabetical order by title.
     */
    public int compareTo(Object other) {
        return getTitle().compareTo(((EWindow) other).getTitle());
    }
}

