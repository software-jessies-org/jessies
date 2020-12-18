package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class EvergreenMenuBar extends EMenuBar {
    public EvergreenMenuBar() {
        add(makeFileMenu());
        add(makeEditMenu());
        add(makeFindMenu());
        add(makeViewMenu());
        add(makeScmMenu());
        add(makeWorkspaceMenu());
        add(makeToolsMenu());
        add(makeHelpMenu());
    }

    private static class ExitAction extends AbstractAction {
        public ExitAction() {
            GuiUtilities.configureAction(this, GuiUtilities.isWindows() ? "E_xit" : "_Quit", null);
            GnomeStockIcon.configureAction(this);
        }

        public void actionPerformed(ActionEvent e) {
            Evergreen.getInstance().doQuit();
        }
    }

    private JMenu makeFileMenu() {
        JMenu menu = GuiUtilities.makeMenu("File", 'F');
        menu.add(new NewFileAction());
        menu.add(new OpenQuicklyAction());

        menu.addSeparator();
        menu.add(new CloseWindowAction());
        menu.add(new SaveAction());
        menu.add(new SaveAllAction());
        menu.add(new RevertToSavedAction());

        menu.addSeparator();
        menu.add(new FilePropertiesAction());

        if (GuiUtilities.isMacOs() == false) {
            menu.addSeparator();
            menu.add(new JMenuItem(new ExitAction()));
        }
        return menu;
    }

    private JMenu makeEditMenu() {
        JMenu menu = GuiUtilities.makeMenu("Edit", 'E');
        menu.add(PActionFactory.makeUndoAction());
        menu.add(PActionFactory.makeRedoAction());

        menu.addSeparator();
        menu.add(PActionFactory.makeCutAction());
        menu.add(PActionFactory.makeCopyAction());
        menu.add(PActionFactory.makePasteAction());
        menu.add(new AutoCompleteAction());
        menu.add(PActionFactory.makeSelectAllAction());

        menu.addSeparator();
        menu.add(new ReformatFileAction());
        menu.add(new JoinLinesAction());
        menu.add(new CorrectIndentationAction());
        menu.add(new InsertInterfaceAction());

        menu.addSeparator();
        menu.add(new ShowMisspellingsAction());
        menu.add(new CompareSelectionAndClipboardAction());

        Evergreen.getInstance().getPreferences().initPreferencesMenuItem(menu);
        menu.add(Evergreen.getInstance().getFileTypePreferences().makeShowPreferencesAction("_Filetype Preferences..."));
        menu.add(new NewConfigWorkspaceAction());

        return menu;
    }

    private JMenu makeFindMenu() {
        JMenu menu = GuiUtilities.makeMenu("Find", 'n');
        menu.add(FindAction.INSTANCE);
        menu.add(PActionFactory.makeFindNextAction());
        menu.add(PActionFactory.makeFindPreviousAction());

        menu.addSeparator();
        menu.add(new FindAndReplaceAction());

        menu.addSeparator();
        menu.add(new GoToLineAction());
        menu.add(new GoToTagAction());

        menu.addSeparator();
        menu.add(new FindInFilesAction());

        menu.addSeparator();
        menu.add(PActionFactory.makeFindMatchingBracketAction());
        menu.add(new ScrollToSelectionAction());

        return menu;
    }

    private JMenu makeViewMenu() {
        JMenu menu = GuiUtilities.makeMenu("View", 'V');
        menu.add(new ProportionalFontAction());
        menu.add(new FixedFontAction());
        menu.add(new AppropriateFontAction());

        menu.addSeparator();
        menu.add(new ShowCounterpartAction());
        menu.add(new ShowDocumentationAction());

        menu.addSeparator();
        menu.add(ShowHideTagsAction.makeMenuItem());

        menu.addSeparator();
        menu.add(new CycleWindowsAction(1));
        menu.add(new CycleWindowsAction(-1));

        menu.addSeparator();
        menu.add(new ExpandWindowAction());

        return menu;
    }

    private JMenu makeToolsMenu() {
        final JMenu menu = GuiUtilities.makeMenu("Tools", 'T');

        menu.add(new ClearErrorsAction());

        menu.addSeparator();
        menu.add(new CheckForLintAction());

        menu.addSeparator();
        List<ExternalToolAction> actions = ExternalTools.getAllTools();
        for (ExternalToolAction action : actions) {
            if (action != null) {
                menu.add(action);
            } else {
                menu.addSeparator();
            }
        }

        return menu;
    }

    private JMenu makeScmMenu() {
        JMenu menu = GuiUtilities.makeMenu("SCM", 'S');
        menu.add(new CheckInChangesAction());
        menu.add(new ShowHistoryAction());
        return menu;
    }

    private JMenu makeWorkspaceMenu() {
        JMenu menu = GuiUtilities.makeMenu("Workspace", 'W');
        menu.add(new NewWorkspaceAction());
        menu.add(new EditWorkspaceAction());
        menu.add(new CloseWorkspaceAction());
        menu.add(new NewConfigWorkspaceAction());
        menu.addSeparator();
        menu.add(new BuildAction(false));
        menu.add(new BuildAction(true));
        menu.addSeparator();
        menu.add(new RescanWorkspaceAction());
        menu.addSeparator();
        menu.add(new CycleWorkspacesAction(1));
        menu.add(new CycleWorkspacesAction(-1));
        return menu;
    }

    private JMenu makeHelpMenu() {
        HelpMenu helpMenu = new HelpMenu();
        return helpMenu.makeJMenu();
    }

    @Override protected boolean processKeyBinding(KeyStroke ks, KeyEvent event, int condition, boolean pressed) {
        int modifier = KeyEvent.ALT_DOWN_MASK;
        if ((event.getModifiersEx() & modifier) == modifier) {
            char ch = event.getKeyChar();
            final int newIndex = TabbedPane.keyCharToTabIndex(ch);
            if (newIndex != -1) {
                Evergreen.getInstance().goToWorkspaceByIndex(newIndex);
                return true;
            }
        }
        return super.processKeyBinding(ks, event, condition, pressed);
    }
}
