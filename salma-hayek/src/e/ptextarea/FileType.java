package e.ptextarea;

import e.util.*;
import java.util.*;
import java.util.regex.*;

public class FileType {
    private static final Map<String, FileType> ALL_FILE_TYPES = new HashMap<>();
    
    /**
     * Call this function, preferably once, to initialize the preferences for each filetype, and load them from a file.
     */
    public static Preferences preferencesFromFile(String filename) {
        FileTypePreferences result = new FileTypePreferences(filename);
        for (FileType type: ALL_FILE_TYPES.values()) {
            try {
                PIndenter indenter = type.indenterClass.getConstructor(PTextArea.class).newInstance(new Object[] {null});
                result.addPreferencesForIndenter(type.getName(), indenter);
            } catch (Exception ex) {
                throw new RuntimeException("preferencesFromFile failed", ex);
            }
        }
        if (!filename.isEmpty()) {
            result.readFromDisk();
        }
        return result;
    }
    
    public static final FileType ASSEMBLER = new FileType("Assembler",
                 PNoOpIndenter.class,
                 PAssemblerTextStyler.class,
                 new String[] { ".s", ".S" });
    
    public static final FileType BASH = new FileType("Bash",
                 PBashIndenter.class,
                 PBashTextStyler.class,
                 new String[] { ".sh", "bash.bashrc", "bash.logout", ".bash_profile", ".bashrc", ".bash_logout" });
    
    public static final FileType C_PLUS_PLUS = new FileType("C++",
                 PCppIndenter.class,
                 PCPPTextStyler.class,
                 new String[] { ".cpp", ".hpp", ".c", ".h", ".m", ".mm", ".hh", ".cc", ".strings" });
    
    public static final FileType C_SHARP = new FileType("C#",
                 PJavaIndenter.class,
                 PCSharpTextStyler.class,
                 new String[] { ".cs" });
    
    public static final FileType EMAIL = new FileType("Email",
                 PNoOpIndenter.class,
                 PEmailTextStyler.class,
                 new String[] { ".email" });
    
    public static final FileType GO = new FileType("Go",
                 PGoIndenter.class,
                 PGoTextStyler.class,
                 new String[] { ".go" })
        .setLanguageDefinedIndentation("\t");
    
    public static final FileType JAVA = new FileType("Java",
                 PJavaIndenter.class,
                 PJavaTextStyler.class,
                 new String[] { ".java" });
    
    public static final FileType JAVA_SCRIPT = new FileType("JavaScript",
                 PNoOpIndenter.class,
                 PJavaScriptTextStyler.class,
                 new String[] { ".js" });
    
    public static final FileType KOTLIN = new FileType("Kotlin",
                 PGoIndenter.class,
                 PKotlinTextStyler.class,
                 new String[] { ".kt" })
        .setLanguageDefinedIndentation("    ");
    
    public static final FileType LUA = new FileType("Lua",
                 PLuaIndenter.class,
                 PLuaTextStyler.class,
                 new String[] { ".lua" });
    
    public static final FileType MAKE = new FileType("Make",
                 PNoOpIndenter.class,
                 PMakefileTextStyler.class,
                 new String[] { "Makefile", "GNUmakefile", "makefile", ".make", ".mk" });
    
    public static final FileType PATCH = new FileType("Patch",
                 PNoOpIndenter.class,
                 PPatchTextStyler.class,
                 new String[] { ".diff", ".patch" });
    
    public static final FileType PERL = new FileType("Perl",
                 PPerlIndenter.class,
                 PPerlTextStyler.class,
                 new String[] { ".pl", ".pm" });
    
    public static final FileType PHP = new FileType("PHP",
                 PNoOpIndenter.class,
                 PPhpTextStyler.class,
                 new String[] { ".php" });
    
    public static final FileType PLAIN_TEXT = new FileType("Plain Text",
                 PNoOpIndenter.class,
                 PPlainTextStyler.class,
                 new String[] { ".txt" });
    
    public static final FileType PROTO = new FileType("Protocol Buffer",
                 PCppIndenter.class,
                 PProtoTextStyler.class,
                 new String[] { ".proto" });
    
