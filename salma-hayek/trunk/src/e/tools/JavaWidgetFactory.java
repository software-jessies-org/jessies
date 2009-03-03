package e.tools;

import e.gui.*;
import e.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * A simple imitation of Richard Stellingwerff's "The Widget Factory" for GNOME.
 * See http://www.stellingwerff.com/?page_id=10 for the latest version of the original.
 */
// FIXME: we're missing various vertical JSeparators because, seemingly, although you can ask for a VERTICAL JSeparator, they're always horizontal?
public class JavaWidgetFactory extends JPanel {
    public JavaWidgetFactory() {
        setBorder(makeEmptyBorder());
        setLayout(new BorderLayout());
        // FIXME: menu
        // FIXME: toolbar
        add(makeWestPane(), BorderLayout.WEST);
        add(makeCenterPane(), BorderLayout.CENTER);
        add(makeTablePane(), BorderLayout.EAST);
        add(makeTabbedPanes(), BorderLayout.SOUTH);
    }
    
    private JComponent makeWestPane() {
        Object[] items = { "ComboBoxEntry" };
        JComboBox comboBox1 = new JComboBox(items);
        JComboBox comboBox2 = new JComboBox(items);
        comboBox2.setEnabled(false);
        
        JTextField textField1 = new JTextField("GtkEntry");
        JTextField textField2 = new JTextField("GtkEntry");
        textField2.setEnabled(false);
        
        JSpinner spinner1 = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        JSpinner spinner2 = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        spinner2.setEnabled(false);
        
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridx = 0; c.gridy = 0; panel.add(comboBox1, c);
        c.gridx = 0; c.gridy = 1; panel.add(comboBox2, c);
        c.gridx = 0; c.gridy = 2; panel.add(Box.createVerticalStrut(4), c);
        c.gridx = 0; c.gridy = 3; panel.add(textField1, c);
        c.gridx = 0; c.gridy = 4; panel.add(textField2, c);
        c.gridx = 0; c.gridy = 5; panel.add(Box.createVerticalStrut(4), c);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 6; panel.add(spinner1, c);
        c.gridx = 1; c.gridy = 6; panel.add(spinner2, c);
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 7; panel.add(Box.createVerticalStrut(4), c);
        c.gridx = 0; c.gridy = 8; panel.add(makeCheckBoxAndRadioButtonPanel(), c);
        panel.setBorder(makeEmptyBorder());
        return panel;
    }
    
    private JComponent makeCheckBoxAndRadioButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(makeCheckBoxPanel());
        panel.add(makeRadioButtonPanel());
        return panel;
    }
    
    private JComponent makeCheckBoxPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        JCheckBox checkBox1 = new JCheckBox("checkbutton1");
        JCheckBox checkBox2 = new JCheckBox("checkbutton2");
        checkBox2.setSelected(true);
        JCheckBox checkBox3 = new JCheckBox("checkbutton3");
        checkBox3.setEnabled(false);
        JCheckBox checkBox4 = new JCheckBox("checkbutton4");
        checkBox4.setEnabled(false);
        checkBox4.setSelected(true);
        panel.add(checkBox1);
        panel.add(checkBox2);
        panel.add(checkBox3);
        panel.add(checkBox4);
        return panel;
    }
    
    private JComponent makeRadioButtonPanel() {
        JRadioButton radioButton1 = new JRadioButton("radiobutton1");
        JRadioButton radioButton2 = new JRadioButton("radiobutton2");
        radioButton2.setSelected(true);
        JRadioButton radioButton3 = new JRadioButton("radiobutton3");
        radioButton3.setEnabled(false);
        JRadioButton radioButton4 = new JRadioButton("radiobutton4");
        radioButton4.setEnabled(false);
        radioButton4.setSelected(true);
        
        ButtonGroup group1 = new ButtonGroup();
        group1.add(radioButton1);
        group1.add(radioButton2);
        ButtonGroup group2 = new ButtonGroup();
        group2.add(radioButton3);
        group2.add(radioButton4);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(radioButton1);
        panel.add(radioButton2);
        panel.add(radioButton3);
        panel.add(radioButton4);
        return panel;
    }
    
    private JComponent makeCenterPane() {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(makeButtonPanel());
        panel.add(makeSliderPanel());
        return panel;
    }
    
    private JComponent makeButtonPanel() {
        JButton button1 = new JButton("button1");
        JButton button2 = new JButton("button2");
        button2.setEnabled(false);
        
        // FIXME: how do you use JToggleButton? this doesn't seem to work right.
        JToggleButton toggleButton1 = new JToggleButton("togglebutton1", true);
        JToggleButton toggleButton2 = new JToggleButton("togglebutton2");
        toggleButton2.setEnabled(false);
        
        Object[] items = { "ComboBox" };
        JComboBox comboBox1 = new JComboBox(items);
        comboBox1.setEditable(false);
        JComboBox comboBox2 = new JComboBox(items);
        comboBox2.setEditable(false);
        comboBox2.setEnabled(false);
        
        // FIXME: what is a GTK+ OptionMenu?
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridx = 0; panel.add(button1, c);
        c.gridx = 0; panel.add(button2, c);
        c.gridx = 0; panel.add(Box.createVerticalStrut(4), c);
        c.gridx = 0; panel.add(toggleButton1, c);
        c.gridx = 0; panel.add(toggleButton2, c);
        c.gridx = 0; panel.add(Box.createVerticalStrut(4), c);
        c.gridx = 0; panel.add(comboBox1, c);
        c.gridx = 0; panel.add(comboBox2, c);
        return panel;
    }
    
    private JComponent makeSliderPanel() {
        // FIXME: Swing doesn't support GTK+'s weird "negative" progress bars.
        JProgressBar progressBar1 = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progressBar1.setValue(50);
        JProgressBar progressBar2 = new JProgressBar(JProgressBar.VERTICAL, 0, 100);
        progressBar2.setValue(50);
        
        JSlider slider1 = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        JSlider slider2 = new JSlider(JSlider.VERTICAL, 0, 100, 50);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 0, 0);
        c.gridy = GridBagConstraints.RELATIVE;
        c.gridx = 0; c.gridy = 0; panel.add(progressBar1, c);
        c.gridx = 0; c.gridy = 1; panel.add(slider1, c);
        c.gridx = 0; c.gridy = 2; panel.add(progressBar2, c);
        c.gridx = 1; c.gridy = 2; panel.add(slider2, c);
        return panel;
    }
    
    private JComponent makeTablePane() {
        TableModel tableModel = new AbstractTableModel() {
            public int getColumnCount() {
                return 2;
            }
            
            public int getRowCount() {
                return 0;
            }
            
            public Object getValueAt(int y, int x) {
                return "data";
            }
            
            public String getColumnName(int i) {
                return "Column" + (i + 1);
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setBorder(makeEmptyBorder());
        return panel;
    }
    
    private javax.swing.border.Border makeEmptyBorder() {
        return GuiUtilities.createEmptyBorder(GuiUtilities.getComponentSpacing());
    }
    
    private JComponent makeTabbedPanes() {
        JPanel panel = new JPanel(new GridLayout(1, 4));
        panel.add(makeTabbedPane(JTabbedPane.TOP));
        panel.add(makeTabbedPane(JTabbedPane.BOTTOM));
        panel.add(makeTabbedPane(JTabbedPane.LEFT));
        panel.add(makeTabbedPane(JTabbedPane.RIGHT));
        
        JPanel outerPanel = new JPanel(new BorderLayout(8, 8));
        outerPanel.add(new JSeparator(), BorderLayout.NORTH);
        outerPanel.add(panel, BorderLayout.CENTER);
        return outerPanel;
    }
    
    private JComponent makeTabbedPane(int kind) {
        JTabbedPane tabbedPane = new JTabbedPane(kind);
        tabbedPane.addTab("tab1", new JLabel(" "));
        tabbedPane.addTab("tab2", new JLabel(" "));
        tabbedPane.addTab("tab3", new JLabel(" "));
        return tabbedPane;
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                e.util.GuiUtilities.initLookAndFeel();
                MainFrame frame = new MainFrame("Java Widget Factory");
                frame.setContentPane(new JavaWidgetFactory());
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
