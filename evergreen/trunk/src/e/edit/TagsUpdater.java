package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import e.gui.*;

public class TagsUpdater {
    private ETree tree;
    private JPanel uiPanel;
    private ETextWindow textWindow;
    private boolean followCaretChanges;
    
    public TagsUpdater(ETextWindow textWindow) {
        this.textWindow = textWindow;
        createUI();
        installListeners();
    }
    
    private void installListeners() {
        final ETextArea text = textWindow.getText();
        // Rebuild tags when the document line count changes.
        text.getDocument().addDocumentListener(new DocumentListener() {
            private int lastLineCount;
            
            public void changedUpdate(DocumentEvent e) {
                // We don't care about style changes.
            }
            public void insertUpdate(DocumentEvent e) {
                update();
            }
            public void removeUpdate(DocumentEvent e) {
                update();
            }
            
            public void update() {
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
            public void focusGained(FocusEvent e) {
                followCaretChanges = true;
                showTags();
                selectTagAtCaret(text);
            }
        });
        text.getCaret().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (followCaretChanges) {
                    selectTagAtCaret(text);
                }
            }
        });
    }
    
    private void selectTagAtCaret(ETextArea text) {
        try {
            int lineNumber = text.getLineOfOffset(text.getCaretPosition());
            selectTreeNode(getTagForLine(lineNumber));
        } catch (BadLocationException ex) {
            ex = ex;
        }
    }
    
    private void createUI() {
        tree = new ETree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                if (node == null) {
                    return;
                }
                followCaretChanges = false;
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                textWindow.goToLine(tag.lineNumber);
            }
        });
        tree.setCellRenderer(new TagsPanel.TagsTreeRenderer());
        
        final SearchField searchField = new SearchField();
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tree.clearSelection();
                tree.selectNodesMatching(searchField.getText());
            }
        });
        
        uiPanel = new JPanel(new BorderLayout());
        uiPanel.add(searchField, BorderLayout.NORTH);
        uiPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
    }
    
    public ETextWindow getTextWindow() {
        return textWindow;
    }
    
    public void updateTags() {
        new TagsPanel.TagsScanner(this).doScan();
    }
    
    public void setTreeModel(TreeModel treeModel) {
        tree.setModel(treeModel);
        tree.expandAll();
        showTags();
    }
    
    public void showTags() {
        Edit.getTagsPanel().setTagsTree(uiPanel);
    }
    
    /**
     * Returns the tree node containing the tag for the line. If there's no tag for the line, finds the
     * closest tag with an enclosing scope and returns that. If that fails, returns null.
     */
    public TreeNode getTagForLine(int lineNumber) throws BadLocationException {
        lineNumber++;    // JTextComponent numbers lines from 0, ectags from 1.
        
        TagReader.Tag nearestTag = null;
        TreeNode nearestNode = null;
        
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (Enumeration e = root.breadthFirstEnumeration(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof TagReader.Tag) {
                TagReader.Tag tag = (TagReader.Tag) node.getUserObject();
                if (tag.lineNumber == lineNumber) {
                    return node;
                } else if (tag.isContainerType() && tag.lineNumber < lineNumber) {
                    if (nearestTag == null || tag.lineNumber > nearestTag.lineNumber) {
                        nearestTag = tag;
                        nearestNode = node;
                    }
                }
            }
        }
        return nearestNode;
    }
    
    public void selectTreeNode(TreeNode node) {
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
}
