package e.edit;

import e.util.*;
import java.awt.*;
import javax.swing.*;

public class EvergreenPreferences extends Preferences {
    public static final String ALWAYS_USE_FIXED_FONT = "alwaysUseFixedFont";
    public static final String DEFAULT_INDENTATION = "defaultIndentation";
    public static final String PROPORTIONAL_FONT = "proportionalFont";
    public static final String FIXED_FONT = "fixedFont";
    public static final String TRIM_TRAILING_WHITESPACE = "trimTrailingWhitespace";
    public static final String UNINTERESTING_EXTENSIONS = "uninterestingExtensions";
    //public static final String BACKGROUND_COLOR = "backgroundColor";
    //public static final String FOREGROUND_COLOR = "foregroundColor";
    
    protected String getPreferencesFilename() {
        return Evergreen.getPreferenceFilename("app-preferences");
    }
    
    protected void initPreferences() {
        addPreference(DEFAULT_INDENTATION, "    ", "Default indentation string");
        addPreference(TRIM_TRAILING_WHITESPACE, Boolean.FALSE, "Trim trailing whitespace on save");
        addSeparator();
        addPreference(FIXED_FONT, new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 12), "Fixed font");
        addPreference(PROPORTIONAL_FONT, new Font("Verdana", Font.PLAIN, 12), "Proportional font");
        addPreference(ALWAYS_USE_FIXED_FONT, Boolean.TRUE, "Always use fixed font");
        addSeparator();
        addPreference(UNINTERESTING_EXTENSIONS, ".a;.aux;.bak;.bin;.class;.d;.elf;.exe;.gif;.icns;.jar;.jpeg;.jpg;.lib;.log;.map;.o;.obj;.orig;.pdf;.png;.ps;.pyc;.pyo;.rej;.swp;.texshop;.tiff;.toc", "Don't index");
        
        // FIXME: Evergreen and PTextArea need more work before we can change colors at run-time.
        //addSeparator();
        //addPreference(BACKGROUND_COLOR, UIManager.getColor("EditorPane.background"), "Background");
        //addPreference(FOREGROUND_COLOR, UIManager.getColor("EditorPane.foreground"), "Foreground");
    }
}
