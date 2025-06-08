package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Offers completions for the word before the caret.
 */
public class AutoCompleteAction extends ETextAction {
    private JPanel completionsWindow;
    
    public AutoCompleteAction() {
        // All Cocoa text components use alt-escape, but IDEA and Visual Studio both use control-space (and the window manager gets alt-escape under GNOME).
        super("Complete", GuiUtilities.isMacOs() ? KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.ALT_DOWN_MASK)
                                                 : KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK));
    }
    
    /**
     * Returns the word up to but not past the caret. The intended use is
     * working out what to offer as completions in AutoCompleteAction.
     */
    private static String getWordUpToCaret(PTextArea textArea) {
        CharSequence chars = textArea.getTextBuffer();
        // FIXME - selection
        int end = textArea.getSelectionStart();
        int start = end;
        while (start > 0) {
            char ch = chars.charAt(start - 1);
            if (ch != '_' && Character.isLetterOrDigit(ch) == false) {
                break;
            }
            --start;
        }
        return chars.subSequence(start, end).toString();
    }
    
    public void actionPerformed(ActionEvent e) {
        final PTextArea textArea = getFocusedTextArea();
        if (textArea == null) {
            return;
        }
        List<LSP.Completion> completionsList;
        
        final LSP.FileClient lspClient = getFocusedTextWindow().getLspClient();
        String prefix = "";
        if (lspClient != null) {
            completionsList = lspClient.suggestCompletionsAt(textArea.getCoordinates(textArea.getSelectionStart()));
        } else {
            prefix = getWordUpToCaret(textArea);
            // FIXME - selection
            final int end = textArea.getSelectionStart();
            final int start = end - prefix.length();
            
            // We should probably ditch this eventually, and just use LSPs. It would be nice
            // to find a Java LSP that copes with Evergreen's rather particular build system.
            // For now, just adapt the old JavaResearcher interface to generate LSP.Completion
            // structs from it. 
            JavaResearcher javaResearcher = JavaResearcher.getSharedInstance();
            completionsList = new ArrayList<LSP.Completion>();
            for (String text : javaResearcher.listIdentifiersStartingWith(prefix)) {
                completionsList.add(new LSP.Completion(new LSP.CompletionEdit(start, end, text), null, null));
            }
        }
        
        boolean noCompletions = completionsList.isEmpty();
        if (noCompletions) {
            int pos = textArea.getSelectionStart();
            completionsList.add(new LSP.Completion(new LSP.CompletionEdit(pos, pos, "No completions found."), null, null));
        }
        final LSP.Completion[] completions = completionsList.toArray(new LSP.Completion[completionsList.size()]);
        final JList<LSP.Completion> completionsUi = new JList<>(completions);
        final JTextArea docViewer = new JTextArea();
        docViewer.setBackground(new Color(230, 230, 230));
        docViewer.setEditable(false);
        docViewer.setLineWrap(true);
        
        completionsUi.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                hideCompletionsWindow();
            }
        });
        completionsUi.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    applyCompletion(textArea, completionsUi.getSelectedValue());
                    hideCompletionsWindow();
                    textArea.requestFocus();
                    e.consume();
                }
            }
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideCompletionsWindow();
                    textArea.requestFocus();
                    e.consume();
                }
            }
        });
        completionsUi.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = completionsUi.locationToIndex(e.getPoint());
                applyCompletion(textArea, completionsUi.getModel().getElementAt(index));
                hideCompletionsWindow();
                textArea.requestFocus();
                e.consume();
            }
        });
        completionsUi.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                LSP.Completion sel = completionsUi.getSelectedValue();
                if (sel == null) {
                    docViewer.setText("no selection");
                    return;
                }
                String doc = sel.documentation;
                if (doc == null || doc.equals("")) {
                    docViewer.setText("no documentation for this entry");
                } else {
                    docViewer.setText(doc);
                }
            }
        });
        if (completions.length > 0) {
            completionsUi.setSelectedIndex(0);
        }
        if (noCompletions) {
            completionsUi.setForeground(Color.GRAY);
        }
        completionsUi.setFont(textArea.getFont());
        completionsUi.setVisibleRowCount(Math.min(20, completionsUi.getModel().getSize()));
        
        Point windowLocation = textArea.getViewCoordinates(textArea.getCoordinates(textArea.getSelectionStart()));
        Dimension desiredSize = pickReasonableSize(completionsUi, completions);
        
        // TODO(phil): Improve how the size and y location is generated.
        showCompletionsWindow(textArea, windowLocation, desiredSize, new JScrollPane(completionsUi), docViewer);
        Log.warn("showCompletionsWindow at " + windowLocation + " with size " + desiredSize);
        completionsUi.requestFocus();
    }
    
    // These size adjustments seem to be what we need to avoid scrollbars appearing in the autocomplete popup.
    // Of course, they'll depend on the look and feel, so they're likely wrong on Mac, for example. It would
    // be great to do this properly, but I don't see how to get Swing to properly compute the size of the
    // component without putting it in a JFrame and calling pack(). As we want to sneakily insert the component
    // inside a PTextArea (to avoid problems with tiling window managers), we're not able to do this.
    // I don't like this, it's ugly as hell, but sometimes you just have to do ugly stuff to make things work.
    // If anyone knows of a better way, please let me know.
    private static final int EXTRA_LIST_WIDTH = 50;
    private static final int EXTRA_LIST_ITEM_HEIGHT = 3;
    private static final int MAX_VISIBLE_COMPLETIONS = 20;
    
    private Dimension pickReasonableSize(JList<LSP.Completion> completionsUi, LSP.Completion[] completions) {
        FontMetrics metrics = completionsUi.getFontMetrics(completionsUi.getFont());
        int width = 100;
        for (LSP.Completion comp : completions) {
            width = Math.max(width, metrics.stringWidth(comp.toString()) + EXTRA_LIST_WIDTH);
        }
        completionsUi.revalidate();
        int height = Math.min(completions.length, MAX_VISIBLE_COMPLETIONS) * (metrics.getMaxAscent() + metrics.getMaxDescent() + EXTRA_LIST_ITEM_HEIGHT);
        return new Dimension(width, height);
    }
    
    private void applyCompletion(PTextArea textArea, LSP.Completion completion) {
        if (completion == null) {
            return;
        }
        ArrayList<LSP.CompletionEdit> allEdits = new ArrayList<>();
        allEdits.add(completion.edit);
        allEdits.addAll(completion.otherEdits);
        // Sort the edits into reverse order (latest in file first). This means we can
        // just go through the list and apply them, and the ones we apply first won't
        // affect the positions of the ones we apply later.
        Collections.sort(allEdits, new Comparator<LSP.CompletionEdit>() {
            public int compare(LSP.CompletionEdit left, LSP.CompletionEdit right) {
                return right.start - left.start;
            }
        });
        if (allEdits.size() > 1) {
            textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
        }
        for (LSP.CompletionEdit edit : allEdits) {
            Log.warn("Applying completion " + edit.text + " between " + edit.start + " and " + edit.end);
            textArea.replaceRange(edit.text, edit.start, edit.end);
        }
        if (allEdits.size() > 1) {
            textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
        }
    }
    
    private void hideCompletionsWindow() {
        if (completionsWindow != null) {
            PTextArea textArea = (PTextArea) SwingUtilities.getAncestorOfClass(PTextArea.class, completionsWindow);
            if (textArea != null) {
                textArea.remove(completionsWindow);
            }
            completionsWindow = null;
        }
    }
    
    private void showCompletionsWindow(PTextArea textArea, Point location, Dimension desiredSize, Container content, Container docViewer) {
        hideCompletionsWindow();
        
        completionsWindow = new JPanel(new BorderLayout());
        completionsWindow.add(content, BorderLayout.CENTER);
        completionsWindow.add(docViewer, BorderLayout.EAST);
        completionsWindow.setLocation(location);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int docViewerWidth = textArea.getWidth() / 3;
        docViewer.setPreferredSize(new Dimension(docViewerWidth, 50));
        docViewer.setSize(new Dimension(docViewerWidth, 50));
        final int maxHeight = screenSize.height;
        Dimension windowSize = desiredSize;
        if (windowSize.height > maxHeight) {
            windowSize.height = maxHeight;
            if (content instanceof JScrollPane) {
                // By making the window shorter, we will have introduced a
                // vertical scroll bar, so we have to increase the width to
                // prevent a horizontal scroll bar appearing too.
                JScrollPane scrollPane = (JScrollPane) content;
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                windowSize.width += scrollBar.getPreferredSize().width;
            }
        }
        // The trailing margin on a JList is the same as the leading margin,
        // but lists look better with a slightly wider trailing margin.
        windowSize.width += 8 + docViewerWidth;
        completionsWindow.setSize(windowSize);
        textArea.add(completionsWindow);
        completionsWindow.validate();
    }
}
