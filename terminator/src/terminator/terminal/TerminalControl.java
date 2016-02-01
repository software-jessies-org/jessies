package terminator.terminal;

import e.util.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.event.*;
import terminator.*;
import terminator.model.*;
import terminator.view.*;
import terminator.terminal.escape.*;

/**
 * Ties together the subprocess reader thread, the subprocess writer thread, and the thread that processes the subprocess' output.
 * Some basic processing is done here.
 */
public class TerminalControl {
    // Andrew Giddings wanted "windows-1252" for his Psion.
    private static final String CHARSET_NAME = "UTF-8";
    
    // This should be around your system's pipe size.
    // Too much larger and you'll waste time copying unused char[].
    // Too much smaller and you'll waste time making excessive system calls reading just part of what's available.
    // FIXME: add a JNI call to return PIPE_BUF? (It's not strictly required to be the value we're looking for, but it probably is.)
    private static final int INPUT_BUFFER_SIZE = 8192;
    
    // We use "new String" here because we're going to use reference equality later to recognize Terminator-supplied defaults.
    private static final String TERMINATOR_DEFAULT_SHELL = new String(System.getenv("SHELL"));
    
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_STEP_MODE = false;
    private static final boolean SHOW_ASCII_RENDITION = false;
    
    private static BufferedReader stepModeReader;
    
    private JTerminalPane pane;
    private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
    private TerminalModel model;
    private PtyProcess ptyProcess;
    private boolean processIsRunning;
    private boolean processHasBeenDestroyed = false;
    
    private InputStreamReader in;
    private OutputStream out;
    
    private ExecutorService writerExecutor;
    private Thread readerThread;
    
    private int characterSet;
    private char[] g = new char[4];
    
    private boolean automaticNewline;
    
    private TerminalLogWriter terminalLogWriter;
    
    private StringBuilder lineBuffer = new StringBuilder();
    
    private EscapeParser escapeParser;
    
    // Buffer of TerminalActions to perform.
    private ArrayList<TerminalAction> terminalActions = new ArrayList<TerminalAction>();
    // Semaphore to prevent us from overrunning the EDT.
    private Semaphore flowControl = new Semaphore(30);
    
    public TerminalControl(JTerminalPane pane, TerminalModel model) {
        reset();
        this.pane = pane;
        this.model = model;
    }
    
    public void initProcess(List<String> command, String workingDirectory) throws Throwable {
        // We always want to start a login shell.
        // This used to be an option, but it wasn't very useful and it caused confusion.
        // It's also hard to explain the difference without assuming a detailed knowledge of the particular shell.
        // We used to use "--login", but "-l" was more portable.
        // tcsh(1) is so broken that "-l" can only appear on its own.
        // POSIX's sh(1) doesn't even have the notion of login shell (though it does specify vi-compatible line editing).
        // So now we use the 1970s trick of prefixing argv[0] with "-".
        String[] argv = command.toArray(new String[command.size()]);
        String executable = argv[0];
        // We deliberately use reference equality here so we're sure we know what we're meddling with.
        // We only want to modify a call to the user's default shell that Terminator itself inserted into 'command'.
        // If the user's messing about with -e, they get what they ask for no matter what that is.
        // Since we only support -e for compatibility purposes, it's important to have a compatible implementation!
        if (argv[0] == TERMINATOR_DEFAULT_SHELL) {
            argv[0] = "-" + argv[0];
        }
        
        // We log an announceConnectionLost message if we fail to create the PtyProcess, so we need the TerminalLogWriter first.
        this.terminalLogWriter = new TerminalLogWriter(command);
        this.ptyProcess = new PtyProcess(executable, argv, workingDirectory);
        this.processIsRunning = true;
        Log.warn("Created " + ptyProcess + " and logging to " + terminalLogWriter.getInfo());
        this.in = new InputStreamReader(ptyProcess.getInputStream(), CHARSET_NAME);
        this.out = ptyProcess.getOutputStream();
        writerExecutor = ThreadUtilities.newSingleThreadExecutor(makeThreadName("Writer"));
    }
    
