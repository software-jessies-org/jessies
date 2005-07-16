package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

import e.forms.*;
import e.gui.*;
import e.util.*;

public class Edit implements com.apple.eawt.ApplicationListener {
    private static Edit instance;
    
    private com.apple.eawt.Application application;
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private TagsPanel tagsPanel;
    private Advisor advisor;
    private EStatusBar statusLine;
    private Minibuffer minibuffer;
    private JPanel statusArea;
    
    /** Extensions that shouldn't be opened by Edit. */
    private String[] externalApplicationExtensions;
    
    /** The global find history for all FindDialog instances. */
    private EHistoryComboBoxModel findHistory = new ChronologicalComboBoxModel();
    
    /** Tests false once we're fully started. */
    private boolean initializing = true;
    
    private List<String> initialFilenames;
    
    private ArrayList<Element> initialWorkspaces;
    
    private JPanel statusLineAndProgressContainer = new JPanel(new BorderLayout());
    
    private JProgressBar progressBar = new JProgressBar();
    private JPanel progressBarAndKillButton = new JPanel(new BorderLayout());
    private Process process;
    
    public static synchronized Edit getInstance() {
        if (instance == null) {
            instance = new Edit();
            instance.init();
        }
        return instance;
    }
    
    /** Returns the frame Edit is using for its main window. */
    public Frame getFrame() {
        return (Frame) frame;
    }
    
    public EHistoryComboBoxModel getFindHistory() {
        return findHistory;
    }
    
    public TagsPanel getTagsPanel() {
        return tagsPanel;
    }

    public Advisor getAdvisor() {
        return advisor;
    }

    public void showStatus(String status) {
        statusLine.setText(status);
    }

    public void showAlert(String title, String message) {
        SimpleDialog.showAlert(getFrame(), title, message);
    }

    public boolean askQuestion(String title, String message, String continueText) {
        return SimpleDialog.askQuestion(getFrame(), title, message, continueText);
    }

    public String askQuestion(String title, String message, String continueTextYes, String continueTextNo) {
        return SimpleDialog.askQuestion(getFrame(), title, message, continueTextYes, continueTextNo);
    }

    private void showDocument(String url) {
        try {
            BrowserLauncher.openURL(url);
        } catch (Throwable th) {
            SimpleDialog.showDetails(getFrame(), "Hyperlink", th);
        }
    }
    
    public boolean isFileForExternalApplication(String filename) {
        if (externalApplicationExtensions == null) {
            externalApplicationExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.externalApplicationExtensions", ""));
        }
        return FileUtilities.nameEndsWithOneOf(filename, externalApplicationExtensions);
    }
    
    public void openFileWithExternalApplication(String filename) {
        try {
            Runtime.getRuntime().exec(new String[] {
                Parameters.getParameter("open.command"), filename
            });
        } catch (Exception ex) {
            showAlert("Run", "Couldn't open '" + filename + "' with an external application (" + ex.getMessage() + ")");
        }
    }
    
    /**
     * Sees if any of the "path.rewrite" configuration applies to the given filename,
     * and replaces the matching prefix with the appropriate substitution. Returns the
     * original filename if there's no applicable rewrite.
     */
    public String processPathRewrites(String filename) {
        String from;
        for (int i = 0; (from = Parameters.getParameter("path.rewrite.from." + i)) != null; i++) {
            if (filename.startsWith(from)) {
                String to = Parameters.getParameter("path.rewrite.to." + i);
                String result = to + filename.substring(from.length());
                return result;
            }
        }
        return filename;
    }

    /**
     * Opens a file. If the file's already open, it receives the focus. The 'filename'
     * parameter is actually a grep-style filename:address string.
     * 
     * Returns the EWindow corresponding to the file opened, or null if
     * no file was opened or the file was passed to an external program.
     */
    public EWindow openFile(String filename) {
        try {
            return openFileNonInteractively(filename);
        } catch (Exception ex) {
            Log.warn("Problem opening file \"" + filename + "\"", ex);
            showAlert("Open", ex.getMessage());
            return null;
        }
    }
    
