package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import e.util.*;

public class EditMenuBar extends JMenuBar implements MenuListener {
    public EditMenuBar() {
    }
    
    public void populate() {
        add(makeFileMenu());
        add(makeEditMenu());
        add(makeFindMenu());
        add(makeViewMenu());
        add(makeWorkspaceMenu());
        add(makeToolsMenu());
    }
    
    public class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit");
        }
        public void actionPerformed(ActionEvent e) {
            Edit.getInstance().handleQuit(null);
        }
    }
    
    public JMenu makeFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(makeAcceleratedItem(new NewFileAction(), 'N'));
        menu.add(makeAcceleratedItem(new OpenAction(), 'O'));
        menu.add(makeAcceleratedItemEx(new OpenQuicklyAction(), 'D', true));
        // FIXME: Should be "Open Recent >" here.

        menu.add(new JSeparator());
        menu.add(makeAcceleratedItem(new CloseWindowAction(), 'W'));
        menu.add(makeAcceleratedItem(new SaveAction(), 'S'));
        menu.add(makeAcceleratedItemEx(new SaveAsAction(), 'S', true));
        menu.add(new SaveAllAction());
        menu.add(new RevertToSavedAction()); // FIXME: Should be on C-U, but I'm not convinced we should make this so easy.

        if (System.getProperty("os.name").indexOf("Mac") == -1) {
            menu.add(new JSeparator());
            menu.add(new JMenuItem(new ExitAction()));
        }
        return menu;
    }

    public JMenuItem makeAcceleratedItem(Action action, char character) {
        return makeAcceleratedItemEx(action, character, false);
    }

    public JMenuItem makeAcceleratedItemEx(Action action, char character, boolean shifted) {
        JMenuItem item = new JMenuItem(action);
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        if (shifted) modifiers |= InputEvent.SHIFT_MASK;
        String keycodeName = "VK_" + character;
        int keycode;
        try {
            keycode = KeyEvent.class.getField(keycodeName).getInt(KeyEvent.class);
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keycode, modifiers);
            item.setAccelerator(keyStroke);
        } catch (Exception e) {
            Log.warn("Couldn't find virtual keycode for '" + character);
        }
        return item;
    }
    
    public JMenuItem makeCompletionItem(Action action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.ALT_MASK);
        JMenuItem item = new JMenuItem(action);
        item.setAccelerator(keyStroke);
        return item;
    }
    
    public JMenu makeEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.add(makeAcceleratedItem(new UndoAction(), 'Z'));
        menu.add(makeAcceleratedItemEx(new RedoAction(), 'Z', true));

        menu.add(new JSeparator());
        menu.add(makeAcceleratedItem(new CutAction(), 'X'));
        menu.add(makeAcceleratedItem(new CopyAction(), 'C'));
        menu.add(makeAcceleratedItem(new PasteAction(), 'V'));
        menu.add(makeCompletionItem(new AutoCompleteAction()));

        menu.add(new JSeparator());
        menu.add(makeAcceleratedItem(new CorrectIndentationAction(), 'I'));

        menu.add(new JSeparator());
        menu.add(new ShowMisspellingsAction());

        return menu;
    }

    public JMenu makeFindMenu() {
        JMenu menu = new JMenu("Find");
        menu.add(makeAcceleratedItem(FindAction.INSTANCE, 'F'));
        menu.add(makeAcceleratedItem(new FindNextAction(), 'G'));
        menu.add(makeAcceleratedItem(new FindPreviousAction(), 'D'));
        //   Use Selection for Find C-E
        menu.add(makeAcceleratedItem(new ScrollToSelectionAction(), 'J'));

        menu.add(new JSeparator());
        menu.add(makeAcceleratedItem(new FindAndReplaceAction(), 'R'));

        menu.add(new JSeparator());
        menu.add(makeAcceleratedItem(new GotoAction(), 'L'));
        
        menu.add(new JSeparator());
        menu.add(makeAcceleratedItemEx(new FindFilesAction(), 'G', true));
        
        return menu;
    }
    
    public JMenu makeViewMenu() {
        JMenu menu = new JMenu("View");
        menu.add(new ChangeFontAction(true));
        menu.add(new ChangeFontAction(false));

        menu.add(new JSeparator());
        menu.add(new FilePropertiesAction());
        return menu;
    }
    
    public JMenu makeToolsMenu() {
        final JMenu menu = new JMenu("Tools");
        menu.add(makeAcceleratedItem(new BuildAction(), 'B'));
        menu.add(new SetBuildTargetAction());
        menu.add(new OpenMakefileAction());
        
        menu.add(new JSeparator());
        menu.add(makeAcceleratedItem(new KillErrorsAction(), 'K'));

        ExternalToolsParser toolsParser = new ExternalToolsParser() {
            public void addItem(Action action) {
                menu.add(action);
            }

            public void addItem(Action action, char keyboardEquivalent) {
                menu.add(makeAcceleratedItemEx(action, keyboardEquivalent, true));
            }

            public void addSeparator() {
                menu.add(new JSeparator());
            }
        };
        toolsParser.parse();
        return menu;
    }

    public JMenu makeWorkspaceMenu() {
        JMenu menu = new JMenu("Workspace");
        menu.add(new AddWorkspaceAction());
        menu.add(new RemoveWorkspaceAction());
        return menu;
    }
    
    public JMenu add(JMenu menu) {
        traverseMenu(menu);
        menu.addMenuListener(this);
        return super.add(menu);
    }
    
    public void menuCanceled(MenuEvent e) {
    }
    
    /**
     * This nastiness is all about getting a pull model for enabling/disabling
     * menu items.
     * When the menu comes up, we'll go through the Actions, invoking
     * isEnabled (because JMenu's too damn lazy to do it itself) to use
     * its result as the parameter to setEnabled on the JMenuItem.
     */
    public void menuSelected(MenuEvent e) {
        JMenu menu = (JMenu) e.getSource();
        traverseMenu(menu);
    }
    
    public void traverseMenu(JMenu menu) {
        MenuElement[] elements = menu.getSubElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] instanceof JMenu) {
                traverseMenu((JMenu) elements[i]);
            } else if (elements[i] instanceof JPopupMenu) {
                traverseMenu((JPopupMenu) elements[i]);
            } else {
                JMenuItem menuItem = (JMenuItem) elements[i];
                Action action = menuItem.getAction();
                if (action == null) {
                    Log.warn("Actionless menu item found: " + menuItem);
                }
                Log.warn(menuItem.toString() + ": " + action.isEnabled());
                menuItem.setEnabled(action.isEnabled());
            }
        }
    }
    
    public void traverseMenu(JPopupMenu menu) {
        MenuElement[] elements = menu.getSubElements();
        for (int i = 0; i < elements.length; i++) {
            JMenuItem menuItem = (JMenuItem) elements[i];
            Action action = menuItem.getAction();
            if (action == null) {
                Log.warn("Actionless popup menu item found: " + menuItem);
            }
            menuItem.setEnabled(action.isEnabled());
        }
    }
    
    public void menuDeselected(MenuEvent e) {
    }
}
