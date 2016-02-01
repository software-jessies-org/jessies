package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import e.gui.*;
import e.util.*;

/**
 * Simple TELNET support.
 */
public class ETelnetWindow extends EWindow implements LinkListener {
    private JTextPane text;
    private LinkRecognizer linkRecognizer;
    private LinkFormatter linkFormatter;
    
    private ETextField inputLine;
    private Socket socket;
    private TelnetSocket telnetSocket;
    private StyleContext styles = new StyleContext();
    
    private String hostname;
    private int portNumber;
    
    public ETelnetWindow(String hostAndPort) {
        super("telnet://" + hostAndPort + "/");
        this.hostname = hostAndPort;
        this.portNumber = 23;
        if (hostname.indexOf(':') != -1) {
            portNumber = Integer.parseInt(hostname.substring(hostname.indexOf(':') + 1));
            hostname = hostname.substring(0, hostname.indexOf(':'));
            if (hostname.endsWith("/")) {
                hostname = hostname.substring(0, hostname.length() - 1);
            }
        }
        attemptLogin();
    }

    public void requestFocus() {
        inputLine.requestFocus();
    }
    
    public void showPromptDialog(String prompt) {
        PromptDialog promptDialog = new PromptDialog(this, prompt);
        promptDialog.setLocationRelativeTo(this);
        promptDialog.setVisible(true);
    }
    
    public class PromptDialog extends EDialog {
        private String prompt;
        private JTextField field; // Can be ETextField or JPasswordField.
        
        public PromptDialog(EWindow window, String prompt) {
            super(window);
            this.prompt = prompt;
            this.setTitle(prompt);
        }
        
