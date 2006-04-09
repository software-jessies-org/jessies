package e.gui;

import com.apple.eawt.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * A simple "about box".
 */
public class AboutBox {
    private static final AboutBox INSTANCE = new AboutBox();
    
    private ImageIcon icon;
    private String applicationName;
    private ArrayList<String> versionLines = new ArrayList<String>();
    private ArrayList<String> copyrightLines = new ArrayList<String>();
    
    private String projectRevision = "unknown";
    private String libraryRevision = "unknown";
    
    private AboutBox() {
        initMacOs();
        initIcon();
        findBuildRevisionFile();
    }
    
    public static AboutBox getSharedInstance() {
        return INSTANCE;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public boolean isConfigured() {
        return (applicationName != null);
    }
    
    public void setImage(String filename) {
        this.icon = new ImageIcon(filename);
        
        // Apple's HIG says scale to 64x64. I'm not sure there's any advice for the other platforms.
        this.icon = new ImageIcon(ImageUtilities.scale(icon.getImage(), 64, 64, ImageUtilities.InterpolationHint.BICUBIC));
        
        if (GuiUtilities.isMacOs()) {
            // Apple's HIG says that these dialog icons should be the application icon.
            UIManager.put("OptionPane.errorIcon", icon);
            UIManager.put("OptionPane.informationIcon", icon);
            UIManager.put("OptionPane.questionIcon", icon);
            UIManager.put("OptionPane.warningIcon", icon);
        }
    }
    
    public void addVersion(String version) {
        versionLines.add(version);
    }
    
    /**
     * Adds a line of copyright text. You can add as many as you like. ASCII
     * renditions of the copyright symbol are automatically converted to the
     * real thing.
     */
    public void addCopyright(String copyright) {
        copyrightLines.add(copyright.replaceAll("\\([Cc]\\)", "\u00a9"));
    }
    
    private Frame findSuitableOwner() {
        if (GuiUtilities.isMacOs()) {
            // On Mac OS, we're supposed to be in the center of the display.
            return null;
        }
        // Find an owner for the about box so we inherit the frame icon and get a sensible relative position.
        Frame owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner());
        if (owner == null) {
            Frame[] frames = Frame.getFrames();
            if (frames.length > 0) {
                owner = frames[0];
            }
        }
        return owner;
    }
    
    public void show() {
        JDialog dialog = new JDialog(findSuitableOwner());
        makeUi(dialog);
        dialog.setVisible(true);
    }
    
