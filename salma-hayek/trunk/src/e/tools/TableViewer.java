package e.tools;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.*;
import e.util.*;

/**
 * A stand-alone program to render a table in a window. This is sometimes
 * a convenient thing to have, and something no other tool I know of really
 * helps with.
 */
public class TableViewer extends JFrame {
    private JTable table;
    
    public TableViewer(String filename) throws IOException {
        Log.setApplicationName("TableViewer");
        initTable(filename);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Table Viewer");
        getContentPane().add(new JScrollPane(table));
        setSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
    }
    
    public void initTable(String filename) throws IOException {
        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        InputStream inputStream = System.in;
        if (filename.equals("-") == false) {
            inputStream = new FileInputStream(FileUtilities.fileFromString(filename));
        }
        LineNumberReader in = new LineNumberReader(new InputStreamReader(inputStream));
        String line;
        while ((line = in.readLine()) != null) {
            String[] fields = line.split("\t");
            if (fields.length > model.getColumnCount()) {
                model.setColumnCount(fields.length);
            }
            model.addRow(fields);
        }
        Log.warn("rows:"+model.getRowCount()+ ",columns:"+model.getColumnCount());
        for (int i = 0; i < model.getColumnCount(); i++) {
            packColumn(i);
        }
    }

    public void packColumn(int columnIndex) {
        DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn column = columnModel.getColumn(columnIndex);

        // Get width of column header.
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component component = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, 0);
        int width = component.getPreferredSize().width;
    
        // Get maximum width of column data.
        for (int row = 0; row < table.getRowCount(); row++) {
            renderer = table.getCellRenderer(row, columnIndex);
            component = renderer.getTableCellRendererComponent(table, table.getValueAt(row, columnIndex), false, false, row, columnIndex);
            width = Math.max(width, component.getPreferredSize().width);
        }
    
        // Set the width
        column.setPreferredWidth(width + 4);
    }
    
    public static void main(String[] args) {
        try {
            GuiUtilities.initLookAndFeel();
            for (int i = 0; i < args.length; i++) {
                TableViewer tableViewer = new TableViewer(args[i]);
                tableViewer.setVisible(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