        public JComponent createUI() {
            if (prompt.equals("Password")) {
                field = new JPasswordField(10);
            } else {
                field = new ETextField(10);
            }
            if (prompt.equals("Username")) {
                field.setText(Parameters.getParameter("user.name", ""));
            }
            field.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    telnetSocket.sendLine(field.getText());
                    inputLine.requestFocus();
                    PromptDialog.this.setVisible(false);
                    dispose();
                }
            });
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
            panel.add(new JLabel(prompt + ":", SwingConstants.TRAILING), BorderLayout.WEST);
            panel.add(field, BorderLayout.CENTER);
            return panel;
        }

        public void aboutToBeShown() {
            field.selectAll();
        }
    }

    public void attemptLogin() {
        this.text = new JTextPane();
        this.linkFormatter = new LinkFormatter(text);
        this.linkRecognizer = new LinkRecognizer(text, this);
        text.setCaret(new ECaret());
        initTextStyles();
        this.inputLine = new ETextField();
        inputLine.addActionListener(new SendLineAction());
        inputLine.addFocusListener(this);
        inputLine.getCaret().setBlinkRate(0);
        add(inputLine, BorderLayout.SOUTH);
        add(new JScrollPane(text), BorderLayout.CENTER);
        attachPopupMenuTo(text);
        connect();
    }
    
    public void linkActivated(String link) {
        Edit.openFile(link);
    }
    
    public Collection getPopupMenuItems() {
        ArrayList items = new ArrayList();
        items.add(new StopProcessAction());
        items.add(new ClearAction());
        return items;
    }
    
    public class SendLineAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            telnetSocket.sendLine(inputLine.getText());
        }
    }
    
    public class ClearAction extends AbstractAction {
        public ClearAction() {
            super("Clear");
        }
        public void actionPerformed(ActionEvent e) {
            text.setText("");
        }
    }
    
    public class StopProcessAction extends AbstractAction {
        public StopProcessAction() {
            super("Interrupt");
        }
        public void actionPerformed(ActionEvent e) {
            telnetSocket.sendInterrupt();
        }
    }
    
    public void connect() {
        try {
            this.socket = new Socket(hostname, portNumber);
            this.telnetSocket = new TelnetSocket(socket);
            telnetSocket.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private boolean localEcho = false;
    private StringBuffer lineBuffer = new StringBuffer();
    private javax.swing.Timer receiveTimeout = new javax.swing.Timer(300, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            flushLineBuffer();
        }
    });
    private boolean readingOSC = false;
    
    public void processChar(char ch) {
        if (readingOSC) {
            if (ch == '\\' || ch == 0x7) {
                readingOSC = false;
            }
            return;
        }
        if (ch <= 0x7) {
            // Most telnetd(1) implementations seem to have a bug whereby
            // they send the NUL byte at the end of the C strings they want to
            // output when you first connect. Since all Unixes are pretty much
            // copy and pasted from one another these days, this silly mistake
            // only needed to be made once.
            
            // Nothing below BEL is printable anyway.
            
            // And neither is BEL, really.
            return;
        }
        if (false && ch < ' ') { Log.warn("received "+Integer.toHexString(ch)); }
        if (ch == '\r') {
            // FIXME: if the character following the CR isn't NL, should we erase
            // to beginning of line instead of just pretend we never saw the CR?
            return;
        }
        if (ch == ESC) {
            flushLineBuffer();
            inEscape = true;
            return;
        }
        if (inEscape) {
            escapeBuffer.append(ch);
            //FIXME: does anything else terminate an ANSI escape sequence?
            if (Character.isLetter(ch) || ch == '>') {
                processEscape();
            }
        } else {
            //Log.warn("appending char " + Integer.toString(ch) + " hex:" + Integer.toHexString(ch));
            lineBuffer.append(ch);
            receiveTimeout.restart();
            if (ch == '\n') {
                flushLineBuffer();
            }
        }
    }
    
    public boolean inEscape = false;
    private StringBuffer escapeBuffer = new StringBuffer();
    
    public void processEscape() {
        inEscape = false;
        String sequence = escapeBuffer.toString();
        escapeBuffer.setLength(0);
        Log.warn("sequence '" + sequence + "'");
        if (sequence.startsWith("[") && sequence.endsWith("m")) {
            processFontEscape(sequence);
        } else if (sequence.startsWith("]")) {
            // OSC.
            readingOSC = true;
        } else if (sequence.equals("c")) {
            // Full reset (RIS).
            text.setText("");
        } else {
            Log.warn("Unknown escape sequence: '" + sequence);
        }
    }
    
    public void processFontEscape(String sequence) {
        String newStyleName = sequence.substring(1, sequence.length() - 1);
        if (newStyleName.startsWith("1;") || newStyleName.startsWith("0;")) {
            // We embolden all colors anyway.
            newStyleName = newStyleName.substring(2);
        }
        if (newStyleName.length() == 0 || newStyleName.equals("00")) {
            newStyleName = "0";
        }
        linkFormatter.setCurrentStyle(styles.getStyle(newStyleName));
    }
    
    public static final int ESC = 0x1b;
    
    public void flushLineBuffer() {
        String line = lineBuffer.toString();
        lineBuffer.setLength(0);
        if (line.endsWith("login: ")) {
            showPromptDialog("Username");
        } else if (line.endsWith("Password: ") || line.endsWith("Password:") || line.endsWith(" password:")) {
            showPromptDialog("Password");
            getWorkspace().registerTextComponent(text);
        } else {
            linkFormatter.appendLine(line);
        }
        receiveTimeout.stop();
    }
    
    public void initTextStyles() {
        Style plainStyle = styles.addStyle("plain", styles.getStyle(StyleContext.DEFAULT_STYLE));
        StyleConstants.setFontFamily(plainStyle, Parameters.getParameter("font.name", "verdana"));
        StyleConstants.setFontSize(plainStyle, Parameters.getParameter("font.size", 12));
        if (Parameters.getParameter("telnet.retroLook", false)) {
            StyleConstants.setForeground(plainStyle, Color.WHITE);
            text.setBackground(new Color(0, 0, 69));
        }
        
        Style style = styles.addStyle("0", plainStyle);
        linkFormatter.setCurrentStyle(style);
        
        style = styles.addStyle("1", plainStyle);
        StyleConstants.setBold(style, true);
        style = styles.addStyle("4", plainStyle);
        StyleConstants.setUnderline(style, true);
        
        addColorStyle("30", Color.BLACK);
        addColorStyle("31", Color.RED);
        addColorStyle("32", Color.GREEN);
        addColorStyle("33", Color.ORANGE);
        addColorStyle("34", Color.BLUE);
        addColorStyle("35", Color.MAGENTA);
        addColorStyle("36", Color.CYAN);
        addColorStyle("37", Color.LIGHT_GRAY);
        
        Style linkStyle = styles.addStyle("link", plainStyle);
        StyleConstants.setForeground(linkStyle, Color.BLUE);
        StyleConstants.setUnderline(linkStyle, true);
        linkFormatter.setLinkStyle(linkStyle);
    }
    
    public void addColorStyle(String number, Color color) {
        Style defaultStyle = styles.getStyle("plain");
        Style style = styles.addStyle(number, defaultStyle);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, true); // Colors don't show up well otherwise.
    }
    
    public class TelnetSocket extends Thread {
        private BufferedReader reader;
        private InputStream in;
        private OutputStream out;
        
        public TelnetSocket(Socket s) throws IOException {
            in = s.getInputStream();
            out = s.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(in));
        }
        
        public void sendLine(String line) {
            try {
                out.write(line.getBytes());
                out.write('\r');
                out.write('\n');
                out.flush();
                inputLine.selectAll();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        public void sendInterrupt() {
            try {
                sendCommand(INTERRUPT_PROCESS);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        public void run() {
            try {
                while (true) {
                    int i = in.read();
                    if (i == -1) { return; }
                    if (i != IAC) {
                        processChar((char) i);
                    } else {
                        i = in.read();
                        if (i == -1) { return; }
                        if (i != IAC) {
                            interpretAsCommand(i);
                        } else {
                            processChar((char) i);
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                Log.warn("Returning from TelnetSocket.run()");
                setTitle(getTitle() + " - Disconnected");
                inputLine.setEnabled(false);
            }
        }
        
        public void sendCommand(int type) throws IOException {
            out.write(new byte[] { (byte) IAC, (byte) type });
        }
        
        public void sendCommand(int type, int detail) throws IOException {
            out.write(new byte[] { (byte) IAC, (byte) type, (byte) detail });
        }

        public void sendDo(int what) throws IOException {
            sendCommand(DO, what);
        }
        
        public void sendDoNot(int what) throws IOException {
            sendCommand(DO_NOT, what);
        }
        
        public void sendWill(int what) throws IOException {
            sendCommand(WILL, what);
        }
        
        public void sendWillNot(int what) throws IOException {
            sendCommand(WILL_NOT, what);
        }
        
        public void interpretAsCommand(int b) throws IOException {
            if (b == WILL) {
                int what = in.read();
                if (what == TelnetOption.ECHO || what == TelnetOption.SUPPRESS_GO_AHEAD) {
                    sendDo(what);
                } else if (what == TelnetOption.STATUS) {
                    sendDoNot(what);
                } else {
                    Log.warn("Got WILL  " + what);
                }
            } else if (b == WILL_NOT) {
                int what = in.read();
                Log.warn("Got WON'T " + what);
            } else if (b == DO) {
                int what = in.read();
                if (what == TelnetOption.ECHO) {
                    sendWill(TelnetOption.ECHO);
                    localEcho = true;
                    Log.warn("Enabling local echo...");
                } else if (what == TelnetOption.TERMINAL_TYPE) {
                    sendWill(what);
                } else if (what == TelnetOption.TERMINAL_SPEED || what == TelnetOption.NAWS || what == TelnetOption.NEW_ENVIRON || what == TelnetOption.X_DISPLAY_LOCATION || what == TelnetOption.TOGGLE_FLOW_CONTROL) {
                    sendWillNot(what);
                } else {
                    Log.warn("Got DO    " + what + "; refusing.");
                    sendWillNot(what);
                }
            } else if (b == DO_NOT) {
                int what = in.read();
                if (what == TelnetOption.ECHO) {
                    Log.warn("Disabling local echo...");
                    localEcho = false;
                    sendWillNot(TelnetOption.ECHO);
                } else {
                    Log.warn("Got DON'T " + what + "; don't understand!");
                }
            } else if (b == SB) {
                int what = in.read();
                Log.warn("Start sub-option negotiation for " + what);
                if (what == TelnetOption.TERMINAL_TYPE) {
                    out.write(new byte[] { (byte) IAC, (byte) SB, (byte) TelnetOption.TERMINAL_TYPE, (byte) IS });
                    out.write("vt100".getBytes());
                    sendCommand(SE);
                }
                // FIXME: should we skip bytes until we see IAC SE?
            } else if (b == SE) {
                // Subnegotiation is over.
                Log.warn("End sub-option negotiation");
            } else {
                Log.warn("Got unknown command " + Integer.toHexString(b));
            }
        }
    }
    
//    public void addWindowSpecificMenuItems(Collection items) {
//        items.add(null);
//        items.add(new ClearAction());
//    }
    
//    public class ClearAction extends EAction {
//        public String getName() { return "Clear"; }
//        public void actionPerformed(ActionEvent e) { setText("");}
//    }
    
    /** Errors windows are never considered dirty because they're not worth preserving. */
    public boolean isDirty() {
        return false;
    }
    
    public String getWordSelectionStopChars() {
        return " \t\n";
    }
    
    public String getContext() {
        return "";
    }
    
    /** TELNET: End of subnegotiation parameters. */
    private static final int SE = 240;
    
    /** TELNET: No Operation. */
    private static final int NOP = 241;
    
    /**
     * TELNET: The data stream portion of a Synch.
     * This should always be accompanied by a TCP Urgent notification.
     */
    private static final int DATA_MARK = 242 ;
    
    private static final int BREAK = 243;
    private static final int INTERRUPT_PROCESS = 244;
    private static final int ABORT_OUTPUT = 245;
    private static final int ARE_YOU_THERE = 246;
    private static final int ERASE_CHARACTER = 247;
    private static final int ERASE_LINE = 248;
    private static final int GO_AHEAD = 249;
    private static final int SB = 250; // Indicates that what follows is subnegotiation of the indicated option
    private static final int IS = 0;
    
    private static final int WILL = 251; // Indicates the desire to begin performing, or confirmation that you are now performing, the indicated option
    private static final int WILL_NOT = 252; // Indicates the refusal to perform, or continue performing, the indicated option.
    private static final int DO = 253; // Indicates the request that the other party perform, or confirmation that you are expecting the other party to perform, the indicated option.
    private static final int DO_NOT = 254; // Indicates the demand that the other party stop performing, or confirmation that you are no longer expecting the other party to perform, the indicated option.
    
    /** TELNET IAC: Interpret As Command. */
    private static final int IAC = 255;
    
    public class TelnetOption {
        public static final int ECHO = 1;
        public static final int SUPPRESS_GO_AHEAD = 3;
        public static final int STATUS = 5;
        public static final int TERMINAL_TYPE = 24;
        /** Negotiate About Window Size. */
        public static final int NAWS = 31;
        public static final int TERMINAL_SPEED = 32;
        public static final int TOGGLE_FLOW_CONTROL = 33;
        public static final int X_DISPLAY_LOCATION = 35;
        public static final int OLD_ENVIRON = 36;
        public static final int NEW_ENVIRON = 39;

        private TelnetOption() { }
    }
}
