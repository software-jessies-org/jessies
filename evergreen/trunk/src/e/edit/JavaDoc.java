package e.edit;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import e.util.*;

/**
Finds and shows Javadoc documentation corresponding to the selected class, method or field.

FIXME: parts of this class are pretty much vestigial; most of the methods in here should probably be in a class called e.util.ReflectionUtilities or something.

*/
public class JavaDoc {
    /** Holds the name of every package we know of. */
    private static ArrayList packageNames = new ArrayList();
    
    /** Places to look for javadoc. */
    private static String[] docs = new String[0];
    
    /** Places to look for source code. */
    private static String[] sources = new String[0];
    
    /** Cached javadoc locations for classes we've already seen. */
    private static HashMap docLinks = new HashMap();
    
    /** Cached source code locations for classes we've already seen. */
    private static HashMap sourceLinks = new HashMap();
    
    /** Tells us if the text around the caret is a genuine classname or not. */
    private static URLClassLoader classLoader;
    
    static {
        long start = System.currentTimeMillis();
        Log.warn("Collecting JavaDoc...");
        
        /**
        * Find all the packages that are on Edit's classpath. All classes in these packages can
        * be loaded by the system classloader without any help.
        */
        Package[] ps = Package.getPackages();
        for (int i = 0; i < ps.length; i++) {
            packageNames.add(ps[i].getName());
        }
        
        // Add the default package, and also permit looking for a fully-qualified class name.
        packageNames.add("");
        int systemPkgs= packageNames.size();
        
        /**
        * Find all the packages specified by the "java.advisor.classpath" property.
        */
        String[] advisorClasspath = FileUtilities.getArrayOfPathElements(Parameters.getParameter("java.advisor.classpath", ""));
        ArrayList urls = new ArrayList();
        for (int i = 0; i < advisorClasspath.length; i++) {
            if (advisorClasspath[i].length() > 0) {
                File classpath = FileUtilities.fileFromString((String) advisorClasspath[i]);
                try {
                    urls.add(classpath.toURL());
                    findPackagesIn(classpath);
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        classLoader = new URLClassLoader((URL []) urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
        
        // Note the locations of documents and source files.
        docs = FileUtilities.getArrayOfPathElements(Parameters.getParameter("java.advisor.doc", ""));
        sources = FileUtilities.getArrayOfPathElements(Parameters.getParameter("java.advisor.source", ""));
        
        int totalPkgs = packageNames.size();
        long timeTaken = System.currentTimeMillis() - start;
        Log.warn(totalPkgs + " packages total (" + systemPkgs + " system classpath, " + (totalPkgs - systemPkgs) + " advisor.classpath) in " + timeTaken + "ms.");
    }
    
    /**
    * Registers all the packages in a classpath element, either by scanning a jar file or
    * by finding the names of all interesting subdirectories..
    */
    public static void findPackagesIn(File classpath) {
        String path = classpath.getAbsolutePath();
        if (path.endsWith(".jar")) {
            addPackagesInJarFile(classpath);
        } else {
            addPackagesInDirectory(classpath, path);
        }
    }
    
    /**
    * Finds all the directories below the starting directory that haven't been marked as uninteresting,
    * and adds their names to the list of known packages.
    */
    public static void addPackagesInDirectory(File startingDirectory, String classpath) {
        if (startingDirectory.isDirectory() == false) { return; }
        File[] files = startingDirectory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory() && FileUtilities.isIgnoredDirectory(files[i]) == false) {
                String packageName = files[i].getAbsolutePath().substring(classpath.length() + 1).replace(File.separatorChar, '.');
//                System.err.println(packageName);
                packageNames.add(packageName);
                addPackagesInDirectory(files[i], classpath);
            }
        }
    }
    
    /**
    * Learns the names of all the packages in a jar file.
    */
    public static void addPackagesInJarFile(File jarFile) {
        System.err.println("Scanning " + jarFile);
        try {
            JarFile jar = new JarFile(jarFile);
            for (Enumeration e = jar.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String name = entry.getName().replace('/', '.');
                if (name.startsWith("META-INF")) {
                    continue;
                }
                if (name.endsWith(".")) {
                    name = name.substring(0, name.length() - 1);
                }
                if (entry.isDirectory() && packageNames.contains(name) == false) {
                    packageNames.add(name);
                    System.err.println("Found package " + name);
                } else {
                    // Some jar files seem to have only files in them, and no directory entries.
                    int i = entry.getName().lastIndexOf("/");
                    if (i != -1) {
                        String pkg = name.substring(0, i).replace('/', '.');
                        if (packageNames.contains(pkg) == false) {
                            packageNames.add(pkg);
                            System.err.println("Found package " + pkg);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.err.println("Finished scanning " + jarFile);
    }
    
    /**
    * Returns the location of the documentation for a class.
    */
    public static String getDocLink(Class c) {
        String className = c.getName();
        if (c.isPrimitive()) {
            return className;
        } else {
            int i = className.lastIndexOf(".");
            String pkg = className.substring(0, i);
            String name = className.substring(i);
            return getDocLink(name, pkg, true);
        }
    }
    
    /** Used to choose between source or javadoc links. */
    private static final int DOC = 0;
    private static final int SRC = 1;
    
    /**
    * Returns the location of the documentation for a class.
    */
    public static String getDocLink(String className, String pkg, boolean showPkg) {
        return getLink(className, pkg, showPkg, DOC);
    }
    
    /**
    * Returns the location of the source code for a class.
    */
    public static String getSourceLink(String className, String pkg) {
        return getLink(className, pkg, false, SRC);
    }
    
    public static String getLink(String className, String pkg, boolean showPkg, int type) {
        String[] roots = (type == DOC) ? docs : sources;
        HashMap map = (type == DOC) ? docLinks : sourceLinks;
        String key = (type == DOC) ? (className + "." + pkg + String.valueOf(showPkg)) : (className + "." + pkg);
        String cached = (String) map.get(key);
        if (cached != null) {
            return cached;
        }
        for (int i = 0; i < roots.length; i++) {
            // The separator is always '/' for a URL, but it might not be for source.
            // FIXME: should we insist on "file://" for specifying source locations?
            char separator = (type == DOC) ? '/' : File.separatorChar;
            
            // Start with a root that ends in a separator.
            StringBuffer s = new StringBuffer(roots[i]);
            if (s.charAt(s.length() - 1) != separator) {
                s.append(separator);
            }
            
            // Add the package and class names, using the appropriate separators.
            s.append(pkg.replace('.', separator));
            s.append(separator);
            s.append(className.replace('.', separator));
            
            // And finally add the file type.
            if (type == DOC) {
                s.append(".html");
            } else if (type == SRC) {
                s.append(".java");
            }
            
            String candidate = s.toString();
            if (FileUtilities.nameStartsWithOneOf(candidate, FileUtilities.getArrayOfPathElements(Parameters.getParameter("url.prefixes", "")))) {
                if (urlExists(candidate)) {
                    String link;
                    if (type == DOC) {
                        link = formatAsDocLink(candidate, className, pkg, showPkg);
                        map.put(className + "." + pkg + String.valueOf(showPkg), link);
                    } else {
                        link = formatAsSourceLink(candidate);
                        map.put(className + "." + pkg, link);
                    }
                    return link;
                }
            } else if (candidate.startsWith(File.separator)) {
                File file = FileUtilities.fileFromString(candidate);
                if (file.exists()) {
                    String link;
                    if (type == DOC) {
                        link = formatAsDocLink(candidate, className, pkg, showPkg);
                        map.put(className + "." + pkg + String.valueOf(showPkg), link);
                    } else {
                        link = formatAsSourceLink(candidate);
                        map.put(className + "." + pkg, link);
                    }
                    return link;
                }
            }
        }
        
        // Didn't find any links at all. Return a classname in plain text, or nothing at all if there's no source.
        String link = (type == DOC) ? pkg + "." + className : "";
        map.put(key, link);
        return link;
    }
    
    /** Returns true if the given url points to an accessible resource, false otherwise. */
    public static boolean urlExists(String urlString) {
        try {
            URL url = new URL(urlString);
            url.getContent();
            return true;
        } catch (Exception ex) {
            /* Didn't find what we're looking for. */
            // FIXME: is there a better way to do URL.exists()?
            return false;
        }
    }
    
    public static final String DECORATED_LINK_START = "<a href=\"";
    public static final String UNDECORATED_LINK_START = "<a style=\"text-decoration: none\" href=\"";
    public static final String HREF_END = "\">";
    public static final String LINK_END = "</a>";
    
    public static String formatAsSourceLink(String uri) {
        StringBuffer link = new StringBuffer();
        link.append("[");
        link.append(UNDECORATED_LINK_START);
        link.append(uri);
        link.append(HREF_END);
        link.append("src");
        link.append(LINK_END);
        link.append("]");
        return link.toString();
    }
    
    public static String formatAsDocLink(String uri, String className, String pkg, boolean showPkg) {
        StringBuffer link = new StringBuffer();
        if (showPkg) {
            String packageIndex = uri.substring(0, uri.lastIndexOf("/")) + "/package-summary.html";
            link.append(UNDECORATED_LINK_START);
            link.append(packageIndex);
            link.append(HREF_END);
            link.append(pkg);
            link.append(LINK_END);
            link.append("<br>");
            link.append(UNDECORATED_LINK_START);
        } else {
            link.append(UNDECORATED_LINK_START);
        }
        link.append(uri);
        link.append(HREF_END);
        link.append(className);
        link.append(LINK_END);
        return link.toString();
    }
    
    /**
    * Returns a short text summary of the package and the names
    * of all classes in it.
    */
    public static String[] getPackageInfo(String pkgName) {
        return new String[] { getLink("package-summary", pkgName, true, DOC) };
        /*        String docIndex = "";
        for (Iterator i = groups.iterator(); i.hasNext(); ) {
            ArrayList packages = (ArrayList) i.next();
            if (packages.contains(pkgName)) {
                docIndex = (String) docIndices.get(packages);
                break;
            }
        }
        ArrayList pkgInfo = new ArrayList();
        String uri = docIndex + pkgName.replace('.', '/') + "/package-summary.html";
        try {
            URL url = new URL(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            
            StringBuffer summary = new StringBuffer();
            boolean readingSummary = false;
            boolean readSummary = false;
            String line;
            while ((line = reader.readLine()) != null && readSummary == false) {
                if (readingSummary && line.indexOf("<P>") != -1) {
                    readingSummary = false;
                    readSummary = true;
                }
                if (readingSummary) {
                    summary.append(line);
                    summary.append("\n");
                }
                if (line.indexOf("</H2>") != -1) {
                    readingSummary = true;
                }
            }
            pkgInfo.add(summary.toString());
            
            boolean expectingClass = false;
            while ((line = reader.readLine()) != null) {
                if (expectingClass) {
                    int start = line.indexOf("<A HREF=\"") + 9;
                    int end = line.indexOf(".html\">");
                    pkgInfo.add(line.substring(start, end));
                    expectingClass = false;
                }
                if (line.indexOf("TableRowColor") != -1) {
                    expectingClass = true;
                }
            }
            reader.close();
        } catch (FileNotFoundException fnfex) {
            return new String[] { "No javadoc for " + pkgName + ".", "" };
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (String[]) pkgInfo.toArray(new String[pkgInfo.size()]);
        */
    }
    
    /**
    * Returns all classes whose name matches the supplied string.
    */
    public static Class[] getClasses(String className) {
        ArrayList classes = new ArrayList();
        for (int j = 0; j < packageNames.size(); j++) {
            String pkg = (String) packageNames.get(j);
            String soughtClass = pkg + ((pkg.length() > 0) ? "." : "") + className;
            try {
                classes.add(Class.forName(soughtClass, false, classLoader));
            } catch (ClassNotFoundException ex) {
                /*
                 * Ignore: just means that we haven't yet found the class
                 * we're looking for. We are just guessing its location,
                 * after all.
                 */
                ex = ex;
            }
        }
        return (Class[]) classes.toArray(new Class[classes.size()]);
    }

    /** Prevents instantiation. */
    private JavaDoc() {
    }
}
