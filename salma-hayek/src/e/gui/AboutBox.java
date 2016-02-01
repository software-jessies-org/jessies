package e.gui;

import com.apple.eawt.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

/**
 * A simple "about box".
 */
public class AboutBox {
    public static enum License {
        UNKNOWN("Unknown license.\n"),
        
        APL_2(
            // Unlike the GPL, I couldn't find standard text suitable for UI.
            // I've used the friendly first line of the usual GPL UI formulation,
            // followed by the APL2 per-file comment, replacing "file" with "software".
            "%APP% is free software: you can redistribute it and/or modify " +
            "it under the terms of the Apache License, Version 2.0 (the \"License\"); " +
            "you may not use this software except in compliance with the License. " +
            "You may obtain a copy of the License at\n" +
            "\n" +
            "   http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "Unless required by applicable law or agreed to in writing, software " +
            "distributed under the License is distributed on an \"AS IS\" BASIS, " +
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." +
            "See the License for the specific language governing permissions and " +
            "limitations under the License."
        ),
        
        GPL_2_OR_LATER(
            "%APP% is free software: you can redistribute it and/or modify\n" +
            "it under the terms of the GNU General Public License as published by\n" +
            "the Free Software Foundation; either version 2 of the License, or\n" +
            "(at your option) any later version.\n" +
            "\n" +
            "%APP% is distributed in the hope that it will be useful,\n" +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "GNU General Public License for more details.\n" +
            "\n"+
            "You should have received a copy of the GNU General Public License\n" +
            "along with %APP%; If not, see <http://www.gnu.org/licenses/>.\n"
        );
        
        public final String text;
        
        private License(String text) {
            this.text = text;
        }
    }
    
    private static final AboutBox INSTANCE = new AboutBox();
    
    private License license = License.UNKNOWN;
    private ImageIcon icon;
    private String webSiteAddress;
    private ArrayList<String> versionLines = new ArrayList<String>();
    private ArrayList<String> copyrightLines = new ArrayList<String>();
    
    private String packageVersion = "unknown";
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
    
    public String getWebSiteAddress() {
        return webSiteAddress;
    }
    
    /**
     * Sets the base URL for the application's website.
     * The current requirements are:
     * 1. that the address itself goes to some meaningful page about the application.
     * 2. that there be a "ChangeLog.html" and a "faq.html" under this location.
     */
    public void setWebSiteAddress(String webSiteAddress) {
        this.webSiteAddress = webSiteAddress;
    }
    
    public boolean isConfigured() {
        return (Log.getApplicationName() != null);
    }
    
    public void setImage(String filename) {
        try {
            // Apple's HIG says scale to 64x64. I'm not sure there's any advice for the other platforms.
            this.icon = new ImageIcon(ImageUtilities.scale(ImageIO.read(FileUtilities.fileFromString(filename)), 64, 64, ImageUtilities.InterpolationHint.BICUBIC));
            
            if (GuiUtilities.isMacOs()) {
                // Apple's HIG says that these dialog icons should be the application icon.
                UIManager.put("OptionPane.errorIcon", icon);
                UIManager.put("OptionPane.informationIcon", icon);
                UIManager.put("OptionPane.questionIcon", icon);
                UIManager.put("OptionPane.warningIcon", icon);
            }
        } catch (Exception ex) {
            // We can live without an icon.
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
    
    public void setLicense(License license) {
        this.license = license;
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
        // See MainFrame for rationale.
        JDialog dialog = new JDialog(findSuitableOwner()) {
            @Override
            public void setVisible(boolean newVisibility) {
                super.setVisible(newVisibility);
                if (newVisibility == false) {
                    dispose();
                }
            }
        };
        makeUi(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        JFrameUtilities.closeOnEsc(dialog);
        dialog.setVisible(true);
    }
    
    private void makeUi(final JDialog dialog) {
        if (GuiUtilities.isMacOs() == false) {
            // GNOME and Windows applications give their about boxes titles.
            dialog.setTitle("About " + Log.getApplicationName());
        }
        
        // FIXME: add GNOME and Windows implementations.
        // http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/XHIGWindows/chapter_17_section_5.html#//apple_ref/doc/uid/20000961-TPXREF17
        
        // Mac OS font defaults.
        Font applicationNameFont = new Font("Lucida Grande", Font.BOLD, 14);
        Font versionFont = new Font("Lucida Grande", Font.PLAIN, 10);
        Font copyrightFont = new Font("Lucida Grande", Font.PLAIN, 10);
        Font linkFont = versionFont;
        
        if (GuiUtilities.isWindows()) {
            // I don't think this is quite the right font, but it seems to be as close as we can get with the Windows LAF.
            applicationNameFont = versionFont = copyrightFont = linkFont = UIManager.getFont("MenuItem.font");
        } else if (GuiUtilities.isGtk()) {
            final float PANGO_SCALE_SMALL = (1.0f / 1.2f);
            final float PANGO_SCALE_XX_LARGE = (1.2f * 1.2f * 1.2f);
            final Font gnomeBaseFont = UIManager.getFont("TextArea.font");
            final float baseSize = gnomeBaseFont.getSize2D();
            applicationNameFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_XX_LARGE).deriveFont(Font.BOLD);
            versionFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_SMALL);
            copyrightFont = gnomeBaseFont.deriveFont(baseSize * PANGO_SCALE_SMALL);
            linkFont = gnomeBaseFont;
        }
        
        int bottomBorder = 20;
        if (GuiUtilities.isGtk()) {
            bottomBorder = 12;
        }
        
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, bottomBorder, 12));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        Dimension spacerSize = new Dimension(1, 8);
        
