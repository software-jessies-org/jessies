package terminator.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;
import org.jessies.os.*;
import terminator.*;
import terminator.model.*;
import terminator.terminal.*;
import terminator.view.highlight.*;

public class JTerminalPane extends JPanel {
    // The probably over-simplified belief here is that Unix terminals always send ^?.
    // Search the change log for "backspace" for more information.
    private static final String ERASE_STRING = String.valueOf(Ascii.DEL);
    
    private TerminalPaneHost host;
    private TerminalControl control;
    private TerminalView view;
    private JScrollPane scrollPane;
    private VisualBellViewport viewport;
    private FindPanel findPanel;
    private String name;
    private boolean wasCreatedAsNewShell;
    private Dimension currentSizeInChars;
    private MenuItemProvider menuItemProvider;
    
    /**
     * Creates a new terminal with the given name, running the given command.
     */
    private JTerminalPane(String name, String workingDirectory, List<String> command, boolean wasCreatedAsNewShell) {
        super(new BorderLayout());
        this.name = name;
        this.wasCreatedAsNewShell = wasCreatedAsNewShell;
        init(command, workingDirectory);
    }
    
    /**
     * For XTerm-like "-e" support.
     */
    public static JTerminalPane newCommandWithArgV(String name, String workingDirectory, List<String> argV) {
        if (argV.size() == 0) {
            argV = TerminalControl.getDefaultShell();
        }
        if (name == null) {
            name = argV.get(0);
        }
        return new JTerminalPane(name, workingDirectory, argV, false);
    }
    
    /**
     * Creates a new terminal running the given command, with the given
     * name. If 'name' is null, we use the command as the the name.
     */
    public static JTerminalPane newCommandWithName(String originalCommand, String name, String workingDirectory) {
        if (name == null) {
            name = originalCommand;
        }
        
        // Avoid having to interpret the command (as java.lang.Process brokenly does) by passing it to the shell as-is.
        ArrayList<String> command = TerminalControl.getDefaultShell();
        command.add("-c");
        command.add(originalCommand);
        
        return new JTerminalPane(name, workingDirectory, command, false);
    }
    
    /**
     * Creates a new terminal running the user's shell.
     */
    public static JTerminalPane newShell() {
        return newShellWithName(null, null);
    }
    
    /**
     * Creates a new terminal running the user's shell with the given name.
     */
    public static JTerminalPane newShellWithName(String name, String workingDirectory) {
        if (name == null) {
            String user = System.getProperty("user.name");
            name = user + "@localhost";
        }
        return new JTerminalPane(name, workingDirectory, TerminalControl.getDefaultShell(), true);
    }
    
    public JTerminalPane newShellHere() {
        int fd = control.getPtyProcess().getFd();
        int foregroundPid = Posix.tcgetpgrp(fd);
        if (foregroundPid < 0) {
            SimpleDialog.showAlert(this, "Failed to create new shell", "Could not find foreground process on pipe with fd " + fd);
            return null;
        }
        String workingDirectory = ProcessUtilities.findCurrentWorkingDirectory(foregroundPid);
        if (workingDirectory == null) {
            SimpleDialog.showAlert(this, "Failed to create new shell", "Could not find current working directory of pid " + foregroundPid);
            return null;
        }
        return newShellWithName(null, workingDirectory);
    }
    
    public Dimension getPaneSize() {
        return viewport.getSize();
    }
    
    public void optionsDidChange() {
        // We're called before start().
        if (host != null) {
            // "Use alt key as meta key" affects the keyboard shortcuts displayed on the pop-up menu.
            this.menuItemProvider = host.createMenuItemProvider(this);
        }
        view.optionsDidChange();
        viewport.setBackground(view.getBackground());
        updateTerminalSize();
        scrollPane.invalidate();
        validate();
    }
    
