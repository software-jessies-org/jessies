package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
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
        add(makeDocumentationMenu());
        add(makeHelpMenu());
    }
    
    private static class ExitAction extends AbstractAction {
        public ExitAction() {
            GuiUtilities.configureAction(this, GuiUtilities.isWindows() ? "E_xit" : "_Quit", null);
            GnomeStockIcon.configureAction(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            Evergreen.getInstance().handleQuit(null);
        }
    }
    
    private JMenu makeFileMenu() {
        JMenu menu = makeMenu("File", 'F');
        menu.add(new NewFileAction());
        menu.add(new OpenQuicklyAction());
        menu.add(new OpenImportAction());
        menu.add(new OpenAction());
        // FIXME: Should be "Open Recent >" here.

        menu.addSeparator();
        menu.add(new CloseWindowAction());
        menu.add(new SaveAction());
        menu.add(new SaveAsAction());
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
        JMenu menu = makeMenu("Edit", 'E');
        menu.add(PActionFactory.makeUndoAction());
        menu.add(PActionFactory.makeRedoAction());

        menu.addSeparator();
        menu.add(PActionFactory.makeCutAction());
        menu.add(PActionFactory.makeCopyAction());
        menu.add(PActionFactory.makePasteAction());
        menu.add(new AutoCompleteAction());
        menu.add(PActionFactory.makeSelectAllAction());

        menu.addSeparator();
        menu.add(new JoinLinesAction());
        menu.add(new CorrectIndentationAction());
        menu.add(new InsertInterfaceAction());
        
        menu.addSeparator();
        menu.add(new ShowMisspellingsAction());
        menu.add(new CompareSelectionAndClipboardAction());
        
        Evergreen.getInstance().getPreferences().initPreferencesMenuItem(menu);
        
        return menu;
    }

    private JMenu makeFindMenu() {
        JMenu menu = makeMenu("Find", 'n');
        menu.add(FindAction.INSTANCE);
        menu.add(PActionFactory.makeFindNextAction());
        menu.add(PActionFactory.makeFindPreviousAction());

        menu.addSeparator();
        menu.add(new FindAndReplaceAction());

        menu.addSeparator();
        menu.add(new GoToLineAction());
        menu.add(new GoToTagAction());
        
        menu.addSeparator();
        menu.add(new FindFilesContainingSelectionAction());
        
        menu.addSeparator();
        menu.add(PActionFactory.makeFindMatchingBracketAction());
        menu.add(new ScrollToSelectionAction());
        
        return menu;
    }
    
    private JMenu makeViewMenu() {
        JMenu menu = makeMenu("View", 'V');
        menu.add(new ProportionalFontAction());
        menu.add(new FixedFontAction());
        menu.add(new AppropriateFontAction());
        
        menu.addSeparator();
        menu.add(new ShowCounterpartAction());
        
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
        final JMenu menu = makeMenu("Tools", 'T');
        menu.add(new OpenMakefileAction());
        
        menu.addSeparator();
        menu.add(new KillErrorsAction());
        
        menu.addSeparator();
        menu.add(new CheckForLintAction());
        
        menu.addSeparator();
        menu.add(makeExternalToolAction("Sort", "sort"));
        menu.add(makeExternalToolAction("Sort and Remove Duplicates", "sort -u"));
        
        ExternalToolsParser toolsParser = new ExternalToolsParser() {
            public void addItem(ExternalToolAction action) {
                menu.add(action);
            }

            public void addSeparator() {
                menu.addSeparator();
            }
        };
        toolsParser.parse();
        return menu;
    }
    
    private ExternalToolAction makeExternalToolAction(String name, String command) {
        ExternalToolAction result = new ExternalToolAction(name, ToolInputDisposition.SELECTION_OR_DOCUMENT, ToolOutputDisposition.REPLACE, command);
        result.setNeedsFile(true);
        return result;
    }
    
    private JMenu makeDocumentationMenu() {
        JMenu menu = makeMenu("Documentation", 'D');
        menu.add(new ShowDocumentationAction());
        menu.add(new ShowManPageAction());
        
        // UI guidelines.
        menu.addSeparator();
        menu.add(makeLocalOrRemoteLink("_Apple Human Interface Guidelines", "/Developer/Documentation/DocSets/com.apple.ADC_Reference_Library.CoreReference.docset/Contents/Resources/Documents/documentation/UserExperience/Conceptual/OSXHIGuidelines/index.html", "http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/index.html"));
        menu.add(makeLocalOrRemoteLink("_GNOME Human Interface Guidelines", null, "http://library.gnome.org/devel/hig-book/stable/"));
        
        // C/C++.
        menu.addSeparator();
        menu.add(makeLocalOrRemoteLink("GNU _C Library Documentation", "/usr/share/doc/glibc-doc/html/index.html", "http://www.gnu.org/software/libc/manual/html_node/index.html"));
        menu.add(makeLocalOrRemoteLink("_STL Documentation", "/usr/share/doc/stl-manual/html/index.html", "http://www.sgi.com/tech/stl/"));
        
        // Java.
        menu.addSeparator();
        menu.add(makeLocalOrRemoteLink("_Java 6 API", "/usr/share/doc/sun-java6-jdk/html/api/overview-summary.html", "http://java.sun.com/javase/6/docs/api/overview-summary.html"));
        menu.add(makeLocalOrRemoteLink("Java _Language Specification, 3e", null, "http://java.sun.com/docs/books/jls/third_edition/html/j3TOC.html"));
        menu.add(makeLocalOrRemoteLink("Java _Tutorial", null, "http://java.sun.com/docs/books/tutorial/"));
        menu.add(makeLocalOrRemoteLink("Java _VM Specification, 2e", null, "http://java.sun.com/docs/books/vmspec/2nd-edition/html/VMSpecTOC.doc.html"));
        
        // Tools.
        menu.addSeparator();
        menu.add(makeLocalOrRemoteLink("GNU _Make Manual", null, "http://www.gnu.org/software/make/manual/make.html"));
        
        // General.
        menu.addSeparator();
        // FIXME: some day it would be nice to have our own regular expression quick reference.
        menu.add(makeLocalOrRemoteLink("_Regular Expression Documentation", "/usr/share/doc/sun-java6-jdk/html/api/java/util/regex/Pattern.html", PatternUtilities.DOCUMENTATION_URL));
        
        return menu;
    }
    
    private WebLinkAction makeLocalOrRemoteLink(String name, String localFilename, String remoteUrl) {
        String url = (localFilename != null && FileUtilities.exists(localFilename)) ? "file://" + localFilename : remoteUrl;
        return new WebLinkAction(name, url);
    }
    
    private JMenu makeScmMenu() {
        JMenu menu = makeMenu("SCM", 'S');
        menu.add(new CheckInChangesAction());
        menu.add(new ShowHistoryAction());
        return menu;
    }
    
    private JMenu makeWorkspaceMenu() {
        JMenu menu = makeMenu("Workspace", 'W');
        menu.add(new BuildAction(false));
        menu.add(new BuildAction(true));
        menu.addSeparator();
        menu.add(new RescanWorkspaceAction());
        menu.addSeparator();
        menu.add(new AddWorkspaceAction());
        menu.add(new EditWorkspaceAction());
        menu.add(new MergeWorkspaceAction());
        menu.add(new CloseWorkspaceAction());
        menu.addSeparator();
        menu.add(new CycleWorkspacesAction(1));
        menu.add(new CycleWorkspacesAction(-1));
        return menu;
    }
    
    private JMenu makeMenu(String name, char mnemonic) {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }
    
    private JMenu makeHelpMenu() {
        HelpMenu helpMenu = new HelpMenu();
        return helpMenu.makeJMenu();
    }
    
    @Override protected boolean processKeyBinding(KeyStroke ks, KeyEvent event, int condition, boolean pressed) {
        int modifier = KeyEvent.ALT_MASK;
        if ((event.getModifiers() & modifier) == modifier) {
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
