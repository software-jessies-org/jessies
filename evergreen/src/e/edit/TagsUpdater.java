package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.*;
import org.jdesktop.swingworker.SwingWorker;

public class TagsUpdater {
    private static final ExecutorService executorService = ThreadUtilities.newSingleThreadExecutor("Tags Updater");
    private static final Stopwatch tagsUpdaterStopwatch = Stopwatch.get("TagsUpdater");
    private static final Comparator<String> TAG_COMPARATOR = new SmartStringComparator();

    private final ETextWindow textWindow;

    private static int latestSerialNumber = 0;

    private ETree tree;
    private JScrollPane uiPanel;

    private boolean followCaretChanges;

    private byte[] tagsDigest = new byte[0];

    public TagsUpdater(ETextWindow textWindow) {
        this.textWindow = textWindow;
        installListeners();
    }

    private void installListeners() {
        final PTextArea text = getTextArea();
        // Rebuild tags when the document line count changes.
        text.getTextBuffer().addTextListener(new PTextListener() {
            private int lastLineCount;

            public void textCompletelyReplaced(PTextEvent e) {
                update();
            }

            public void textRemoved(PTextEvent e) {
                update();
            }

            public void textInserted(PTextEvent e) {
                update();
            }

            public void update() {
                // FIXME: shouldn't this be testing whether e.getCharacters()
                // contains a '\n' instead of counting lines?
                int newLineCount = text.getLineCount();
                if (lastLineCount == newLineCount) {
                    return;
                }
                lastLineCount = newLineCount;
                updateTags();
            }
        });
        // Select the corresponding tag in the tree when the focus is gained or the caret moves.
        text.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                followCaretChanges = true;
                createUi();
                updateTags();
                showTags();
                selectTagAtCaret(text);
            }
        });
        text.addCaretListener(new PCaretListener() {
            public void caretMoved(PTextArea textArea, int oldOffset, int newOffset) {
                if (followCaretChanges) {
                    selectTagAtCaret(text);
                }
            }
        });
    }

    private void selectTagAtCaret(PTextArea text) {
        // FIXME - selection
        int lineNumber = text.getLineOfOffset(text.getSelectionStart());
        selectTreeNode(getTagForLine(lineNumber));
    }

    private synchronized void createUi() {
        if (tree != null) {
            return;
        }

        tree = new ETree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        ToolTipManager.sharedInstance().registerComponent(tree);
        // We can't trust JTree's row height calculation. Make it use
        // the preferred height of the cell renderer component instead.
        tree.setRowHeight(-1);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // Make both space and enter in the tree jump to the currently selected tag, and transfer focus back to the file.
        Action selectTagAction = new AbstractAction("select-tag") {
            public void actionPerformed(ActionEvent e) {
                getTextArea().goToLine(((TagReader.Tag) ((DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject()).lineNumber);
                getTextArea().requestFocusInWindow();
            }
        };
        ComponentUtilities.initKeyBinding(tree, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectTagAction);
        ComponentUtilities.initKeyBinding(tree, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), selectTagAction);

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node == null) {
                    return;
                }
                followCaretChanges = false;
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                getTextArea().goToLine(tag.lineNumber);
            }
        });
        tree.setFont(UIManager.getFont("TableHeader.font"));
        tree.setCellRenderer(new TagsPanel.TagsTreeRenderer(getTextArea()));

        uiPanel = new JScrollPane(tree);
    }

    public ETextWindow getTextWindow() {
        return textWindow;
    }

    public PTextArea getTextArea() {
        return getTextWindow().getTextArea();
    }

    public void updateTags() {
        if (tree == null) {
            // No point updating tags if we've never been shown yet.
            return;
        }
        int serialNumber = ++latestSerialNumber;
        executorService.execute(new TreeModelBuilder(serialNumber));
    }

    private void setTreeModel(TreeModel treeModel) {
        tree.setModel(treeModel);
        tree.expandAll();
        showTags();
        selectTagAtCaret(getTextArea());
    }

    public void showTags() {
        Evergreen.getInstance().getTagsPanel().setTagsTree(uiPanel);
    }

    /**
     * Returns the tree node containing the tag for the line. If there's no
     * tag for the line, finds the nearest tag before the caret. If that
     * fails, returns null.
     */
    private TreeNode getTagForLine(int lineNumber) {
        lineNumber++; // JTextComponent numbers lines from 0, ectags from 1.

        TagReader.Tag nearestTag = null;
        TreeNode nearestNode = null;

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (Enumeration<?> e = root.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof TagReader.Tag) {
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                if (tag.lineNumber == lineNumber) {
                    return node;
                } else if (tag.lineNumber < lineNumber) {
                    if (nearestTag == null || tag.lineNumber > nearestTag.lineNumber) {
                        nearestTag = tag;
                        nearestNode = node;
                    }
                }
            }
        }
        return nearestNode;
    }

    private void selectTreeNode(TreeNode node) {
        if (node == null) {
            tree.clearSelection();
        } else {
            DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
            TreePath path = new TreePath(treeModel.getPathToRoot(node));
            tree.setSelectionPath(path);
            if (followCaretChanges) {
                tree.scrollPathToVisible(path);
            }
        }
    }

    public class TreeModelBuilder extends SwingWorker<TreeModel, TagReader.Tag> implements TagReader.TagListener {
        private int serialNumber;
        private MessageDigest md5;
        private boolean successful = true;
        private Stopwatch.Timer stopwatchTimer;
        private Timer progressTimer;

        private DefaultMutableTreeNode root;
        private DefaultTreeModel treeModel;
        private Map<String, DefaultMutableTreeNode> branches = new HashMap<String, DefaultMutableTreeNode>();

        public TreeModelBuilder(int serialNumber) {
            this.serialNumber = serialNumber;
            try {
                this.md5 = MessageDigest.getInstance("MD5");
            } catch (Exception ex) {
                Log.warn("Your JDK doesn't support MD5!", ex);
            }
        }

        @Override
        protected TreeModel doInBackground() {
            // Don't waste time during start-up, and don't waste time if we're already out of date before we start.
            Evergreen.getInstance().awaitInitialization();
            if (serialNumber != latestSerialNumber) {
                return null;
            }

            stopwatchTimer = tagsUpdaterStopwatch.start();
            root = new BranchNode("root");
            treeModel = new DefaultTreeModel(root);
            branches.clear();
            branches.put("", root);
            progressTimer = new Timer(500, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Evergreen.getInstance().getTagsPanel().showProgressBar();
                }
            });
            progressTimer.setRepeats(false);
            progressTimer.start();
            scanTags();
            return treeModel;
        }

        public void tagFound(TagReader.Tag tag) {
            DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(tag);

            if (tag.type.isContainer()) {
                leaf = new BranchNode(tag);
                branches.put(tag.getClassQualifiedName(), leaf);
            }

            DefaultMutableTreeNode branch = branches.get(tag.containingClass);
            if (branch == null) {
                branch = new BranchNode(tag.containingClass);
                branches.put(tag.containingClass, branch);
                root.add(branch);
            }

            branch.add(leaf);

            final byte SEPARATOR = 0;
            md5.update(tag.identifier.getBytes());
            md5.update(SEPARATOR);
            md5.update(tag.type.getName().getBytes());
            md5.update(SEPARATOR);
            md5.update(tag.context.getBytes());
            md5.update(SEPARATOR);
            md5.update(tag.containingClass.getBytes());
            md5.update(SEPARATOR);
            md5.update(Integer.toString(tag.lineNumber).getBytes());
            md5.update(SEPARATOR);
            md5.update(booleanToByte(tag.isStatic));
            md5.update(booleanToByte(tag.isAbstract));
            md5.update(booleanToByte(tag.isPrototype));
        }

        private byte booleanToByte(boolean b) {
            return (byte) (b ? 1 : 0);
        }

        public void taggingFailed(Exception ex) {
            successful = false;
            Evergreen.getInstance().getTagsPanel().showError("<b>Is Exuberant Ctags installed and on your path?</b><p>There was an error reading its output: " + ex.getMessage());
            ex.printStackTrace();
        }

        public void scanTags() {
            File temporaryFile = null;
            try {
                final ETextWindow textWindow = getTextWindow();
                final FileType fileType = textWindow.getFileType();

                // FIXME: generalize this and implement more taggers in Java?
                if (fileType == FileType.XML) {
                    new HtmlTagger(getTextArea(), this).scan();
                    return;
                }

                if (TagReader.ctagsLanguageForFileType(fileType) == null) {
                    Evergreen.getInstance().getTagsPanel().showError("(Ctags doesn't support " + fileType.getName() + ".)");
                    successful = false;
                    return;
                }

                if (textWindow.isDirty() || hasGTests()) {
                    temporaryFile = File.createTempFile("e.edit.TagsUpdater-", "");
                    temporaryFile.deleteOnExit();
                    if (hasGTests()) {
                        // Rewrite gtests in a way that ctags can understand, corresponding to
                        // the actual symbols the macros expand to.
                        String content = getTextArea().getTextBuffer().toString();
                        content = content.replaceAll("TEST_F\\((.*),\\s*(.*)\\)", "void $1::$2()");
                        content = content.replaceAll("TEST\\((.*),\\s*(.*)\\)", "void $1_$2_Test()");
                        StringUtilities.writeFile(temporaryFile, content);
                    } else {
                    getTextArea().getTextBuffer().writeToFile(temporaryFile);
                }
                }
                String charsetName = (String) getTextArea().getTextBuffer().getProperty(PTextBuffer.CHARSET_PROPERTY);
                final File inputFile = (temporaryFile != null) ? temporaryFile : FileUtilities.fileFromString(textWindow.getFilename());
                TagReader tagReader = new TagReader(inputFile, fileType, charsetName, this);
            } catch (Exception ex) {
                Evergreen.getInstance().getTagsPanel().showError("Couldn't make tags: " + ex.getMessage());
                Log.warn("Couldn't make tags", ex);
                successful = false;
            } finally {
                if (temporaryFile != null) {
                    temporaryFile.delete();
                }
            }
        }

        @Override
        protected void done() {
            if (progressTimer != null) {
                progressTimer.stop();
            }
            if (successful) {
                showTags();
                byte[] newDigest = md5.digest();
                boolean tagsHaveChanged = !MessageDigest.isEqual(newDigest, tagsDigest);
                if (tagsHaveChanged && serialNumber == latestSerialNumber) {
                    setTreeModel(treeModel);
                    tagsDigest = newDigest;
                }
            }
            if (stopwatchTimer != null) {
                stopwatchTimer.stop();
            }
        }

        private boolean hasGTests() {
            if (getTextWindow().getFileType() != FileType.C_PLUS_PLUS) {
                return false;
            }
            // Match TEST(a, b) or TEST_F(a, b), but only at the start of a line.
            return Pattern.matches("(?ms).*^TEST(_F)?\\(.*", getTextArea().getTextBuffer());
        }
    }

    private static class BranchNode extends DefaultMutableTreeNode {
        private ArrayList<String> kidSortKeys = new ArrayList<String>();

        public BranchNode(Object userObject) {
            super(userObject);
        }

        @Override public void add(MutableTreeNode node) {
            Object o = ((DefaultMutableTreeNode) node).getUserObject();
            if (o instanceof TagReader.Tag) {
                insert(node, getInsertIndex((TagReader.Tag) o));
            } else {
                super.add(node);
            }
        }

        private int getInsertIndex(TagReader.Tag tag) {
            String insertString = tag.getSortIdentifier();
            // Behave more like Windows and less like GTK+, sorting Main between init_handlers and print,
            // rather than on its own at the top, while consistently sorting transform_A before transform_a.
            // upperBound will keep otherwise identical tags, like those for overloaded constructors, in their original order.
            int index = CollectionUtilities.upperBound(kidSortKeys, insertString, TAG_COMPARATOR);
            // FIXME: This is O(n*n) but works OK with the worst real-world use case yet measured (stone1/.../soapC.cpp, with ~300 kloc).
            kidSortKeys.add(index, insertString);
            return index;
        }
    }
}