    public EWindow openFileNonInteractively(String filename) {
        /* Special case for a URI. */
        if (FileUtilities.nameStartsWithOneOf(filename, FileUtilities.getArrayOfPathElements(Parameters.getParameter("url.prefixes", "")))) {
            showDocument(filename);
            return null;
        }
        
        filename = processPathRewrites(filename);
        
        /* Tidy up wrong-OS pathnames. Programs like jikes do this to us. */
        if (File.separatorChar == '\\') {
            filename = filename.replace('/', '\\');
        } else {
            filename = filename.replace('\\', '/');
        }
        
        /* Extract any address; a trailing sequence of ":\d+" with an optional trailing ':'. */
        Pattern addressPattern = Pattern.compile("^(.+?)((:\\d+)*):?$");
        Matcher addressMatcher = addressPattern.matcher(filename);
        final String address;
        if (addressMatcher.find()) {
            address = addressMatcher.group(2);
            filename = addressMatcher.group(1);
        } else {
            address = "";
        }
        
        Log.warn("Opening '" + filename + "' at '" + address + "'");
        
        /* Remove local-directory fluff. */
        if (filename.startsWith("./") || filename.startsWith(".\\")) {
            filename = filename.substring(2);
        }
        
        if (isFileForExternalApplication(filename)) {
            openFileWithExternalApplication(filename);
            return null;
        }
        
        /* Give up if the file doesn't exist. */
        if (FileUtilities.exists(filename) == false) {
            throw new RuntimeException("File '" + filename + "' does not exist.");
        }
        
        try {
            /*
             * Open the file a symbolic link points to, and not the link itself.
             * Clean paths like a/b/../c/d. Let the Java API do this.
             */
            filename = FileUtilities.fileFromString(filename).getCanonicalPath();
        } catch (IOException ex) {
            /* Harmless. */
            ex = ex;
        }
        filename = normalizeWorkspacePrefix(filename);
        filename = FileUtilities.getUserFriendlyName(filename);
        
        /* Refuse to open directories. */
        if (FileUtilities.fileFromString(filename).isDirectory()) {
            throw new RuntimeException("Edit can't edit directories, which is what '" + filename + "' is.");
        }
        
        /* Limit ourselves (rather arbitrarily) to files under half a gigabyte. That's quite a strain on us, at present. */
        final int KB = 1024;
        final int MB = 1024 * KB;
        long fileLength = FileUtilities.fileFromString(filename).length();
        if (fileLength > 512 * MB) {
            throw new RuntimeException("Edit can't really handle files as large as '" + filename + "', which is " + fileLength + " bytes long. This file will not be opened.");
        }
        
        /* Find which workspace this file is on/should be on, and make it visible. */
        Workspace workspace = getBestWorkspaceForFilename(filename);
        tabbedPane.setSelectedComponent(workspace);
        
        /* If the user already has this file open, we shouldn't open it again. */
        EWindow alreadyOpenWindow = workspace.findIfAlreadyOpen(filename, address);
        if (alreadyOpenWindow != null) {
            return alreadyOpenWindow;
        }
        
        /* Add an appropriate viewer for the filename to the chosen workspace. */
        return workspace.addViewerForFile(filename, address);
    }
    
    /** Returns an array of all the workspaces. */
    public Workspace[] getWorkspaces() {
        Workspace[] result = new Workspace[tabbedPane.getTabCount()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = (Workspace) tabbedPane.getComponentAt(i);
        }
        return result;
    }

    /**
     * Substituting ~/ for /home/userName isn't the only friendly transformation
     * that we can usefully and easily make.  If the user has configured us
     * with workspace paths which include symbolic links, then it's a reasonable
     * presumption that the user would like us to consistently use the form of
     * the path which they supplied rather than the OS-canonical form (supplied
     * by File.getCanonicalPath).
     * A symbolic link within such a workspace would have both symbolic links
     * canonicalized in openFile and so would end up with a path prefixed by
     * the desired Workspace's canonical name rather than its friendly name.
     * This would send it to the wrong Workspace.
     * FIXME: Consistency probably demands that this be called from
     * FileUtilities.getUserFriendlyName rather than Edit.openFile.
     */
    public String normalizeWorkspacePrefix(String filename) {
        for (Workspace workspace : getWorkspaces()) {
            try {
                String friendlyPrefix = workspace.getRootDirectory();
                String canonicalPrefix = workspace.getCanonicalRootDirectory();
                if (filename.startsWith(canonicalPrefix)) {
                    return friendlyPrefix + filename.substring(canonicalPrefix.length());
                }
            } catch (IOException ex) {
                /* Harmless. */
                ex = ex;
            }
        }
        return filename;
    }
    