    private void makeUi(final JDialog dialog) {
        if (GuiUtilities.isMacOs() == false) {
            // GNOME and Win32 applications give their about boxes titles.
            dialog.setTitle("About " + applicationName);
        }
        
        // FIXME: add GNOME and Win32 implementations.
        // http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/XHIGWindows/chapter_17_section_5.html#//apple_ref/doc/uid/20000961-TPXREF17
        
        // Mac OS font defaults.
        Font applicationNameFont = new Font("Lucida Grande", Font.BOLD, 14);
        Font versionFont = new Font("Lucida Grande", Font.PLAIN, 10);
        Font copyrightFont = new Font("Lucida Grande", Font.PLAIN, 10);
        
        if (GuiUtilities.isWindows()) {
            // I don't think this is quite the right font, but it seems to be as close as we can get with the Win32 LAF.
            applicationNameFont = versionFont = copyrightFont = UIManager.getFont("MenuItem.font");
        } else if (GuiUtilities.isGtk()) {
            final float PANGO_SCALE_SMALL = (1.0f / 1.2f);
            final float PANGO_SCALE_XX_LARGE = (1.2f * 1.2f * 1.2f);
            final Font gnomeBaseFont = UIManager.getFont("TextArea.font");
            final float baseSize = gnomeBaseFont.getSize2D();
            applicationNameFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_XX_LARGE).deriveFont(Font.BOLD);
            versionFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_SMALL);
            copyrightFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_SMALL);
        }
        
        int bottomBorder = 20;
        if (GuiUtilities.isGtk()) {
            bottomBorder = 12;
        }
        
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(8, 12, bottomBorder, 12));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        Dimension spacerSize = new Dimension(1, 8);
        
        if (icon != null) {
            addLabel(panel, new JLabel(icon));
            panel.add(Box.createRigidArea(spacerSize));
            panel.add(Box.createRigidArea(spacerSize));
        }
        
        addLabel(panel, applicationNameFont, applicationName);
        panel.add(Box.createRigidArea(spacerSize));
        
        for (String version : versionLines) {
            addLabel(panel, versionFont, version);
        }
        if (versionLines.size() > 0) {
            panel.add(Box.createRigidArea(spacerSize));
        }
        
        for (String copyright : copyrightLines) {
            addLabel(panel, copyrightFont, copyright);
        }
        
        if (GuiUtilities.isGtk()) {
            JButton closeButton = new JButton("Close");
            GnomeStockIcon.useStockIcon(closeButton, "gtk-close");
            closeButton.setMnemonic(KeyEvent.VK_C);
            closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            JButton creditsButton = new JButton("Credits");
            creditsButton.setEnabled(false);
            creditsButton.setMnemonic(KeyEvent.VK_R);
            
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(creditsButton, BorderLayout.WEST);
            buttonPanel.add(closeButton, BorderLayout.EAST);
            
            panel.add(Box.createRigidArea(spacerSize));
            panel.add(buttonPanel);
        }
        
        dialog.setContentPane(panel);
        
        // Set an appropriate size.
        dialog.pack();
        // Disable the "maximize" button.
        dialog.setMaximumSize(dialog.getPreferredSize());
        dialog.setMinimumSize(dialog.getPreferredSize());
        // Stop resizing.
        dialog.setResizable(false);
        
        // Center on the display.
        // FIXME: use the visual center.
        dialog.setLocationRelativeTo(dialog.getOwner());
    }
    
    private static void addLabel(JPanel panel, Icon icon) {
        addLabel(panel, new JLabel(icon));
    }
    
    private static void addLabel(JPanel panel, Font font, String text) {
        // FIXME: Mac OS actually uses selectable text components which is handy for copying & pasting version information.
        // FIXME: support HTML and automatically install code to change the mouse cursor when hovering over links, and use BrowserLauncher when a link is clicked?
        JLabel label = new JLabel(text);
        label.setFont(font);
        addLabel(panel, label);
    }
    
    private static void addLabel(JPanel panel, JLabel label) {
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.add(label);
    }
    
    private void initMacOs() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        initMacOsAboutMenu();
        initMacOsIcon();
    }
    
    private void initMacOsAboutMenu() {
        Application.getApplication().addApplicationListener(new ApplicationAdapter() {
            public void handleAbout(ApplicationEvent e) {
                AboutBox.getSharedInstance().show();
                e.setHandled(true);
            }
        });
    }
    
    private void initMacOsIcon() {
        // FIXME: we need to look for the icon in a way that will work on Linux too.
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (key.startsWith("APP_ICON_")) {
                String icnsFilename = env.get(key);
                // FIXME: if we had a .icns reader for ImageIO, we wouldn't need to mess around like this.
                String pngFilename = icnsFilename.replaceAll("\\.icns$", "-128.png");
                setImage(pngFilename);
                return;
            }
        }
    }
    
    private void initIcon() {
        if (icon != null) {
            return;
        }
        
        String frameIconFilename = System.getProperty("org.jessies.frameIcon");
        if (frameIconFilename != null) {
            setImage(frameIconFilename);
        }
    }
    
    /**
     * "universal.make" arranges to write build information to a file that
     * should be included in any distribution. This method tries to find such
     * a file, and pass it to parseBuildRevisionFile.
     */
    private void findBuildRevisionFile() {
        for (String directory : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File file = new File(directory + File.separator + ".." + File.separator + ".generated" + File.separator + "build-revision.txt");
            if (file.exists()) {
                parseBuildRevisionFile(file);
                return;
            }
        }
    }
    
    /**
     * Extracts information from the make-generated "build-revision.txt" and
     * turns it into version information for our about box. It's ugly, but it's
     * automated and honest.
     */
    private void parseBuildRevisionFile(File file) {
        String[] content = StringUtilities.readLinesFromFile(file.toString());
        String buildDate = content[0];
        projectRevision = content[1];
        libraryRevision = content[2];
        String[] info = new String[] {
            "Revision " + projectRevision + " (" + libraryRevision + ")",
            "Built " + buildDate,
        };
        for (String line : info) {
            addVersion(line);
            Log.warn(line);
        }
    }
    
    public String getBugReportSubject() {
        return applicationName + "%20%28" + projectRevision + "%2f" + libraryRevision + "%29%20bug";
    }
    
    public static void main(String[] args) {
        GuiUtilities.initLookAndFeel();
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setApplicationName("Demonstration");
        aboutBox.addCopyright("Copyright (C) 2006, Elliott Hughes");
        aboutBox.show();
    }
}
