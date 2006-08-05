package e.edit;

import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import e.ptextarea.*;
import e.util.*;

public class JavaResearcher implements WorkspaceResearcher {
    private static final String NEWLINE = "<br>&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final String COMMA = ",&nbsp;";
    
    /** A regular expression matching 'new' expressions. */
    private static final Pattern NEW_PATTERN = Pattern.compile("(?x) .* \\b new \\s+ (\\S+) \\s* \\($");
    
    private static String[] javaDocSummary;
    private static TreeSet<String> uniqueIdentifiers;
    private static TreeSet<String> uniqueWords;
    
    private static JavaResearcher INSTANCE = new JavaResearcher();
    
    private JavaResearcher() {
    }
    
    public synchronized static JavaResearcher getSharedInstance() {
        if (javaDocSummary == null) {
            readJavaDocSummary();
        }
        return INSTANCE;
    }
    
    private synchronized static void readJavaDocSummary() {
        long start = System.currentTimeMillis();
        
        String filename = Evergreen.getInstance().getResourceFilename("javadoc-summary.txt");
        Log.warn("Reading JavaDoc summary from \"" + filename + "\"...");
        if (FileUtilities.exists(filename)) {
            javaDocSummary = StringUtilities.readLinesFromFile(filename);
        } else {
            javaDocSummary = new String[0];
        }
        
        Log.warn("Scanning JavaDoc summary...");
        Pattern identifierPattern = Pattern.compile("^[MCFEA]:(\\S+?)(\\(|\t).*$");
        uniqueIdentifiers = new TreeSet<String>();
        
        int classCount = 0;
        for (String line : javaDocSummary) {
            if (line.startsWith("Class:")) {
                classCount++;
                // Some classes don't have accessible constructors, so add
                // the class name anyway (SwingUtilities is an example).
                String className = line.substring(line.lastIndexOf('.') + 1);
                uniqueIdentifiers.add(className);
            } else {
                // Is it a constructor, method or field definition?
                Matcher matcher = identifierPattern.matcher(line);
                if (matcher.find()) {
                    uniqueIdentifiers.add(matcher.group(1));
                }
            }
        }
        Log.warn("Extracting unique words from JavaDoc summary...");
        
        uniqueWords = extractUniqueWords(uniqueIdentifiers.iterator());
        
        long timeTaken = System.currentTimeMillis() - start;
        Log.warn("Read summarized JavaDoc for " + classCount + " classes (" + javaDocSummary.length + " lines, " + uniqueIdentifiers.size() + " unique identifiers) in " + timeTaken + "ms.");
    }
    
    /**
     * Extracts all the unique words from the identifiers in the JDK.
     */
    public synchronized static TreeSet<String> extractUniqueWords(Iterator<String> iterator) {
        TreeSet<String> result = new TreeSet<String>();
        while (iterator.hasNext()) {
            String identifier = iterator.next();
            String[] words = identifier.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase().split(" ");
            for (String word : words) {
                result.add(word);
            }
        }
        return result;
    }
    
    /**
     * Adds all the unique words from the identifiers in the JDK to the given set.
     * This might be useful for spelling checking or word completion purposes.
     */
    public synchronized void addJavaWordsTo(Set<String> set) {
        set.addAll(uniqueWords);
    }
    
    public synchronized List<String> listIdentifiersStartingWith(String prefix) {
        ArrayList<String> result = new ArrayList<String>();
        final int prefixLength = prefix.length();
        for (String identifier : uniqueIdentifiers) {
            if (identifier.startsWith(prefix)) {
                result.add(identifier);
            }
        }
        return result;
    }
    
