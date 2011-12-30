package e.ptextarea;

import e.util.*;
import java.util.*;

public class FileTypePreferences extends Preferences {
    private final String filename;
    
    public FileTypePreferences(String filename) {
        this.filename = filename;
    }
    
    @Override
    protected String getPreferencesFilename() {
        return filename;
    }
    
    @Override
    protected void initPreferences() {
        // Do nothing here - we initialize our preferences in the addPreferencesForIndenter method.
    }
    
    /**
     * typeName is the name of the file type, eg "Assembler", "C++" etc.
     */
    public void addPreferencesForIndenter(String typeName, PIndenter indenter) {
        ArrayList<PIndenter.Preference> preferences = indenter.getPreferences();
        for (PIndenter.Preference pref: preferences) {
            // Use type-specific namespaces to ensure different indenters' preferences don't conflict.
            String prefKey = typeName + "." + pref.getKey();
            addPreference(typeName, prefKey, pref.getValue(), pref.getDescription());
        }
    }
}
