package e.edit;

import e.ptextarea.FileType;
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
    
    private FileType fileType;
    
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
            final String PREFIX = getClass().getName() + "-";
            file = FileUtilities.createTemporaryFile(PREFIX, ".tmp", "file containing " + label(), content);
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
     * Returns the FileType of the original file, or null if unknown.
     * That is, a patch between two Java files would return JAVA rather than PATCH.
     */
    public FileType fileType() {
        return fileType;
    }
    
    public Diffable setFileType(FileType newFileType) {
        this.fileType = newFileType;
        return this;
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
            try {
                content = StringUtilities.readFile(file);
            } catch (RuntimeException ex) {
                Log.warn("Could not read " + file, ex);
                // If the file has been deleted, then it has effectively empty content.
                // ex.getCause() might not be a FileNotFoundException.
                content = "";
            }
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
