package e.edit;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;

public class EditMenuBar extends JMenuBar implements MenuListener {
    public EditMenuBar() {
    }
    
    public void populate() {
        add(makeFileMenu());
        add(makeEditMenu());
        add(makeFindMenu());
        add(makeViewMenu());
        add(makeScmMenu());
        add(makeWorkspaceMenu());
        add(makeToolsMenu());
        add(makeWindowMenu());
        add(makeHelpMenu());
    }
    
    public class ExitAction extends AbstractAction {
        public ExitAction() {
            super(GuiUtilities.isWindows() ? "Exit" : "Quit");
            GnomeStockIcon.useStockIcon(this, "gtk-quit");
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

        if (GuiUtilities.isMacOs() == false) {
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
        menu.add(PActionFactory.makeUndoAction());
        menu.add(PActionFactory.makeRedoAction());

        menu.add(new JSeparator());
        menu.add(PActionFactory.makeCutAction());
        menu.add(PActionFactory.makeCopyAction());
        menu.add(PActionFactory.makePasteAction());
        menu.add(new AutoCompleteAction());
        menu.add(PActionFactory.makeSelectAllAction());

        menu.add(new JSeparator());
        menu.add(new CorrectIndentationAction());
        menu.add(new InsertInterfaceAction());
        
        menu.add(new JSeparator());
        menu.add(new ShowMisspellingsAction());
        menu.add(new CompareSelectionAndClipboardAction());

        return menu;
    }

    public JMenu makeFindMenu() {
        JMenu menu = new JMenu("Find");
        menu.add(FindAction.INSTANCE);
        menu.add(PActionFactory.makeFindNextAction());
        menu.add(PActionFactory.makeFindPreviousAction());
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
        menu.add(new ProportionalFontAction());
        menu.add(new FixedFontAction());
        menu.add(new AppropriateFontAction());

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
    
    public JMenu makeScmMenu() {
        JMenu menu = new JMenu("SCM");
        menu.add(new CheckInChangesAction());
        menu.add(new ShowHistoryAction());
        return menu;
    }
    
    public JMenu makeWorkspaceMenu() {
        JMenu menu = new JMenu("Workspace");
        menu.add(new AddWorkspaceAction());
        menu.add(new RemoveWorkspaceAction());
        menu.add(new JSeparator());
        menu.add(new CycleWorkspacesAction(1));
        menu.add(new CycleWorkspacesAction(-1));
        return menu;
    }
    
    public JMenu makeWindowMenu() {
        JMenu menu = new JMenu("Window");
        menu.add(new CycleWindowsAction(1));
        menu.add(new CycleWindowsAction(-1));
        return menu;
    }
    
    private JMenu makeHelpMenu() {
        HelpMenu helpMenu = new HelpMenu("Edit");
        helpMenu.setWebsite("http://software.jessies.org/edit/");
        helpMenu.setChangeLog("http://software.jessies.org/edit/ChangeLog.html");
        return helpMenu.makeJMenu();
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
