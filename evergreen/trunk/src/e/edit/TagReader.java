package e.edit;

import java.awt.Color;
import java.io.*;
import java.util.regex.*;

public class TagReader {
    private static final Pattern TAG_LINE_PATTERN = Pattern.compile("([^\t]+)\t([^\t])+\t(\\d+);\"\t(\\w)(?:\t(.*))?");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:struct|class|enum|interface|namespace):([^\t]+).*");
    
    private TagListener listener;
    private String fileType;

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
        return tagsFile;
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
        public String typeSortOrder = "cdeECDfgimnpst";
        public String classSeparator = ".";
        
        public String identifier;
        public char type;
        public String context;
        public String containingClass;
        public int lineNumber;
        public boolean isStatic;
        
        public Tag(String identifier, int lineNumber, char type, String context, String containingClass) {
            this.identifier = identifier;
            this.lineNumber = lineNumber;
            this.type = type;
            this.context = context;
            this.containingClass = containingClass;
            
            // Recognize constructors. Using the same name as the containing
            // class is a pattern common to most languages.
            if (containingClass.equals(identifier)) {
                this.type = 'C';
            } else if (containingClass.endsWith("." + identifier)) {
                // An inner class constructor.
                this.type = 'C';
            }
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
        
        public Color visibilityColor() {
            if (context.indexOf("access:public") != -1) {
                return Color.GREEN.darker();
            } else if (context.indexOf("access:private") != -1) {
                return Color.RED.darker();
            } else if (context.indexOf("access:protected") != -1) {
                return Color.ORANGE;
            } else {
                return Color.GRAY;
            }
        }
        
        public String describe() {
            return identifier;
        }
        
        public String toString() {
            return describe();
        }
        
        public boolean isContainerType() {
            return "cgins".indexOf(type) != -1;
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
        public JavaTag(String identifier, int lineNumber, char type, String context, String containingClass) {
            super(identifier, lineNumber, type, context, containingClass);
            typeSortOrder = "pfCmci";
        }
        
        public String describe() {
            switch (type) {
                case 'c': return "class " + identifier;
                case 'C': return identifier;
                case 'f': return identifier;
                case 'i': return "interface " + identifier;
                case 'm': return identifier + "()";
                case 'p': return "package " + identifier;
                default: return identifier;
            }
        }
    }
    
    public static class RubyTag extends Tag {
        public RubyTag(String identifier, int lineNumber, char type, String context, String containingClass) {
            super(identifier, lineNumber, type, context, containingClass);
        }
        
        public String describe() {
            switch (type) {
                case 'c': return "class " + identifier;
                case 'm': return "module " + identifier;
                default: return identifier;
            }
        }
        
        public boolean isContainerType() {
            return super.isContainerType() || type == 'm';
        }
    }
    
    public static class CTag extends Tag {
        public CTag(String identifier, int lineNumber, char type, String context, String containingClass) {
            super(identifier, lineNumber, type, context, containingClass);
            classSeparator = "::";
            
            // Recognize a C++ destructor.
            if (identifier.equals("~" + containingClass)) {
                this.type = 'D';
            }
        }
        
        public String describe() {
            switch (type) {
                case 'c': return "class " + identifier;
                case 'd': return identifier + " macro";
                case 'e': return identifier;
                case 'f': return identifier + "()";
                case 'g': return "enum " + identifier;
                case 'm': return identifier;
                case 'n': return "namespace " + identifier;
                case 'p': return identifier + " prototype";
                case 's': return "struct " + identifier;
                case 't': return "typedef " + identifier;
                case 'u': return "union " + identifier;
                case 'v': return identifier;
                case 'x': return "extern " + identifier;
                default: return identifier;
            }
        }
    }
    
    public interface TagListener {
        public void tagFound(TagReader.Tag tag);
        
        public void taggingFailed(Exception ex);
    }
}
