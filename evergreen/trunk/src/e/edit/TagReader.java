package e.edit;

import java.awt.Color;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import e.util.*;

public class TagReader {
    private static final Pattern TAG_LINE_PATTERN = Pattern.compile("([^\t]+)\t([^\t])+\t(\\d+);\"\t(\\w)(?:\t(.*))?");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:struct|class|enum|interface|namespace):([^\t]+).*");
    
    private TagListener listener;
    private String fileType;
    private String digest;

    public TagReader(File file, String fileType, TagListener tagListener) {
        this.listener = tagListener;
        this.fileType = fileType;
        
        try {
            File tagsFile = createTagsFileFor(file);
            readTagsFile(tagsFile);
            tagsFile.delete();
        } catch (Exception ex) {
            listener.taggingFailed(ex);
        }
    }
    
    private File createTagsFileFor(File file) throws InterruptedException, IOException {
        File tagsFile = File.createTempFile("e.util.TagReader-tags-", ".tags");
        tagsFile.deleteOnExit();
        Process p = Runtime.getRuntime().exec(new String[] {
            "ctags",
            "--c++-types=+p", "-n", "--fields=+a", "-u",
            "--regex-java=/( static )/\1/S/",
            "--regex-c++=/( static )/\1/S/",
            "-f", tagsFile.getAbsolutePath(),
            file.getAbsolutePath()
        });
        p.waitFor();
        digest = FileUtilities.md5(tagsFile);
        return tagsFile;
    }
    
    public String getTagsDigest() {
        return digest;
    }
    
    private void readTagsFile(File tagsFile) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(tagsFile)));
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
        if (fileType == ETextWindow.JAVA) {
            tag = new JavaTag(identifier, lineNumber, type, context, containingClass);
        } else if (fileType == ETextWindow.C_PLUS_PLUS) {
            tag = new CTag(identifier, lineNumber, type, context, containingClass);
        } else if (fileType == ETextWindow.RUBY) {
            tag = new RubyTag(identifier, lineNumber, type, context, containingClass);
        } else {
            tag = new TagReader.Tag(identifier, lineNumber, type, context, containingClass);
        }
        
        tag.isStatic = (lineNumber == staticTagLineNumber);
        staticTagLineNumber = 0;
        
        listener.tagFound(tag);
    }
    
    public static class Tag {
        public static final String CLASS = "class";
        public static final String CONSTRUCTOR = "constructor";
        public static final String DESTRUCTOR = "destructor";
        public static final String INTERFACE = "interface";
        public static final String FIELD = "field";
        public static final String METHOD = "method";
        public static final String MACRO = "macro";
        public static final String MODULE = "module";
        public static final String ENUMERATOR = "enumerator";
        public static final String ENUM = "enum";
        public static final String NAMESPACE = "namespace";
        public static final String PACKAGE = "package";
        public static final String PROTOTYPE = "prototype";
        public static final String STRUCT = "struct";
        public static final String TYPEDEF = "typedef";
        public static final String UNION = "union";
        public static final String VARIABLE = "variable";
        public static final String EXTERN = "extern";
        
        private static final Map TYPES = new HashMap();
        static {
            TYPES.put("c", CLASS);
            TYPES.put("C", CONSTRUCTOR);
            TYPES.put("D", DESTRUCTOR);
            TYPES.put("i", INTERFACE);
            TYPES.put("f", FIELD);
            TYPES.put("m", METHOD);
            TYPES.put("M", MODULE);
            TYPES.put("d", MACRO);
            TYPES.put("e", ENUMERATOR);
            TYPES.put("g", ENUM);
            TYPES.put("n", NAMESPACE);
            TYPES.put("p", PACKAGE);
            TYPES.put("P", PROTOTYPE);
            TYPES.put("s", STRUCT);
            TYPES.put("t", TYPEDEF);
            TYPES.put("u", UNION);
            TYPES.put("v", VARIABLE);
            TYPES.put("x", EXTERN);
        }
        
        public static final List CONTAINER_TYPES = Collections.unmodifiableList(Arrays.asList(new String[] {
            CLASS, ENUM, INTERFACE, NAMESPACE, STRUCT, MODULE
        }));
        
        private static final Map DESCRIPTION_FORMATS = new HashMap();
        static {
            DESCRIPTION_FORMATS.put(PACKAGE, new MessageFormat(PACKAGE + " {0}"));
            DESCRIPTION_FORMATS.put(CLASS, new MessageFormat(CLASS + " {0}"));
            DESCRIPTION_FORMATS.put(INTERFACE, new MessageFormat(INTERFACE + " {0}"));
            DESCRIPTION_FORMATS.put(METHOD, new MessageFormat("{0}()"));
            DESCRIPTION_FORMATS.put(CONSTRUCTOR, new MessageFormat("{0}()"));
            DESCRIPTION_FORMATS.put(DESTRUCTOR, new MessageFormat("{0}()"));
            DESCRIPTION_FORMATS.put(MODULE, new MessageFormat(MODULE + " {0}"));
            DESCRIPTION_FORMATS.put(ENUM, new MessageFormat(ENUM + " {0}"));
            DESCRIPTION_FORMATS.put(NAMESPACE, new MessageFormat(NAMESPACE + " {0}"));
            DESCRIPTION_FORMATS.put(PROTOTYPE, new MessageFormat("{0} " + PROTOTYPE));
            DESCRIPTION_FORMATS.put(STRUCT, new MessageFormat(STRUCT + " {0}"));
            DESCRIPTION_FORMATS.put(TYPEDEF, new MessageFormat(TYPEDEF + " {0}"));
            DESCRIPTION_FORMATS.put(UNION, new MessageFormat(UNION + " {0}"));
            DESCRIPTION_FORMATS.put(EXTERN, new MessageFormat(EXTERN + " {0}"));
        }
        
        public List typeSortOrder = Collections.unmodifiableList(Arrays.asList(new String[] {
            NAMESPACE, CLASS, MACRO, CONSTRUCTOR, DESTRUCTOR,
            PROTOTYPE, METHOD, FIELD, ENUM, STRUCT, TYPEDEF
        }));
        public String classSeparator = ".";
        
        public String identifier;
        public String type;
        public String context;
        public String containingClass;
        public int lineNumber;
        public boolean isStatic;
        
        public Tag(String identifier, int lineNumber, char tagType, String context, String containingClass) {
            this.identifier = identifier;
            this.lineNumber = lineNumber;
            this.context = context;
            this.containingClass = containingClass;
            
            // Recognize constructors. Using the same name as the containing
            // class is a pattern common to most languages.
            if (containingClass.equals(identifier)) {
                tagType = 'C';
            } else if (containingClass.endsWith("." + identifier)) {
                // An inner class constructor.
                tagType = 'C';
            }
            this.type = (String) TYPES.get(String.valueOf(tagType));
        }
        
        public String describeVisibility() {
            if (context.indexOf("access:public") != -1) {
                return "+";
            } else if (context.indexOf("access:private") != -1) {
                return "-";
            } else if (context.indexOf("access:protected") != -1) {
                return "#";
            } else {
                return "?";
            }
        }
        
        public static final Color PUBLIC = Color.GREEN.darker();
        public static final Color PROTECTED = Color.ORANGE;
        public static final Color PRIVATE = new Color(255, 140, 140);
        
        public Color visibilityColor() {
            if (context.indexOf("access:public") != -1) {
                return PUBLIC;
            } else if (context.indexOf("access:private") != -1) {
                return PRIVATE;
            } else if (context.indexOf("access:protected") != -1) {
                return PROTECTED;
            } else {
                return Color.GRAY;
            }
        }
        
        public String describe() {
            MessageFormat formatter = (MessageFormat) DESCRIPTION_FORMATS.get(type);
            if (formatter != null) {
                return formatter.format(new String[] { identifier });
            } else {
                return identifier;
            }
        }
        
        public String toString() {
            return describe();
        }
        
        public boolean isContainerType() {
            return CONTAINER_TYPES.contains(type);
        }
        
        public int getTypeSortIndex() {
            return typeSortOrder.indexOf(type);
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
            typeSortOrder = Collections.unmodifiableList(Arrays.asList(new String[] {
                PACKAGE, FIELD, CONSTRUCTOR, METHOD, CLASS, INTERFACE
            }));
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
    
    public static class CTag extends Tag {
        public CTag(final String identifier, final int lineNumber, final char tagType, final String context, final String containingClass) {
            super(identifier, lineNumber, fixType(tagType), context, containingClass);
            classSeparator = "::";
            
            // Recognize a C++ destructor.
            if (identifier.equals("~" + containingClass)) {
                this.type = DESTRUCTOR;
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
    }
    
    public interface TagListener {
        public void tagFound(TagReader.Tag tag);
        
        public void taggingFailed(Exception ex);
    }
}
