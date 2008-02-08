package e.ptextarea;

import e.util.*;
import java.util.*;
import java.util.regex.*;

public enum FileType {
    PLAIN_TEXT  ("Plain Text",
                 PNoOpIndenter.class,
                 PPlainTextStyler.class,
                 new String[] { ".txt" }),
    
    ASSEMBLER   ("Assembler",
                 PNoOpIndenter.class,
                 PAssemblerTextStyler.class,
                 new String[] { ".s", ".S" }),
    
    BASH        ("Bash",
                 PBashIndenter.class,
                 PBashTextStyler.class,
                 new String[] { ".sh", "bash.bashrc", "bash.logout", ".bash_profile", ".bashrc", ".bash_logout" }),
    
    C_PLUS_PLUS ("C++",
                 PCppIndenter.class,
                 PCPPTextStyler.class,
                 new String[] { ".cpp", ".hpp", ".c", ".h", ".m", ".mm", ".hh", ".cc", ".strings" }),
    
    C_SHARP     ("C#",
                 PJavaIndenter.class,
                 PCSharpTextStyler.class,
                 new String[] { ".cs" }),
    
    EMAIL       ("Email",
                 PNoOpIndenter.class,
                 PEmailTextStyler.class,
                 new String[] { ".email" }),
    
    JAVA        ("Java",
                 PJavaIndenter.class,
                 PJavaTextStyler.class,
                 new String[] { ".java" }),
    
    JAVA_SCRIPT ("JavaScript",
                 PNoOpIndenter.class,
                 PJavaScriptTextStyler.class,
                 new String[] { ".js" }),
    
    MAKE        ("Make",
                 PNoOpIndenter.class,
                 PMakefileTextStyler.class,
                 new String[] { "Makefile", "GNUmakefile", "makefile", ".make" }),
    
    RUBY        ("Ruby",
                 PRubyIndenter.class,
                 PRubyTextStyler.class,
                 new String[] { ".rb" }),
    
    PATCH       ("Patch",
                 PNoOpIndenter.class,
                 PPatchTextStyler.class,
                 new String[] { ".diff", ".patch" }),
    
    PBASIC      ("PBASIC",
                 PNoOpIndenter.class,
                 PPBasicTextStyler.class,
                 new String[] { ".bs1", ".bs2", ".bse", ".bsx", ".bsp", ".bpe", ".bpx" }),
    
    PERL        ("Perl",
                 PPerlIndenter.class,
                 PPerlTextStyler.class,
                 new String[] { ".pl", ".pm" }),
    
    PHP         ("PHP",
                 PNoOpIndenter.class,
                 PPhpTextStyler.class,
                 new String[] { ".php" }),
    
    PYTHON      ("Python",
                 PNoOpIndenter.class,
                 PPythonTextStyler.class,
                 new String[] { ".py" }),
    
    TALC        ("Talc",
                 PJavaIndenter.class,
                 PTalcTextStyler.class,
                 new String[] { ".talc" }),
    
    VHDL        ("VHDL",
                 PNoOpIndenter.class,
                 PVhdlTextStyler.class,
                 new String[] { ".vhd" }),
    
    XML         ("XML",
                 PNoOpIndenter.class,
                 PXmlTextStyler.class,
                 new String[] { ".xml", ".html", ".shtml", ".vm" });
    
    private final String name;
    private final Class<? extends PIndenter> indenterClass;
    private final Class<? extends PTextStyler> stylerClass;
    private final String[] extensions;
    
    private FileType(String name, Class<? extends PIndenter> indenterClass, Class<? extends PTextStyler> stylerClass, String[] extensions) {
        this.name = name;
        this.indenterClass = indenterClass;
        this.stylerClass = stylerClass;
        this.extensions = extensions;
    }
    
    public void configureTextArea(PTextArea textArea) {
        if (textArea.getFileType() == this) {
            return;
        }
        
        textArea.setFileType(this);
        textArea.setWrapStyleWord(this == FileType.PLAIN_TEXT);
        try {
            textArea.setIndenter(indenterClass.getConstructor(PTextArea.class).newInstance(textArea));
            textArea.setTextStyler(stylerClass.getConstructor(PTextArea.class).newInstance(textArea));
        } catch (Exception ex) {
            throw new RuntimeException("configureTextArea failed", ex);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public static Set<String> getAllFileTypeNames() {
        TreeSet<String> allFileTypeNames = new TreeSet<String>();
        for (FileType fileType : EnumSet.allOf(FileType.class)) {
            allFileTypeNames.add(fileType.getName());
        }
        return allFileTypeNames;
    }
    
    public static FileType fromName(String name) {
        for (FileType fileType : EnumSet.allOf(FileType.class)) {
            if (fileType.getName().equals(name)) {
                return fileType;
            }
        }
        throw new IllegalArgumentException("\"" + name + "\" doesn't denote a FileType");
    }
    
    public static FileType guessFileType(String filename, CharSequence content) {
        // The whole point of emacs mode lines is that they override all other possibilities.
        FileType modeType = extractFileTypeFromModeLine(content);
        if (modeType != null) {
            return modeType;
        }
        // See if we can infer the type by name first and fall back to guessing from the content.
        // If you don't do it this way round, you get fooled by files (such as this one) that contain things that look like suggestive content.
        // It's hard to see that there's ever any excuse for having the wrong filename extension (and not having an emacs mode line).
        FileType fileType = guessFileTypeByName(filename);
        if (fileType == FileType.PLAIN_TEXT) {
            fileType = guessFileTypeByContent(content);
        }
        return fileType;
    }
    
    /**
     * Guesses the type from the given filename by checking the FileTypes' registered extensions.
     */
    private static FileType guessFileTypeByName(String filename) {
        for (FileType type : EnumSet.allOf(FileType.class)) {
            for (String extension : type.extensions) {
                if (filename.endsWith(extension)) {
                    return type;
                }
            }
        }
        return FileType.PLAIN_TEXT;
    }
    
    private static FileType guessFileTypeByContent(CharSequence content) {
        if (isBashContent(content)) {
            return FileType.BASH;
        } else if (isCPlusPlusContent(content)) {
            return FileType.C_PLUS_PLUS;
        } else if (isPatchContent(content)) {
            return FileType.PATCH;
        } else if (isPerlContent(content)) {
            return FileType.PERL;
        } else if (isPythonContent(content)) {
            return FileType.PYTHON;
        } else if (isRubyContent(content)) {
            return FileType.RUBY;
        } else if (isInterpretedContent(content, "talc")) {
            return FileType.TALC;
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
        return Pattern.compile("#ifndef").matcher(content).find();
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
        final int LINES_TO_CHECK = 2;
        // FIXME: does the major mode name always come first? It seems to, but that doesn't mean anything.
        Pattern modeLinePattern = Pattern.compile("(?i)-\\*- ?(?:mode: )?([^:; ]+).* ?-\\*-");
        String[] lines = Pattern.compile("\n").split(content, LINES_TO_CHECK + 1);
        for (int i = 0; i < Math.min(lines.length, LINES_TO_CHECK); ++i) {
            Matcher matcher = modeLinePattern.matcher(lines[i]);
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
                for (FileType type : EnumSet.allOf(FileType.class)) {
                    if (possibleMajorModeName.equalsIgnoreCase(type.name)) {
                        return type;
                    }
                }
            }
        }
        return null;
    }
}
