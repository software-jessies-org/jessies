package e.edit;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import e.gui.*;
import e.util.*;

public class TagsPanel extends JPanel {
    private ETextWindow textWindow;

    private JProgressBar progressBar = new JProgressBar();
    private JPanel progressPanel;
    
    private JPanel emptyPanel;

    private JPanel detailView;

    private ETree tree;
    private Hashtable branches;
    
    public TagsPanel() {
        setLayout(new BorderLayout());
        add(createUI(), BorderLayout.CENTER);
        startFollowingFocusChanges();
    }
    
    private void startFollowingFocusChanges() {
        new KeyboardFocusMonitor() {
            public void focusChanged(Component oldOwner, Component newOwner) {
                ETextWindow newTextWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, newOwner);
                if (newTextWindow != null) {
                    ensureTagsCorrespondTo(newTextWindow);
                }
            }
        };
    }
    
    public JComponent createUI() {
        /*
         * Embed the progress bar in a panel so that on platforms where a
         * progress bar can become arbitrarily tall (everything but Mac OS X,
         * seemingly), we don't. Because it looks stupid. Unfortunately, this
         * isn't as nice as the default Mac OS X behavior, which is to center
         * the progress bar vertically.
         */
        progressPanel = new JPanel();
        progressPanel.add(progressBar);
        
        emptyPanel = new JPanel();
        emptyPanel.setBackground(UIManager.getColor("Tree.background"));
        
        detailView = new JPanel(new BorderLayout());
        tree = new ETree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                Tag tag = (Tag) node.getUserObject();
                textWindow.goToLine(tag.lineNumber);
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);

        JScrollPane scrollPane = new JScrollPane(tree);
        detailView.add(scrollPane, BorderLayout.CENTER);

        return detailView;
    }
    
    public Workspace getWorkspace() {
        return (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, textWindow);
    }
    
    public void ensureTagsCorrespondTo(ETextWindow newTextWindow) {
        if (isShowing() == false) {
            return;
        }
        
        this.textWindow = newTextWindow;
        new TagsScanner().doScan();
    }
    
    public void ensureTagsAreHidden() {
        this.textWindow = null;
        setVisibleComponent(emptyPanel);
    }
    
    public class Tag {
        public String identifier;
        public char type;
        public String context;
        public int lineNumber;
        
        public Tag(String identifier, int lineNumber, char type, String context) {
            this.identifier = identifier;
            this.lineNumber = lineNumber;
            this.type = type;
            this.context = context;
        }
        
        public String describeVisibility() {
            if (context.indexOf("access:public") != -1) {
                return "+";
            } else if (context.indexOf("access:private") != -1) {
                return "-";
            } else if (context.indexOf("access:protected") != -1) {
                return "#";
            } else {
                return "?";
            }
        }
        
        public String describe() {
            if (textWindow != null) {
                if (textWindow.isJava()) {
                    return describeJavaTag();
                } else if (textWindow.isCPlusPlus()) {
                    return describeCPlusPlusTag();
                } else if (textWindow.isRuby()) {
                    return describeRubyTag();
                }
            }
            return identifier;
        }
        
        public String describeRubyTag() {
            switch (type) {
                case 'c': return "class " + identifier;
                case 'm': return "module " + identifier;
                default: return identifier;
            }
        }
        
        public String describeJavaTag() {
            switch (type) {
                case 'c': return "class " + identifier;
                case 'f': return identifier;
                case 'i': return "interface " + identifier;
                case 'm': return identifier + "()";
                case 'p': return "package " + identifier;
                default: return identifier;
            }
        }
            
        public String describeCPlusPlusTag() {
            switch (type) {
                case 'c': return "class " + identifier;
                case 'd': return identifier + " macro";
                case 'e': return identifier;
                case 'f': return identifier + "()";
                case 'g': return "enum " + identifier;
                case 'm': return identifier;
                case 'n': return "namespace " + identifier;
                case 'p': return identifier + " prototype";
                case 's': return "struct " + identifier;
                case 't': return "typedef " + identifier;
                case 'u': return "union " + identifier;
                case 'v': return identifier;
                case 'x': return "extern " + identifier;
                default: return identifier;
            }
        }
        
        public String toString() {
            return describe();
        }
    }
    
    private Pattern tagLinePattern = Pattern.compile("([^\t]+)\t([^\t])+\t(\\d+);\"\t(\\w)(?:\t(.*))?");
    private Pattern classPattern = Pattern.compile("(?:struct|class|enum):([^\t]+).*");
    public void processTagLine(DefaultMutableTreeNode root, String line) {
        // The format is: <identifier>\t<filename>\t<line>;"\t<tag type>[\t<context>]
        // For example:
        // A   headers/openssl/bn.h    257;"   m   struct:bn_blinding_st
        // Here the tag is called "A", is in "headers/openssl/bn.h" on line 257, and
        // is a 'm'ember of the struct called "bn_blinding_st".
        Matcher matcher = tagLinePattern.matcher(line);
        if (matcher.matches() == false) {
            return;
        }

        String identifier = matcher.group(1);
        String filename = matcher.group(2);
        int lineNumber = Integer.parseInt(matcher.group(3));
        char type = matcher.group(4).charAt(0);
        String context = matcher.group(5);
        if (context == null) {
            context = "";
        }

        Matcher classMatcher = classPattern.matcher(context);
        String containingClass = (classMatcher.matches() ? classMatcher.group(1) : "");
        //Log.warn(context + " => " + containingClass);

        Tag tag = new Tag(identifier, lineNumber, type, context);
        DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(tag);

        boolean isBranch = type == 'c' || type == 'g' /*enum*/ || type == 's' /*struct*/ || (textWindow.isRuby() && type == 'm' /* module */);
        if (isBranch) {
            branches.put(tag.toString(), leaf);
        }

        DefaultMutableTreeNode branch = (DefaultMutableTreeNode) branches.get(containingClass);
        if (branch == null) {
            branch = new DefaultMutableTreeNode(containingClass);
            branches.put(containingClass, branch);
            root.add(branch);
        }

        if (isBranch == false) {
            branch.add(leaf);
        }
    }
    
    public DefaultTreeModel makeTagsFor(File file) throws InterruptedException, IOException {
        File tagsFile = File.createTempFile("edit-tags-", ".tags");
        tagsFile.deleteOnExit();
        Process p = Runtime.getRuntime().exec(new String[] { "ctags", "--c++-types=+p", "-n", "--fields=+a", "-f", tagsFile.getAbsolutePath(), file.getAbsolutePath() });
        p.waitFor();
        DefaultTreeModel result = readTagsFile(tagsFile);
        tagsFile.delete();
        return result;
    }
    
    public DefaultTreeModel readTagsFile(File tagsFile) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel newModel = new DefaultTreeModel(root);
        branches = new Hashtable();
        branches.put("", root);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(tagsFile)));
            String line;
            boolean foundValidHeader = false;
            while ((line = reader.readLine()) != null && line.startsWith("!_TAG_")) {
                foundValidHeader = true;
                //TODO: check the tags file is sorted? of a suitable version?
            }
            if (foundValidHeader == false) {
                Edit.getCurrentWorkspace().reportError("", "The tags file didn't have a valid header.");
                return null;
            }
            if (line != null) {
                do {
                    processTagLine(root, line);
                } while ((line = reader.readLine()) != null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //FIXME: this is too dangerous, but it would be nice to give some feedback!
            Edit.getCurrentWorkspace().reportError("", "There was an error reading the tags file ("+ ex.getMessage() + ")");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                // What can we do? Nothing.
                ex = ex;
            }
        }

        return newModel;
    }

    public void setVisibleComponent(Component c) {
        removeAll();
        add(c, BorderLayout.CENTER);
        c.invalidate();
        revalidate();
        repaint();
    }

    public void showProgressBar() {
        setVisibleComponent(progressPanel);
        progressBar.setIndeterminate(true);
    }

    public void hideProgressBar() {
        setVisibleComponent(detailView);
        progressBar.setIndeterminate(false);
    }

    public class TagsScanner extends SwingWorker {
        private long startTime;

        public void doScan() {
            showProgressBar();
            start();
            startTime = System.currentTimeMillis();
        }

        public String getFilenameSuffix() {
            String suffix = ".txt";
            String filename = textWindow.getFilename();
            int lastDot = filename.lastIndexOf('.');
            if (lastDot != -1) {
                suffix = filename.substring(lastDot);
            }
            return suffix;
        }

        public Object construct() {
            DefaultTreeModel model = null;
            try {
                /*
                 * It's important to use the same suffix as the original
                 * file, because that's how ctags guesses the file's type,
                 * and hence which parser to use.
                 */
                File temporaryFile = File.createTempFile("edit-", getFilenameSuffix());
                temporaryFile.deleteOnExit();
                textWindow.writeCopyTo(temporaryFile);
                model = makeTagsFor(temporaryFile);
                temporaryFile.delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return model;
        }

        public void finished() {
            DefaultTreeModel model = (DefaultTreeModel) get();
            if (model == null) {
                Edit.getCurrentWorkspace().reportError("", "Couldn't make tags.");
            } else {
                tree.setModel(model);
                tree.expandAll();
            }

            hideProgressBar();

            long endTime = System.currentTimeMillis();
            double duration = ((double) (endTime - startTime)) / 1000.0;
            //Log.warn("Time taken reading tags: " + duration + "s");
        }
    }
}
