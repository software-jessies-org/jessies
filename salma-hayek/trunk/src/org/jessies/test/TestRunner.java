package org.jessies.test;

import e.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Runs tests for the simple Java unit testing framework.
 * 
 * Given a list of directories, all the class files in those directories trees
 * are scanned for methods annotated with @Test. All the tests are then run,
 * and the test results reported.
 * 
 * The trade-off for not requiring any configuration or naming convention is
 * that we need to load all the classes in the supplied directories to see if
 * they contain tests.
 * 
 * Some notes on why this particular wheel needed reinventing, to help future
 * readers judge whether it's time to retire this stuff:
 * 
 * 1. JUnit is the established player, but it's not part of the JRE/JDK, so it
 * would have been another build dependency to deal with (even if there weren't
 * other reasons to dislike JUnit). Sorting that out for all our supported
 * platforms seems more expensive than a couple of hundred lines of simple Java.
 * 
 * 2. As mentioned above, the desire was to be able to say "run all the tests
 * to be found in classes in this directory".
 * 
 * 3. Keeping tests in separate classes (let alone a separate tree) is distasteful
 * because it seems to lead to a lowering of standards for code that's "only test
 * code", extra contortions in the tested code to provide access to otherwise
 * inaccessible internals, and no reason to ask "is this test worth writing?"
 * which seems to be a disease amongst the "test infected" (even though their
 * leaders warn against this, everyone loves a silver bullet).
 * 
 * 4. Trivially, it's always annoyed me that equality tests in JUnit-inspired
 * frameworks are backwards, in the pre-GCC "0 == rc" style C programmers used
 * to use to protect against =/== mistakes. That's no longer an appropriate C/C++
 * style, and that has never been an appropriate Java style. Let it go, dudes.
 */
public class TestRunner {
    // We're likely to be run by make(1), so we follow usual Unix status conventions.
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;
    
    private boolean color = true; // FIXME: add a flag.
    private boolean verbose = false; // FIXME: add a flag.
    
    private long setupTime;
    private long runningTime;
    
    public static void main(String[] args) throws Exception {
        new TestRunner().runTests(args);
    }
    
    private void runTests(String[] directoryNames) throws Exception {
        // FIXME: parallelize; start running tests as they're found?
        System.exit(runTests(findTests(directoryNames)));
    }
    
    private List<Method> findTests(String[] directoryNames) throws Exception {
        this.setupTime = System.nanoTime();
        List<Method> testMethods = findTestMethods(makeClassLoader(directoryNames), findClassNames(directoryNames));
        this.setupTime = System.nanoTime() - setupTime;
        verbose("Setup time: " + TimeUtilities.nsToString(setupTime));
        return testMethods;
    }
    
    // Makes a ClassLoader that can load classes from the given directories.
    private ClassLoader makeClassLoader(String[] classPathDirectoryNames) throws Exception {
        final URL[] classPath = new URL[classPathDirectoryNames.length];
        for (int i = 0; i < classPath.length; ++i) {
            classPath[i] = new File(classPathDirectoryNames[i]).toURI().toURL();
        }
        return new URLClassLoader(classPath, getClass().getClassLoader());
    }
    
    private List<String> findClassNames(String[] directoryNames) {
        List<String> result = new ArrayList<String>();
        for (String directoryName : directoryNames) {
            File directory = new File(directoryName);
            if (directory.isDirectory()) {
                findClassNames(directory, "", result);
            } else {
                error("'" + directoryName + "' is not a directory");
            }
        }
        verbose("Total classes found: " + result.size());
        return result;
    }
    
    private void findClassNames(File directory, String packageName, List<String> result) {
        // FIXME: parallelize?
        for (String filename : directory.list()) {
            if (filename.indexOf('$') != -1) {
                // We're not interested in inner classes.
            } else if (filename.endsWith(".class")) {
                String className = filename.substring(0, filename.length() - ".class".length());
                result.add(packageName + className);
            } else {
                findClassNames(new File(directory, filename), packageName + filename + ".", result);
            }
        }
    }
    
    private List<Method> findTestMethods(ClassLoader classLoader, List<String> classNames) throws Exception {
        List<Method> result = new ArrayList<Method>();
        for (String className : classNames) {
            final Class testClass = classLoader.loadClass(className);
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    ensureTestMethodIsSuitable(method);
                    method.setAccessible(true);
                    result.add(method);
                }
            }
        }
        verbose("Total tests found: " + result.size());
        return result;
    }
    
    // Check that the given method, which was annotated with @Test, is actually suitable to be a test method.
    private void ensureTestMethodIsSuitable(Method testMethod) {
        if (!Modifier.isPrivate(testMethod.getModifiers()) || !Modifier.isStatic(testMethod.getModifiers())) {
            error("test methods should be private static; got " + testMethod);
        }
        if (testMethod.getReturnType() != Void.TYPE) {
            error("test methods should be void; got " + testMethod);
        }
        if (testMethod.getParameterTypes().length > 0) {
            error("test methods should take no arguments; got " + testMethod);
        }
    }
    
    private int runTests(List<Method> testMethods) {
        this.runningTime = System.nanoTime();
        
        int failCount = 0;
        for (Method testMethod : testMethods) {
            final String testName = testMethod.getDeclaringClass().getName() + "." + testMethod.getName();
            try {
                testMethod.invoke(null);
                System.out.println(green("PASS") + " " + testName);
            } catch (InvocationTargetException wrappedEx) {
                ++failCount;
                System.out.println(red("FAIL") + " " + testName);
                final Throwable ex = wrappedEx.getCause();
                // FIXME: we can print this a lot more nicely:
                // * remove the leading "java.lang.RuntimeException: " from the message.
                // * don't print the (bottom) part of the stack that's us invoking the method.
                // * maybe don't print the (top) part of the stack that's in our Assert class?
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                // This can't happen, so just rethrow and bail out.
                throw new RuntimeException(ex);
            }
        }
        
        this.runningTime = System.nanoTime() - runningTime;
        verbose("Running time: " + TimeUtilities.nsToString(runningTime));
        
        final int testCount = testMethods.size();
        if (failCount == 0) {
            System.out.println(green("All " + testCount + " tests passed in " + TimeUtilities.nsToString(runningTime + setupTime) + "."));
            return EXIT_SUCCESS;
        } else {
            System.out.printf(red("Tested: %d, Passed: %d, Failed: %d.\n"), testCount, testCount - failCount, failCount);
            return EXIT_FAILURE;
        }
    }
    
    private void error(String message) {
        System.err.println(red("ERROR:") + " " + message);
        System.exit(EXIT_FAILURE);
    }
    
    private String red(String message) {
        return color ? ("\u001b[31;1m" + message + "\u001b[0m") : message;
    }
    
    private String green(String message) {
        return color ? ("\u001b[32;1m" + message + "\u001b[0m") : message;
    }
    
    private void verbose(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }
}
