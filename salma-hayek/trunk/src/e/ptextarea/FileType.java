package e.ptextarea;

import e.util.*;
import java.util.*;
import java.util.regex.*;

public enum FileType {
    PLAIN_TEXT  ("Plain Text", PNoOpIndenter.class,      PPlainTextStyler.class),
    ASSEMBLER   ("Assembler",  PNoOpIndenter.class,      PAssemblerTextStyler.class),
    BASH        ("Bash",       PNoOpIndenter.class,      PBashTextStyler.class),
    C_PLUS_PLUS ("C++",        PCppIndenter.class,       PCPPTextStyler.class),
    JAVA        ("Java",       PJavaIndenter.class,      PJavaTextStyler.class),
    MAKE        ("Make",       PNoOpIndenter.class,      PMakefileTextStyler.class),
    RUBY        ("Ruby",       PRubyIndenter.class,      PRubyTextStyler.class),
    PERL        ("Perl",       PPerlIndenter.class,      PPerlTextStyler.class),
    PYTHON      ("Python",     PNoOpIndenter.class,      PPythonTextStyler.class),
    VHDL        ("VHDL",       PNoOpIndenter.class,      PVhdlTextStyler.class),
    XML         ("XML",        PNoOpIndenter.class,      PXmlTextStyler.class);
    
    private final String name;
    private final Class<? extends PIndenter> indenterClass;
    private final Class<? extends PTextStyler> stylerClass;
    
    private FileType(String name, Class<? extends PIndenter> indenterClass, Class<? extends PTextStyler> stylerClass) {
        this.name = name;
        this.indenterClass = indenterClass;
        this.stylerClass = stylerClass;
    }
    
    public void configureTextArea(PTextArea textArea) {
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
    
    /**
     * There are three main ways to guess a file's type: by content, by name,
     * or by emacs mode string. The initFileType methods implement this, though
     * there's no uniform checking for emacs mode strings (the perlrun man page,
     * for example, mentions "#!/bin/sh -- # -*- perl -*- -p" but we wouldn't
     * currently recognize such a script as a Perl script, even though we would
     * recognize a C++ files by its emacs mode string).
     */
    public static FileType guessFileType(String filename, CharSequence content) {
        // See if we can infer the type by name first and fall back to guessing
        // from the content. If you don't do it this way round, you get fooled
        // by files (such as this one) that contain things that look like
        // suggestive content. It's hard to see that there's ever any excuse
        // for having the wrong filename extension.
        FileType fileType = guessFileTypeByName(filename);
        if (fileType == FileType.PLAIN_TEXT) {
            fileType = guessFileTypeByContent(content);
        }
        return fileType;
    }
    
    private static FileType guessFileTypeByName(String filename) {
        if (filename.endsWith(".java")) {
            return FileType.JAVA;
        } else if (filename.endsWith(".cpp") || filename.endsWith(".hpp") || filename.endsWith(".c") || filename.endsWith(".h") || filename.endsWith(".m") || filename.endsWith(".mm") || filename.endsWith(".hh") || filename.endsWith(".cc") || filename.endsWith(".strings")) {
            return FileType.C_PLUS_PLUS;
        } else if (filename.endsWith(".pl") || filename.endsWith(".pm")) {
            return FileType.PERL;
        } else if (filename.endsWith(".py")) {
            return FileType.PYTHON;
        } else if (filename.endsWith(".rb")) {
            return FileType.RUBY;
        } else if (filename.endsWith(".s") || filename.endsWith(".S")) {
            return FileType.ASSEMBLER;
        } else if (filename.endsWith(".sh") || filename.endsWith("bash.bashrc") || filename.endsWith("bash.logout") || filename.endsWith(".bash_profile") || filename.endsWith(".bashrc") || filename.endsWith(".bash_logout")) {
            return FileType.BASH;
        } else if (filename.endsWith("Makefile") || filename.endsWith("GNUmakefile") || filename.endsWith("makefile") || filename.endsWith(".make")) {
            return FileType.MAKE;
        } else if (filename.endsWith(".vhd")) {
            return FileType.VHDL;
        } else if (filename.endsWith(".xml") || filename.endsWith(".html") || filename.endsWith(".shtml") || filename.endsWith(".vm")) {
            return FileType.XML;
        } else {
            return FileType.PLAIN_TEXT;
        }
    }
    
    private static FileType guessFileTypeByContent(CharSequence content) {
        if (isRubyContent(content)) {
            return FileType.RUBY;
        } else if (isBashContent(content)) {
            return FileType.BASH;
        } else if (isCPlusPlusContent(content)) {
            return FileType.C_PLUS_PLUS;
        } else if (isPerlContent(content)) {
            return FileType.PERL;
        } else if (isPythonContent(content)) {
            return FileType.PYTHON;
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
     * 
     * A standard C++ header file (such as <string>) might not have any extension,
     * though it's likely in that case start with #ifndef.
     * 
     * GNU headers tend to have an emacs mode hint, so let's obey those too (I think
     * emacs scans the whole file, but GNU headers seem to use the first line).
     */
    private static boolean isCPlusPlusContent(CharSequence content) {
        // FIXME: this method has an over-simplified idea of emacs mode strings, based on the STL header file usage.
        // Here are two examples. The former shows that we could use a similar check for Ruby; the latter that we ought to recognize another form, and that we can also potentially find the tab string this way.
        // hydrogen:/usr/lib/ruby/1.8$ grep -n -- '-\*-' *
        // getoptlong.rb:1:#                                                         -*- Ruby -*-
        // yaml.rb:1:# -*- mode: ruby; ruby-indent-level: 4; tab-width: 4 -*- vim: sw=4 ts=4
        // We should also probably recognize plain C (since our C_PLUS_PLUS means C/C++/Objective-C/Objective-C++):
        // powerpc-darwin8.0/dl.h:1:/* -*- C -*-
        // FIXME: emacs mode strings should be handled separately, and override content-based file type determination.
        // FIXME: gEdit's "modelines" plug-in http://cvs.gnome.org/viewcvs/gedit/plugins/modelines/ details emacs(1), kate(1), and vim(1) mode lines.
        return Pattern.compile("(#ifndef|" + StringUtilities.regularExpressionFromLiteral("-*- C++ -*-") + ")").matcher(content).find();
    }
    
    /** Tests whether the 'content' looks like XML. */
    private static boolean isXmlContent(CharSequence content) {
        return Pattern.compile("(?i)^<\\?xml").matcher(content).find();
    }
}