    /** Returns the workspace whose root directory shares the longest common prefix with the given filename. */
    public Workspace getBestWorkspaceForFilename(String filename) {
        Workspace bestWorkspace = (Workspace) tabbedPane.getSelectedComponent();
        int bestLength = 0;
        for (Workspace workspace : getWorkspaces()) {
            String workspaceRoot = workspace.getRootDirectory();
            if (filename.startsWith(workspaceRoot)) {
                int length = workspaceRoot.length();
                if (length > bestLength) {
                    bestWorkspace = workspace;
                    bestLength = length;
                }
            }
        }
        return bestWorkspace;
    }
    
    public Workspace getCurrentWorkspace() {
        if (tabbedPane == null) {
            return null;
        }
        return (Workspace) tabbedPane.getSelectedComponent();
    }
    
    /**
     * Finds the appropriate index in tabbedPane for a workspace with the given name.
     * The idea is that sorting workspaces alphabetically will remove the urge to want
     * manual control over workspace order. Alphabetical order is sensible enough that
     * we shouldn't be upset by a workspace seeming to be in the wrong place, as was
     * so easily the case with the previous implicit chronological order.
     */
    public int getWorkspaceIndexInTabbedPane(String name) {
        int index = 0;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            if (name.compareTo(title) <= 0) {
                break;
            }
            index++;
        }
        return index;
    }
    
    public void cycleWorkspaces(int indexDelta) {
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) {
            return;
        }
        int newIndex = (index + indexDelta) % tabbedPane.getTabCount();
        if (newIndex == -1) {
            newIndex = tabbedPane.getTabCount() - 1;
        }
        tabbedPane.setSelectedIndex(newIndex);
    }
    
    private void addWorkspace(final Workspace workspace) {
        String name = workspace.getTitle();
        tabbedPane.insertTab(name, null, workspace, workspace.getRootDirectory(), getWorkspaceIndexInTabbedPane(name));
        tabbedPane.setSelectedComponent(workspace);
        fireTabbedPaneTabCountChange();
        
        // We need to ensure that the workspace has been validated so that it
        // and its children have bounds, so that EColumn has a non-zero height
        // so that when we come to add components to it using EColumn.moveTo,
        // we don't stack them all on top of one another.
        workspace.revalidate();
        frame.validate();
        
        showStatus("Added workspace '" + name + "' (" + workspace.getRootDirectory() + ")");
    }

    public Workspace createWorkspace(String name, String root) {
        boolean noNonEmptyWorkspaceOfThisNameExists = removeWorkspaceByName(name);
        if (noNonEmptyWorkspaceOfThisNameExists == false) {
            showAlert("Edit", "A non-empty workspace of the name '" + name + "' already exists.");
            return null;
        }
        Workspace workspace = new Workspace(name, root);
        addWorkspace(workspace);
        moveFilesToBestWorkspaces();
        return workspace;
    }
    
    /**
     * Removes any workspaces with the given name. Returns false if there was a workspace,
     * but it couldn't be removed because it had open files, true otherwise.
     */
    public boolean removeWorkspaceByName(String name) {
        for (Workspace workspace : getWorkspaces()) {
            if (workspace.getTitle().equals(name)) {
                if (workspace.isEmpty() == false) {
                    return false;
                }
                tabbedPane.remove(workspace);
                fireTabbedPaneTabCountChange();
            }
        }
        return true;
    }
    
    public void moveFilesToBestWorkspaces() {
        for (Workspace workspace : getWorkspaces()) {
            workspace.moveFilesToBestWorkspaces();
        }
    }
    
    public void removeCurrentWorkspace() {
        Workspace workspace = getCurrentWorkspace();
        if (workspace == null) {
            return;
        }
        if (getWorkspaces().length == 1) {
            showAlert("Edit", "Cannot remove the last workspace.");
            return;
        }
        tabbedPane.remove(workspace);
        fireTabbedPaneTabCountChange();
        workspace.moveFilesToBestWorkspaces();
    }
    
    /**
     * Writes out the workspace list on a new thread to avoid disk access on
     * the event dispatch thread. Any code that adds or removes workspaces
     * should invoke this method. Annoyingly, JTabbedPane doesn't make such
     * changes observable, so it's down to us to be careful.
     */
    private void fireTabbedPaneTabCountChange() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                rememberState();
            }
        });
        thread.start();
    }
    
    public void createWorkspaceForCurrentDirectory() {
        String currentDirectory = Parameters.getParameter("user.dir");
        String workspaceName = currentDirectory.substring(currentDirectory.lastIndexOf(File.separatorChar) + 1);
        createWorkspace(workspaceName, currentDirectory);
    }
    
    public String getResourceFilename(String leafName) {
        return System.getenv("EDIT_HOME") + File.separatorChar + leafName;
    }
    
    public void initWindowIcon() {
        ImageIcon icon = new ImageIcon(getResourceFilename("icon.gif"));
        frame.setIconImage(icon.getImage());
    }
    
    public void handleAbout(com.apple.eawt.ApplicationEvent e) {
        showAlert("Edit", "Edit is free software. See the file COPYING for copying permission.");
        if (e != null) {
            e.setHandled(true);
        }
    }
    
    public void handleOpenApplication(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleOpenApplication");
    }
    
    public void handleReOpenApplication(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleReOpenApplication");
    }
    
    public void handleOpenDocument(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleOpenDocument");
    }
    public void handleOpenFile(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handleOpenFile");
    }
    
    public void handlePreferences(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handlePreferences");
    }
    
    public void handlePrintDocument(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handlePrintDocument");
    }
    public void handlePrintFile(com.apple.eawt.ApplicationEvent e) {
        Log.warn("handlePrintFile");
    }
    
    /**
     * Attempts to quit Edit. All the workspaces are asked if it's safe for them to be
     * closed. Only if all workspaces agree that it's safe will Edit actually quit.
     */
    public void handleQuit(com.apple.eawt.ApplicationEvent e) {
        boolean isSafeToQuit = true;
        boolean onMacOS = (e != null);
        for (Workspace workspace : getWorkspaces()) {
            if (workspace.getDirtyTextWindows().length != 0) {
                isSafeToQuit = false;
                // Ensure that the workspace in question is visible.
                tabbedPane.setSelectedComponent(workspace);
            }
        }
        
        if (onMacOS) {
            // Let Apple's library code know whether or not to terminate us when we return.
            e.setHandled(isSafeToQuit);
        }
        
        if (isSafeToQuit == false) {
            showAlert("Edit", "There are unsaved files. Please deal with them and try again.");
            return;
        }
        
        // We're definitely going to quit now...
        rememberState();
        if (onMacOS == false) {
            System.exit(0);
        }
    }

    /**
     * Writes out all our various bits of state to disk, so that next
     * time we start, we start more or less where we left off.
     */
    public void rememberState() {
        if (initializing) {
            // If we haven't finished initializing, we may not have
            // read all the state in, so writing it back out could
            // actually throw state away. We don't want to do that.
            return;
        }

        SpellingChecker.dumpKnownBadWordsTo(System.out);
        FormDialog.writeGeometriesTo(getDialogGeometriesPreferenceFilename());
        
        writeSavedState();
    }
    
    /** Returns the full pathname for the given preference file. */
    public String getPreferenceFilename(String leafname) {
        return System.getProperty("preferencesDirectory") + File.separator + leafname;
    }
    
    public String getDialogGeometriesPreferenceFilename() {
        return getPreferenceFilename("dialog-geometries");
    }
    
    public String getOpenFileListPreferenceFilename() {
        return getPreferenceFilename("open-file-list");
    }
    
    public String getOpenWorkspaceListPreferenceFilename() {
        return getPreferenceFilename("open-workspace-list");
    }
    
    /** Opens all the files listed in the file we remembered them to last time we quit. */
    public void openRememberedFiles() {
        showStatus("Opening remembered files...");
        if (initialFilenames != null) {
            for (String filename : initialFilenames) {
                openFile(filename);
            }
            showStatus((initialFilenames.size() == 0) ? "No files to open" : "Finished opening files");
        }
    }
    
    /** Opens all the workspaces listed in the file we remembered them to last time we quit. */
    public void openRememberedWorkspaces() {
        Log.warn("Opening remembered workspaces...");
        if (initialWorkspaces != null) {
            for (Element info : initialWorkspaces) {
                addWorkspace(info.getAttribute("name"), info.getAttribute("root"), info.getAttribute("buildTarget"));
            }
            return;
        }
    }
    
    private void readSavedState() {
        Point initialLocation = new Point(0, 0);
        Dimension initialSize = new Dimension(800, 730);
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(FileUtilities.fileFromString(getPreferenceFilename("saved-state.xml")));
            
            Element root = document.getDocumentElement();
            initialLocation.x = Integer.parseInt(root.getAttribute("x"));
            initialLocation.y = Integer.parseInt(root.getAttribute("y"));
            initialSize.width = Integer.parseInt(root.getAttribute("width"));
            initialSize.height = Integer.parseInt(root.getAttribute("height"));
            
            initialWorkspaces = new ArrayList<Element>();
            initialFilenames = new ArrayList<String>();
            for (Node workspace = root.getFirstChild(); workspace != null; workspace = workspace.getNextSibling()) {
                if (workspace instanceof Element) {
                    initialWorkspaces.add((Element) workspace);
                    for (Node file = workspace.getFirstChild(); file != null; file = file.getNextSibling()) {
                        if (file instanceof Element) {
                            initialFilenames.add(((Element) file).getAttribute("name"));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.warn("Problem reading saved state", ex);
        }
        frame.setLocation(initialLocation);
        frame.setSize(initialSize);
    }
    
    private synchronized void writeSavedState() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();
            
            Element root = document.createElement("edit");
            document.appendChild(root);
            
            Point position = frame.getLocation();
            root.setAttribute("x", Integer.toString((int) position.getX()));
            root.setAttribute("y", Integer.toString((int) position.getY()));
            Dimension size = frame.getSize();
            root.setAttribute("width", Integer.toString((int) size.getWidth()));
            root.setAttribute("height", Integer.toString((int) size.getHeight()));
            
            for (Workspace workspace : getWorkspaces()) {
                workspace.serializeAsXml(document, root);
            }
            
            // Write the XML to a new file...
            String filename = getPreferenceFilename("saved-state.xml2");
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(filename)));

            // ...and then rename it to the file we want, to protect against
            // losing all our state on failure.
            File realFile = FileUtilities.fileFromString(getPreferenceFilename("saved-state.xml"));
            FileUtilities.fileFromString(filename).renameTo(realFile);
        } catch (Exception ex) {
            Log.warn("Problem writing saved state", ex);
        }
    }
    
    public void addWorkspace(String name, String root, String buildTarget) {
        Log.warn("Opening workspace '" + name + "' with root '" + root + "'");
        Workspace workspace = createWorkspace(name, root);
        if (workspace == null) {
            return;
        }
        
        workspace.setBuildTarget(buildTarget);
        File rootDirectory = FileUtilities.fileFromString(workspace.getRootDirectory());
        int which = tabbedPane.indexOfComponent(workspace);
        if (rootDirectory.exists() == false) {
            tabbedPane.setForegroundAt(which, Color.RED);
            tabbedPane.setToolTipTextAt(which, root + " doesn't exist.");
        } else if (rootDirectory.isDirectory() == false) {
            tabbedPane.setForegroundAt(which, Color.RED);
            tabbedPane.setToolTipTextAt(which, root + " isn't a directory.");
        }
    }
    
    public void initWindowListener() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleQuit(null);
            }
        });
    }
    
    public void initWindow() {
        initWindowIcon();
        initWindowListener();
    }
    
    public void initTagsPanel() {
        tagsPanel = new TagsPanel();
    }
    
    public void initAdvisor() {
        advisor = new Advisor();
    }
    
    public void initPreferences() {
        Parameters.readPropertiesFile(getPreferenceFilename("edit.properties"));
    }
    
    public void initStatusArea() {
        statusLine = new EStatusBar();
        minibuffer = new Minibuffer();
        
        statusLineAndProgressContainer.add(statusLine, BorderLayout.CENTER);
        
        statusArea = new JPanel(new BorderLayout());
        statusArea.setBorder(new javax.swing.border.EmptyBorder(2, 2, 2, 2));
        statusArea.add(statusLineAndProgressContainer, BorderLayout.NORTH);
        statusArea.add(minibuffer, BorderLayout.SOUTH);
        
        // Add some padding so that the tall Mac OS progress bar doesn't cause
        // the status line to jiggle when it appears...
        statusLine.setBorder(new javax.swing.border.EmptyBorder(2, 0, 2, 0));
        // ...and so that it doesn't intrude on the area reserved for the
        // grow box (and flicker as they fight about who gets drawn on top).
        statusLineAndProgressContainer.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 20));
        
        progressBarAndKillButton.add(progressBar, BorderLayout.CENTER);
        progressBarAndKillButton.add(makeKillButton(), BorderLayout.EAST);
    }
    
    private JButton makeKillButton() {
        JButton killButton = new JButton(new e.gui.StopIcon(Color.RED));
        killButton.setPressedIcon(new e.gui.StopIcon(Color.RED.darker()));
        killButton.setRolloverIcon(new e.gui.StopIcon(new Color(255, 100, 100)));
        killButton.setRolloverEnabled(true);
        killButton.setBorder(null);
        killButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProcessUtilities.spawn(null, new String[] { "kill", "-TERM", Integer.toString(ProcessUtilities.getProcessId(process)) });
            }
        });
        return killButton;
    }
    
    public synchronized void showProgressBar(Process process) {
        this.process = process;
        progressBar.setIndeterminate(true);
        statusLineAndProgressContainer.add(progressBarAndKillButton, BorderLayout.EAST);
        statusLineAndProgressContainer.repaint();
    }
    
    public synchronized void hideProgressBar() {
        this.process = null;
        statusLineAndProgressContainer.remove(progressBarAndKillButton);
        progressBar.setIndeterminate(false);
        statusLineAndProgressContainer.repaint();
    }
    
    public void showMinibuffer(MinibufferUser minibufferUser) {
        minibuffer.activate(minibufferUser);
    }
    
    private Edit() {
    }

    private void init() {
        long start = System.currentTimeMillis();
        
        application = new com.apple.eawt.Application();
        application.addApplicationListener(this);

        frame = new JFrame("Edit");
        if (GuiUtilities.isMacOs()) {
            frame.setVisible(true);
        }
        
        initPreferences();
        FormDialog.readGeometriesFrom(getDialogGeometriesPreferenceFilename());
        
        /* FIXME: is there a better way round this two-stage menubar construction? */
        EditMenuBar menuBar = new EditMenuBar();
        frame.setJMenuBar(menuBar);
        menuBar.populate();
        
        readSavedState();
        initWindow();
        initTagsPanel();
        initAdvisor();
        initStatusArea();
        
        new EditServer(this);
        
        UIManager.put("TabbedPane.useSmallLayout", Boolean.TRUE);
        tabbedPane = new JTabbedPane(GuiUtilities.isMacOs() ? JTabbedPane.LEFT : JTabbedPane.TOP);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Edit.getInstance().getTagsPanel().ensureTagsAreHidden();
                getCurrentWorkspace().restoreFocusToRememberedTextWindow();
            }
        });
        
        JSplitPane tagsAndAdvisorSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tagsPanel, advisor);
        tagsAndAdvisorSplitPane.setDividerLocation(0.8);
        tagsAndAdvisorSplitPane.setResizeWeight(0.8);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, tabbedPane, tagsAndAdvisorSplitPane);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusArea, BorderLayout.SOUTH);
        
        openRememberedWorkspaces();
        // If that didn't create any workspaces, give the user one for free...
        if (tabbedPane.getTabCount() == 0) {
            createWorkspaceForCurrentDirectory();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                openRememberedFiles();
                initializing = false;
            }
        });
        
        frame.setVisible(true);
        splitPane.setDividerLocation(0.8);
        splitPane.setResizeWeight(0.8);
        
        long end = System.currentTimeMillis();
        Log.warn("Ready to use after " + (end - start) + " ms!");
        
        startScanningWorkspaces();
    }

    private void startScanningWorkspaces() {
        for (Workspace workspace : getWorkspaces()) {
            workspace.updateFileList(null);
        }
    }
}
