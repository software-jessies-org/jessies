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
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.*;
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
    private JSplitPane splitPane;
    private TagsPanel tagsPanel;
    private EStatusBar statusLine;
    private Minibuffer minibuffer;
    private JPanel statusArea;
    
    /** Extensions that we shouldn't open. */
    private String[] externalApplicationExtensions;
    
    /** The global find history for all FindDialog instances. */
    private EHistoryComboBoxModel findHistory = new ChronologicalComboBoxModel();
    
    /** Whether we're fully started. */
    private CountDownLatch startSignal = new CountDownLatch(1);
    
    private InitialState initialState = new InitialState();
    
    private class InitialState {
        private ArrayList<InitialWorkspace> initialWorkspaces = new ArrayList<InitialWorkspace>();
        private Point initialLocation = new Point(0, 0);
        private Dimension initialSize = new Dimension(800, 730);
        private boolean showTagsPanel;
        private int splitPaneDividerLocation = -1;
        
        /**
         * Opens all the workspaces listed in the file we remembered them to last time we quit.
         * Also makes the last-visible workspace visible again.
         */
        private void openRememberedWorkspaces() {
            Log.warn("Opening remembered workspaces...");
            if (initialWorkspaces == null) {
                return;
            }
            
            Workspace initiallyVisibleWorkspace = null;
            for (InitialWorkspace initialWorkspace : initialState.initialWorkspaces) {
                Element info = initialWorkspace.xmlWorkspace;
                Workspace workspace = openWorkspace(info.getAttribute("name"), info.getAttribute("root"), info.getAttribute("buildTarget"), initialWorkspace.initialFiles);
                if (info.hasAttribute("selected")) {
                    initiallyVisibleWorkspace = workspace;
                }
            }
            initialWorkspaces = null;
            
            if (initiallyVisibleWorkspace != null) {
                tabbedPane.setSelectedComponent(initiallyVisibleWorkspace);
            }
        }
        
        /** Shows or hides the tags panel and sets the divider location, as it was last time we quit. */
        private void configureTagsPanel() {
            if (showTagsPanel) {
                if (splitPaneDividerLocation >= 0) {
                    splitPane.setDividerLocation(splitPaneDividerLocation);
                } else {
                    // Defaults.
                    splitPane.setDividerLocation(0.8);
                    splitPane.setResizeWeight(0.8);
                }
            } else {
                ShowHideTagsAction.setTagsPanelVisibility(false);
                ShowHideTagsAction.oldDividerLocation = splitPaneDividerLocation;
            }
        }
    }
    
    private static class InitialWorkspace {
        Element xmlWorkspace;
        private List<InitialFile> initialFiles = new ArrayList<InitialFile>();
        
        private InitialWorkspace(Element xmlWorkspace) {
            this.xmlWorkspace = xmlWorkspace;
        }
        
        private void addInitialFile(String name, int y, boolean lastFocused) {
            initialFiles.add(new InitialFile(name, y, lastFocused));
        }
    }
    
    public static class InitialFile {
        String filename; // strictly, filename(:address)?
        int y;
        boolean lastFocused;
        
        private InitialFile(String filename, int y, boolean lastFocused) {
            this.filename = filename;
            this.y = y;
            this.lastFocused = lastFocused;
        }
        
        private InitialFile(String filename) {
            this(filename, -1, false);
        }
    }
    
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
    
    public JSplitPane getSplitPane() {
        return splitPane;
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
    
    public EWindow openFile(InitialFile file) {
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
        
        // Special case for URIs. We don't insist on matching two slashes because Java has a habit of turning a File into something like "file:/usr/bin/bash".
        if (filename.matches("[a-z]+:/.*")) {
            if (filename.startsWith("file:") && filename.matches("file:/.*\\.html") == false) {
                // We open non-HTML file: references ourselves.
                filename = filename.substring(5);
                // Removing leading slashes is tricky, because file URLs aren't as consistent as http ones.
                // There's one common case we have to fix, though, where "file://" has been prepended to a path starting with "~".
                if (filename.startsWith("//~")) {
                    filename = filename.substring(2);
                }
            } else {
                // Everything else is the platform's problem.
                showDocument(filename);
                return null;
            }
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
            Pattern addressPattern = Pattern.compile("^((?:[A-Za-z]:\\\\){0,1}.+?)((:\\d+)*)(:|:?$)");
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
        if (startSignal.getCount() == 0) {
            tabbedPane.setSelectedComponent(workspace);
        }
        
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
        Workspace[] result = new Workspace[tabbedPane != null ? tabbedPane.getTabCount() : 0];
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
            String friendlyPrefix = workspace.getRootDirectory();
            String canonicalPrefix = workspace.getCanonicalRootDirectory();
            if (filename.startsWith(canonicalPrefix)) {
                return friendlyPrefix + filename.substring(canonicalPrefix.length());
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
    
    public void goToWorkspaceByIndex(int workspaceIndex) {
        if (workspaceIndex >= 0 && workspaceIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(workspaceIndex);
        }
    }
    
    private void addWorkspaceToTabbedPane(final Workspace workspace) {
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
        addWorkspaceToTabbedPane(workspace);
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
        String currentDirectory = System.getProperty("user.dir");
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
                Workspace firstDirtyWorkspace = dirtyWorkspaces.get(0);
                tabbedPane.setSelectedComponent(firstDirtyWorkspace);
                // Ensure that at least the first dirty window is maximally visible.
                ETextWindow firstDirtyWindow = firstDirtyWorkspace.getDirtyTextWindows()[0];
                firstDirtyWindow.expand();
                firstDirtyWindow.requestFocusInWindow();
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
        if (startSignal.getCount() != 0) {
            // If we haven't finished initializing, we may not have
            // read all the state in, so writing it back out could
            // actually throw state away. We don't want to do that.
            return;
        }

        //SpellingChecker.dumpKnownBadWordsTo(System.out);
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
    
    private void readSavedState() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(FileUtilities.fileFromString(getPreferenceFilename("saved-state.xml")));
            
            Element root = document.getDocumentElement();
            initialState.initialLocation.x = Integer.parseInt(root.getAttribute("x"));
            initialState.initialLocation.y = Integer.parseInt(root.getAttribute("y"));
            initialState.initialSize.width = Integer.parseInt(root.getAttribute("width"));
            initialState.initialSize.height = Integer.parseInt(root.getAttribute("height"));
            
            initialState.showTagsPanel = true;
            if (root.hasAttribute("showTagsPanel")) {
                initialState.showTagsPanel = Boolean.parseBoolean(root.getAttribute("showTagsPanel"));
                if (root.hasAttribute("splitPaneDividerLocation")) {
                    initialState.splitPaneDividerLocation = Integer.parseInt(root.getAttribute("splitPaneDividerLocation"));
                }
            }
            
            for (Node xmlWorkspace = root.getFirstChild(); xmlWorkspace != null; xmlWorkspace = xmlWorkspace.getNextSibling()) {
                if (xmlWorkspace instanceof Element) {
                    InitialWorkspace workspace = new InitialWorkspace((Element) xmlWorkspace);
                    initialState.initialWorkspaces.add(workspace);
                    for (Node file = xmlWorkspace.getFirstChild(); file != null; file = file.getNextSibling()) {
                        if (file instanceof Element) {
                            Element fileElement = (Element) file;
                            workspace.addInitialFile(fileElement.getAttribute("name"), Integer.parseInt(fileElement.getAttribute("y")), fileElement.hasAttribute("lastFocused"));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.warn("Problem reading saved state", ex);
        }
        frame.setLocation(initialState.initialLocation);
        frame.setSize(initialState.initialSize);
        JFrameUtilities.constrainToScreen(frame);
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
            
            root.setAttribute("showTagsPanel", Boolean.toString(tagsPanel.isVisible()));
            root.setAttribute("splitPaneDividerLocation", Integer.toString(tagsPanel.isVisible() ? splitPane.getDividerLocation() : ShowHideTagsAction.oldDividerLocation));
            
            for (Workspace workspace : getWorkspaces()) {
                workspace.serializeAsXml(document, root);
            }
            
            // Set up a Transformer to produce indented XML output.
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
            
            // Create the XML content...
            StringWriter content = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(content));
            
            // And then carefully write it to disk.
            File file = FileUtilities.fileFromString(getPreferenceFilename("saved-state.xml"));
            if (writeAtomicallyTo(file, content.toString()) == false) {
                Log.warn("\"" + file + "\" content should have been:\n" + content.toString());
            }
        } catch (Exception ex) {
            Log.warn("Problem writing saved state", ex);
        }
    }
    
    private static boolean writeAtomicallyTo(File file, CharSequence chars) {
        // We save to a new file first, to reduce our chances of corrupting the real file, or at least increase our chances of having one intact copy.
        File backupFile = new File(file.toString() + ".bak");
        try {
            StringUtilities.writeFile(backupFile, chars);
        } catch (Exception ex) {
            return false;
        }
        
        // Now we write to the intended destination.
        // If the destination was a symbolic link on a CIFS server, it's important to write to the original file rather than creating a new one.
        
        // CIFS also causes problems if we try renaming the backup file to the intended file.
        // For one thing, the destination must not exist, but removing the destination would make it harder to be atomic.
        // Also, the source must not be open, which is not easy to guarantee in Java, and often not the case as soon as you'd like.
        try {
            StringUtilities.writeFile(file, chars);
        } catch (Exception ex) {
            return false;
        }
        
        // Everything went well so far, so delete the backup file (ignoring failures) and return success.
        backupFile.delete();
        return true;
    }
    
    private Workspace openWorkspace(String name, String root, String buildTarget, List<InitialFile> initialFiles) {
        Log.warn("Opening workspace '" + name + "' with root '" + root + "'");
        Workspace workspace = createWorkspace(name, root);
        if (workspace == null) {
            return null;
        }
        
        workspace.setBuildTarget(buildTarget);
        workspace.setInitialFiles(initialFiles);
        File rootDirectory = FileUtilities.fileFromString(workspace.getRootDirectory());
        int which = tabbedPane.indexOfComponent(workspace);
        if (rootDirectory.exists() == false) {
            tabbedPane.setForegroundAt(which, Color.RED);
            tabbedPane.setToolTipTextAt(which, root + " doesn't exist.");
        } else if (rootDirectory.isDirectory() == false) {
            tabbedPane.setForegroundAt(which, Color.RED);
            tabbedPane.setToolTipTextAt(which, root + " isn't a directory.");
        }
        return workspace;
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
        frame = new JFrame("Evergreen");
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
        statusArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        statusArea.add(statusLineAndProgressContainer, BorderLayout.NORTH);
        statusArea.add(minibuffer, BorderLayout.SOUTH);
        
        // Add some padding so that the tall and fixed-height Mac OS progress
        // bar doesn't cause the status line to jiggle when it appears, and so
        // that on Linux the progress bar doesn't allow itself to look squashed.
        statusLine.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        if (GuiUtilities.isMacOs()) {
            // Make room on Mac OS so that our components don't intrude on the
            // area reserved for the grow box (and cause flicker as they fight
            // about who gets drawn on top).
            statusLineAndProgressContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
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
        aboutBox.setWebSiteAddress("http://software.jessies.org/Evergreen/");
        aboutBox.addCopyright("Copyright (C) 2004-2007 Free Software Foundation, Inc.");
        aboutBox.addCopyright("All Rights Reserved.");
    }
    
    public void awaitInitialization() {
        try {
            startSignal.await();
        } catch (InterruptedException ex) {
        }
    }
    
    private void initRememberedFilesOpener() {
        ChangeListener initialFileOpener = new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        getCurrentWorkspace().openRememberedFiles();
                    }
                });
            }
        };
        tabbedPane.addChangeListener(initialFileOpener);
        // Bring the listener up to speed with the current situation.
        // We couldn't add the listener in advance, because we want the tabbed pane on the display before we start listening.
        initialFileOpener.stateChanged(null);
    }
    
    private void init() {
        final long t0 = System.nanoTime();
        
        initPreferences();
        initMacOs();
        initAboutBox();
        JavaResearcher.initOnBackgroundThread();
        FormDialog.readGeometriesFrom(getDialogGeometriesPreferenceFilename());
        initWindow();
        initTagsPanel();
        tabbedPane = new EvergreenTabbedPane();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, tabbedPane, tagsPanel);
        initStatusArea();
        readSavedState();
        
        InetAddress wildcardAddress = null;
        new InAppServer("EditServer", getPreferenceFilename("edit-server-port"), wildcardAddress, EditServer.class, new EditServer(this));
        
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusArea, BorderLayout.SOUTH);
        frame.setJMenuBar(new EvergreenMenuBar());
        
        initialState.openRememberedWorkspaces();
        
        frame.setVisible(true);
        GuiUtilities.finishGnomeStartup();
        Log.warn("Frame visible after " + TimeUtilities.nsToString(System.nanoTime() - t0) + ".");
        
        // These things want to be done after the frame is visible...
        
        initialState.configureTagsPanel();
        initRememberedFilesOpener();
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (tabbedPane.getTabCount() == 0) {
                    // If we didn't create any workspaces, give the user some help...
                    showAlert("Welcome to Evergreen!", "This looks like the first time you've used Evergreen. You'll need to create workspaces corresponding to the projects you wish to work on.<p>Choose \"Add Workspace...\" from the the \"Workspace\" menu.<p>You can create as many workspaces as you like, but you'll need at least one to be able to do anything.");
                }
                
                Thread fileListUpdaterStarterThread = new Thread(new Runnable() {
                    public void run() {
                        final long t1 = System.nanoTime();
                        try {
                            // What I really want to say is "wait until the event queue is empty and everything's caught up".
                            // I haven't yet found out how to do that, and the fact that using EventQueue.invokeLater here doesn't achieve the desired effect makes me think that what I want might not be that simple anyway.
                            // Seemingly, though, if we run at MIN_PRIORITY, we don't get much of a look-in until the EDT is idle anyway.
                            // Hans Muller's got code to wait for the event queue to clear: https://appframework.dev.java.net/source/browse/appframework/trunk/AppFramework/src/application/Application.java?rev=36&view=auto&content-type=text/vnd.viewcvs-markup
                            // Until that's available as GPL/LGPL, or as part of the JDK, here's a simpler alternative that's good enough for our purposes here.
                            EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
                            while (queue.peekEvent() != null) {
                                Thread.sleep(100);
                            }
                        } catch (InterruptedException ex) {
                        }
                        
                        Log.warn("Workers free to start after " + TimeUtilities.nsToString(System.nanoTime() - t0) + " (slept for " + TimeUtilities.nsToString(System.nanoTime() - t1) + ").");
                        startSignal.countDown();
                    }
                });
                fileListUpdaterStarterThread.setPriority(Thread.MIN_PRIORITY);
                fileListUpdaterStarterThread.start();
            }
        });
    }
}
