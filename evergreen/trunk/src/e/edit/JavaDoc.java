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
    private static ArrayList<String> packageNames = new ArrayList<String>();
    
    /** Places to look for JavaDoc. */
    private static ArrayList<String> javaDocLocations = new ArrayList<String>();
    
    /** Cached JavaDoc locations for classes we've already seen. */
    private static HashMap<String, String> docLinks = new HashMap<String, String>();
    
    /** Tells us if the text around the caret is a genuine classname or not. */
    private static URLClassLoader classLoader;
    
    static {
        long start = System.currentTimeMillis();
        Log.warn("Collecting JavaDoc...");
        
        /**
        * Find all the packages that are on our classpath. All classes in these packages can
        * be loaded by the system classloader without any help.
        */
        for (Package p : Package.getPackages()) {
            packageNames.add(p.getName());
        }
        
        // Add the default package, and also permit looking for a fully-qualified class name.
        packageNames.add("");
        int systemPkgs= packageNames.size();
        
        /**
        * Find all the packages specified by the "java.advisor.classpath" property.
        */
        String[] advisorClasspath = FileUtilities.getArrayOfPathElements(Parameters.getParameter("java.advisor.classpath", ""));
        ArrayList<URL> urls = new ArrayList<URL>();
        for (String classPathItem : advisorClasspath) {
            if (classPathItem.length() > 0) {
                File classpath = FileUtilities.fileFromString(classPathItem);
                try {
                    urls.add(classpath.toURI().toURL());
                    findPackagesIn(classpath);
                } catch (MalformedURLException ex) {
                    Log.warn("Bad URL on java.advisor.classpath (" + classpath + ")", ex);
                }
            }
        }
        classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
        
        // Note the locations of JavaDoc HTML files.
        javaDocLocations.addAll(Arrays.asList(FileUtilities.getArrayOfPathElements(Parameters.getParameter("java.advisor.doc", ""))));
        
        // On Mac OS, we may have the documentation installed in a well-known place.
        // FIXME: this should be CurrentJDK rather than 1.5.0, but 1.5.0 isn't the default on Mac OS yet.
        String macOsJavaDocDir = "/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Resources/Documentation/Reference/";
        if (FileUtilities.exists(macOsJavaDocDir)) {
            javaDocLocations.add("file://" + macOsJavaDocDir + "doc/api/");
            javaDocLocations.add("file://" + macOsJavaDocDir + "appledoc/api/");
        }
        
        // In an emergency, fall back to Sun's copy on the web.
        if (javaDocLocations.isEmpty()) {
            javaDocLocations.add("http://java.sun.com/j2se/1.5.0/docs/api/");
        }
        
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
        for (File file : startingDirectory.listFiles()) {
            if (file.isDirectory()) {
                String packageName = file.getAbsolutePath().substring(classpath.length() + 1).replace(File.separatorChar, '.');
                packageNames.add(packageName);
                addPackagesInDirectory(file, classpath);
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
            Log.warn("Problem scanning " + jarFile, ex);
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
    
    /**
     * Returns the location of the source file, as a plain String.
     */
    public static List<String> findSourceFilenames(String dottedClassName) {
        String suffix = "\\b" + dottedClassName.replace('.', File.separatorChar) + "\\.java$";
        List<String> result = new ArrayList<String>();
        for (Workspace workspace : Edit.getInstance().getWorkspaces()) {
            for (String leafName : workspace.getListOfFilesMatching(suffix)) {
                result.add(workspace.prependRootDirectory(leafName));
            }
        }
        return result;
    }
    
    /**
     * Returns the location of the documentation for a class.
     */
    public static String getDocLink(String className, String pkg, boolean showPkg) {
        String key = (className + "." + pkg + String.valueOf(showPkg));
        String cached = docLinks.get(key);
        if (cached != null) {
            return cached;
        }
        for (String root : javaDocLocations) {
            if (root.length() == 0) {
                continue;
            }
            
            // The separator is always '/' for a URL.
            char separator = '/';
            
            // Start with a root that ends in a separator.
            StringBuilder s = new StringBuilder(root);
            if (s.charAt(s.length() - 1) != separator) {
                s.append(separator);
            }
            
            // Add the package and class names, using the appropriate separators.
            s.append(pkg.replace('.', separator));
            s.append(separator);
            s.append(className.replace('.', separator));
            
            // And finally add the file type.
            s.append(".html");
            
            String candidate = s.toString();
            if (FileUtilities.nameStartsWithOneOf(candidate, FileUtilities.getArrayOfPathElements(Parameters.getParameter("url.prefixes", "")))) {
                if (urlExists(candidate)) {
                    String link = formatAsDocLink(candidate, className, pkg, showPkg);
                    docLinks.put(className + "." + pkg + String.valueOf(showPkg), link);
                    return link;
                }
            } else if (FileUtilities.fileFromString(candidate).exists()) {
                String link = formatAsDocLink(candidate, className, pkg, showPkg);
                docLinks.put(className + "." + pkg + String.valueOf(showPkg), link);
                return link;
            }
        }
        
        // Didn't find any links at all. Return a classname in plain text.
        String link = pkg + "." + className;
        docLinks.put(key, link);
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
    
    private static String formatAnchor(String uri, String text) {
        return  "<a style=\"text-decoration: none\" href=\"" + uri + "\" alt=\"" + uri + "\">" + text + "</a>";
    }
    
    public static String formatAsSourceLink(String uri) {
        return "[" + formatAnchor(uri, "src") + "]";
    }
    
    public static String formatAsDocLink(String uri, String className, String packageName, boolean showPackage) {
        StringBuilder link = new StringBuilder();
        if (showPackage) {
            String packageIndexUri = uri.substring(0, uri.lastIndexOf("/")) + "/package-summary.html";
            link.append(formatAnchor(packageIndexUri, packageName));
            link.append("<br>");
        }
        link.append(formatAnchor(uri, className));
        return link.toString();
    }
    
    /**
    * Returns a short text summary of the package and the names
    * of all classes in it.
    */
    public static String[] getPackageInfo(String pkgName) {
        return new String[] { getDocLink("package-summary", pkgName, true) };
    }
    
    /**
    * Returns all classes whose name matches the supplied string.
    */
    public static Class[] getClasses(String className) {
        ArrayList<Class> classes = new ArrayList<Class>();
        for (String packageName : packageNames) {
            String soughtClass = packageName + ((packageName.length() > 0) ? "." : "") + className;
            try {
                classes.add(Class.forName(soughtClass, false, classLoader));
            } catch (ClassNotFoundException ex) {
                /*
                 * Ignore: just means that we haven't yet found the class
                 * we're looking for. We are just guessing its location,
                 * after all.
                 */
                ex = ex;
            } catch (NoClassDefFoundError ex) {
                /*
                 * Ignore: occurs when the text next to the caret is a real class
                 * name followed by a dot, but the text is in the wrong case. E.g.:
                 * positioning the caret just beyond " javadoc. " will cause
                 * java.lang.NoClassDefFoundError: e/edit/javadoc (wrong name: e/edit/JavaDoc)
                 */
                ex = ex;
            }
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /** Prevents instantiation. */
    private JavaDoc() {
    }
}