    /**
    * Builds HTML containing useful information
    * about the text around the caret.
    */
    private String makeResult(String text) {
        // Does it look like the user wants help with a constructor?
        boolean listConstructors = false;
        Matcher newMatcher = NEW_PATTERN.matcher(text);
        if (newMatcher.find()) {
            listConstructors = true;
            text = newMatcher.group(1);
        }
        
        // How about a class method or class field?
        boolean listMembers = false;
        Pattern accessPattern = Pattern.compile("(?x) (\\S+) \\.$");
        Matcher accessMatcher = accessPattern.matcher(text);
        if (accessMatcher.find()) {
            listMembers = true;
            text = accessMatcher.group(1);
        }
        
        boolean listClasses = (listConstructors == false) && text.matches("^.*\\b[A-Z]\\S*$");
        
        if (listConstructors == false && listMembers == false && listClasses == false) {
            return makeInstanceMethodOrFieldResult(text);
        }
        
        // At this point, 'text' should be a class name.
        Class[] classes = JavaDoc.getClasses(text);
        if (classes.length == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (Class klass : classes) {
            if (result.length() > 0) {
                result.append("<br><br>");
            }
            if (listClasses) {
                result.append(listClass(klass));
            }
            if (listConstructors) {
                result.append(listConstructors(klass));
            }
            if (listMembers) {
                result.append(listMembers(klass));
            }
        }
        return result.toString();
    }
    
    private String makePackageResult(String text) {
        Pattern importPattern = Pattern.compile("import (.+)\\..+;");
        Matcher matcher = importPattern.matcher(text);
        if (matcher.find()) {
            String packageName = matcher.group(1);
            return listPackage(packageName);
        } else {
            return "";
        }
    }
    
    private String makeInstanceMethodOrFieldResult(String text) {
        // FIXME: ideally, we'd find the name of the most recent method with unclosed parentheses.
        // So, in "doSomething(getThing(x), " we'd know that "doSomething" is interesting and "getThing" isn't.
        Pattern namePattern = Pattern.compile("(\\S+)\\s*(\\(?).*");
        Matcher matcher = namePattern.matcher(text);
        if (matcher.find()) {
            String methodOrFieldName = matcher.group(1);
            return listMethodsOrFields(methodOrFieldName);
        } else {
            return "";
        }
    }
    
    /**
     * Returns HTML linking to all the classes in the given package.
     */
    private synchronized String listPackage(String packageName) {
        StringBuilder result = new StringBuilder(packageName + " contains:\n");
        String searchTerm = "Class:" + packageName + ".";
        String htmlFile = "";
        for (String line : javaDocSummary) {
            if (line.startsWith("File:")) {
                htmlFile = line.substring(5);
            } else if (line.startsWith(searchTerm)) {
                String className = line.substring(searchTerm.length());
                /* Just check it is actually a class in this package, and not a class in a sub-package. */
                if (Character.isUpperCase(className.charAt(0))) {
                    result.append("<br><a href=\"" + urlFromHtmlFile(htmlFile) + "\">" + className + "</a>\n");
                }
            }
        }
        return result.toString();
    }
    
    private String urlFromHtmlFile(String htmlFile) {
        if (FileUtilities.exists(htmlFile)) {
            // The file exists locally, so we can use it.
            return "file://" + htmlFile;
        } else {
            // The file doesn't exist locally with the name we have cached in "javadoc-summary.txt", so we'll have to get it from the local override, or direct from Sun.
            String localDocLocation = Parameters.getParameter("java.advisor.doc");
            String sunDocLocation = "http://java.sun.com/j2se/1.5.0/docs/api/";
            return (localDocLocation != null ? localDocLocation : sunDocLocation) + htmlFile.replaceAll("^.*/api/", "/");
        }
    }
    
    private synchronized String listMethodsOrFields(String name) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("^[MF]:(" + StringUtilities.regularExpressionFromLiteral(name) + "[^(\t]*)(\\([^\t]+)\t");
        Matcher matcher;
        String htmlFile = "";
        String className = "";
        for (String line : javaDocSummary) {
            if (line.startsWith("File:")) {
                htmlFile = line.substring(5);
            } else if (line.startsWith("Class:")) {
                className = line.substring(6);
            } else if ((matcher = pattern.matcher(line)).find()) {
                String url = urlFromHtmlFile(htmlFile);
                result.append("<br><a href=\"" + url + "#" + matcher.group(1) + matcher.group(2) + "\">" + matcher.group(1) + "</a> in " + makeClassLink(new ClassAndPackage(className), true, true));
            }
        }
        if (result.length() == 0) {
            return "";
        }
        result.insert(0, name + " could be:\n");
        return result.toString();
    }
    
