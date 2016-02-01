package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import e.util.*;

public class ETabbedPane extends JComponent {
    private JPanel tabPanel;
    private JPanel contentPanel;
    private Tab displayedTab;
    private CardLayout layout;
    
    public ETabbedPane() {
        setLayout(new BorderLayout());
        setFont(getDefaultFont());
        add(createTabPanel(), BorderLayout.WEST);
        add(createContentPanel(), BorderLayout.CENTER);
    }
    
    public Font getDefaultFont() {
        return new Font(Parameters.getParameter("font.name", "verdana"), Font.BOLD,
        Parameters.getParameter("font.size", 12));
    }
    
    public JComponent createTabPanel() {
        tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setPreferredSize(new Dimension(15, Short.MAX_VALUE));
        tabPanel.setBackground(Color.getColor("titlebar.leftColor").darker());
        return tabPanel;
    }
    
    public JComponent createContentPanel() {
        layout = new CardLayout();
        contentPanel = new JPanel(layout);
        return contentPanel;
    }
    
    private Hashtable components = new Hashtable();
    
    public void addTab(String name, Component contents) {
        Tab tab = new Tab(name);
        components.put(name, contents);
        tabPanel.add(tab, tabPanel.getComponentCount());
        contentPanel.add(contents, name);
        if (displayedTab == null) setDisplayedTab(tab);
    }
    
    public void setDisplayedTab(Tab displayedTab) {
        this.displayedTab = displayedTab;
        layout.show(contentPanel, displayedTab.name);
        repaint();
    }
    
    public Component getDisplayedComponent() {
        return (Component) components.get(displayedTab.getName());
    }
    
    public class Tab extends EButton implements ActionListener {
        public String name;
        
        public Tab(String name) {
            super("");
            this.name = name;
            addActionListener(this);
        }
        
        public String getName() {
            return name;
        }
        
        public Dimension getPreferredSize() {
            return new Dimension(16, 100);
        }
        
        public void actionPerformed(ActionEvent e) {
            setDisplayedTab(this);
        }
        
        public void paint(Graphics og) {
            Graphics2D g = (Graphics2D) og;
            paintTab(g);
            paintLabel(g);
        }
        
        public void paintTab(Graphics2D g) {
            GeneralPath p = new GeneralPath();
            p.moveTo(16f, 0f);
            p.lineTo(1f, 7f);
            p.lineTo(1f, (float) (this.getHeight() - 7));
            p.lineTo(16f, (float) this.getHeight());
            p.closePath();
            
            Color leftColor = Color.getColor("titlebar.leftColor");
            Color rightColor = Color.getColor("titlebar.rightColor");
            if (this != displayedTab) {
                leftColor = leftColor.darker();
                rightColor = rightColor.darker();
            }
            GradientPaint gradient = new GradientPaint(0, 10, rightColor, 14, 10, leftColor);
            g.setPaint(gradient);
            g.fill(p);
        }
        
        public void paintLabel(Graphics2D g) {
            g.rotate(Math.toRadians(-90));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(getFont());
            g.setColor(Color.getColor("titlebar.textColor"));
            //FIXME: this is badly broken. rotation seems weird.
            int baseline = g.getFontMetrics().getAscent() - 0;
            g.drawString(name, -80, baseline);
        }
    }
}
