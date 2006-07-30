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
                    Evergreen.getInstance().getTagsPanel().ensureTagsAreHidden();
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
    private static final ExecutorService watermarkUpdateExecutor = ThreadUtilities.newSingleThreadExecutor("Watermark Updater");
    
    private static final Color FOCUSED_SELECTION_COLOR = new Color(0.70f, 0.83f, 1.00f);
    private static final Color UNFOCUSED_SELECTION_COLOR = new Color(0.83f, 0.83f, 0.83f);
    
    public static final String PLAIN_TEXT = "Plain Text";
    public static final String BASH = "Bash";
    public static final String C_PLUS_PLUS = "C++";
    public static final String JAVA = "Java";
    public static final String MAKE = "Make";
    public static final String RUBY = "Ruby";
    public static final String PERL = "Perl";
    public static final String PYTHON = "Python";
    
    private String fileType = PLAIN_TEXT;
    
    private static final HashMap<String, HashSet<String>> SPELLING_EXCEPTIONS_MAP = new HashMap<String, HashSet<String>>();

    private Timer findResultsUpdater;
    
    private HashSet<String> initSpellingExceptionsFor(String language) {
        HashSet<String> result = new HashSet<String>();
        
        // The text styler knows all the language's keywords.
        text.getTextStyler().addKeywordsTo(result);
        
        // The JavaResearcher knows all the words used in JDK identifiers.
        if (language == JAVA) {
            JavaResearcher.getSharedInstance().addJavaWordsTo(result);
        }
        // The ManPageResearcher knows all the words used in identifiers that have man pages.
        if (language == C_PLUS_PLUS) {
            ManPageResearcher.getSharedInstance().addManPageWordsTo(result);
        }
        
        // And there may be a file of extra spelling exceptions for this language.
        String exceptionsFileName = Evergreen.getInstance().getResourceFilename("spelling-exceptions-" + language);
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
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewport(watermarkViewPort);
        
        initCaretListener();
        initFocusListener();
        this.birdView = new BirdView(new PTextAreaBirdsEye(text), scrollPane.getVerticalScrollBar());
        add(scrollPane, BorderLayout.CENTER);
        add(birdView, BorderLayout.EAST);
        
        fillWithContent();
        initUserConfigurableDefaults();
        
        tagsUpdater = new TagsUpdater(this);
        initFindResultsUpdater();
    }
    
    private void initUserConfigurableDefaults() {
        // Since the user can modify these settings, we should only set them
        // from the constructor to avoid reverting the user's configuration.
        // I don't think that even reverting to the saved content should revert
        // configuration changes, until I see a convincing demonstration
        // otherwise.
        // Phil's been bitten by the indentation string reverting while editing
        // TSV files, and I've been repeatedly bitten by a file I want to see
        // in a fixed font reverting to a proportional font each time I save.
        CharSequence content = text.getTextBuffer();
        text.setFont(ChangeFontAction.getAppropriateFontForContent(content));
        text.getIndenter().setIndentationPropertyBasedOnContent(content);
    }
    
    private void initBugDatabaseLinks() {
        BugDatabaseHighlighter.highlightBugs(text);
    }
    
    private void initCaretListener() {
        text.addCaretListener(new PCaretListener() {
            public void caretMoved(PTextArea textArea, int selectionStart, int selectionEnd) {
                String message = "";
                if (selectionStart != selectionEnd) {
                    // Describe the selected range.
                    final int startLineNumber = 1 + text.getLineOfOffset(selectionStart);
                    final int endLineNumber = 1 + text.getLineOfOffset(selectionEnd);
                    if (startLineNumber == endLineNumber) {
                        final int endLineStartOffset = text.getLineStartOffset(endLineNumber - 1);
                        message = "Selected " + addressFromOffset(selectionStart, "line ", " columns ") + "-" + (1 + emacsDistance(text.getTextBuffer(), selectionEnd, endLineStartOffset));
                    } else {
                        message = "Selected from line " + startLineNumber + " to " + endLineNumber;
                    }
                    
                    // Describe the size of the selection.
                    message += " (";
                    message += StringUtilities.pluralize(selectionEnd - selectionStart, "character", "characters");
                    // FIXME: if we report this, we look right if the user's selected whole lines; if we add one, we look right if the user's selected partial lines. It would be nice to give a natural answer in both cases, but for now we go with whole lines because if the user's counting lines, that's more likely to be what they're interested in.
                    final int lineCount = endLineNumber - startLineNumber;
                    if (lineCount > 1) {
                        message += " on " + lineCount + " lines";
                    }
                    message += ")";
                } else {
                    // Describe the location of the caret.
                    message = "At " + addressFromOffset(selectionStart, "line ", ", column ");
                }
                Evergreen.getInstance().showStatus(message);
            }
        });
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
        String result = addressFromOffset(text.getSelectionStart(), ":", ":");
        if (text.hasSelection()) {
            // emacs end offsets seem to include the character following.
            result += addressFromOffset(text.getSelectionEnd() - 1, ":", ":");
        }
        return result;
    }
    
    private String addressFromOffset(final int offset, final String linePrefix, final String columnPrefix) {
        int lineNumber = 1 + text.getLineOfOffset(offset);
        int lineStart = text.getLineStartOffset(lineNumber - 1);
        int columnNumber = 1 + emacsDistance(text.getTextBuffer(), offset, lineStart);
        return linePrefix + lineNumber + columnPrefix + columnNumber;
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
    
    /**
     * Tests whether the 'content' looks like a Unix shell script. If
     * 'interpreter' is a shell, you should probably prepend "/" to avoid
     * false positives; if 'interpreter' is a scripting language, you should
     * probably avoid doing so because it's still relatively common practice
     * to use the env(1) hack. We could perhaps automate this by matching
     * either ("/" + interpreter) or ("/env\\s[^\\n]*" + interpreter).
     */
    private static boolean isInterpretedContent(CharSequence content, String interpreter) {
        return Pattern.compile("^#![^\\n]*" + interpreter).matcher(content).find();
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Ruby variety. */
    private static boolean isRubyContent(CharSequence content) {
        return isInterpretedContent(content, "ruby");
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Perl variety. */
    private static boolean isPerlContent(CharSequence content) {
        return isInterpretedContent(content, "perl");
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Python variety. */
    private static boolean isPythonContent(CharSequence content) {
        return isInterpretedContent(content, "python");
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
        // FIXME: this method has an over-simplified idea of emacs mode strings, based on the STL header file usage.
        // Here are two examples. The former shows that we could use a similar check for Ruby; the latter that we ought to recognize another form, and that we can also potentially find the tab string this way.
        // hydrogen:/usr/lib/ruby/1.8$ grep -n -- '-\*-' *
        // getoptlong.rb:1:#                                                         -*- Ruby -*-
        // yaml.rb:1:# -*- mode: ruby; ruby-indent-level: 4; tab-width: 4 -*- vim: sw=4 ts=4
        // We should also probably recognize plain C (since our C_PLUS_PLUS means C/C++/Objective-C/Objective-C++):
        // powerpc-darwin8.0/dl.h:1:/* -*- C -*-
        // FIXME: emacs mode strings should be handled separately, and override content-based file type determination.
        // FIXME: gEdit's "modelines" plug-in http://cvs.gnome.org/viewcvs/gedit/plugins/modelines/ details emacs(1), kate(1), and vim(1) mode lines.
        return Pattern.compile("(#ifndef|" + StringUtilities.regularExpressionFromLiteral("-*- C++ -*-") + ")").matcher(content).find();
    }
    
    private void highlightMergeConflicts() {
        // FIXME: this works for BitKeeper, but is it right for Subversion too?
        final String MERGE_CONFLICT_REGULAR_EXPRESSION = "(<{7} .*|>{7})";
        if (Pattern.compile(MERGE_CONFLICT_REGULAR_EXPRESSION).matcher(text.getTextBuffer()).find()) {
            System.err.println("found evidence of merge conflicts");
            FindAction.INSTANCE.findInText(this, MERGE_CONFLICT_REGULAR_EXPRESSION);
        }
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
            highlightMergeConflicts();
            text.getTextBuffer().getUndoBuffer().resetUndoBuffer();
            text.getTextBuffer().getUndoBuffer().setCurrentStateClean();
            getTitleBar().repaint();
        } catch (Throwable th) {
            Log.warn("in ContentLoader exception handler", th);
            Evergreen.getInstance().showAlert("Couldn't open file \"" + FileUtilities.getUserFriendlyName(file) + "\"", th.getMessage());
            throw new RuntimeException("can't open " + FileUtilities.getUserFriendlyName(file));
        }
    }
    
    /**
     * There are three main ways to guess a file's type: by content, by name,
     * or by emacs mode string. The initFileType methods implement this, though
     * there's no uniform checking for emacs mode strings (the perlrun man page,
     * for example, mentions "#!/bin/sh -- # -*- perl -*- -p" but we wouldn't
     * currently recognize such a script as a Perl script, even though we would
     * recognize a C++ files by its emacs mode string).
     */
    private void initFileType() {
        // See if we can infer the type by name first and fall back to guessing
        // from the content. If you don't do it this way round, you get fooled
        // by files (such as this one) that contain things that look like
        // suggestive content. It's hard to see that there's ever any excuse
        // for having the wrong filename extension.
        initFileTypeByName();
        if (fileType == PLAIN_TEXT) {
            initFileTypeByContent();
        }
    }
    private void initFileTypeByContent() {
        CharSequence content  = text.getTextBuffer();
        if (isRubyContent(content)) {
            fileType = RUBY;
        } else if (isBashContent(content)) {
            fileType = BASH;
        } else if (isCPlusPlusContent(content)) {
            fileType = C_PLUS_PLUS;
        } else if (isPerlContent(content)) {
            fileType = PERL;
        } else if (isPythonContent(content)) {
            fileType = PYTHON;
        } else {
            fileType = PLAIN_TEXT;
        }
    }
    private void initFileTypeByName() {
        if (filename.endsWith(".java")) {
            fileType = JAVA;
        } else if (filename.endsWith(".cpp") || filename.endsWith(".hpp") || filename.endsWith(".c") || filename.endsWith(".h") || filename.endsWith(".m") || filename.endsWith(".mm") || filename.endsWith(".hh") || filename.endsWith(".cc") || filename.endsWith(".strings")) {
            fileType = C_PLUS_PLUS;
        } else if (filename.endsWith(".pl") || filename.endsWith(".pm")) {
            fileType = PERL;
        } else if (filename.endsWith(".py")) {
            fileType = PYTHON;
        } else if (filename.endsWith(".rb")) {
            fileType = RUBY;
        } else if (filename.endsWith(".sh") || filename.endsWith("bash.bashrc") || filename.endsWith("bash.logout") || filename.endsWith(".bash_profile") || filename.endsWith(".bashrc") || filename.endsWith(".bash_logout")) {
            fileType = BASH;
        } else if (filename.endsWith("Makefile") || filename.endsWith("GNUmakefile") || filename.endsWith("makefile") || filename.endsWith(".make")) {
            fileType = MAKE;
        } else {
            fileType = PLAIN_TEXT;
        }
    }
    
    private void configureForGuessedFileType() {
        String originalFileType = fileType;
        initFileType();
        if (fileType == JAVA) {
            text.setIndenter(new PJavaIndenter(text));
            text.setTextStyler(new PJavaTextStyler(text));
        } else if (fileType == RUBY) {
            text.setIndenter(new PRubyIndenter(text));
            text.setTextStyler(new PRubyTextStyler(text));
        } else if (fileType == C_PLUS_PLUS) {
            text.setIndenter(new PCppIndenter(text));
            text.setTextStyler(new PCPPTextStyler(text, filename.matches(".*\\.(m|mm)$")));
        } else if (fileType == PERL) {
            text.setIndenter(new PPerlIndenter(text));
            text.setTextStyler(new PPerlTextStyler(text));
        } else if (fileType == PYTHON) {
            //text.setIndenter(new PPythonIndenter(text));
            text.setTextStyler(new PPythonTextStyler(text));
        } else if (fileType == BASH) {
            text.setTextStyler(new PBashTextStyler(text));
        } else if (fileType == MAKE) {
            text.setTextStyler(new PMakefileTextStyler(text));
        } else {
            // Plain text.
            text.setWrapStyleWord(true);
        }
        if (fileType != originalFileType) {
            initSpellingExceptionsForDocument();
        }
    }
    
    private void uncheckedRevertToSaved() {
        // FIXME - work with non-empty selection
        int originalCaretPosition = text.getSelectionStart();
        fillWithContent();
        text.setCaretPosition(originalCaretPosition);
        Evergreen.getInstance().showStatus("Reverted to saved version of " + filename);
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
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), title);
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("", new JLabel(question));
        formPanel.addRow("Patch:", patchView);
        return form.show(buttonLabel);
    }
    
    public void revertToSaved() {
        if (file.exists() == false) {
            Evergreen.getInstance().showAlert("Can't revert to saved", "\"" + getFilename() + "\" does not exist.");
            return;
        }
        if (isDirty() == false && file.exists() && isOutOfDateWithRespectToDisk() == false) {
            Evergreen.getInstance().showAlert("Can't revert to saved", "\"" + getFilename() + "\" is the same on disk as in the editor.");
            return;
        }
        if (showPatchAndAskForConfirmation("Revert to Saved", "Revert to on-disk version of \"" + file.getName() + "\"? (Equivalent to applying the following patch.)", true)) {
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
        Evergreen.getInstance().showStatus("Closed " + filename);
        // FIXME: what else needs doing to ensure that we give back memory?
    }
    
    /**
     * Closes this text window if the text isn't dirty.
     */
    public void closeWindow() {
        if (isDirty()) {
            if (showPatchAndAskForConfirmation("Discard Changes", "Discard changes to \"" + file.getName() + "\"? (Equivalent to applying the following patch.)", true) == false) {
                return;
            }
        }
        Evergreen.getInstance().getTagsPanel().ensureTagsAreHidden();
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
            Evergreen.getInstance().openFile(getContext() + File.separator + counterpartFilename);
        } else {
            Evergreen.getInstance().showAlert("Can't switch to counterpart", "File \"" + filename + "\" has no counterpart.");
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
        text.centerOnNewSelection(offset, endOffset);
    }
    
    /**
    * Returns the name of the context for this window. In this case, the directory the file's in.
    */
    public String getContext() {
        return FileUtilities.getUserFriendlyName(file.getParent());
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
            if (showPatchAndAskForConfirmation("Overwrite", "Overwrite the currently saved version of \"" + file.getName() + "\"? (Equivalent to applying the following patch.)", false) == false) {
                return false;
            }
        }
        
        Evergreen editor = Evergreen.getInstance();
        
        // Try to save a backup copy first. Ideally, we should do this from
        // a timer and always have a recent backup around.
        File backupFile = FileUtilities.fileFromString(this.filename + ".bak");
        if (file.exists()) {
            try {
                writeToFile(backupFile);
            } catch (Exception ex) {
                editor.showAlert("Couldn't save \"" + this.filename + "\"", "Couldn't create backup file.");
                return false;
            }
        }
        
        try {
            editor.showStatus("Saving " + filename + "...");
            // The file may be a symbolic link on a CIFS server.
            // In this case, it's important that we write into the original file rather than creating a new one.
            writeToFile(file);
            text.getTextBuffer().getUndoBuffer().setCurrentStateClean();
            getTitleBar().repaint();
            editor.showStatus("Saved " + filename);
            backupFile.delete();
            this.lastModifiedTime = file.lastModified();
            configureForGuessedFileType();
            updateWatermarkAndTitleBar();
            tagsUpdater.updateTags();
            SaveMonitor.getInstance().fireSaveListeners();
            return true;
        } catch (Exception ex) {
            editor.showStatus("");
            editor.showAlert("Couldn't save file \"" + filename + "\"", ex.getMessage());
            Log.warn("Problem saving \"" + filename + "\"", ex);
        }
        return false;
    }
    
    /** Saves the text to a file with the given name. Returns true if the file was saved okay. */
    public boolean saveAs(String newFilename) {
        try {
            File newFile = FileUtilities.fileFromString(newFilename);
            if (newFile.exists()) {
                boolean replace = Evergreen.getInstance().askQuestion("Overwrite existing file?", "An item named \"" + newFilename + "\" already exists in this location. Do you want to replace it with the one you are saving?", "Replace");
                if (replace == false) {
                    return false;
                }
            }
            writeToFile(newFile);
            Evergreen.getInstance().openFile(newFile.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            Evergreen.getInstance().showAlert("Couldn't save file \"" + newFilename + "\"", ex.getMessage());
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

