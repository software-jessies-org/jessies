package e.edit;

import e.util.*;
import java.io.*;

/**
 * Input for SimplePatchDialog. Allows callers to simply supply what they have. Temporary files will be created if necessary.
 */
public class Diffable {
    private final String label;
    
    private File file;
    private boolean isTemporaryFile;
    
    private String content;
    
    // If anyone actually had both the content and a file containing it, it would be more efficient for them to use this.
    private Diffable(String label, File file, String content) {
        this.label = label;
        this.file = file;
        this.isTemporaryFile = false;
        this.content = content;
    }
    
    /**
     * Creates a Diffable for the on-disk file 'file'.
     * The label will be used to describe this diffable, and will typically be the result of file.toString().
     */
    public Diffable(String label, File file) {
        this(label, file, null);
    }
    
    /**
     * Creates a Diffable for the content 'content' that isn't already available on disk.
     * The label will be used to describe this diffable.
     * A temporary file will be created if file() or filename() are called, and dispose() should be used to tidy up.
     */
    public Diffable(String label, String content) {
        this(label, null, content);
    }
    
    /**
     * Returns the file provided to the constructor, or a temporary file containing the content provided to the constructor.
     */
    public File file() {
        if (file == null) {
            final String PREFIX = getClass().toString() + "-";
            file = FileUtilities.fileFromString(FileUtilities.createTemporaryFile(PREFIX, "file containing " + label(), content));
            isTemporaryFile = true;
        }
        return file;
    }
    
    /**
     * Returns the path to the result of file() for convenience.
     */
    public String filename() {
        return file().toString();
    }
    
    /**
     * Returns the label to be used to describe this Diffable.
     * Do not use filename() in UI because you don't know whether you're dealing with a user-visible file or a temporary file.
     */
    public String label() {
        return label;
    }
    
    /**
     * Returns the content provided to the constructor, or reads it from the file passed to the constructor.
     */
    public String content() {
        if (content == null) {
            content = StringUtilities.readFile(file);
        }
        return content;
    }
    
    /**
     * Tidies up any temporary files.
     * The consumer of a Diffable should call this.
     */
    public void dispose() {
        if (file != null && isTemporaryFile) {
            file.delete();
        }
    }
}
