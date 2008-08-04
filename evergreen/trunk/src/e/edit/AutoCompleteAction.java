package e.edit;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

/**
 * Offers completions for the word before the caret.
 */
public class AutoCompleteAction extends ETextAction {
    private JWindow completionsWindow;
    
    public AutoCompleteAction() {
        // All Cocoa text components use alt-escape, but IDEA and Visual Studio both use control-space (and the window manager gets alt-escape under GNOME).
        super("Complete", GuiUtilities.isMacOs() ? KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.ALT_MASK)
                                                 : KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_MASK));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea textArea = getFocusedTextArea();
        if (textArea != null) {
            offerCompletions(textArea);
        }
    }
    
    /**
     * Returns the word up to but not past the caret. The intended use is
     * working out what to offer as completions in AutoCompleteAction.
     */
    private static String getWordUpToCaret(ETextArea textArea) {
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
    
    public void offerCompletions(final ETextArea textArea) {
        String prefix = getWordUpToCaret(textArea);
        // FIXME - selection
        final int endPosition = textArea.getSelectionStart();
        final int startPosition = endPosition - prefix.length();
        
        // FIXME: we should be able to offer completions for other languages.
        JavaResearcher javaResearcher = JavaResearcher.getSharedInstance();
        List<String> completionsList = javaResearcher.listIdentifiersStartingWith(prefix);
        boolean noCompletions = completionsList.isEmpty();
        if (noCompletions) {
            completionsList.add("No completions found.");
        }
        
        String[] completions = completionsList.toArray(new String[completionsList.size()]);
        final JList completionsUi = new JList(completions);
        completionsUi.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                hideCompletionsWindow();
            }
        });
        completionsUi.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    textArea.replaceRange((String) completionsUi.getSelectedValue(), startPosition, endPosition);
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
                textArea.replaceRange((String) completionsUi.getModel().getElementAt(index), startPosition, endPosition);
                hideCompletionsWindow();
                textArea.requestFocus();
                e.consume();
            }
        });
        if (noCompletions) {
            completionsUi.setForeground(Color.GRAY);
        }
        completionsUi.setFont(textArea.getFont());
        completionsUi.setVisibleRowCount(Math.min(20, completionsUi.getModel().getSize()));
        
        Point origin = textArea.getLocationOnScreen();
        Point windowLocation = textArea.getViewCoordinates(textArea.getCoordinates(startPosition));
        windowLocation.translate(origin.x, origin.y);
        
        Window owner = SwingUtilities.getWindowAncestor(textArea);
        showCompletionsWindow(owner, windowLocation, new JScrollPane(completionsUi));
        completionsUi.requestFocus();
    }
    
    private void hideCompletionsWindow() {
        if (completionsWindow != null) {
            completionsWindow.setVisible(false);
            completionsWindow.dispose();
            completionsWindow = null;
        }
    }
    
    private void showCompletionsWindow(Window owner, Point location, Container content) {
        hideCompletionsWindow();
        
        completionsWindow = new JWindow(owner);
        completionsWindow.setContentPane(content);
        completionsWindow.setLocation(location);
        completionsWindow.pack();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int maxHeight = screenSize.height - location.y;
        Dimension windowSize = completionsWindow.getSize();
        if (windowSize.height > maxHeight) {
            windowSize.height= maxHeight;
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
        windowSize.width += 8;
        completionsWindow.setSize(windowSize);
        
        completionsWindow.setVisible(true);
    }
}