    private void init(List<String> command, String workingDirectory) {
        view = new TerminalView();
        view.addKeyListener(new KeyHandler());
        
        EPopupMenu popupMenu = new EPopupMenu(view);
        // Indirection because we've not yet created the real MenuItemProvider.  
        popupMenu.addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent event, Collection<Action> actions) {
                menuItemProvider.provideMenuItems(event, actions);
            }
        });
        
        viewport = new VisualBellViewport();
        viewport.setView(view);
        
        scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewport(viewport);
        if (GuiUtilities.isMacOs()) {
            scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new FakeScrollBar());
        }
        
        optionsDidChange();
        
        BirdView birdView = new BirdView(view.getBirdsEye(), scrollPane.getVerticalScrollBar());
        view.setBirdView(birdView);
        
        findPanel = new FindPanel(this);
        findPanel.setVisible(false);
        
        add(scrollPane, BorderLayout.CENTER);
        add(birdView, BorderLayout.EAST);
        add(findPanel, BorderLayout.SOUTH);
        GuiUtilities.keepMaximumShowing(scrollPane.getVerticalScrollBar());
        
        view.sizeChanged();
        try {
            control = new TerminalControl(this, view.getModel());
            view.setTerminalControl(control);
            control.initProcess(command, workingDirectory);
            initSizeMonitoring();
        } catch (final Throwable th) {
            Log.warn("Couldn't initialize terminal", th);
            // We can't call announceConnectionLost off the EDT.
            new Thread(new Runnable() {
                public void run() {
                    String exceptionDetails = StringUtilities.stackTraceFromThrowable(th).replaceAll("\n", "\n\r");
                    control.announceConnectionLost(exceptionDetails + "[Couldn't initialize terminal: " + th.getClass().getSimpleName() + ".]");
                }
            }).start();
        }
    }
    
    // On Mac OS there's an ugly hole between the horizontal scroll bar and the grow box.
    // Fill that hole with what looks like an empty horizontal scroll bar track.
    // I don't know how to get a JScrollBar to do the rendering for us, so for now here's a work-around.
    // FIXME: this is broken if Apple change the scroll bar appearance or the user has a high-DPI display.
    // Ideally, I'd have liked to have the bird view to the inside of the vertical scroll bar, encroaching on the terminal's space.
    private static class FakeScrollBar extends JComponent {
        private Color[] colors;
        public Color[] getColors() {
            if (colors == null) {
                int[] pixelColors = new int[] { 0xd4d4d4, 0xd9d9d9, 0xdedede, 0xe5e5e5, 0xe9e9e9, 0xefefef, 0xf3f3f3, 0xf7f7f7, 0xfafafa, 0xfcfcfc, 0xfdfdfd, 0xfdfdfd, 0xfbfbfb, 0xf8f8f8, 0xf5f5f5 };
                colors = new Color[pixelColors.length];
                for (int i = 0; i < pixelColors.length; ++i) {
                    colors[i] = new Color(pixelColors[i]);
                }
            }
            return colors;
        }
        @Override public void paintComponent(Graphics oldGraphics) {
            Graphics2D g = (Graphics2D) oldGraphics;
            int x = 0;
            for (Color color : getColors()) {
                g.setColor(color);
                g.drawLine(0, x, getWidth(), x);
                ++x;
            }
        }
    }
    
    private void initSizeMonitoring() {
        class SizeMonitor extends ComponentAdapter {
            @Override
            public void componentShown(ComponentEvent event) {
                // Force a size check whenever we're shown in case we're a tab whose window resized while we weren't showing, because in that case we wouldn't have received a componentResized notification.
                componentResized(event);
            }
            
            @Override
            public void componentResized(ComponentEvent event) {
                updateTerminalSize();
            }
        }
        scrollPane.getViewport().addComponentListener(new SizeMonitor());
    }
    
    private void updateTerminalSize() {
        Dimension size = view.getVisibleSizeInCharacters();
        if (size.equals(currentSizeInChars) == false) {
            try {
                control.sizeChanged(size, view.getVisibleSize());
                if (host != null) {
                    host.setTerminalSize(size);
                }
            } catch (Exception ex) {
                if (control != null) {
                    Log.warn("Failed to notify " + control.getPtyProcess() + " of size change", ex);
                }
            }
            currentSizeInChars = size;
        }
    }
    
    public TerminalView getTerminalView() {
        return view;
    }
    
    /** 
     * Starts the process listening once all the user interface stuff is set up.
     * 
     * @param host Reference to the environment hosting this JTerminalPane.
     */
    public void start(TerminalPaneHost host) {
        this.host = host;
        this.menuItemProvider = host.createMenuItemProvider(this);
        control.start();
    }
    
    public void reset() {
        control.reset();
    }
    
    public TerminalControl getControl() {
        return control;
    }
    
    public String getTerminalName() {
        return name;
    }
    
    public boolean shouldHoldOnExit(int status) {
        // bash (and probably other shells) return as their own exit status that of the last command executed.
        // The user will already have seen any failure in a shell window, so we ignore them.
        return (wasCreatedAsNewShell == false) && (status != 0);
    }
    
    public void setTerminalName(String name) {
        this.name = name;
        host.terminalNameChanged(this);
    }
    
    public Dimension getOptimalViewSize() {
        return view.getOptimalViewSize();
    }
    
    private class KeyHandler implements KeyListener {
        private javax.swing.Timer waitForCorrespondingOutputTimer;
        private Location cursorPositionAfterOutput;
        private javax.swing.Timer waitForCursorStabilityTimer;
        
        public KeyHandler() {
            // If your remote-echoing device is more than roundTripMilliseconds away and doesn't automatically wrap at the
            // terminal width, the automatic horizontal scrolling as you type won't work.
            // If you raise the time-out, the automatic horizontal scrolling becomes less responsive.
            int roundTripMilliseconds = 200;
            
            // Give the corresponding output time to come out and so move the cursor, to which we'll scroll...
            waitForCorrespondingOutputTimer = new javax.swing.Timer(roundTripMilliseconds, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cursorPositionAfterOutput = view.getCursorPosition();
                    waitForCursorStabilityTimer.start();
                }
            });
            waitForCorrespondingOutputTimer.setRepeats(false);
            
            // ... providing that it doesn't move again for a while.
            waitForCursorStabilityTimer = new javax.swing.Timer(100, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // If the cursor is now in a different position, we conclude that the previous output was coincidental to user's
                    // key press.
                    // (What we'd really like to check is whether there's been any more output.
                    // Checking the cursor position is just an approximation.)
                    // This has the slightly unfortunate effect that, if the user is holding a key down,
                    // we won't scroll until they let go.
                    // The benefit is that we won't leave the window scrolled by half a width if the output goes
                    // briefly off-screen just after a user's key press.
                    if (cursorPositionAfterOutput != view.getCursorPosition()) {
                        return;
                    }
                    view.scrollHorizontallyToShowCursor();
                }
            });
            waitForCursorStabilityTimer.setRepeats(false);
            
            // This automatic scrolling has caused minor trouble a lot of times.
            // Here's some test code which you wouldn't want to cause scrolling but which used to, all the time,
            // and now doesn't.
            // ruby -e 'while true; $stdout.write("X" * 90); $stdout.flush(); sleep(0.05); puts(); end'
        }
        
        public void keyPressed(KeyEvent event) {
            if (doKeyboardScroll(event) || doKeyboardTabAction(event)) {
                event.consume();
                return;
            }
            // Leave the event for TerminatorMenuBar if it has appropriate modifiers.
            if (TerminatorMenuBar.isKeyboardEquivalent(event)) {
                return;
            }
            if (event.getKeyCode() == KeyEvent.VK_INSERT && event.isShiftDown()) {
                // Legacy CUA-style (http://en.wikipedia.org/wiki/Cut,_copy,_and_paste) paste.
                // gnome-terminal and xterm paste the X11 selection rather than the clipboard.
                view.middleButtonPaste();
                event.consume();
                return;
            }
            String sequence = getEscapeSequenceForKeyCode(event);
            if (sequence != null) {
                if (sequence.length() == 1) {
                    char ch = sequence.charAt(0);
                    // We don't get a KEY_TYPED event for the escape key or keypad enter on Mac OS, where we have to handle it in keyPressed.
                    // We can't tell the difference between control-tab and control-i in keyTyped, so we have to handle that here too.
                    if (ch != Ascii.ESC && ch != Ascii.CR && ch != Ascii.HT) {
                        Log.warn("The constraint about not handling keys that generate KEY_TYPED events in keyPressed was probably violated when handling " + event);
                    }
                }
                control.sendUtf8String(sequence);
                view.userIsTyping();
                scroll();
                event.consume();
            }
        }

        private String getEscapeSequenceForKeyCode(KeyEvent event) {
            final char keyChar = event.getKeyChar();
            final int keyCode = event.getKeyCode();
            // If this event will be followed by a KEY_TYPED event (that is, has a corresponding Unicode character), you must NOT handle it here; see keyTyped.
            if (keyChar == '\t') {
                // Here's our first awkward case: tab.
                // In keyTyped, we can't tell the difference between control-tab and control-i.
                // We have to handle both here.
                if (event.isControlDown() && keyCode == KeyEvent.VK_TAB) {
                    // Control-tab: no corresponding text.
                    return null;
                }
                // Plain old tab, or control-i: insert a tab.
                return "\t";
            }
            switch (keyCode) {
                case KeyEvent.VK_ESCAPE:
                    // Annoyingly, while Linux sends a KEY_TYPED event for the escape key, Mac OS doesn't.
                    return GuiUtilities.isMacOs() ? String.valueOf(Ascii.ESC) : null;
                case KeyEvent.VK_ENTER:
                    // Annoyingly, while Linux sends a KEY_TYPED event for the keypad enter, Mac OS doesn't.
                    return (GuiUtilities.isMacOs() && event.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD) ? String.valueOf(Ascii.CR) : null;
                
                case KeyEvent.VK_HOME: return editingKeypadSequence(event, 1);
                case KeyEvent.VK_INSERT: return editingKeypadSequence(event, 2);
                case KeyEvent.VK_END: return editingKeypadSequence(event, 4);
                case KeyEvent.VK_PAGE_UP: return editingKeypadSequence(event, 5);
                case KeyEvent.VK_PAGE_DOWN: return editingKeypadSequence(event, 6);
                
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_LEFT:
                {
                    char letter = "DACB".charAt(keyCode - KeyEvent.VK_LEFT);
                    return Ascii.ESC + "[" + oldStyleKeyModifiers(event) + letter;
                }
                
                case KeyEvent.VK_F1:
                case KeyEvent.VK_F2:
                case KeyEvent.VK_F3:
                case KeyEvent.VK_F4:
                    // F1-F4 are special cases whose sequences don't look anything like the other F-keys.
                    // Beware of out-of-date xterm terminfo in this area.
                    // These are the sequences generated by xterm version 224.
                    String modifiers = functionKeyModifiers(event);
                    return Ascii.ESC + (modifiers.isEmpty() ? "O" : "[1" + modifiers) + "PQRS".charAt(keyCode - KeyEvent.VK_F1);
                case KeyEvent.VK_F5:
                    return functionKeySequence(15, keyCode, KeyEvent.VK_F5, event);
                case KeyEvent.VK_F6:
                case KeyEvent.VK_F7:
                case KeyEvent.VK_F8:
                case KeyEvent.VK_F9:
                case KeyEvent.VK_F10:
                    // "ESC[16~" isn't used.
                    return functionKeySequence(17, keyCode, KeyEvent.VK_F6, event);
                case KeyEvent.VK_F11:
                case KeyEvent.VK_F12:
                    // "ESC[22~" isn't used.
                    return functionKeySequence(23, keyCode, KeyEvent.VK_F11, event);
                    // The function key codes from here on are VT220 codes.
                case KeyEvent.VK_F13:
                case KeyEvent.VK_F14:
                    // Java has a discontinuity between VK_F12 and VK_F13.
                    return functionKeySequence(25, keyCode, KeyEvent.VK_F13, event);
                case KeyEvent.VK_F15:
                case KeyEvent.VK_F16:
                    // "ESC[27~" isn't used.
                    return functionKeySequence(28, keyCode, KeyEvent.VK_F15, event);
                    // X11 key codes go up to F35.
                    // Java key codes goes up to F24.
                    // Escape sequences mentioned in XTerm's "ctlseqs.ms" go up to F20 (VT220).
                    // Current Apple keyboards go up to F16, so that's where we stop.
                    
                default:
                    return null;
            }
        }
        
        private String functionKeySequence(int base, int keyCode, int keyCodeBase, KeyEvent event) {
            int argument = base + (keyCode - keyCodeBase);
            return Ascii.ESC + "[" + argument + functionKeyModifiers(event) + "~";
        }
        
        private String functionKeyModifiers(KeyEvent event) {
            int modifiers = 1;
            if (event.isMetaDown()) {
                modifiers += 8;
            }
            if (event.isControlDown()) {
                modifiers += 4;
            }
            if (event.isAltDown()) {
                modifiers += 2;
            }
            if (event.isShiftDown()) {
                modifiers += 1;
            }
            return (modifiers == 1) ? "" : ";" + modifiers;
        }
        
        private String oldStyleKeyModifiers(KeyEvent event) {
            String modifiers = functionKeyModifiers(event);
            return modifiers.isEmpty() ? "" : "1" + modifiers;
        }
        
        private String editingKeypadSequence(KeyEvent event, int csiDigit) {
            return Ascii.ESC + "[" + csiDigit + functionKeyModifiers(event) + "~";
        }
        
        public void keyReleased(KeyEvent event) {
        }
        
        // Handle key presses which generate keyTyped events.
        private String getUtf8ForKeyEvent(KeyEvent e) {
            char ch = e.getKeyChar();
            // Interpret the alt key as meta if that's what the user asked for. 
            if (Terminator.getPreferences().getBoolean(TerminatorPreferences.USE_ALT_AS_META) && e.isAltDown()) {
                return Ascii.ESC + String.valueOf(e.getKeyChar());
            }
            if (ch == '\t') {
                // We handled tab in keyPressed because only there can we distinguish control-i and control-tab.
                return null;
            }
            // This modifier test lets Ctrl-H and Ctrl-J generate ^H and ^J instead of
            // mangling them into ^? and ^M.
            // That's useful on those rare but annoying occasions where Backspace and
            // Enter aren't working and it's how other terminals behave.
            if (e.isControlDown() && ch < ' ') {
                return String.valueOf(ch);
            }
            // Work around Sun bug 6320676, and provide support for various terminal eccentricities.
            if (e.isControlDown()) {
                // Control characters are usually typed unshifted, for convenience...
                if (ch >= 'a' && ch <= 'z') {
                    return String.valueOf((char) (ch - '`'));
                }
                // ...but the complete range is really from ^@ (ASCII NUL) to ^_ (ASCII US).
                if (ch >= '@' && ch <= '_') {
                    return String.valueOf((char) (ch - '@'));
                }
                // There are two special cases that correspond to ASCII NUL.
                // Control-' ' is important for emacs(1).
                if (ch == ' ' || ch == '`') {
                    return "\u0000";
                }
                // And one last special case: control-/ is ^_ (ASCII US).
                if (ch == '/') {
                    return String.valueOf(Ascii.US);
                }
            }
            if (ch == Ascii.LF) {
                return String.valueOf(Ascii.CR);
            } else if (ch == Ascii.CR) {
                return control.isAutomaticNewline() ? "\r\n" : "\r";
            } else if (ch == Ascii.BS) {
                return ERASE_STRING;
            } else if (ch == Ascii.DEL) {
                return editingKeypadSequence(e, 3);
            } else {
                return String.valueOf(ch);
            }
        }
        
        /**
         * Handling keyTyped instead of doing everything via keyPressed and keyReleased lets us rely on Sun's translation of key presses to characters.
         * This includes alt-keypad character composition on Windows.
         */
        public void keyTyped(KeyEvent event) {
            if (TerminatorMenuBar.isKeyboardEquivalent(event)) {
                event.consume();
                return;
            }
            
            String utf8 = getUtf8ForKeyEvent(event);
            if (utf8 != null) {
                control.sendUtf8String(utf8);
                view.userIsTyping();
                scroll();
                event.consume();
            }
        }
        
        // gnome-terminal offers these shifted shortcuts, but nothing for scrolling by a single line.
        private boolean doKeyboardScroll(KeyEvent e) {
            final int keyCode = e.getKeyCode();
            if (e.getModifiersEx() == InputEvent.SHIFT_DOWN_MASK) {
                if (keyCode == KeyEvent.VK_HOME) {
                    view.scrollToTop();
                    return true;
                } else if (keyCode == KeyEvent.VK_END) {
                    view.scrollToEnd();
                    return true;
                } else if (keyCode == KeyEvent.VK_PAGE_UP) {
                    pageUp();
                    return true;
                } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
                    pageDown();
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Although we only advertise one pair of keystrokes on the menu, we actually support a variety of methods for changing tab.
         * The idea is that someone who subconsciously uses some other major application's keystrokes won't ever have to learn ours.
         */
        private boolean doKeyboardTabAction(KeyEvent e) {
            final int keyCode = e.getKeyCode();
            if (e.isControlDown() && keyCode == KeyEvent.VK_TAB) {
                // Emulates Firefox's control-tab/control-shift-tab cycle-tab behavior.
                // This is Terminator's native keystroke in the case where we're using Ctrl+Shift as our default modifier for shortcuts.
                host.cycleTab(e.isShiftDown() ? -1 : 1);
                return true;
            } else if (e.isControlDown() && e.isShiftDown() == false && (keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN)) {
                // Emulates gnome-terminal and Firefox's control-page up/down cycle-tab behavior.
                // Strictly, we're supposed to send an escape sequence for these strokes, but I don't know of anything that uses them.
                host.cycleTab(keyCode == KeyEvent.VK_PAGE_UP ? -1 : 1);
                return true;
            } else if (e.isMetaDown() && e.isShiftDown() && (keyCode == KeyEvent.VK_OPEN_BRACKET || keyCode == KeyEvent.VK_CLOSE_BRACKET)) {
                // Emulates Mac OS X Firefox, Chrome, and Safari's command-{/command-} cycle-tab behavior.
                // (See the huge comment in CycleTabAction for more on why this isn't on our menu bar on Mac OS.)
                host.cycleTab(keyCode == KeyEvent.VK_OPEN_BRACKET ? -1 : 1);
                return true;
            } else if (e.isControlDown() && e.isShiftDown() && (keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN)) {
                // Emulates gnome-terminal's control-shift page up/page down move-tab behavior.
                // When this clashes with Terminator's native keystroke for scrolling, defer to that.
                if (TerminatorMenuBar.isKeyboardEquivalent(e)) {
                    return false;
                }
                host.moveCurrentTab(keyCode == KeyEvent.VK_PAGE_UP ? -1 : 1);
                return true;
            } else if (e.isControlDown() && e.isShiftDown() && (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT)) {
                // Emulates konsole's control-shift left/right move-tab behavior.
                // This is Terminator's native keystroke in the case where we're using Ctrl+Shift as our default modifier for shortcuts.
                host.moveCurrentTab(keyCode == KeyEvent.VK_LEFT ? -1 : 1);
                return true;
            } else if (TerminatorMenuBar.isKeyboardEquivalent(e)) {
                // Emulates gnome-terminal's alt-<number> jump-to-tab behavior, or an analog of Terminal.app's command-<number> jump-to-window behavior.
                // We rely on VK_0 being '0' et cetera, and use the key code rather than the key char so that both alt-<number> and control-shift-<number> can work.
                final char ch = (char) e.getKeyCode();
                final int newIndex = TabbedPane.keyCharToTabIndex(ch);
                if (newIndex != -1) {
                    host.setSelectedTabIndex(newIndex);
                    return true;
                }
                // The shift-down case is handled elsewhere as a default Terminator keystroke.
                if ((keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) && e.isShiftDown() == false) {
                    // gnome-terminal behaves a little differently - it disables the left action on the first tab and the right action on the last tab.
                    host.cycleTab(keyCode == KeyEvent.VK_LEFT ? -1 : 1);
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Scrolls the display to the bottom if we're configured to do so.
         * This should be invoked after any action is performed as a
         * result of a key press/release/type.
         */
        public void scroll() {
            if (Terminator.getPreferences().getBoolean(TerminatorPreferences.SCROLL_ON_KEY_PRESS)) {
                view.scrollToBottomButNotHorizontally();
                waitForCorrespondingOutputTimer.stop();
                waitForCursorStabilityTimer.stop();
                waitForCorrespondingOutputTimer.start();
            }
        }
    }
    
    public SelectionHighlighter getSelectionHighlighter() {
        return view.getSelectionHighlighter();
    }
    
    public void selectAll() {
        getSelectionHighlighter().selectAll();
    }
    
    public void pageUp() {
        scrollVertically(-0.5);
    }
    
    public void pageDown() {
        scrollVertically(0.5);
    }
    
    public void lineUp() {
        scrollVertically(-1.0/currentSizeInChars.height);
    }
    
    public void lineDown() {
        scrollVertically(1.0/currentSizeInChars.height);
    }
    
    private void scrollVertically(double yMul) {
        // Translate JViewport's terrible confusing names into plain English.
        final int totalHeight = viewport.getViewSize().height;
        final int visibleHeight = viewport.getExtentSize().height;
        
        Point p = viewport.getViewPosition();
        p.y += (int) (yMul * visibleHeight);
        
        // Don't go off the top...
        p.y = Math.max(0, p.y);
        // Or bottom...
        p.y = Math.min(p.y, totalHeight - visibleHeight);
        
        viewport.setViewPosition(p);
    }
    
    /**
     * Hands focus to our text pane.
     */
    @Override public void requestFocus() {
        view.requestFocus();
    }
    public void doCopyAction() {
        getSelectionHighlighter().copyToSystemClipboard();
    }
    public void doPasteAction() {
        view.pasteSystemClipboard();
    }
    
    public void destroyProcess() {
        control.destroyProcess();
    }
    
    private boolean shouldClose() {
        final PtyProcess ptyProcess = control.getPtyProcess();
        if (ptyProcess == null) {
            // This can happen if the JNI side failed to start.
            // There's no reason not to close such a terminal.
            return true;
        }

        final int directChildPid = ptyProcess.getPid();
        final String processesUsingTty = ptyProcess.listProcessesUsingTty();

        if (processesUsingTty.length() == 0) {
            // There's nothing still running, so just close.
            return true;
        }
        if (processesUsingTty.equals("(pty closed)")) {
            return true;
        }

        // We're trying to protect the user from accidentally killing or hiding some background process they've forgotten about.
        // We used to try to prevent them from killing something, other than a shell that we started, by accident.
        // We stopped doing that because it was a pain confirming the desire to close connections to a serial port,
        // where killing the connecting program kills nothing that's running behind the serial port.
        // An SSH session remains a confusing case (Mac OS actually has a user-editable list of programs to ignore).
        // FIXME: ideally, PtyProcess would give us a List<ProcessInfo>, but that opens a whole can of JNI worms. Hence the following hack.
        final String[] processList = processesUsingTty.split(", ");
        if (processList.length == 1 && processList[0].matches("^.*\\(" + directChildPid + "\\)$")) {
            return true;
        }

        return host.confirmClose(processesUsingTty);
    }

    /**
     * Closes the terminal pane after checking with the user.
     * Returns false if the user canceled the close, true otherwise.
     */
    public boolean doCheckedCloseAction() {
        if (shouldClose()) {
            doCloseAction();
            return true;
        }
        return false;
    }
    
    public void doCloseAction() {
        destroyProcess();
        control.getTerminalLogWriter().close();
        host.closeTerminalPane(this);
    }
    
    /**
     * Implements visual bell.
     */
    public void flash() {
        viewport.flash();
    }
    
    public TerminalPaneHost getHost() {
        return host;
    }
    
    public FindPanel getFindPanel() {
        return findPanel;
    }
}
