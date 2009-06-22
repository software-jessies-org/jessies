package org.jessies.test;

import e.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.jessies.cli.*;
import org.jessies.os.*;

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
    @Option(names = { "--color" })
    private boolean color = true;
    
    @Option(names = { "-v", "--verbose" })
    private boolean verbose = false;
    
    private final long startTime = System.nanoTime();
    
    private final ExecutorService executor;
    private final ArrayList<TestResult> successes;
    private final ArrayList<TestResult> failures;
    
    public static void main(String[] args) throws Exception {
        new TestRunner(args);
    }
    
    private TestRunner(String[] args) throws Exception {
        this.executor = ThreadUtilities.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), "test runner");
        this.successes = new ArrayList<TestResult>();
        this.failures = new ArrayList<TestResult>();
        
        final List<String> directories = new OptionParser(this).parse(args);
        
        scheduleTests(directories);
        
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        
        reportResults();
    }
    
    private void reportResults() {
        final long runningTime = System.nanoTime() - startTime;
        verbose("Running time: " + TimeUtilities.nsToString(runningTime));
        
        // Sort the results because a consistent output order is useful for human observers.
        Collections.sort(successes);
        Collections.sort(failures);
        
        // Show successes first, then failures, since the latter are more likely to need investigation.
        for (TestResult success : successes) {
            success.print();
        }
        for (TestResult failure : failures) {
            failure.print();
        }
        
        final int failCount = failures.size();
        final int testCount = successes.size() + failCount;
        if (testCount == 0) {
            System.out.println(red("No tests found!\n"));
            System.exit(Posix.EXIT_FAILURE);
        } else if (failCount == 0) {
            System.out.println(green("All " + testCount + " tests passed in " + TimeUtilities.nsToString(runningTime) + "."));
            System.exit(Posix.EXIT_SUCCESS);
        } else {
            System.out.printf(red("Tested: %d, Passed: %d, Failed: %d.\n"), testCount, testCount - failCount, failCount);
            System.exit(Posix.EXIT_FAILURE);
        }
    }
    
    private void scheduleTests(List<String> directoryNames) throws Exception {
        findTestMethods(makeClassLoader(directoryNames), findClassNames(directoryNames));
    }
    
    // Makes a ClassLoader that can load classes from the given directories.
    private ClassLoader makeClassLoader(List<String> classPathDirectoryNames) throws Exception {
        final URL[] classPath = new URL[classPathDirectoryNames.size()];
        for (int i = 0; i < classPath.length; ++i) {
            classPath[i] = new File(classPathDirectoryNames.get(i)).toURI().toURL();
        }
        return new URLClassLoader(classPath, getClass().getClassLoader());
    }
    
    private List<String> findClassNames(List<String> directoryNames) {
        List<String> result = new ArrayList<String>();
        for (String directoryName : directoryNames) {
            File directory = new File(directoryName);
            if (directory.isDirectory()) {
                findClassNames(directory, "", result);
            } else {
                error("'" + directoryName + "' is not a directory");
            }
        }
        verbose("Total classes scanned: " + result.size());
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
                File file = new File(directory, filename);
                if (file.isDirectory() == false) {
                    error("'" + file + "' is not a directory or a .class file");
                }
                findClassNames(file, packageName + filename + ".", result);
            }
        }
    }
    
    private void findTestMethods(ClassLoader classLoader, List<String> classNames) throws Exception {
        for (String className : classNames) {
            final Class<?> testClass = classLoader.loadClass(className);
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    ensureTestMethodIsSuitable(method);
                    method.setAccessible(true);
                    executor.execute(new TestRunnable(method));
                }
            }
        }
    }
    
    private class TestRunnable implements Runnable {
        private final Method testMethod;
        
        public TestRunnable(Method testMethod) {
            this.testMethod = testMethod;
        }
        
        public void run() {
            final String testName = testMethod.getDeclaringClass().getName() + "." + testMethod.getName();
            try {
                testMethod.invoke(null);
                synchronized (successes) {
                    successes.add(new TestResult(testName));
                }
            } catch (InvocationTargetException wrappedEx) {
                final Throwable ex = wrappedEx.getCause();
                synchronized (failures) {
                    failures.add(new TestResult(testName, ex));
                }
            } catch (IllegalAccessException ex) {
                // This can't happen, so just rethrow and bail out.
                throw new RuntimeException(ex);
            }
        }
    }
    
    private class TestResult implements Comparable<TestResult> {
        private final String name;
        private final Throwable ex;
        
        // For passes.
        public TestResult(String name) {
            this(name, null);
        }
        
        // For failures.
        public TestResult(String name, Throwable ex) {
            this.name = name;
            this.ex = ex;
        }
        
        public boolean isFailure() {
            return (ex != null);
        }
        
        public void print() {
            if (isFailure()) {
                System.out.println(red("FAIL") + " " + name);
                // FIXME: we can print this a lot more nicely:
                // * remove the leading "java.lang.RuntimeException: " from the message.
                // * don't print the (bottom) part of the stack that's us invoking the method.
                // * maybe don't print the (top) part of the stack that's in our Assert class?
                ex.printStackTrace();
            } else {
                System.out.println(green("PASS") + " " + name);
            }
        }
        
        public int compareTo(TestResult rhs) {
            return name.compareTo(rhs.name);
        }
        
        @Override public boolean equals(Object rhs) {
            return (rhs instanceof TestResult) && name.equals(((TestResult) rhs).name);
        }
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
    
    private void error(String message) {
        System.err.println(red("ERROR:") + " " + message);
        System.exit(Posix.EXIT_FAILURE);
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