        // The application icon comes first...
        if (icon != null) {
            addLabel(panel, new JLabel(icon));
            panel.add(Box.createRigidArea(spacerSize));
            panel.add(Box.createRigidArea(spacerSize));
        }
        
        // Then the application name...
        addLabel(panel, applicationNameFont, Log.getApplicationName());
        panel.add(Box.createRigidArea(spacerSize));
        
        // Then version information...
        for (String version : versionLines) {
            addLabel(panel, versionFont, version);
        }
        if (versionLines.size() > 0) {
            panel.add(Box.createRigidArea(spacerSize));
        }
        
        // Then copyright information...
        for (String copyright : copyrightLines) {
            addLabel(panel, copyrightFont, copyright);
        }
        
        // Then any hyperlink...
        if (webSiteAddress != null) {
            panel.add(Box.createRigidArea(spacerSize));
            addLabel(panel, new JHyperlinkButton(webSiteAddress, webSiteAddress, linkFont));
        }
        
        // And finally, for the GTK LAF, buttons...
        if (GuiUtilities.isGtk()) {
            JButton creditsButton = new JButton("Credits");
            GnomeStockIcon.configureButton(creditsButton);
            creditsButton.setEnabled(false);
            
            JButton licenseButton = new JButton("License");
            GnomeStockIcon.configureButton(licenseButton);
            licenseButton.addActionListener(new ShowLicenseActionListener());
            
            JButton closeButton = makeCloseButton(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            
            ComponentUtilities.tieButtonSizes(creditsButton, licenseButton, closeButton);
            
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(creditsButton);
            buttonPanel.add(Box.createHorizontalGlue());
            if (license != License.UNKNOWN) {
                buttonPanel.add(licenseButton);
                buttonPanel.add(Box.createHorizontalGlue());
            }
            buttonPanel.add(closeButton);
            
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
    
    private static void addLabel(JPanel panel, Font font, String text) {
        // FIXME: Mac OS actually uses selectable text components which is handy for copying & pasting version information.
        // FIXME: support HTML and automatically install code to change the mouse cursor when hovering over links, and use BrowserLauncher when a link is clicked?
        JLabel label = new JLabel(text);
        label.setFont(font);
        addLabel(panel, label);
    }
    
    private static void addLabel(JPanel panel, JComponent label) {
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panel.add(label);
    }
    
    private void initMacOs() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        initMacOsAboutMenu();
    }
    
    private void initMacOsAboutMenu() {
        Application.getApplication().addApplicationListener(new ApplicationAdapter() {
            public void handleAbout(ApplicationEvent e) {
                AboutBox.getSharedInstance().show();
                e.setHandled(true);
            }
        });
    }
    
    private void initIcon() {
        if (icon != null) {
            return;
        }
        
        String aboutBoxIconFilename = System.getProperty("org.jessies.aboutBoxIcon");
        if (aboutBoxIconFilename != null) {
            setImage(aboutBoxIconFilename);
        }
    }
    
    /**
     * "universal.make" arranges to write build information to a file that
     * should be included in any distribution. This method tries to find such
     * a file, and pass it to parseBuildRevisionFile.
     */
    private void findBuildRevisionFile() {
        for (String directory : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File classPathEntry = new File(directory);
            File file = new File(classPathEntry.getParentFile().getParent(), ".generated" + File.separator + "build-revision.txt");
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
        String[] content = StringUtilities.readLinesFromFile(file);
        String buildDate = content[0];
        projectRevision = content[1];
        libraryRevision = content[2];
        packageVersion = content[3];
        String[] info = new String[] {
            "Package " + packageVersion,
            "Revision " + projectRevision + " (" + libraryRevision + ")",
            "Built " + buildDate,
        };
        for (String line : info) {
            addVersion(line);
            Log.warn(line);
        }
    }
    
    public String getProblemReportSubject() {
        String systemDetails = Log.getSystemDetailsForProblemReport();
        String subject = "<<<YourSubjectHere>>> " + Log.getApplicationName() + " problem (" + packageVersion + "/" + projectRevision + "/" + libraryRevision + "/" + systemDetails + ")";
        return StringUtilities.urlEncode(subject).replaceAll("\\+", "%20");
    }
    
    public String getIdentificationString() {
        return Log.getApplicationName() + " (" + packageVersion + "/" + projectRevision + "/" + libraryRevision + "/" + Log.getSystemDetailsForProblemReport() + ")";
    }
    
    private JButton makeCloseButton(ActionListener actionListener) {
        JButton closeButton = new JButton("Close");
        GnomeStockIcon.configureButton(closeButton);
        closeButton.addActionListener(actionListener);
        return closeButton;
    }
    
    private class ShowLicenseActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
            
            PTextArea textArea = new PTextArea(13, 40);
            textArea.setEditable(false);
            textArea.setText(license.text.replaceAll("%APP%", Log.getApplicationName()));
            textArea.setWrapStyleWord(true);
            
            JButton closeButton = makeCloseButton(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Frame owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, panel);
                    owner.setVisible(false);
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
            buttonPanel.add(closeButton);
            
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            JFrameUtilities.makeSimpleWindow("License", panel).setVisible(true);
        }
    }
    
    public static void main(String[] args) {
        Log.setApplicationName("AboutBoxTest");
        GuiUtilities.initLookAndFeel();
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setWebSiteAddress("https://code.google.com/p/jessies/");
        aboutBox.addCopyright("Copyright (C) 2006, Elliott Hughes");
        aboutBox.show();
    }
}
