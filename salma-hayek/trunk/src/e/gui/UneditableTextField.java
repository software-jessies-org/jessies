package e.gui;

import e.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Implements various hacks to best approximate an uneditable text field.
 * Windows used these heavily to show information that couldn't be edited while making it easy to copy the information.
 * A good idea that sadly didn't really catch on with the other platforms.
 */
public class UneditableTextField extends JTextField {
    public UneditableTextField() {
        this("");
    }
    
    public UneditableTextField(String initialValue) {
        super(initialValue);
        // Text fields with setEditable(false) don't look very different on any platform but Windows.
        // Windows is the only platform that clearly distinguishes between all the combinations of editable and enabled.
        // It's sadly unclear that those responsible for the other platforms even understand the distinction.
        // Although Cocoa makes a overly-subtle visual distinction, Apple's Java doesn't reproduce it.
        // As a work-around, we use a trick various Mac OS programs use: make the uneditable text fields look like labels.
        // You lose the visual clue that you can select and copy the text, but that's less important than obscuring the visual clue on editable fields that they're editable.
        // FIXME: at the moment, we're far too wide when there are lots of processes with access to the terminal.
        // FIXME: a PTextArea would retain the selection behavior but add wrapping, but we need to change FormPanel first because GridBagLayout won't let us do the right thing when (say) the "dimensions" and "processes" need one line-height each, but "log filename" needs two line-heights.
        // FIXME: because of the way the GTK+ LAF renders text fields, this looks awful. setOpaque has no effect. See getName for a partial work-around.
        setBorder(new EmptyBorder(getBorder().getBorderInsets(this)));
        setOpaque(false);
        setEditable(false);
    }
    
    public String getName() {
        // The GTK+ LAF insists on rendering a border unless we're a tree cell editor. So pretend to be a tree cell editor.
        // This still doesn't look quite right, but it's the best I know how to do, and it's significantly better than doing nothing.
        return GuiUtilities.isGtk() ? "Tree.cellEditor" : super.getName();
    }
}
