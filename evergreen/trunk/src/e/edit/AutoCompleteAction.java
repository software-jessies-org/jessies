package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;
import e.util.*;

/**
 * Offers completions for the word before the caret.
 */
public class AutoCompleteAction extends ETextAction {
    public static final String ACTION_NAME = "Complete";
    
    private JWindow completionsWindow;
    
    public AutoCompleteAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.ALT_MASK));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        if (target != null) {
            offerCompletions(target);
        }
    }
    
    public void offerCompletions(final ETextArea target) {
        String prefix = target.getWordUpToCaret();
        final int endPosition = target.getCaretPosition();
        final int startPosition = endPosition - prefix.length();
        
        // FIXME: we should be able to offer completions for other languages.
        JavaResearcher javaResearcher = JavaResearcher.getSharedInstance();
        List completionsList = javaResearcher.listIdentifiersStartingWith(prefix);
        boolean noCompletions = completionsList.isEmpty();
        if (noCompletions) {
            completionsList.add("No completions found.");
        }
        
        String[] completions = (String[]) completionsList.toArray(new String[completionsList.size()]);
        final JList completionsUi = new JList(completions);
        completionsUi.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                hideCompletionsWindow();
            }
        });
        completionsUi.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    target.replaceRange((String) completionsUi.getSelectedValue(), startPosition, endPosition);
                    hideCompletionsWindow();
                    target.requestFocus();
                    e.consume();
                }
            }
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideCompletionsWindow();
                    target.requestFocus();
                    e.consume();
                }
            }
        });
        if (noCompletions) {
            completionsUi.setForeground(Color.GRAY);
        }
        completionsUi.setFont(target.getFont());
        completionsUi.setVisibleRowCount(Math.min(20, completionsUi.getModel().getSize()));
        
        try {
            Rectangle startRectangle = target.modelToView(startPosition);
            Point origin = target.getLocationOnScreen();
            Point windowLocation = new Point(startRectangle.x, startRectangle.y + startRectangle.height);
            windowLocation.translate(origin.x, origin.y);
            
            Frame owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, target);
            showCompletionsWindow(owner, windowLocation, new JScrollPane(completionsUi));
            completionsUi.requestFocus();
        } catch (BadLocationException ex) {
            Log.warn("Couldn't show the completions.", ex);
        }
    }
    
    private void hideCompletionsWindow() {
        if (completionsWindow != null) {
            completionsWindow.setVisible(false);
            completionsWindow.dispose();
            completionsWindow = null;
        }
    }
    
    private void showCompletionsWindow(Frame owner, Point location, Container content) {
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