    public static ArrayList<String> getDefaultShell() {
        ArrayList<String> command = new ArrayList<String>();
        command.add(TERMINATOR_DEFAULT_SHELL);
        return command;
    }
    
    public void destroyProcess() {
        if (processIsRunning) {
            try {
                ptyProcess.destroy();
                processHasBeenDestroyed = true;
            } catch (IOException ex) {
                Log.warn("Failed to destroy process " + ptyProcess, ex);
            }
        }
    }
    
    /**
     * Starts listening to output from the child process. This method is
     * invoked when all the user interface stuff is set up.
     */
    public void start() {
        if (readerThread != null) {
            // Detaching a tab causes start to be invoked again, but we shouldn't do anything.
            return;
        }
        
        if (ptyProcess == null) {
            // If the PtyProcess couldn't start, there's no point carrying on.
            return;
        }
        
        readerThread = startThread("Reader", new ReaderRunnable());
    }
    
    private Thread startThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, makeThreadName(name));
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
    
    private String makeThreadName(String role) {
        return "Process " + ptyProcess.getPid() + " (" + ptyProcess.getPtyName() + ") " + role;
    }
    
    private class ReaderRunnable implements Runnable {
        public void run() {
            try {
                while (true) {
                    char[] chars = new char[INPUT_BUFFER_SIZE];
                    int readCount = in.read(chars, 0, chars.length);
                    if (readCount == -1) {
                        Log.warn("read returned -1 from " + ptyProcess);
                        return; // This isn't going to fix itself!
                    }
                    
                    try {
                        processBuffer(chars, readCount);
                    } catch (Throwable th) {
                        Log.warn("Problem processing output from " + ptyProcess, th);
                    }
                }
            } catch (Throwable th) {
                Log.warn("Problem reading output from " + ptyProcess, th);
            } finally {
                // Our reader might throw an exception before the child has terminated.
                // So "handleProcessTermination" is perhaps not the ideal name.
                handleProcessTermination();
            }
        }
    }
    
    public void invokeCharacterSet(int index) {
        this.characterSet = index;
    }
    
    /**
     * Ensures that ^N and ^O are handled at the proper time.
     * If we just call invokeCharacterSet they jump any pending text in the terminalActions queue.
     * Before this fix, "echo hello ^N world" would all appear in character set 1.
     */
    public void invokeCharacterSetLater(final int index) {
        flushLineBuffer();
        terminalActions.add(new TerminalAction() {
            public void perform(TerminalModel model) {
                invokeCharacterSet(index);
            }
        });
    }
    
    public void setAutomaticNewline(boolean automatic) {
        this.automaticNewline = automatic;
    }
    
    public boolean isAutomaticNewline() {
        return automaticNewline;
    }
    
    /**
     * Invoked both on construction to set the defaults and by the "Reset"
     * action in the UI.
     */
    public void reset() {
        setAutomaticNewline(false);
        invokeCharacterSet(0);
        designateCharacterSet(0, 'B');
        designateCharacterSet(1, 'B');
        designateCharacterSet(2, 'B');
        designateCharacterSet(3, 'B');
        if (model != null) {
            model.setStyle(Style.getDefaultStyle());
        }
    }
    
    public void designateCharacterSet(int index, char set) {
        g[index] = set;
    }
    
    private static final void doStep() {
        if (DEBUG_STEP_MODE) {
            try {
                if (stepModeReader == null) {
                    stepModeReader = new BufferedReader(new InputStreamReader(System.in));
                }
                stepModeReader.readLine();
            } catch (IOException ex) {
                Log.warn("Problem waiting for stepping input", ex);
            }
        }
    }
    
    private void handleProcessTermination() {
        processIsRunning = false;

        // The readerThread will have shut itself down by now.
        // We need to handle the writer ExecutorService ourselves.
        if (writerExecutor != null) {
            writerExecutor.shutdownNow();
        }
        
        // If the JNI side failed to start, ptyProcess can be null.
        // In that case, we'll already have reported the error.
        if (ptyProcess == null) {
            return;
        }

        Log.warn("calling waitFor on " + ptyProcess);
        try {
            ptyProcess.waitFor();
        } catch (Exception ex) {
            Log.warn("Problem waiting for " + ptyProcess, ex);
            String exceptionDetails = StringUtilities.stackTraceFromThrowable(ex).replaceAll("\n", "\n\r");
            announceConnectionLost(exceptionDetails + "[Problem waiting for process.]");
            return;
        }
        Log.warn("waitFor returned on " + ptyProcess);
        if (ptyProcess.didExitNormally()) {
            int status = ptyProcess.getExitStatus();
            if (pane.shouldHoldOnExit(status)) {
                announceConnectionLost("\n\r[Process exited with status " + status + ".]");
                return;
            }
        } else if (ptyProcess.wasSignaled()) {
            announceConnectionLost("\n\r[Process killed by " + ptyProcess.getSignalDescription() + ".]");
            return;
        } else {
            announceConnectionLost("\n\r[Lost contact with process.]");
            return;
        }

        // If it wasn't a pane close that caused us to get here, close the pane.
        if (processHasBeenDestroyed == false) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pane.doCloseAction();
                }
            });
        }
    }
    
    public void announceConnectionLost(String message) {
        try {
            final char[] buffer = message.toCharArray();
            processBuffer(buffer, buffer.length);
            pane.getTerminalView().setCursorVisible(false);
        } catch (Exception ex) {
            Log.warn("Couldn't say \"" + message + "\"", ex);
        }
    }
    
    /** Must be called in the AWT dispatcher thread. */
    public void sizeChanged(final Dimension sizeInChars, final Dimension sizeInPixels) throws IOException {
        TerminalAction sizeChangeAction = new TerminalAction() {
            public void perform(TerminalModel model) {
                model.sizeChanged(sizeInChars);
            }
            
            @Override public String toString() {
                return "TerminalAction[Size change to " + sizeInChars + "]";
            }
        };
        model.processActions(new TerminalAction[] { sizeChangeAction });
        // Notify the pty that the size has changed.
        ptyProcess.sendResizeNotification(sizeInChars, sizeInPixels);
    }
    
    private synchronized void processBuffer(char[] buffer, int size) throws IOException {
        boolean sawNewline = false;
        for (int i = 0; i < size; ++i) {
            char ch = buffer[i];
            if (ch == '\n') {
                sawNewline = true;
            }
            processChar(ch);
        }
        terminalLogWriter.append(buffer, size, sawNewline);
        flushLineBuffer();
        flushTerminalActions();
        fireChangeListeners();
    }
    
    private synchronized void flushTerminalActions() {
        if (terminalActions.size() == 0) {
            return;
        }
        
        final TerminalAction[] actions = terminalActions.toArray(new TerminalAction[terminalActions.size()]);
        terminalActions.clear();
        
        boolean didAcquire = false;
        try {
            flowControl.acquire();
            didAcquire = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        model.processActions(actions);
                    } catch (Throwable th) {
                        Log.warn("Couldn't process terminal actions for " + ptyProcess, th);
                    } finally {
                        flowControl.release();
                    }
                }
            });
        } catch (Throwable th) {
            Log.warn("Couldn't flush terminal actions for " + ptyProcess, th);
            if (didAcquire) {
                flowControl.release();
            }
        }
    }
    
    /**
     * According to vttest, these cursor movement characters are still
     * treated as such, even when they occur within an escape sequence.
     */
    private final boolean countsTowardsEscapeSequence(char ch) {
        return (ch != Ascii.BS && ch != Ascii.CR && ch != Ascii.VT);
    }
    
    private synchronized void processChar(final char ch) {
        // Enable this if you're having trouble working out what we're being asked to interpret.
        if (SHOW_ASCII_RENDITION) {
            if (ch >= ' ' || ch == '\n') {
                System.out.print(ch);
            } else {
                System.out.print(".");
            }
        }
        
        if (ch == Ascii.ESC) {
            flushLineBuffer();
            // If the old escape sequence is interrupted; we start a new one.
            if (escapeParser != null) {
                Log.warn("Escape parser discarded with string \"" + escapeParser + "\"");
            }
            escapeParser = new EscapeParser();
            return;
        }
        if (escapeParser != null && countsTowardsEscapeSequence(ch)) {
            escapeParser.addChar(ch);
            if (escapeParser.isComplete()) {
                processEscape();
                escapeParser = null;
            }
        } else if (ch == Ascii.LF || ch == Ascii.CR || ch == Ascii.BS || ch == Ascii.HT || ch == Ascii.VT) {
            flushLineBuffer();
            doStep();
            processSpecialCharacter(ch);
        } else if (ch == Ascii.SO) {
            invokeCharacterSetLater(1);
        } else if (ch == Ascii.SI) {
            invokeCharacterSetLater(0);
        } else if (ch == Ascii.BEL) {
            pane.flash();
        } else if (ch == Ascii.NUL) {
            // Most telnetd(1) implementations seem to have a bug whereby
            // they send the NUL byte at the end of the C strings they want to
            // output when you first connect. Since all Unixes are pretty much
            // copy and pasted from one another these days, this silly mistake
            // only needed to be made once.
        } else {
            lineBuffer.append(ch);
        }
    }
    
    private static class PlainTextAction implements TerminalAction {
        private String line;
        
        private PlainTextAction(String line) {
            this.line = line;
        }
        
        public void perform(TerminalModel model) {
            if (DEBUG) {
                Log.warn("Processing line \"" + line + "\"");
            }
            model.processLine(line);
        }
        
        @Override public String toString() {
            return "TerminalAction[Process line: " + line + "]";
        }
    }
    
    public String translate(String characters) {
        if (g[characterSet] == 'B') {
            return characters;
        }
        StringBuilder translation = new StringBuilder(characters.length());
        for (int i = 0; i < characters.length(); ++i) {
            translation.append(translateToCharacterSet(characters.charAt(i)));
        }
        return translation.toString();
    }
    
    private synchronized void flushLineBuffer() {
        if (lineBuffer.length() == 0) {
            // Nothing to flush!
            return;
        }
        
        final String line = lineBuffer.toString();
        lineBuffer = new StringBuilder();
        
        doStep();
        
        // Conform to the stated claim that the model's always mutated in the AWT dispatch thread.
        terminalActions.add(new PlainTextAction(line));
    }
    
    public synchronized void processSpecialCharacter(final char ch) {
        terminalActions.add(new TerminalAction() {
            public void perform(TerminalModel model) {
                if (DEBUG) {
                    Log.warn("Processing special char \"" + getCharDesc(ch) + "\"");
                }
                model.processSpecialCharacter(ch);
            }
            
            @Override public String toString() {
                return "TerminalAction[Special char " + getCharDesc(ch) + "]";
            }
            
            private String getCharDesc(char ch) {
                switch (ch) {
                    case Ascii.LF: return "LF";
                    case Ascii.CR: return "CR";
                    case Ascii.HT: return "HT";
                    case Ascii.VT: return "VT";
                    case Ascii.BS: return "BS";
                    default: return "UK";
                }
            }
        });
    }
    
    public synchronized void processEscape() {
        if (DEBUG) {
            Log.warn("Processing escape sequence \"" + StringUtilities.escapeForJava(escapeParser.toString()) + "\"");
        }
        
        // Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
        // to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
        doStep();
        TerminalAction action = escapeParser.getAction(this);
        if (action != null) {
            terminalActions.add(action);
        }
    }
    
    private char translateToCharacterSet(char ch) {
        switch (g[characterSet]) {
        case '0':
            return translateToGraphicalCharacterSet(ch);
        case 'A':
            return translateToUkCharacterSet(ch);
        default:
            return ch;
        }
    }
    
    /**
     * Translate ASCII to the nearest Unicode characters to the special
     * graphics and line drawing characters.
     * 
     * Run this in xterm(1) for reference:
     * 
     *   ruby -e 'cs="abcdefghijklmnopqrstuvwxyz"; puts(cs); \
     *            print("\x1b(0\x1b)B\x0f");puts(cs);print("\x0e")'
     * 
     * Or try test 3 of vttest.
     * 
     * We use the Unicode box-drawing characters, but the characters
     * extend out of the bottom of the font's bounding box, spoiling
     * the effect. Bug parade #4896465.
     * 
     * Konsole initially used fonts but switched to doing actual drawing
     * because of this kind of problem. (Konsole has a menu item to run
     * a new instance of mc(1), so they need this.)
     */
    private char translateToGraphicalCharacterSet(char ch) {
        switch (ch) {
        case '`':
            return '\u2666'; // BLACK DIAMOND SUIT
        case 'a':
            return '\u2591'; // LIGHT SHADE
        case 'b':
            return '\u2409'; // SYMBOL FOR HORIZONTAL TABULATION
        case 'c':
            return '\u240c'; // SYMBOL FOR FORM FEED
        case 'd':
            return '\u240d'; // SYMBOL FOR CARRIAGE RETURN
        case 'e':
            return '\u240a'; // SYMBOL FOR LINE FEED
        case 'f':
            return '\u00b0'; // DEGREE SIGN
        case 'g':
            return '\u00b1'; // PLUS-MINUS SIGN
        case 'h':
            return '\u2424'; // SYMBOL FOR NEW LINE
        case 'i':
            return '\u240b'; // SYMBOL FOR VERTICAL TABULATION
        case 'j':
            return '\u2518'; // BOX DRAWINGS LIGHT UP AND LEFT
        case 'k':
            return '\u2510'; // BOX DRAWINGS LIGHT DOWN AND LEFT
        case 'l':
            return '\u250c'; // BOX DRAWINGS LIGHT DOWN AND RIGHT
        case 'm':
            return '\u2514'; // BOX DRAWINGS LIGHT UP AND RIGHT
        case 'n':
            return '\u253c'; // BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
        case 'v':
            return '\u2534'; // BOX DRAWINGS LIGHT UP AND HORIZONTAL
        case 'w':
            return '\u252c'; // BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
        case 'o':
        case 'p':
        case 'q':
        case 'r':
        case 's':
            // These should all be different characters,
            // but Unicode only offers one of them.
            return '\u2500'; // BOX DRAWINGS LIGHT HORIZONTAL
        case 't':
            return '\u251c'; // BOX DRAWINGS LIGHT VERTICAL AND RIGHT
        case 'u':
            return '\u2524'; // BOX DRAWINGS LIGHT VERTICAL AND LEFT
        case 'x':
            return '\u2502'; // BOX DRAWINGS LIGHT VERTICAL
        case 'y':
            return '\u2264'; // LESS-THAN OR EQUAL TO
        case 'z':
            return '\u2265'; // GREATER-THAN OR EQUAL TO
        case '{':
            return '\u03c0'; // GREEK SMALL LETTER PI
        case '|':
            return '\u2260'; // NOT EQUAL TO
        case '}':
            return '\u00a3'; // POUND SIGN
        case '~':
            return '\u00b7'; // MIDDLE DOT
        default:
            return ch;
        }
    }
    
    private char translateToUkCharacterSet(char ch) {
        return (ch == '#') ? '\u00a3' : ch;
    }
    
    public void sendUtf8String(final String s) {
        writerExecutor.execute(new Runnable() {
            public void run() {
                try {
                    if (processIsRunning) {
                        out.write(s.getBytes(CHARSET_NAME));
                        out.flush();
                    }
                } catch (IOException ex) {
                    reportFailedSend("string", s, ex);
                }
            }
        });
    }
    
    private void reportFailedSend(String kind, String value, Exception ex) {
        Log.warn("Couldn't send " + kind + " \"" + StringUtilities.escapeForJava(value) + "\" to " + ptyProcess, ex);
    }
    
    public TerminalLogWriter getTerminalLogWriter() {
        return terminalLogWriter;
    }
    
    public PtyProcess getPtyProcess() {
        return ptyProcess;
    }
    
    /**
     * Adds a change listener to be notified when the terminal's content changes.
     */
    public void addChangeListener(final ChangeListener l) {
        changeListeners.add(l);
    }
    
    public void removeChangeListener(final ChangeListener l) {
        changeListeners.remove(l);
    }
    
    private void fireChangeListeners() {
        for (ChangeListener l : changeListeners) {
            l.stateChanged(null);
        }
    }
}
