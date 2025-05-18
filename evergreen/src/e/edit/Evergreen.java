package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*; 
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import org.w3c.dom.*;

public class Evergreen {
    private static Evergreen instance;
    
    private JFrame frame;
    private TabbedPane tabbedPane;
    private JSplitPane splitPane;
    private TagsPanel tagsPanel;
    private EStatusBar statusLine = new EStatusBar();
    private Minibuffer minibuffer;
    private JPanel statusArea;
    
    /** Whether we're fully started. */
    private CountDownLatch startSignal = new CountDownLatch(1);
    
    private EvergreenPreferences preferences;
    private Preferences fileTypePreferences;
    
    private InitialState initialState = new InitialState();
    
    private class InitialState {
        private ArrayList<InitialWorkspace> initialWorkspaces = new ArrayList<>();
        private boolean showTagsPanel = true;
        private boolean tagsPanelOnLeft = false;
        private int tagsPanelSplitPaneDividerLocation = -1;
        
        /**
         * Opens all the workspaces listed in the file we remembered them to last time we quit.
         * Also makes the last-visible workspace visible again.
         */
        private void openRememberedWorkspaces() {
            if (initialWorkspaces == null) {
                return;
            }
            
            Workspace initiallyVisibleWorkspace = null;
            for (InitialWorkspace initialWorkspace : initialState.initialWorkspaces) {
                Element info = initialWorkspace.xmlWorkspace;
                WorkspaceProperties properties = new WorkspaceProperties();
                properties.name = info.getAttribute("name");
                properties.rootDirectory = info.getAttribute("root");
                properties.buildTarget = info.getAttribute("buildTarget");
                Workspace workspace = openWorkspace(properties, initialWorkspace.initialFiles);
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
                if (tagsPanelSplitPaneDividerLocation >= 0) {
                    splitPane.setDividerLocation(tagsPanelSplitPaneDividerLocation);
                } else {
                    // Defaults.
                    splitPane.setDividerLocation(initialState.tagsPanelOnLeft ? 0.2 : 0.8);
                }
                splitPane.setResizeWeight(initialState.tagsPanelOnLeft ? 0.2 : 0.8);
            } else {
                ShowHideTagsAction.setTagsPanelVisibility(false);
                ShowHideTagsAction.oldDividerLocation = tagsPanelSplitPaneDividerLocation;
            }
        }
    }
    
    private static class InitialWorkspace {
        Element xmlWorkspace;
        private List<InitialFile> initialFiles = new ArrayList<>();
        
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
    
    public static synchronized Evergreen getInstance() {
        if (instance == null) {
            instance = new Evergreen();
            instance.init();
        }
        return instance;
    }
    
    public Preferences getPreferences() {
        return preferences;
    }
    
    public Preferences getFileTypePreferences() {
        return fileTypePreferences;
    }
    
