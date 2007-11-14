package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import e.util.*;

/**
 * A text field for search/filter interfaces. The extra functionality includes
 * a placeholder string (when the user hasn't yet typed anything), and a button
 * to clear the currently-entered text.
 * 
 * @author Elliott Hughes
 */

//
// TODO: add a menu of recent searches.
// TODO: make recent searches persistent.
//

public class SearchField extends JTextField {
    private boolean sendsNotificationForEachKeystroke = false;
    private boolean showingPlaceholderText = false;
    private StringHistory history = new StringHistory();
    
    public SearchField(String placeholderText) {
        super(15);
        addFocusListener(new PlaceholderText(placeholderText));
        initBorder();
        initKeyListener();
    }
    
    public SearchField() {
        this("Search");
    }
    
    private void initBorder() {
        if (GuiUtilities.isMacOs() && System.getProperty("os.version").startsWith("10.4") == false) {
            putClientProperty("JTextField.variant", "search");
            putClientProperty("JTextField.FindAction", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    postActionEvent();
                }
            });
            putClientProperty("JTextField.CancelAction", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancel();
                }
            });
        } else {
            new CancelBorder().attachTo(this);
            //FIXME: new MenuBorder().attachTo(this);
        }
    }
    
    private void initKeyListener() {
        addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    history.add(getText());
                } else if (sendsNotificationForEachKeystroke) {
                    maybeNotify();
                }
            }
        });
    }
    
    private void cancel() {
        setText("");
        postActionEvent();
    }
    
    private void maybeNotify() {
        if (showingPlaceholderText) {
            return;
        }
        postActionEvent();
    }
    
    public void setSendsNotificationForEachKeystroke(boolean eachKeystroke) {
        this.sendsNotificationForEachKeystroke = eachKeystroke;
    }
    
    class SearchHistoryAction extends AbstractAction {
        SearchHistoryAction(String item) {
            super(item);
        }
        
        public void actionPerformed(ActionEvent e) {
            showingPlaceholderText = false;
            setText((String) getValue(NAME));
            postActionEvent();
        }
    }
    
    class ClearHistoryAction extends AbstractAction {
        ClearHistoryAction() {
            super("Clear Entries");
        }
        
        public void actionPerformed(ActionEvent e) {
            history.clear();
        }
    }
    
    private void showMenu(int x, int y) {
        // FIXME
        /*
        EPopupMenu menu = new EPopupMenu();
        for (int i = 0; i < history.size(); ++i) {
            menu.add(new SearchHistoryAction(history.get(i)));
        }
        menu.addSeparator();
        menu.add(new ClearHistoryAction());
        menu.show(this, x, y);
        */
    }
    
    private static final Color GRAY = new Color(0.7f, 0.7f, 0.7f);
    
    class MenuBorder extends InteractiveBorder {
        MenuBorder() {
            super(15, true);
            setActivateOnPress(true);
        }
        
        public void paintBorder(Component c, Graphics oldGraphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) oldGraphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g.setColor(Color.DARK_GRAY);
            int[] xs = { x + 2, x + 10, x + 6 };
            int[] ys = { y + 6, y + 6, y + 10 };
            g.fillPolygon(xs, ys, 3);
        }
        
        public void buttonActivated(MouseEvent e) {
            selectAll();
            showMenu(e.getX(), e.getY());
        }
    }
    
    /**
     * Draws the cancel button as a gray circle with a white cross inside.
     */
    class CancelBorder extends InteractiveBorder {
        CancelBorder() {
            super(15, false);
            setActivateOnPress(false);
        }
        
        public void paintBorder(Component c, Graphics oldGraphics, int x, int y, int width, int height) {
            SearchField field = (SearchField) c;
            if (field.showingPlaceholderText || field.getText().length() == 0) {
                return;
            }
            
            Graphics2D g = (Graphics2D) oldGraphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            final int circleL = 14;
            final int circleX = x + width - circleL;
            final int circleY = y + (height - 1 - circleL)/2;
            g.setColor(isArmed() ? Color.GRAY : GRAY);
            g.fillOval(circleX, circleY, circleL, circleL);
            
            final int lineL = circleL - 8;
            final int lineX = circleX + 4;
            final int lineY = circleY + 4;
            g.setColor(Color.WHITE);
            g.drawLine(lineX, lineY, lineX + lineL, lineY + lineL);
            g.drawLine(lineX, lineY + lineL, lineX + lineL, lineY);
        }
        
        /**
         * Handles a click on the cancel button by clearing the text and
         * notifying any ActionListeners.
         */
        public void buttonActivated(MouseEvent e) {
            cancel();
        }
    }
    
    /**
     * Replaces the entered text with a gray placeholder string when the
     * search field doesn't have the focus. The entered text returns when
     * we get the focus back.
     */
    class PlaceholderText implements FocusListener {
        private String placeholderText;
        private String previousText = "";
        private Color previousColor;

        PlaceholderText(String placeholderText) {
            this.placeholderText = placeholderText;
            focusLost(null);
        }

        public void focusGained(FocusEvent e) {
            setForeground(previousColor);
            if (showingPlaceholderText) {
                setText(previousText);
                showingPlaceholderText = false;
            }
        }

        public void focusLost(FocusEvent e) {
            previousText = getText();
            previousColor = getForeground();
            if (previousText.length() == 0) {
                showingPlaceholderText = true;
                setForeground(Color.GRAY);
                setText(placeholderText);
            }
        }
    }
}