    /** Formats class names as a list of links if there's javadoc for them. */
    private String makeClassLinks(Class[] classes, boolean pkg, String conjunct, boolean showSourceLink) {
        StringBuilder s = new StringBuilder();
        for (Class klass : classes) {
            if (s.length() > 0) {
                s.append(conjunct);
            }
            s.append(makeClassLink(klass, pkg, showSourceLink));
        }
        return s.toString();
    }
    
    private static class ClassAndPackage {
        String className;
        String packageName;
        
        ClassAndPackage(String className, String packageName) {
            this.className = className;
            this.packageName = packageName;
        }
        
        ClassAndPackage(String qualifiedName) {
            this.packageName = "";
            final int dot = qualifiedName.lastIndexOf(".");
            if (dot != -1) {
                this.packageName = qualifiedName.substring(0, dot);
                this.className = qualifiedName.substring(dot + 1);
            }
        }
        
        String qualifiedName() {
            if (packageName.length() == 0) {
                return className;
            }
            return packageName + "." + className;
        }
    }
    
    /** Formats a class name as a link if there's javadoc for it. */
    private String makeClassLink(Class c, boolean showPkg, boolean showSourceLink) {
        ClassAndPackage classAndPackage = new ClassAndPackage(c.getName());
        if (c.isArray()) {
            return makeClassLink(c.getComponentType(), showPkg, showSourceLink) + "[]";
        }
        if (c.isPrimitive()) {
            return c.getName();
        }
        return makeClassLink(classAndPackage, showPkg, showSourceLink);
    }
    
    /** Formats a class name as a link if there's javadoc for it. */
    private String makeClassLink(ClassAndPackage classAndPackage, boolean showPkg, boolean showSourceLink) {
        StringBuilder s = new StringBuilder();
        if (classAndPackage.className.contains(".")) {
            s.append(classAndPackage.className);
            return s.toString();
        }
        s.append(JavaDoc.getDocLink(classAndPackage.className, classAndPackage.packageName, showPkg));
        if (showSourceLink) {
            List<String> sourceFiles = JavaDoc.findSourceFilenames(classAndPackage.qualifiedName());
            for (String sourceFile : sourceFiles) {
                s.append("&nbsp;");
                s.append(JavaDoc.formatAsSourceLink(sourceFile));
            }
        }
        return s.toString();
    }
    
    public String makeMethodLinks(Method[] methods) {
        ArrayList<String> list = new ArrayList<String>();
        for (Method method : methods) {
            list.add(makeMethodLink(method));
        }
        return joinAfterSorting(list);
    }
    
