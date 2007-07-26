package e.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import javax.swing.*;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;

/**
 * Shows and decodes the contents of JAR files specified on the command line.
 * Basically just a simple front end to jar(1) and javap(1).
 * 
 * @author Elliott Hughes
 */

//
// TODO: SearchField filtering in the list.
// TODO: do the summary ourselves using reflection.
// TODO: link the summary to the detail (i.e. "show me the disassembly of this method")
// TODO: handle files other than .class files.
//

public class JarExplorer extends MainFrame {
    private JCheckBox showLineNumberAndLocalVariableTables;
    private JCheckBox showVerboseDetail;
    private JList list;
    private DefaultListModel model;

    private PTextArea summaryTextArea;
    private PTextArea detailTextArea;

    private String filename;

    public JarExplorer(String filename) {
        super(filename);
        this.filename = filename;

        try {
            initFromJarFile();
        } catch (IOException ex) {
            SimpleDialog.showDetails(null, "Problem reading JAR file.", ex);
        }

        setContentPane(makeUi());
        pack();
    }

    private void initFromJarFile() throws IOException {
        File file = new File(filename);
        ZipFile zipFile = new ZipFile(file);

        final int totalEntryCount = zipFile.size();
        int entryCount = 0;

        ArrayList<String> entries = new ArrayList<String>();
        Enumeration e = zipFile.entries();
        while (e.hasMoreElements()) {
            ++entryCount;
            ZipEntry entry = (ZipEntry) e.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            entries.add(entry.getName());
        }
        zipFile.close();

        Collections.sort(entries);
        model = new DefaultListModel();
        for (int i = 0; i < entries.size(); ++i) {
            model.addElement(entries.get(i));
        }
    }

    private JComponent makeUi() {
        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateInformation();
            }
        };
        
        final FilteredListModel filteredListModel = new FilteredListModel(model);
        
        list = new JList(filteredListModel);
        list.setCellRenderer(new EListCellRenderer(true));
        list.setVisibleRowCount(10);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    updateInformation();
                }
            }
        });
        
        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showLineNumberAndLocalVariableTables = new JCheckBox("Show Line Number Tables");
        showLineNumberAndLocalVariableTables.addItemListener(itemListener);
        checkBoxPanel.add(showLineNumberAndLocalVariableTables);
        showVerboseDetail = new JCheckBox("Show Verbose Information");
        showVerboseDetail.addItemListener(itemListener);
        checkBoxPanel.add(showVerboseDetail);
        checkBoxPanel.add(filteredListModel.makeSearchField());
        
        JScrollPane entriesScroller = new JScrollPane(list);
        JPanel entriesPanel = new JPanel(new BorderLayout());
        entriesPanel.add(entriesScroller, BorderLayout.CENTER);
        entriesPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        entriesPanel.add(checkBoxPanel, BorderLayout.NORTH);
        
        summaryTextArea = makeTextArea();
        detailTextArea = makeTextArea();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Summary", new JScrollPane(summaryTextArea));
        tabbedPane.add("Detail", new JScrollPane(detailTextArea));

        return new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                              entriesPanel, tabbedPane);
    }

    private PTextArea makeTextArea() {
        PTextArea textArea = new PTextArea(30, 80);
        textArea.setEditable(false);
        return textArea;
    }

    private void updateInformation() {
        updateInformation((String) list.getSelectedValue());
    }
    
    private void updateInformation(String entry) {
        if (entry.endsWith(".class")) {
            String className = entry.replace('/', '.');
            className = className.replaceAll("\\.class$", "");

            summaryTextArea.setText(runJavaP(className, false));
            summaryTextArea.setCaretPosition(0);

            detailTextArea.setText(runJavaP(className, true));
            detailTextArea.setCaretPosition(0);
        }
    }

    private String runJavaP(String className, boolean detail) {
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();

        ArrayList<String> command = new ArrayList<String>();
        command.add("javap");
        command.add("-private");
        if (detail) {
            command.add("-c");
            if (showLineNumberAndLocalVariableTables.isSelected()) {
                command.add("-l");
            }
            if (showVerboseDetail.isSelected()) {
                command.add("-verbose");
            }
        }
        command.add("-classpath");
        command.add(filename);
        command.add(className);

        ProcessUtilities.backQuote(null, makeArray(command), lines, errors);

        List source = (errors.size() == 0) ? lines : errors;
        return StringUtilities.join(source, "\n");
    }

    private String[] makeArray(final List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public static void main(final String[] filenames) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                for (final String filename : filenames) {
                    new JarExplorer(filename).setVisible(true);
                }
            }
        });
    }
}
