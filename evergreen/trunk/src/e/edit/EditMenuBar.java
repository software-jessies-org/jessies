package e.edit;

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
        menu.add(new NewFileAction());
        menu.add(new OpenQuicklyAction());
        menu.add(new OpenAction());
        // FIXME: Should be "Open Recent >" here.

        menu.add(new JSeparator());
        menu.add(new CloseWindowAction());
        menu.add(new SaveAction());
        menu.add(new SaveAsAction());
        menu.add(new SaveAllAction());
        menu.add(new RevertToSavedAction());

        if (System.getProperty("os.name").indexOf("Mac") == -1) {
            menu.add(new JSeparator());
            menu.add(new JMenuItem(new ExitAction()));
        }
        return menu;
    }

    private static JMenuItem makeAcceleratedItem(Action action, String key) {
        return makeAcceleratedItemEx(action, key, false);
    }

    private static JMenuItem makeAcceleratedItemEx(Action action, String key, boolean shifted) {
        JMenuItem item = new JMenuItem(action);
        KeyStroke keyStroke = GuiUtilities.makeKeyStroke(key, shifted);
        if (keyStroke != null) {
            item.setAccelerator(keyStroke);
        }
        return item;
    }
    
    public JMenu makeEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.add(new UndoAction());
        menu.add(new RedoAction());

        menu.add(new JSeparator());
        menu.add(new CutAction());
        menu.add(new CopyAction());
        menu.add(new PasteAction());
        menu.add(new AutoCompleteAction());

        menu.add(new JSeparator());
        menu.add(new CorrectIndentationAction());

        menu.add(new JSeparator());
        menu.add(new ShowMisspellingsAction());

        return menu;
    }

    public JMenu makeFindMenu() {
        JMenu menu = new JMenu("Find");
        menu.add(FindAction.INSTANCE);
        menu.add(new FindNextAction());
        menu.add(new FindPreviousAction());
        menu.add(new ScrollToSelectionAction());

        menu.add(new JSeparator());
        menu.add(new FindAndReplaceAction());

        menu.add(new JSeparator());
        menu.add(new GotoAction());
        
        menu.add(new JSeparator());
        menu.add(new FindFilesContainingSelectionAction());
        
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
        menu.add(new BuildAction());
        menu.add(new SetBuildTargetAction());
        menu.add(new OpenMakefileAction());
        
        menu.add(new JSeparator());
        menu.add(new KillErrorsAction());
        
        menu.add(new JSeparator());
        menu.add(new CheckInChangesAction());
        menu.add(new ShowHistoryAction());

        ExternalToolsParser toolsParser = new ExternalToolsParser() {
            public void addItem(ExternalToolAction action) {
                menu.add(action);
            }

            public void addItem(ExternalToolAction action, char keyboardEquivalent) {
                menu.add(makeAcceleratedItemEx(action, "" + keyboardEquivalent, true));
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
                } else {
                    Log.warn(menuItem.toString() + ": " + action.isEnabled());
                    menuItem.setEnabled(action.isEnabled());
                }
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
            } else {
                menuItem.setEnabled(action.isEnabled());
            }
        }
    }
    
    public void menuDeselected(MenuEvent e) {
    }
}