    public static String joinAfterSorting(List<String> list) {
        Collections.sort(list);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            s.append(list.get(i));
        }
        return s.toString();
    }
    
    public String makeFieldLink(Field f) {
        return "<br>" + makeClassLink(f.getType(), false, false) + " " + f.getName();
    }
    
    public String makeMethodLink(Method m) {
        String parameters = makeClassLinks(m.getParameterTypes(), false, COMMA, false);
        String returnType = makeClassLink(m.getReturnType(), false, false);
        String result = "<br>" + m.getName() + "(" + parameters + ") => " + returnType;
        Class[] exceptions = m.getExceptionTypes();
        if (exceptions.length > 0) {
            result += " throws " + makeClassLinks(exceptions, true, NEWLINE, false);
        }
        return result;
    }
    
    /**
    * Lists the fully-qualified class name and all the modifiers,
    * the superclass name and all implemented interfaces.
    */
    public String listClass(Class c) {
        StringBuilder s = new StringBuilder();
        int modifiers = c.getModifiers();
        if (c.isInterface()) {
            modifiers &= ~Modifier.ABSTRACT;
        }
        s.append(Modifier.toString(modifiers));
        if (c.isInterface() == false) {
            s.append(" class");
        }
        s.append("<br>");
        s.append(makeClassLink(c, true, true));
        
        // Show superclasses.
        Class superclass = c.getSuperclass();
        if (superclass != null && c.isInterface() == false) {
            s.append("<br>extends ");
            int depth = 0;
            for (; superclass != null; superclass = superclass.getSuperclass()) {
                s.append("<br>");
                s.append(StringUtilities.nCopies(++depth, "&nbsp;"));
                s.append(makeClassLink(superclass, false, true));
            }
        }
        
        // Show all implemented interfaces.
        Class[] interfaces = c.getInterfaces();
        if (interfaces.length > 0) {
            s.append("<br>implements ");
            s.append(NEWLINE);
            s.append(makeClassLinks(interfaces, false, NEWLINE, true));
        }
        
        // Show all interface/superinterface methods.
        if (c.isInterface()) {
            s.append("<br><br>interface methods ");
            s.append(makeMethodLinks(c.getMethods()));
        }
        return s.toString();
    }
    
    /** Lists the class name and all the constructors. */
    public String listConstructors(Class c) {
        StringBuilder s = new StringBuilder();
        String classLink = makeClassLink(c, false, false);
        s.append(makeClassLink(c, true, true));
        s.append("<br>");
        Constructor[] constructors = c.getConstructors();
        for (Constructor constructor : constructors) {
            s.append("<br>");
            s.append(classLink);
            s.append("(");
            s.append(makeClassLinks(constructor.getParameterTypes(), false, COMMA, false));
            s.append(")");
            Class[] exceptions = constructor.getExceptionTypes();
            if (exceptions.length > 0) {
                s.append(NEWLINE);
                s.append(" throws ");
                s.append(makeClassLinks(exceptions, false, NEWLINE, false));
            }
        }
        if (c.isInterface()) {
            s.append(makeMethodLinks(c.getDeclaredMethods()));
        }
        return s.toString();
    }
    
    /**
     * Lists all non-private fields and non-private static methods of a class.
     */
    public String listMembers(Class c) {
        StringBuilder s = new StringBuilder("<br>");
        appendAfterSorting(s, collectFields(c));
        s.append("<br>");
        appendAfterSorting(s, collectStaticMethods(c));
        return s.toString();
    }
    
    /** Sorts the strings in the given List, then appends them to the StringBuilder. */
    private void appendAfterSorting(StringBuilder stringBuffer, List<String> items) {
        Collections.sort(items);
        for (int i = 0; i < items.size(); i++) {
            stringBuffer.append(items.get(i));
        }
    }
    
    private List<String> collectFields(Class c) {
        ArrayList<String> result = new ArrayList<String>();
        for (Field field : c.getFields()) {
            if (Modifier.isPrivate(field.getModifiers()) == false) {
                result.add(makeFieldLink(field));
            }
        }
        return result;
    }
    
    private List<String> collectStaticMethods(Class c) {
        ArrayList<String> result = new ArrayList<String>();
        for (Method method : c.getMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPrivate(modifiers) == false && Modifier.isStatic(modifiers)) {
                result.add(makeMethodLink(method));
            }
        }
        return result;
    }

//
// WorkspaceResearcher interface.
//
    
    /** Returns true for Java files. */
    public boolean isSuitable(ETextWindow textWindow) {
        return textWindow.isJava();
    }
    
    public String research(String string) {
        if (string.startsWith("import ")) {
            return makePackageResult(string);
        } else {
            return makeResult(string);
        }
    }
    
    /** We don't implement any non-standard URI schemes. */
    public boolean handleLink(String link) {
        return false;
    }
}