    public static final FileType PYTHON = new FileType("Python",
                 PPythonIndenter.class,
                 PPythonTextStyler.class,
                 new String[] { ".py", "BUILD", ".bzl" });
    
    public static final FileType RUBY = new FileType("Ruby",
                 PRubyIndenter.class,
                 PRubyTextStyler.class,
                 new String[] { ".rb" });
    
    public static final FileType RUST = new FileType("Rust",
                 PCppIndenter.class,
                 PRustTextStyler.class,
                 new String[] { ".rs" });
    
    public static final FileType VHDL = new FileType("VHDL",
                 PNoOpIndenter.class,
                 PVhdlTextStyler.class,
                 new String[] { ".vhd" });
    
    public static final FileType XML = new FileType("XML",
                 PNoOpIndenter.class,
                 PXmlTextStyler.class,
                 new String[] { ".xml", ".html", ".shtml", ".vm" });
    
    private final String name;
    private final Class<? extends PIndenter> indenterClass;
    private final Class<? extends PTextStyler> stylerClass;
    private final String[] extensions;
    private String languageDefinedIndentation;
    
    private FileType(String name, Class<? extends PIndenter> defaultIndenterClass, Class<? extends PTextStyler> defaultStylerClass, String[] extensions) {
        synchronized (ALL_FILE_TYPES) {
            if (ALL_FILE_TYPES.containsKey(name)) {
                throw new RuntimeException("Attempt to redefine FileType \"" + name + "\"");
            }
            this.name = name;
            this.indenterClass = selectClass(name + ".indenterClass", defaultIndenterClass);
            this.stylerClass = selectClass(name + ".stylerClass", defaultStylerClass);
            this.extensions = extensions;
            ALL_FILE_TYPES.put(name, this);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> selectClass(String property, Class<? extends T> defaultClass) {
        try {
            // This is unsafe.
            return (Class<? extends T>) Class.forName(System.getProperty(property));
        } catch (Exception ex) {
            return defaultClass;
        }
    }
    
    private FileType setLanguageDefinedIndentation(String indent) {
        languageDefinedIndentation = indent;
        return this;
    }
    
    // Returns whether this language has a language-defined indentation level (eg go has \t).
    public boolean hasLanguageDefinedIndentationLevel() {
        return getLanguageDefinedIndentationLevel() != null;
    }
    
    // Returns the language-defined indentation level if there is one, or null if there is not.
    public String getLanguageDefinedIndentationLevel() {
        return languageDefinedIndentation;
    }
    
    // Included only for backwards compatibility.  Use the new FileTypePreferences.
    public void configureTextArea(PTextArea textArea) {
        configureTextArea(textArea, preferencesFromFile(""));
    }
    
    public void configureTextArea(PTextArea textArea, Preferences preferences) {
        if (textArea.getFileType() == this) {
            return;
        }
        
        textArea.setFileType(this);
        textArea.setWrapStyleWord(this == FileType.PLAIN_TEXT);
        try {
            PIndenter indenter = indenterClass.getConstructor(PTextArea.class).newInstance(new Object[] {textArea});
            indenter.setPreferences(new PrefixedPreferences(preferences, getName() + "."));
            textArea.setIndenter(indenter);
            textArea.setTextStyler(stylerClass.getConstructor(PTextArea.class).newInstance(new Object[] {textArea}));
        } catch (Exception ex) {
            throw new RuntimeException("configureTextArea failed", ex);
        }
        
        textArea.repaint();
    }
    
    public String[] getKeywords() {
        try {
            final PTextStyler styler = stylerClass.getConstructor(PTextArea.class).newInstance(new Object[] {null});
            return styler.getKeywords();
        } catch (Exception ex) {
            Log.warn("FileType.getKeywords failed (for file type " + getName() + ")", ex);
            return new String[0];
        }
    }
    
    public String getName() {
        return name;
    }
    
    public static Set<String> getAllFileTypeNames() {
        TreeSet<String> allFileTypeNames = new TreeSet<>();
        for (FileType fileType : ALL_FILE_TYPES.values()) {
            allFileTypeNames.add(fileType.getName());
        }
        return allFileTypeNames;
    }
    
    public static FileType fromName(String name) {
        FileType result = ALL_FILE_TYPES.get(name);
        if (result == null) {
            throw new IllegalArgumentException("\"" + name + "\" doesn't denote a FileType");
        }
        return result;
    }
    
    /**
     * Guesses the type from the given filename and the corresponding content.
     */
    public static FileType guessFileType(String filename, CharSequence content) {
        // The whole point of emacs mode lines is that they override all other possibilities.
        FileType modeType = extractFileTypeFromModeLine(content);
        if (modeType != null) {
            return modeType;
        }
        // See if we can infer the type by name first and fall back to guessing from the content.
        // If you don't do it this way round, you get fooled by files (such as this one) that contain things that look like suggestive content.
        // It's hard to see that there's ever any excuse for having the wrong filename extension (and not having an emacs mode line).
        FileType fileType = guessFileTypeByFilename(filename);
        if (fileType == FileType.PLAIN_TEXT) {
            fileType = guessFileTypeByContent(content);
        }
        return fileType;
    }
    
    /**
     * Guesses the type from the given filename by only checking the FileTypes'
     * registered extensions. Use guessFileType if you have the content too;
     * this method's guess is based solely on the filename (no attempt will be
     * made to examine the file, should it exist).
     */
    private static FileType guessFileTypeByFilename(String filename) {
        for (FileType type : ALL_FILE_TYPES.values()) {
            for (String extension : type.extensions) {
                if (filename.endsWith(extension)) {
                    return type;
                }
            }
        }
        return FileType.PLAIN_TEXT;
    }
    
    private static FileType guessFileTypeByContent(CharSequence content) {
        // These tests come first because tests based on isInterpretedContent
        // are completely definitive.
        if (isBashContent(content)) {
            return FileType.BASH;
        } else if (isPerlContent(content)) {
            return FileType.PERL;
        } else if (isPythonContent(content)) {
            return FileType.PYTHON;
        } else if (isRubyContent(content)) {
            return FileType.RUBY;
        }
        
        // The following tests are weaker guesses. A Ruby script containing
        // "#ifdef" (because it generates C++, say) would be assumed to be C++
        // if these tests were all together and in alphabetical order.
        if (isCPlusPlusContent(content)) {
            return FileType.C_PLUS_PLUS;
        } else if (isPatchContent(content)) {
            return FileType.PATCH;
        } else if (isXmlContent(content)) {
            return FileType.XML;
        } else {
            return FileType.PLAIN_TEXT;
        }
    }
    
    /**
     * Tests whether the 'content' looks like a Unix shell script. If
     * 'interpreter' is a shell, you should probably prepend "/" to avoid
     * false positives; if 'interpreter' is a scripting language, you should
     * probably avoid doing so because it's still relatively common practice
     * to use the env(1) hack. We could perhaps automate this by matching
     * either ("/" + interpreter) or ("/env\\s[^\\n]*" + interpreter).
     */
    private static boolean isInterpretedContent(CharSequence content, String interpreter) {
        return Pattern.compile("^#![^\\n]*" + interpreter).matcher(content).find();
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Ruby variety. */
    private static boolean isRubyContent(CharSequence content) {
        return isInterpretedContent(content, "ruby");
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Perl variety. */
    private static boolean isPerlContent(CharSequence content) {
        return isInterpretedContent(content, "perl");
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Python variety. */
    private static boolean isPythonContent(CharSequence content) {
        return isInterpretedContent(content, "python");
    }
    
    /** Tests whether the 'content' looks like a Unix script, of the Bourne (Again) shell variety. */
    private static boolean isBashContent(CharSequence content) {
        return isInterpretedContent(content, "/(bash|sh)");
    }
    
    /**
     * Tests whether the 'content' looks like a C++ file.
     * A standard C++ header file (such as <string>) might not have any extension, though it's likely in that case start with #ifndef.
     */
    private static boolean isCPlusPlusContent(CharSequence content) {
        // Only bother looking at the first 1KiB.
        // If we haven't seen an #ifdef by then, and nothing else suggests C++, this probably isn't.
        final CharSequence relevantContent = content.subSequence(0, Math.min(content.length(), 1024));
        return Pattern.compile("#ifndef").matcher(relevantContent).find();
    }
    
    private static boolean isPatchContent(CharSequence content) {
        return Pattern.compile("^--- .*\n\\+\\+\\+ ").matcher(content).find();
    }
    
    /** Tests whether the 'content' looks like XML. */
    private static boolean isXmlContent(CharSequence content) {
        return Pattern.compile("(?i)^<\\?xml").matcher(content).find();
    }
    
    private static FileType extractFileTypeFromModeLine(CharSequence content) {
        // FIXME: this method has an over-simplified idea of emacs mode lines, based on a few random examples found on an Ubuntu 7.10 system.
        // FIXME: gEdit's "modelines" plug-in http://cvs.gnome.org/viewcvs/gedit/plugins/modelines/ details emacs(1), kate(1), and vim(1) mode lines.
        
        // Here are a few examples.
        // Ruby:
        // /usr/lib/ruby/1.8/yaml.rb:1:# -*- mode: ruby; ruby-indent-level: 4; tab-width: 4 -*- vim: sw=4 ts=4
        //
        // C++ ("C", "C++", and "linux-c"):
        // /usr/include/nspr/prvrsion.h:1: /* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
        // /usr/include/gdkmm-2.4/gdkmm.h:1: // This is -*- C++ -*-
        // /usr/include/nss/preenc.h:1: /* -*- Mode: C; tab-width: 4; indent-tabs-mode: nil -*- */
        // /usr/include/linux/ticable.h:1: /* Hey EMACS -*- linux-c -*-
        
        // The single ' ' after the first -*- and before the last -*- are optional. ("-*-Shell-script-*-" appears to be valid.)
        // Mode lines are case-insensitive.
        // The "mode: " is optional.
        // Emacs only looks at the first two lines.
        // FIXME: does the major mode name always come first? It seems to, but that doesn't mean anything.
        
        final int LINES_TO_CHECK = 2;
        final List<CharSequence> lines = splitLines(content, LINES_TO_CHECK);
        final Pattern modeLinePattern = Pattern.compile("(?i)-\\*- ?(?:mode: )?([^:; ]+).* ?-\\*-");
        for (CharSequence line : lines) {
            final Matcher matcher = modeLinePattern.matcher(line);
            if (matcher.find()) {
                String possibleMajorModeName = matcher.group(1);
                // Sometimes multiple Emacs major modes map to a single one of our FileTypes.
                // Other times, the Emacs major mode has a different name to our corresponding FileType.
                if (possibleMajorModeName.equalsIgnoreCase("C") || possibleMajorModeName.equalsIgnoreCase("Linux-C")) {
                    possibleMajorModeName = C_PLUS_PLUS.name;
                } else if (possibleMajorModeName.equalsIgnoreCase("Makefile")) {
                    possibleMajorModeName = MAKE.name;
                } else if (possibleMajorModeName.equalsIgnoreCase("Shell-script")) {
                    possibleMajorModeName = BASH.name;
                }
                for (FileType type : ALL_FILE_TYPES.values()) {
                    if (possibleMajorModeName.equalsIgnoreCase(type.name)) {
                        return type;
                    }
                }
            }
        }
        return null;
    }
    
    // We used to use Pattern.compile("\n").split(content, LINES_TO_CHECK + 1) but that was expensive on large files.
    // See the implementation of Pattern.split(CharSequence, int) for the full horror.
    // Since that's unlikely to change, let's reinvent a simpler wheel.
    // In particular:
    // We don't need to split with arbitrary regular expressions.
    // We're happy with empty lines.
    // We don't actually need strings; subsequences are fine.
    private static List<CharSequence> splitLines(CharSequence content, int limit) {
        final ArrayList<CharSequence> result = new ArrayList<>();
        final int length = content.length();
        int lastSplitOffset = 0;
        for (int i = 0; i < length; ++i) {
            if (content.charAt(i) == '\n') {
                result.add(content.subSequence(lastSplitOffset, i));
                if (result.size() == limit) {
                    break;
                }
                lastSplitOffset = i + 1;
            }
        }
        return result;
    }
}
