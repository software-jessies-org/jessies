package e.gui;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TabbedPane extends JTabbedPane {
    public TabbedPane() {
        this(TOP);
    }
    
    public TabbedPane(int tabPlacement) {
        super(tabPlacement);
        
        // We want to provide custom tool tips.
        ToolTipManager.sharedInstance().registerComponent(this);
        
        // The tabs themselves (the components with the labels) shouldn't be able to get the focus.
        // If they can, clicking on an already-selected tab takes focus away from the associated content, which is annoying.
        setFocusable(false);
        ComponentUtilities.disableFocusTraversal(this);
    }
    
    // Just overriding getToolTipTextAt is insufficient because the default implementation of getToolTipText doesn't call it.
    @Override public String getToolTipText(MouseEvent event) {
        int index = indexAtLocation(event.getX(), event.getY());
        if (index != -1) {
            return getToolTipTextAt(index);
        }
        return super.getToolTipText(event);
    }
    
    // Support for keyboard equivalents that jump straight to a given tab.
    // Call this when generating tool tip text.
    public static String tabIndexToKey(int index) {
        if ((index + 1) <= 9) {
            return Integer.toString(index + 1);
        } else if ((index + 1) == 10) {
            // This seems strange, and makes the code awkward, but it matches the order of keys (1234567890).
            return "0";
        }
        return null;
    }
    
    // Support for keyboard equivalents that jump straight to a given tab.
    // Call this when interpreting a keystroke.
    public static int keyCharToTabIndex(char ch) {
        if (ch >= '1' && ch <= '9') {
            return (ch - '1');
        } else if (ch == '0') {
            return 9;
        }
        return -1;
    }
    
    // We make use of Java 6's custom tab ear components if available, but we still want to work on Java 5 until Java 6 is widespread.
    // FIXME: Java 6 getTabComponentAt
    protected Component getTabComponentAt_safe(int index) {
        try {
            return (Component) JTabbedPane.class.getMethod("getTabComponentAt", int.class).invoke(this, index);
        } catch (Exception ex) {
            return null;
        }
    }
    
    // We make use of Java 6's custom tab ear components if available, but we still want to work on Java 5 until Java 6 is widespread.
    // FIXME: Java 6 => setTabComponentAt
    protected void setTabComponentAt_safe(int index, Component c) {
        try {
            JTabbedPane.class.getMethod("setTabComponentAt", int.class, Component.class).invoke(this, index, c);
        } catch (Exception ex) {
        }
    }
    
    /**
     * Moves a tab from originalIndex to newIndex.
     */
    public void moveTab(int originalIndex, int newIndex) {
        // Clamp movement to the ends (rather than wrapping round or going out of bounds).
        newIndex = Math.max(0, newIndex);
        newIndex = Math.min(newIndex, getTabCount() - 1);
        if (newIndex == originalIndex) {
            return;
        }
        
        // Remember everything about the original tab.
        final Color background = getBackgroundAt(originalIndex);
        final Component component = getComponentAt(originalIndex);
        final Icon disabledIcon = getDisabledIconAt(originalIndex);
        final int displayedMnemonicIndex = getDisplayedMnemonicIndexAt(originalIndex);
        final boolean enabled = isEnabledAt(originalIndex);
        final Color foreground = getForegroundAt(originalIndex);
        final Icon icon = getIconAt(originalIndex);
        final int mnemonic = getMnemonicAt(originalIndex);
        final String title = getTitleAt(originalIndex);
        final String toolTip = getToolTipTextAt(originalIndex);
        final Component tabComponent = getTabComponentAt_safe(originalIndex);
        
        // Remove the original tab and add a new one.
        remove(originalIndex);
        insertTab(title, icon, component, toolTip, newIndex);
        setSelectedIndex(newIndex);
        
        // Configure the new tab to look like the old one.
        setBackgroundAt(newIndex, background);
        setDisabledIconAt(newIndex, disabledIcon);
        setDisplayedMnemonicIndexAt(newIndex, displayedMnemonicIndex);
        setEnabledAt(newIndex, enabled);
        setForegroundAt(newIndex, foreground);
        setMnemonicAt(newIndex, mnemonic);
        setTabComponentAt_safe(newIndex, tabComponent);
    }
}
