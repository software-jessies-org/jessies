package e.edit;

import e.ptextarea.FileType;
import e.util.*;
import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class TagReader {
    private static final Pattern TAG_LINE_PATTERN = Pattern.compile("([^\t]+)\t([^\t])+\t(\\d+);\"\t(\\w)(?:\t(.*))?");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:struct|class|enum|interface|namespace):([^\t]+).*");
    
    private TagListener listener;
    private FileType fileType;
    private String charsetName;
    private String digest;
    
    public TagReader(File file, FileType fileType, String charsetName, String oldDigest, TagListener tagListener) {
        this.listener = tagListener;
        this.fileType = fileType;
        this.charsetName = charsetName;
        
        File tagsFile = null;
        try {
            tagsFile = createTagsFileFor(file);
            if (oldDigest == null || oldDigest.equals(digest) == false) {
                readTagsFile(tagsFile);
            }
        } catch (Exception ex) {
            listener.taggingFailed(ex);
        } finally {
            if (tagsFile != null) {
                tagsFile.delete();
            }
        }
    }
    
    private File createTagsFileFor(File file) throws InterruptedException, IOException {
        File tagsFile = File.createTempFile("e.edit.TagReader-tags-", ".tags");
        tagsFile.deleteOnExit();
        
        ArrayList<String> errors = new ArrayList<String>();
        ProcessUtilities.backQuote(tagsFile.getParentFile(), new String[] {
            "ctags",
            "--c++-types=+p", "-n", "--fields=+am", "-u",
            "--regex-java=/(\\bstatic\\b)/\1/S/",
            "--regex-c++=/(\\bstatic\\b)/\1/S/",
            "-f", tagsFile.getAbsolutePath(),
            file.getAbsolutePath()
        }, errors, errors);
        // We're not actually expecting anything on stdout or stderr from ctags.
        // All the more reason to output anything it has to say!
        for (String error : errors) {
            Log.warn("ctags: " + error);
        }
        
        digest = FileUtilities.md5(tagsFile);
        return tagsFile;
    }
    
    public String getTagsDigest() {
        return digest;
    }
    
    private void readTagsFile(File tagsFile) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(tagsFile), charsetName));
            String line;
            boolean foundValidHeader = false;
            while ((line = reader.readLine()) != null && line.startsWith("!_TAG_")) {
                foundValidHeader = true;
                //TODO: check the tags file is sorted? of a suitable version?
            }
            if (foundValidHeader == false) {
                throw new RuntimeException("The tags file didn't have a valid header.");
            }

            if (line != null) {
                do {
                    processTagLine(line);
                } while ((line = reader.readLine()) != null);
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                // What can we do? Nothing.
                ex = ex;
            }
        }
    }
    
    /**
     * Set to the number of the last line containing the pattern " static ".
     * If the next tag seen has the same line number, it will be marked as static.
     */
    private int staticTagLineNumber = 0;
    
    private void processTagLine(String line) {
        // The format is: <identifier>\t<filename>\t<line>;"\t<tag type>[\t<context>]
        // For example:
        // A   headers/openssl/bn.h    257;"   m   struct:bn_blinding_st
        // Here the tag is called "A", is in "headers/openssl/bn.h" on line 257, and
        // is a 'm'ember of the struct called "bn_blinding_st".
        Matcher matcher = TAG_LINE_PATTERN.matcher(line);
        if (matcher.matches() == false) {
            return;
        }
        
        String identifier = matcher.group(1);
        String filename = matcher.group(2);
        int lineNumber = Integer.parseInt(matcher.group(3));
        char type = matcher.group(4).charAt(0);
        String context = matcher.group(5);
        if (context == null) {
            context = "";
        }
        
        if (type == 'S') {
            staticTagLineNumber = lineNumber;
            return;
        }
        
        Matcher classMatcher = CLASS_PATTERN.matcher(context);
        String containingClass = (classMatcher.matches() ? classMatcher.group(1) : "");
        //Log.warn(context + " => " + containingClass);
        
        TagReader.Tag tag = null;
        if (fileType == FileType.JAVA) {
            tag = new JavaTag(identifier, lineNumber, type, context, containingClass);
        } else if (fileType == FileType.C_PLUS_PLUS) {
            tag = new CTag(identifier, lineNumber, type, context, containingClass);
        } else if (fileType == FileType.PERL) {
            tag = new PerlTag(identifier, lineNumber, type, context, containingClass);
        } else if (fileType == FileType.RUBY) {
            tag = new RubyTag(identifier, lineNumber, type, context, containingClass);
        } else {
            tag = new TagReader.Tag(identifier, lineNumber, type, context, containingClass);
        }
        
        tag.isStatic = (lineNumber == staticTagLineNumber | tag.isStatic);
        staticTagLineNumber = 0;
        
        listener.tagFound(tag);
    }
    
    public static class Tag {
        public TagType[][] typeSortOrder = new TagType[][] {
            { TagType.NAMESPACE },
            { TagType.CLASS },
            { TagType.MACRO },
            { TagType.CONSTRUCTOR },
            { TagType.DESTRUCTOR },
            { TagType.PROTOTYPE, TagType.METHOD },
            { TagType.FIELD },
            { TagType.ENUM },
            { TagType.STRUCT },
            { TagType.TYPEDEF }
        };
        public String classSeparator = ".";
        
        public String identifier;
        public TagType type;
        public String context;
        public String containingClass;
        public int lineNumber;
        public String toolTip;
        public boolean isStatic;
        public boolean isAbstract;
        public boolean isPrototype;
        
        public Tag(String identifier, int lineNumber, char tagType, String context, String containingClass) {
            this.identifier = identifier;
            this.lineNumber = lineNumber;
            this.context = context;
            this.containingClass = containingClass;
            
            this.isAbstract = context.contains("implementation:abstract");
            this.isPrototype = (TagType.fromChar(tagType) == TagType.PROTOTYPE);
            
            // Recognize constructors. Using the same name as the containing
            // class is a pattern common to most languages.
            if (containingClass.equals(identifier)) {
                tagType = 'C';
            }
            this.type = TagType.fromChar(tagType);
        }
        
        public static final Color PUBLIC = Color.GREEN.darker();
        public static final Color PROTECTED = Color.ORANGE;
        public static final Color PRIVATE = new Color(255, 140, 140);
        
        public Color visibilityColor() {
            if (context.contains("access:public")) {
                return PUBLIC;
            } else if (context.contains("access:private")) {
                return PRIVATE;
            } else if (context.contains("access:protected")) {
                return PROTECTED;
            } else {
                return Color.GRAY;
            }
        }
        
        public String describe() {
            return type.describe(identifier);
        }
        
        public String toString() {
            return describe();
        }
        
        public String getSortIdentifier() {
            for (int i = 0; i < typeSortOrder.length; i++) {
                for (int j = 0; j < typeSortOrder[i].length; j++) {
                    if (typeSortOrder[i][j].equals(type)) {
                        return i + identifier + j;
                    }
                }
            }
            return identifier;
        }
        
        public String getClassQualifiedName() {
            if (containingClass.length() > 0) {
               return containingClass + classSeparator + identifier;
            } else {
                return identifier;
            }
        }
    }
    
    public static class JavaTag extends Tag {
        public JavaTag(String identifier, int lineNumber, char tagType, String context, String containingClass) {
            super(identifier, lineNumber, tagType, context, containingClass);
            typeSortOrder = new TagType[][] {
                { TagType.PACKAGE }, { TagType.FIELD }, { TagType.CONSTRUCTOR },
                { TagType.METHOD }, { TagType.CLASS }, { TagType.INTERFACE }
            };
            
            // Mark interfaces as "abstract" so they're rendered differently.
            if (type == TagType.INTERFACE) {
                this.isAbstract = true;
            }
            
            if (containingClass.endsWith("." + identifier)) {
                // An inner class constructor.
                this.type = TagType.CONSTRUCTOR;
            }
        }
    }
    
    public static class RubyTag extends Tag {
        public RubyTag(String identifier, int lineNumber, char tagType, String context, String containingClass) {
            super(identifier, lineNumber, fixType(tagType), context, containingClass);
        }
        
        private static char fixType(char type) {
            switch (type) {
                case 'm': return 'M'; // Module, not method.
                default: return type;
            }
        }
    }
    
    public static class PerlTag extends Tag {
        public PerlTag(String identifier, int lineNumber, char tagType, String context, String containingClass) {
            super(identifier, lineNumber, fixType(tagType), context, containingClass);
        }
        
        private static char fixType(char type) {
            switch (type) {
                case 's': return 'm'; // Method (subroutine), not struct.
                default: return type;
            }
        }
    }
    
    public static class CTag extends Tag {
        public CTag(final String identifier, final int lineNumber, final char tagType, final String context, final String containingClass) {
            super(identifier, lineNumber, fixType(tagType), context, containingClass);
            classSeparator = "::";
            
            if (identifier.charAt(0) == '~') {
                this.type = TagType.DESTRUCTOR;
            } else if (containingClass.endsWith("::" + identifier)) {
                // A constructor in a namespace.
                this.type = TagType.CONSTRUCTOR;
            }
            
            // Mark prototypes as "abstract" so they're rendered differently.
            if (this.isPrototype) {
                this.isAbstract = true;
            }
        }
        
        /**
         * The tag type needs to be converted to one of our canonical types,
         * to work around ectags' liberal re-use of type tags. In C++ mode,
         * for example, 'p' means function prototype, in Java it means package.
         * It also means "paragraph", "port", "procedure", "program" and
         * "property". Pascal, Sql and Tcl programmers beware!
         * The likely best work-around for this would be to submit a
         * patch to ectags so that it at least records in its tags
         * file which mode it was in.
         */
        private static char fixType(char type) {
            switch (type) {
                case 'f': return 'm'; // Function -> Method.
                case 'm': return 'f'; // Member (data) -> Field.
                case 'p': return 'P'; // Prototype, not package.
                default: return type;
            }
        }
        
        public Color visibilityColor() {
            if (isStatic && containingClass.length() == 0) {
                return PRIVATE;
            }
            return super.visibilityColor();
        }
    }
    
    public interface TagListener {
        public void tagFound(TagReader.Tag tag);
        
        public void taggingFailed(Exception ex);
    }
}
