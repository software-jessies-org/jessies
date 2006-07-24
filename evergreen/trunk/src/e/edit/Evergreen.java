package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*; 
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class Evergreen {
    private static Evergreen instance;
    
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private TagsPanel tagsPanel;
    private EStatusBar statusLine;
    private Minibuffer minibuffer;
    private JPanel statusArea;
    
    /** Extensions that we shouldn't open. */
    private String[] externalApplicationExtensions;
    
    /** The global find history for all FindDialog instances. */
    private EHistoryComboBoxModel findHistory = new ChronologicalComboBoxModel();
    
    /** Tests false once we're fully started. */
    private boolean initializing = true;
    
    private class InitialFile {
        String filename;
        int y;
        private InitialFile(String filename, int y) {
            this.filename = filename;
            this.y = y;
        }
        private InitialFile(String filename) {
            this(filename, -1);
        }
    }
    
    private List<InitialFile> initialFiles;
    
    private ArrayList<Element> initialWorkspaces;
    
    private JPanel statusLineAndProgressContainer = new JPanel(new BorderLayout());
    
    private JProgressBar progressBar = new JProgressBar();
    private JPanel progressBarAndKillButton = new JPanel(new BorderLayout(4, 0));
    private Process process;
    
    public static synchronized Evergreen getInstance() {
        if (instance == null) {
            instance = new Evergreen();
            instance.init();
        }
        return instance;
    }
    
    /** Returns the frame we're using for the main window. */
    public Frame getFrame() {
        return (Frame) frame;
    }
    
    public EHistoryComboBoxModel getFindHistory() {
        return findHistory;
    }
    
    public TagsPanel getTagsPanel() {
        return tagsPanel;
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
    
    private boolean isFileForExternalApplication(String filename) {
        if (externalApplicationExtensions == null) {
            externalApplicationExtensions = FileUtilities.getArrayOfPathElements(Parameters.getParameter("files.externalApplicationExtensions", ""));
        }
        return FileUtilities.nameEndsWithOneOf(filename, externalApplicationExtensions);
    }
    
    private void openFileWithExternalApplication(String filename) {
        try {
            // FIXME: for Java 6, use java.awt.Desktop instead.
            String openCommand = "gnome-open";
            if (GuiUtilities.isMacOs()) {
                openCommand = "/usr/bin/open";
            } else if (GuiUtilities.isWindows()) {
                openCommand = "start";
            }
            ProcessUtilities.spawn(null, new String[] { openCommand, FileUtilities.parseUserFriendlyName(filename) });
        } catch (Exception ex) {
            showAlert("Couldn't open \"" + filename + "\"", "The external application failed to start: " + ex.getMessage() + ".");
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
    
    private EWindow openFile(InitialFile file) {
        try {
            return openFileNonInteractively(file);
        } catch (Exception ex) {
            Log.warn("Problem opening file \"" + file.filename + "\"", ex);
            showAlert("Couldn't open \"" + file.filename + "\"", ex.getMessage());
            return null;
        }
    }
    
    /**
     * Opens a file. If the file's already open, it receives the focus. The 'filename'
     * parameter is actually a grep-style filename:address string.
     * 
     * Returns the EWindow corresponding to the file opened, or null if
     * no file was opened or the file was passed to an external program.
     */
    public EWindow openFile(String filename) {
        return openFile(new InitialFile(filename));
    }
    
    public EWindow openFileNonInteractively(String filename) {
        return openFileNonInteractively(new InitialFile(filename));
    }
    
    public EWindow openFileNonInteractively(InitialFile file) {
        String filename = file.filename;
        
        // Special case for URIs.
        if (filename.matches("^[a-z]+://.*")) {
            showDocument(filename);
            return null;
        }
        // Special case for files to open in external applications.
        if (isFileForExternalApplication(filename)) {
            openFileWithExternalApplication(filename);
            return null;
        }
        
        filename = processPathRewrites(filename);
        
        // Remove local-directory fluff.
        if (filename.startsWith("./") || filename.startsWith(".\\")) {
            filename = filename.substring(2);
        }
        
        // Extract the address and any trailing junk, if this isn't just a
        // filename. We check for the file's existence first to cope with such
        // unfortunate names as "titanfs1-fsm_console-2005-10-27-09:47:47.txt".
        String address = "";
        if (FileUtilities.exists(filename) == false) {
            // Extract any address, which here means a trailing sequence of ":\d+"s.
            // Note that we try hard to cope with trailing junk so we can work with
            // grep(1) matches along the lines of "file.cpp:123:void something()"
            // where the end of the address and the beginning of the actual line
            // are run together. We keep regressing on this behavior!
            Pattern addressPattern = Pattern.compile("^(.+?)((:\\d+)*)(:|:?$)");
            Matcher addressMatcher = addressPattern.matcher(filename);
            if (addressMatcher.find()) {
                address = addressMatcher.group(2);
                filename = addressMatcher.group(1);
            }
        }
        
        Log.warn("Opening '" + filename + "'" + (address.length() > 0 ? (" at '" + address + "'") : ""));
        
        // Give up if the file doesn't exist.
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
        
        // Refuse to open directories.
        if (FileUtilities.fileFromString(filename).isDirectory()) {
            throw new RuntimeException("It's not possible to edit directories, which is what '" + filename + "' is.");
        }
        
        // Limit ourselves (rather arbitrarily) to files under half a gigabyte. That's quite a strain on us, at present.
        final int KB = 1024;
        final int MB = 1024 * KB;
        long fileLength = FileUtilities.fileFromString(filename).length();
        if (fileLength > 512 * MB) {
            throw new RuntimeException("The file '" + filename + "', which is " + fileLength + " bytes long is too large. This file will not be opened.");
        }
        
        // Find which workspace this file is on/should be on, and make it visible.
        Workspace workspace = getBestWorkspaceForFilename(filename);
        tabbedPane.setSelectedComponent(workspace);
        
        // If the user already has this file open, we shouldn't open it again.
        EWindow alreadyOpenWindow = workspace.findIfAlreadyOpen(filename, address);
        if (alreadyOpenWindow != null) {
            return alreadyOpenWindow;
        }
        
        // Add an appropriate viewer for the filename to the chosen workspace.
        return workspace.addViewerForFile(filename, address, file.y);
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
     * FileUtilities.getUserFriendlyName rather than openFile.
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
            showAlert("Couldn't create workspace", "A non-empty workspace with the name \"" + name + "\" already exists.");
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
    
    public void removeWorkspace(Workspace workspace) {
        if (workspace == null) {
            showAlert("Couldn't remove workspace", "There's no workspace selected.");
            return;
        }
        if (getWorkspaces().length == 1) {
            showAlert("Couldn't remove workspace", "The last workspace cannot be removed.");
            return;
        }
        String question = "Do you really want to remove the workspace \"" + workspace.getTitle() + "\"?";
        if (workspace.isEmpty() == false) {
            question += " Open windows will be moved to the next best workspace.";
        }
        boolean remove = askQuestion("Remove workspace?", question, "Remove");
        if (remove == false) {
            return;
        }
        
        tabbedPane.remove(workspace);
        fireTabbedPaneTabCountChange();
        workspace.moveFilesToBestWorkspaces();
        workspace.dispose();
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
        return System.getenv("EDIT_HOME") + File.separator + "lib" + File.separator + "data" + File.separator + leafName;
    }
    
    public void initWindowIcon() {
        JFrameUtilities.setFrameIcon(frame);
    }
    
    /**
     * Attempts to quit. All the workspaces are asked if it's safe for them to be
     * closed. Only if all workspaces agree that it's safe will we actually quit.
     */
    public void handleQuit(com.apple.eawt.ApplicationEvent e) {
        boolean isSafeToQuit = true;
        boolean onMacOS = (e != null);
        
        ArrayList<String> dirtyFileNames = new ArrayList<String>();
        ArrayList<Workspace> dirtyWorkspaces = new ArrayList<Workspace>();
        for (Workspace workspace : getWorkspaces()) {
            ETextWindow[] dirtyWindows = workspace.getDirtyTextWindows();
            for (ETextWindow dirtyWindow : dirtyWindows) {
                dirtyFileNames.add(dirtyWindow.getFilename());
            }
            if (dirtyWindows.length != 0) {
                isSafeToQuit = false;
                dirtyWorkspaces.add(workspace);
            }
        }
        
        if (onMacOS) {
            // Let Apple's library code know whether or not to terminate us when we return.
            e.setHandled(isSafeToQuit);
        }
        
        if (isSafeToQuit == false) {
            if (dirtyWorkspaces.size() == 1) {
                // Ensure that the workspace in question is visible.
                tabbedPane.setSelectedComponent(dirtyWorkspaces.get(0));
            }
            showAlert("You have files with unsaved changes", "There are unsaved files:<p>" + StringUtilities.join(dirtyFileNames, "<br>") + "<p>Please deal with them and try again.");
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
        if (initialFiles != null) {
            for (InitialFile file : initialFiles) {
                openFile(file);
            }
            showStatus((initialFiles.size() == 0) ? "No files to open" : "Finished opening files");
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
            initialFiles = new ArrayList<InitialFile>();
            for (Node workspace = root.getFirstChild(); workspace != null; workspace = workspace.getNextSibling()) {
                if (workspace instanceof Element) {
                    initialWorkspaces.add((Element) workspace);
                    for (Node file = workspace.getFirstChild(); file != null; file = file.getNextSibling()) {
                        if (file instanceof Element) {
                            Element fileElement = (Element) file;
                            // FIXME: this is only needed until everyone's saved state has been upgraded.
                            int y = fileElement.hasAttribute("y") ? Integer.parseInt(fileElement.getAttribute("y")) : -1;
                            InitialFile initialFile = new InitialFile(fileElement.getAttribute("name"), y);
                            initialFiles.add(initialFile);
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
            // FIXME: You can't rename over an existing file in Windows-semantics file systems (but it's nice and atomic in Unix).
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
    
    public void initPreferences() {
        Parameters.readPropertiesFile(getPreferenceFilename("edit.properties"));
    }
    
    public void initStatusArea() {
        statusLine = new EStatusBar();
        minibuffer = new Minibuffer();
        
        statusLineAndProgressContainer.add(statusLine, BorderLayout.CENTER);
        
        statusArea = new JPanel(new BorderLayout());
        statusArea.setBorder(new EmptyBorder(2, 2, 2, 2));
        statusArea.add(statusLineAndProgressContainer, BorderLayout.NORTH);
        statusArea.add(minibuffer, BorderLayout.SOUTH);
        
        // Add some padding so that the tall and fixed-height Mac OS progress
        // bar doesn't cause the status line to jiggle when it appears, and so
        // that on Linux the progress bar doesn't allow itself to look squashed.
        statusLine.setBorder(new EmptyBorder(2, 0, 2, 0));
        if (GuiUtilities.isMacOs()) {
            // Make room on Mac OS so that our components don't intrude on the
            // area reserved for the grow box (and cause flicker as they fight
            // about who gets drawn on top).
            statusLineAndProgressContainer.setBorder(new EmptyBorder(0, 0, 0, 20));
        }
        
        progressBarAndKillButton.add(progressBar, BorderLayout.CENTER);
        progressBarAndKillButton.add(makeKillButton(), BorderLayout.EAST);
    }
    
    private JButton makeKillButton() {
        JButton killButton = StopIcon.makeStopButton();
        killButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProcessUtilities.terminateProcess(process);
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
    
    private Evergreen() {
    }
    
    private void initMacOs() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        com.apple.eawt.Application.getApplication().addApplicationListener(new com.apple.eawt.ApplicationAdapter() {
            public void handleQuit(com.apple.eawt.ApplicationEvent e) {
                Evergreen.this.handleQuit(e);
            }
        });
    }
    
    private void initAboutBox() {
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setApplicationName("Evergreen");
        aboutBox.addCopyright("Copyright (C) 2004-2006 Free Software Foundation, Inc.");
        aboutBox.addCopyright("All Rights Reserved.");
    }
    
    private void init() {
        final long startTimeMillis = System.currentTimeMillis();
        
        initPreferences();
        initMacOs();
        initAboutBox();
        
        frame = new JFrame("Evergreen");
        frame.setJMenuBar(new EvergreenMenuBar());
        
        FormDialog.readGeometriesFrom(getDialogGeometriesPreferenceFilename());
        
        readSavedState();
        initWindow();
        initTagsPanel();
        initStatusArea();
        
        InetAddress wildcardAddress = null;
        new InAppServer("EditServer", getPreferenceFilename("edit-server-port"), wildcardAddress, EditServer.class, new EditServer(this));
        
        UIManager.put("TabbedPane.useSmallLayout", Boolean.TRUE);
        tabbedPane = new JTabbedPane(GuiUtilities.isMacOs() ? JTabbedPane.LEFT : JTabbedPane.TOP);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Evergreen.getInstance().getTagsPanel().ensureTagsAreHidden();
                getCurrentWorkspace().restoreFocusToRememberedTextWindow();
            }
        });
        EPopupMenu tabMenu = new EPopupMenu(tabbedPane);
        tabMenu.addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                // If the user clicked on some part of the tabbed pane that isn't actually a tab, we're not interested.
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (tabIndex == -1) {
                    return;
                }
                
                Workspace workspace = (Workspace) tabbedPane.getComponentAt(tabIndex);
                actions.add(new RescanWorkspaceAction(workspace));
                actions.add(null);
                actions.add(new EditWorkspaceAction(workspace));
                actions.add(new RemoveWorkspaceAction(workspace));
            }
        });
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, tabbedPane, tagsPanel);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusArea, BorderLayout.SOUTH);
        
        openRememberedWorkspaces();
        
        frame.setVisible(true);
        Log.warn("Frame visible after " + (System.currentTimeMillis() - startTimeMillis) + " ms.");
        
        splitPane.setDividerLocation(0.8);
        splitPane.setResizeWeight(0.8);
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                openRememberedFiles();
                initializing = false;
                
                Log.warn("All remembered files opened after " + (System.currentTimeMillis() - startTimeMillis) + " ms.");
                
                startScanningWorkspaces();
                
                // If we didn't create any workspaces, give the user some help...
                if (tabbedPane.getTabCount() == 0) {
                    showAlert("Welcome to Evergreen!", "This looks like the first time you've used Evergreen. You'll need to create workspaces corresponding to the projects you wish to work on.<p>Choose \"Add Workspace...\" from the the \"Workspace\" menu.<p>You can create as many workspaces as you like, but you'll need at least one to be able to do anything.");
                }
            }
        });
    }

    private void startScanningWorkspaces() {
        for (Workspace workspace : getWorkspaces()) {
            workspace.updateFileList(null);
        }
    }
}