    /** Returns the frame we're using for the main window. */
    public JFrame getFrame() {
        return frame;
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

    /**
     * Sees if any of the "path.rewrite" configuration applies to the given filename,
     * and replaces the matching prefix with the appropriate substitution. Returns the
     * original filename if there's no applicable rewrite.
     */
    private String processPathRewrites(String filename) {
        String from;
        for (int i = 0; (from = Parameters.getString("path.rewrite.from." + i, null)) != null; i++) {
            if (filename.startsWith(from)) {
                String to = Parameters.getString("path.rewrite.to." + i, null);
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
        
        // Special case for URIs.
        // We don't insist on matching two slashes because Java has a habit of turning a File into something like "file:/usr/bin/bash".
        // We also insist on at least two characters for the URI scheme, to avoid being fooled by Windows drive letters.
        if (filename.matches("[a-z]{2,}:/.*")) {
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
                GuiUtilities.openUrl(filename);
                return null;
            }
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
            //
            // Note that we try hard to cope with trailing junk so we can work with
            // grep(1) matches along the lines of "file.cpp:123:void something()"
            // where the end of the address and the beginning of the actual line
            // are run together.
            //
            // Note also that we want any trailing ':' because we use that to mean
            // "select the line" rather than just "place the caret at the start of
            // this line".
            // 
            // FIXME: We keep regressing on this behavior! A unit test for this snippet would be an excellent idea.
            Pattern addressPattern = Pattern.compile("^((?:[A-Za-z]:\\\\){0,1}.+?)((:\\d+)*(:|:?$))");
            Matcher addressMatcher = addressPattern.matcher(filename);
            if (addressMatcher.find()) {
                address = addressMatcher.group(2);
                filename = addressMatcher.group(1);
            }
        }
        
        Log.warn("Opening \"" + filename + "\"" + (address.length() > 0 ? (" at \"" + address + "\"") : ""));
        
        // Give up if the file doesn't exist.
        if (FileUtilities.exists(filename) == false) {
            throw new RuntimeException("File \"" + filename + "\" does not exist.");
        }
        
        try {
            filename = resolvePath(filename).toString();
        } catch (IOException ignored) {
            /* Harmless. */
        }
        filename = normalizeWorkspacePrefix(filename);
        filename = FileUtilities.getUserFriendlyName(filename);
        Path path = FileUtilities.pathFrom(filename);
        
        // Refuse to open directories.
        if (Files.isDirectory(path)) {
            throw new RuntimeException("\"" + filename + "\" is a directory and so cannot be edited with Evergreen.");
        }
        
        // Limit ourselves (rather arbitrarily) to files under half a gigabyte. That's quite a strain on us, at present.
        try {
            final int KB = 1024;
            final int MB = 1024 * KB;
            long fileLength = Files.size(path);
            if (fileLength > 512 * MB) {
                throw new RuntimeException("The file \"" + filename + "\", which is " + fileLength + " bytes long is too large. This file will not be opened.");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        // Find which workspace this file is on/should be on, and make it visible.
        Workspace workspace = getBestWorkspaceForFilename(filename, null);
        Collection<Workspace> candidateWorkspaces = (workspace != null) ? Arrays.asList(workspace) : getWorkspaces();
        
        for (Workspace candidateWorkspace : candidateWorkspaces) {    
            // If the user already has this file open, we mustn't open it again on  a different workspace.
            EWindow alreadyOpenWindow = candidateWorkspace.findIfAlreadyOpen(filename, address);
            if (alreadyOpenWindow != null) {                
                if (isInitialized()) {
                    tabbedPane.setSelectedComponent(candidateWorkspace);
                }
                return alreadyOpenWindow;
            }
        }
        
        if (workspace == null) {
            workspace = (Workspace) tabbedPane.getSelectedComponent();
        }
        
        if (isInitialized()) {
            tabbedPane.setSelectedComponent(workspace);
        }
        
        // Add an appropriate viewer for the filename to the chosen workspace.
        return workspace.addViewerForFile(filename, address, file.y);
    }
    
    /**
     * Returns the resolved path, according to the following rules:
     * 1: If this is a symlink directly to a file, then use the canonical name. Always.
     * 2: If one of the parent dirs is a symlink to somewhere within our workspace, resolve it.
     * 3: If one of the parent dirs is a symlink outside the workspace, don't resolve.
     * The rationale here is to try to avoid opening duplicate views on the same file, but
     * if we've set up the workspace with symlinks into some larger tree, resolving everything
     * makes the files look like their outside the tree, and opening the "find in files" does not
     * properly populate the 'directory to search in'. In such a situation, it's helpful to
     * maintain the illusion that we're looking at a single coherent location on disk.
     */
    private Path resolvePath(String filename) throws IOException {
        // Resolve ~, but then convert to an absolute path to get rid of any /../ and other weirdness.
        Path path = FileUtilities.pathFrom(filename).toAbsolutePath();
        // toRealPath follows symlinks by default.
        Path realPath = path.toRealPath();
        // Now we have path and realPath, the latter of which is the actual path name with symlinks resolvedf.
        // Of course, if they're identical, we can just return either without bothering to do anything else.
        if (path.equals(realPath)) {
            return path;
        }
        if (Files.isSymbolicLink(path)) {
            // This is a symlink to a file, so always return the resolved name, so we don't end up overwriting
            // the symlink instead of the file it points to.
            return realPath;
        }
        Path workspacePath = getBestWorkspaceForFilename(path.toString(), getCurrentWorkspace()).getCanonicalRootPath();
        // By this point, we're dealing with a filename which has a symlink somewhere up its path.
        if (realPath.startsWith(workspacePath)) {
            // The actual file is in the same workspace, so use the resolved name so that multiple paths
            // within the same workspace to one file will only open one view of the file.
            return realPath;
        }
        // This is a filename existing outside of the workspace, but which is reached via a symlink,
        // probably but not necessarily linked from within the workspace to outside of it.
        // We assume the user has done this deliberately, for example using symlinks to create a
        // limited 'view' of a larger repository, but wants Evergreen to pretend the files are
        // actually within the workspace location itself.
        // Of course, this does leave the door open to people setting up two symlinks to the same directory
        // outside the workspace, and thus being able to open several views on the same file.
        // But if they do that, they'll just have to cope with the consequences. Or maybe they
        // actually want to, so they can see two parts of the file at once.
        return path;
    }
    
    /** Returns an array of all the workspaces. */
    public Collection<Workspace> getWorkspaces() {
        ArrayList<Workspace> result = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); ++i) {
            result.add((Workspace) tabbedPane.getComponentAt(i));
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
    private String normalizeWorkspacePrefix(String filename) {
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
    public Workspace getBestWorkspaceForFilename(String filename, Workspace bestWorkspace) {
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
    private static int chooseIndexInTabbedPane(JTabbedPane tabbedPane, String name) {
        int index = 0;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            if (name.compareToIgnoreCase(title) <= 0) {
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
        final String name = workspace.getWorkspaceName();
        final int newIndex = chooseIndexInTabbedPane(tabbedPane, name);
        tabbedPane.insertTab(name, null, workspace, workspace.getRootDirectory(), newIndex);
        tabbedPane.setSelectedComponent(workspace);
        
        // We need to ensure that the workspace has been validated so that it
        // and its children have bounds, so that EColumn has a non-zero height
        // so that when we come to add components to it using EColumn.moveTo,
        // we don't stack them all on top of one another.
        workspace.revalidate();
        frame.validate();
        
        showStatus("Added workspace \"" + name + "\" (" + workspace.getRootDirectory() + ")");
    }

    public void selectWorkspace(Workspace workspace) {
        tabbedPane.setSelectedComponent(workspace);
    }

    public Workspace createWorkspace(WorkspaceProperties properties) {
        // Ensure no non-empty workspace with the given name exists.
        final Workspace existingWorkspace = findWorkspaceByName(properties.name);
        if (existingWorkspace != null && !existingWorkspace.isEmpty()) {
            showAlert("Couldn't create workspace", "A non-empty workspace with the name \"" + properties.name + "\" already exists.");
            return null;
        }
        // Remove any empty workspace with the given name.
        if (existingWorkspace != null) {
            tabbedPane.remove(existingWorkspace);
            existingWorkspace.dispose();
        }
        
        final Workspace workspace = new Workspace(properties.name, properties.rootDirectory);
        workspace.setBuildTarget(properties.buildTarget);
        addWorkspaceToTabbedPane(workspace);
        workspaceConfigurationDidChange();
        return workspace;
    }
    
    public Workspace findWorkspaceByName(String name) {
        for (Workspace workspace : getWorkspaces()) {
            if (workspace.getWorkspaceName().equals(name)) {
                return workspace;
            }
        }
        return null;
    }
    
    public void closeWorkspace(Workspace workspace) {
        if (workspace == null) {
            showAlert("Couldn't close workspace", "There's no workspace selected.");
            return;
        }
        if (!workspace.getDirtyTextWindows().isEmpty()) {
            showAlert("Couldn't close workspace", "The workspace has unsaved files.<p>Please deal with them and try again.");
            return;
        }
        
        try {
            Files.delete(workspace.getFileListCachePath());
        } catch (IOException ex) {}
        
        // Removing the workspace from the index stops its windows from being moved to another workspace.
        tabbedPane.remove(workspace);
        // Stealthily support merging of nested workspaces back into their parent.
        // Now that the workspace has been removed from the tabbedPane,
        // which forms the collection of workspaces, the best workspace for any files
        // that were beneath its root directory will be the parent workspace, if there is one.
        // Files belonging to no indexed workspace will remain where they are and so will be closed.
        workspace.moveFilesToBestWorkspaces();
        // After collapsing a nested workspace back into its parent,
        // the parent is usually where you want to be.
        Workspace parent = getBestWorkspaceForFilename(workspace.getRootDirectory(), null);
        if (parent != null) {
            tabbedPane.setSelectedComponent(parent);
        }
        workspaceConfigurationDidChange();
        workspace.dispose();
    }
    
    /**
     * Writes out the workspace list on a new thread to avoid disk access on the EDT.
     * Any code that adds, alters, or removes a workspace must invoke this method.
     * Annoyingly, JTabbedPane (which we use as the workspace container) doesn't make such
     * changes observable, so it's down to us to be careful.
     */
    public void workspaceConfigurationDidChange() {
        new Thread(() -> { rememberState(); }).start();
        
        // Make sure every file is on the most suitable workspace.
        for (Workspace workspace : getWorkspaces()) {
            workspace.moveFilesToBestWorkspaces();
        }
        
        // Re-sort the tabs.
        int i = 0;
        for (Workspace workspace : getWorkspaces()) {
            final int newIndex = chooseIndexInTabbedPane(tabbedPane, workspace.getWorkspaceName());
            tabbedPane.moveTab(i, newIndex);
            ++i;
        }
    }
    
    public static String getResourceFilename(String... components) {
        return System.getProperty("org.jessies.projectRoot") + File.separator + StringUtilities.join(components, File.separator);
    }
    
    private void initWindowIcon() {
        JFrameUtilities.setFrameIcon(frame);
    }
    
    /**
     * Attempts to quit. All the workspaces are asked if it's safe for them to be
     * closed. Only if all workspaces agree that it's safe will we actually quit.
     */
    public void doQuit() {
        if (handleQuit()) System.exit(0);
    }
    
    public boolean handleQuit() {
        boolean isSafeToQuit = true;
        
        ArrayList<String> dirtyFileNames = new ArrayList<>();
        ArrayList<Workspace> dirtyWorkspaces = new ArrayList<>();
        for (Workspace workspace : getWorkspaces()) {
            Collection<ETextWindow> dirtyWindows = workspace.getDirtyTextWindows();
            for (ETextWindow dirtyWindow : dirtyWindows) {
                dirtyFileNames.add(dirtyWindow.getFilename());
            }
            if (!dirtyWindows.isEmpty()) {
                isSafeToQuit = false;
                dirtyWorkspaces.add(workspace);
            }
        }
        
        if (isSafeToQuit == false) {
            if (dirtyWorkspaces.size() == 1) {
                // Ensure that the workspace in question is visible.
                Workspace firstDirtyWorkspace = dirtyWorkspaces.get(0);
                tabbedPane.setSelectedComponent(firstDirtyWorkspace);
                // Ensure that at least the first dirty window is maximally visible.
                ETextWindow firstDirtyWindow = firstDirtyWorkspace.getDirtyTextWindows().iterator().next();
                firstDirtyWindow.expand();
                firstDirtyWindow.requestFocusInWindow();
            }
            showAlert("You have files with unsaved changes", "There are unsaved files:<p>" + StringUtilities.join(dirtyFileNames, "<br>") + "<p>Please deal with them and try again.");
            return false;
        }
        
        // We're definitely going to quit now...
        rememberState();
        return true;
    }

    /**
     * Writes out all our various bits of state to disk, so that next
     * time we start, we start more or less where we left off.
     */
    public void rememberState() {
        if (isInitialized() == false) {
            // If we haven't finished initializing, we may not have
            // read all the state in, so writing it back out could
            // actually throw state away. We don't want to do that.
            return;
        }

        //SpellingChecker.dumpKnownBadWordsTo(System.out);
        JFrameUtilities.writeGeometriesTo(getDialogGeometriesPreferenceFilename());
        writeSavedState();
        preferences.writeToDisk();
    }
    
    /** Returns the full pathname for the given preference file. */
    public static String getPreferenceFilename(String leafname) {
        return getPreferenceDir() + File.separator + leafname;
    }

    public static String getPreferenceDir() {
        return System.getProperty("preferencesDirectory");
    }
    
    private String getDialogGeometriesPreferenceFilename() {
        return getPreferenceFilename("dialog-geometries");
    }
    
    public static String getUserPropertiesFilename() {
        return getPreferenceFilename("evergreen.properties");
    }
    
    private void readSavedState() {
        Point initialLocation = null;
        Dimension initialSize = null;
        
        try {
            Document document = XmlUtilities.readXmlFromDisk(getPreferenceFilename("saved-state.xml"));
            
            Element root = document.getDocumentElement();
            initialLocation = new Point(Integer.parseInt(root.getAttribute("x")), Integer.parseInt(root.getAttribute("y")));
            initialSize = new Dimension(Integer.parseInt(root.getAttribute("width")), Integer.parseInt(root.getAttribute("height")));
            
            if (root.hasAttribute("showTagsPanel")) {
                initialState.showTagsPanel = Boolean.parseBoolean(root.getAttribute("showTagsPanel"));
                if (root.hasAttribute("splitPaneDividerLocation")) {
                    initialState.tagsPanelSplitPaneDividerLocation = Integer.parseInt(root.getAttribute("splitPaneDividerLocation"));
                }
                if (root.hasAttribute("tagsPanelOnLeft")) {
                    initialState.tagsPanelOnLeft = Boolean.parseBoolean(root.getAttribute("tagsPanelOnLeft"));
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
        
        if (initialLocation != null) {
            frame.setSize(initialSize);
            frame.setLocation(initialLocation);
            JFrameUtilities.constrainToScreen(frame);
        } else {
            // FIXME: maybe use the next "natural" size down from the screen size?
            frame.setSize(new Dimension(800, 730));
            // The center of the display is a good default.
            frame.setLocationRelativeTo(null);
        }
    }
    
    private synchronized void writeSavedState() {
        try {
            Document document = XmlUtilities.makeEmptyDocument();
            
            Element root = document.createElement("edit");
            document.appendChild(root);
            
            Point position = frame.getLocation();
            root.setAttribute("x", Integer.toString((int) position.getX()));
            root.setAttribute("y", Integer.toString((int) position.getY()));
            Dimension size = frame.getSize();
            root.setAttribute("width", Integer.toString((int) size.getWidth()));
            root.setAttribute("height", Integer.toString((int) size.getHeight()));
            
            root.setAttribute("showTagsPanel", Boolean.toString(tagsPanel.isVisible()));
            root.setAttribute("tagsPanelOnLeft", Boolean.toString(splitPane.getLeftComponent() == tagsPanel));
            root.setAttribute("splitPaneDividerLocation", Integer.toString(tagsPanel.isVisible() ? splitPane.getDividerLocation() : ShowHideTagsAction.oldDividerLocation));
            
            for (Workspace workspace : getWorkspaces()) {
                workspace.serializeAsXml(document, root);
            }
            
            XmlUtilities.writeXmlToDisk(getPreferenceFilename("saved-state.xml"), document);
        } catch (Exception ex) {
            Log.warn("Problem writing saved state", ex);
        }
    }
    
    private Workspace openWorkspace(WorkspaceProperties properties, List<InitialFile> initialFiles) {
        Workspace workspace = createWorkspace(properties);
        if (workspace == null) {
            return null;
        }
        
        workspace.setInitialFiles(initialFiles);
        return workspace;
    }
    
    private void initWindowListener() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doQuit();
            }
        });
    }
    
    private void initWindow() {
        frame = new JFrame("Evergreen");
        initWindowIcon();
        initWindowListener();
    }
    
    private void initTagsPanel() {
        tagsPanel = new TagsPanel();
    }
    
    private void initConfiguration() {
        Preferences.Listener preferencesListener = new Preferences.Listener() {
            public void preferencesChanged() {
                for (Workspace workspace : getWorkspaces()) {
                    workspace.preferencesChanged();
                }
                tagsPanel.repaint();
            }
        };
        
        // Properties (configuration done by editing text files).
        Parameters.initParameters();
        Parameters.addPreferencesListener(preferencesListener);
        
        // Preferences (configuration done with a GUI).
        this.preferences = new EvergreenPreferences();
        preferences.readFromDisk();
        preferences.addPreferencesListener(preferencesListener);
        
        // Indentation preferences (currently done just by editing text files).
        this.fileTypePreferences = FileType.preferencesFromFile(getPreferenceFilename("file-type-preferences"));
        
        // Set up the LSP stuff. This must happen after the Parameters init.
        LSP.init();
    }
    
    private void initStatusArea() {
        minibuffer = new Minibuffer();
        
        statusArea = new JPanel(new BorderLayout());
        statusArea.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        statusArea.add(statusLine, BorderLayout.NORTH);
        statusArea.add(minibuffer, BorderLayout.SOUTH);
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
        GuiUtilities.setQuitHandler(this::handleQuit);
    }
    
    private void initAboutBox() {
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setWebSiteAddress("https://github.com/software-jessies-org/jessies/wiki/Evergreen");
        aboutBox.addCopyright("Copyright (C) 1999-2019 software.jessies.org team.");
        aboutBox.addCopyright("All Rights Reserved.");
        aboutBox.setLicense(AboutBox.License.GPL_2_OR_LATER);
    }
    
    public void awaitInitialization() {
        try {
            startSignal.await();
        } catch (InterruptedException ex) {
        }
    }
    
    /**
     * Tests whether we've finished starting up.
     * Have we opened all the remembered workspaces and the files on them yet?
     */
    public boolean isInitialized() {
        return (startSignal.getCount() == 0);
    }
    
    private void initTabbedPaneLazyInitializer() {
        final ChangeListener firstExposureListener = new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                GuiUtilities.invokeLater(() -> {
                    getCurrentWorkspace().handlePossibleFirstExposure();
                });
            }
        };
        tabbedPane.addChangeListener(firstExposureListener);
        // Bring the listener up to speed with the current situation.
        // We couldn't add the listener in advance, because we want the tabbed
        // pane fully populated and on the display before we start listening.
        firstExposureListener.stateChanged(null);
    }
    
    private void initMenuBar() {
        updateMenuBar();
        // We need to recreate the menu bar (or at least the "Tools" menu) whenever the tools change.
        ExternalTools.addToolsListener(new ExternalTools.Listener() {
            public void toolsChanged() {
                updateMenuBar();
            }
        });
    }
    
    private void updateMenuBar() {
        EvergreenMenuBar menuBar = new EvergreenMenuBar();
        frame.setJMenuBar(menuBar);
        // Work around Sun bug 4949810 (setJMenuBar doesn't call revalidate/repaint).
        menuBar.revalidate();
    }
    
    private void init() {
        final long t0 = System.nanoTime();
        
        initConfiguration();
        
        Advisor.initResearchersOnBackgroundThread();
        
        initMacOs();
        initAboutBox();
        JFrameUtilities.readGeometriesFrom(getDialogGeometriesPreferenceFilename());
        ExternalTools.initTools();
        initWindow();
        initTagsPanel();
        tabbedPane = new EvergreenTabbedPane();
        initStatusArea();
        readSavedState();
        
        JComponent left = initialState.tagsPanelOnLeft ? tagsPanel : tabbedPane;
        JComponent right = initialState.tagsPanelOnLeft ? tabbedPane : tagsPanel;
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, left, right);
        
        InetAddress wildcardAddress = null;
        new InAppServer("EditServer", getPreferenceFilename("evergreen-server-port"), wildcardAddress, EditServer.class, new EditServer(this));
        
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusArea, BorderLayout.SOUTH);
        initMenuBar();
        
        initialState.openRememberedWorkspaces();
        
        frame.setVisible(true);
        GuiUtilities.finishGnomeStartup();
        
        final long t1 = System.nanoTime();
        Log.warn("Frame visible after " + TimeUtilities.nsToString(t1 - t0) + ".");
        
        // These things want to be done after the frame is visible...
        
        initialState.configureTagsPanel();
        
        GuiUtilities.invokeLater(() -> {
            if (tabbedPane.getTabCount() == 0) {
                // If we didn't create any workspaces, give the user some help...
                showAlert("Welcome to Evergreen!", "This looks like the first time you've used Evergreen. You'll need to create a workspace for each project you wish to work on.<p>Choose \"New Workspace...\" from the the \"Workspace\" menu.<p>You can create as many workspaces as you like, but you'll need at least one to be able to do anything.");
            } else {
                initTabbedPaneLazyInitializer();
            }
            
            Thread initializationDetectionThread = new Thread(() -> {
                final long sleepStartNs = System.nanoTime();
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
                
                final long sleepEndNs = System.nanoTime();
                Log.warn("Workers free to start after " + TimeUtilities.nsToString(sleepEndNs - t0) + " (slept for " + TimeUtilities.nsToString(sleepEndNs - sleepStartNs) + ").");
                startSignal.countDown();
            });
            initializationDetectionThread.setPriority(Thread.MIN_PRIORITY);
            initializationDetectionThread.start();
        });
    }
    
    public static void main(final String[] args) {
        GuiUtilities.invokeLater(() -> {
            GuiUtilities.initLookAndFeel();
            final Evergreen editor = Evergreen.getInstance();
            for (String arg : args) {
                editor.openFile(arg);
            }
        });
    }
}
