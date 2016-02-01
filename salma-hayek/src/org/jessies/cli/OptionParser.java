package org.jessies.cli;

import java.lang.reflect.*;
import java.util.*;
import org.jessies.test.*;

/**
 * Parses command line options.
 * 
 * Strings in the passed-in String[] are parsed left-to-right. Each String is classified as a short option (such as "-v"), a long
 * option (such as "--verbose"), an argument to an option (such as "out.txt" in "-f out.txt"), or a non-option positional argument.
 * 
 * A simple short option is a "-" followed by a short option character. If the option requires an argument (which is true of any
 * non-boolean option), it may be written as a separate parameter, but need not be. That is, "-f out.txt" and "-fout.txt" are both
 * acceptable.
 * 
 * It is possible to specify multiple short options after a single "-" as long as all (except possibly the last) do not require
 * arguments.
 * 
 * A long option begins with "--" followed by several characters. If the option requires an argument, it may be written directly
 * after the option name, separated by "=", or as the next argument. (That is, "--file=out.txt" or "--file out.txt".)
 * 
 * A boolean long option '--name' automatically gets a '--no-name' companion. Given an option "--flag", then, "--flag", "--no-flag",
 * "--flag=true" and "--flag=false" are all valid, though neither "--flag true" nor "--flag false" are allowed (since "--flag" by
 * itself is sufficient, the following "true" or "false" is interpreted separately). You can use "yes" and "no" as synonyms for
 * "true" and "false".
 * 
 * Each String not starting with a "-" and not a required argument of a previous option is a non-option positional argument, as are
 * all successive Strings. Each String after a "--" is a non-option positional argument.
 * 
 * The fields corresponding to options are updated as their options are processed. Any remaining positional arguments are returned
 * as a List<String>.
 * 
 * Here's a simple example:
 * 
 * // This doesn't need to be a separate class, if your application doesn't warrant it.
 * // Non-@Option fields will be ignored.
 * class Options {
 *     @Option(names = { "-o", "--output-file" })
 *     String filename = "-";
 * 
 *     @Option(names = { "-q", "--quiet" })
 *     boolean quiet = false;
 * 
 *     // Boolean options require a long name if it's to be possible to explicitly turn them off.
 *     // Here the user can use --no-color.
 *     @Option(names = { "--color" })
 *     boolean color = true;
 * 
 *     @Option(names = { "-p", "--port" })
 *     int portNumber = 8888; // Supply a default just by setting the field.
 * 
 *     // There's no need to offer a short name for rarely-used options.
 *     @Option(names = { "--timeout" })
 *     double timeout = 1.0;
 * }
 * 
 * class Main {
 *     public static void main(String[] args) {
 *         Options options = new Options();
 *         List<String> inputFilenames = new OptionParser(options).parse(args);
 *         for (String inputFilename : inputFilenames) {
 *             if (!options.quiet) {
 *                 ...
 *             }
 *             ...
 *         }
 *     }
 * }
 * 
 * See also:
 * 
 *  the getopt(1) man page
 *  Python's "optparse" module (http://docs.python.org/library/optparse.html)
 *  the POSIX "Utility Syntax Guidelines" (http://www.opengroup.org/onlinepubs/000095399/basedefs/xbd_chap12.html#tag_12_02)
 *  the GNU "Standards for Command Line Interfaces" (http://www.gnu.org/prep/standards/standards.html#Command_002dLine-Interfaces)
 */
public class OptionParser {
    private static final HashMap<Class<?>, Handler> handlers = new HashMap<Class<?>, Handler>();
    static {
        handlers.put(boolean.class, new BooleanHandler());
        handlers.put(double.class, new DoubleHandler());
        handlers.put(int.class, new IntegerHandler());
        handlers.put(String.class, new StringHandler());
    }
    
    private final Object optionSource;
    private final HashMap<String, Field> optionMap;
    
    /**
     * Constructs a new OptionParser for setting the @Option fields of 'optionSource'.
     */
    public OptionParser(Object optionSource) {
        this.optionSource = optionSource;
        this.optionMap = makeOptionMap();
    }
    
    /**
     * Parses the command-line arguments 'args', setting the @Option fields of the 'optionSource' provided to the constructor.
     * Returns a list of the positional arguments left over after processing all options.
     */
    public List<String> parse(String[] args) {
        return parseOptions(Arrays.asList(args).iterator());
    }
    
    private List<String> parseOptions(Iterator<String> args) {
        final List<String> leftovers = new ArrayList<String>();
        
        // Scan 'args'.
        while (args.hasNext()) {
            final String arg = args.next();
            if (arg.equals("--")) {
                // "--" marks the end of options and the beginning of positional arguments.
                break;
            } else if (arg.startsWith("--")) {
                // A long option.
                parseLongOption(arg, args);
            } else if (arg.startsWith("-")) {
                // A short option.
                parseGroupedShortOptions(arg, args);
            } else {
                // The first non-option marks the end of options.
                leftovers.add(arg);
                break;
            }
        }
        
        // Package up the leftovers.
        while (args.hasNext()) {
            leftovers.add(args.next());
        }
        return leftovers;
    }
    
    private Field fieldForArg(String name) {
        final Field field = optionMap.get(name);
        if (field == null) {
            throw new RuntimeException("unrecognized option '" + name + "'");
        }
        return field;
    }
    
    private void parseLongOption(String arg, Iterator<String> args) {
        String name = arg.replaceFirst("^--no-", "--");
        String value = null;
        
        // Support "--name=value" as well as "--name value".
        final int equalsIndex = name.indexOf('=');
        if (equalsIndex != -1) {
            value = name.substring(equalsIndex + 1);
            name = name.substring(0, equalsIndex);
        }
        
        final Field field = fieldForArg(name);
        final Handler handler = handlers.get(field.getType());
        if (value == null) {
            if (handler.isBoolean()) {
                value = arg.startsWith("--no-") ? "false" : "true";
            } else {
                value = grabNextValue(args, name, field);
            }
        }
        handler.setValue(optionSource, field, arg, value);
    }
    
    // Given boolean options a and b, and non-boolean option f, we want to allow:
    // -ab
    // -abf out.txt
    // -abfout.txt
    // (But not -abf=out.txt --- POSIX doesn't mention that either way, but GNU expressly forbids it.)
    private void parseGroupedShortOptions(String arg, Iterator<String> args) {
        for (int i = 1; i < arg.length(); ++i) {
            final String name = "-" + arg.charAt(i);
            final Field field = fieldForArg(name);
            final Handler handler = handlers.get(field.getType());
            String value;
            if (handler.isBoolean()) {
                value = "true";
            } else {
                // We need a value. If there's anything left, we take the rest of this "short option".
                if (i + 1 < arg.length()) {
                    value = arg.substring(i + 1);
                    i = arg.length() - 1;
                } else {
                    value = grabNextValue(args, name, field);
                }
            }
            handler.setValue(optionSource, field, name, value);
        }
    }
    
    // Returns the next element of 'args' if there is one. Uses 'name' and 'field' to construct a helpful error message.
    private String grabNextValue(Iterator<String> args, String name, Field field) {
        if (!args.hasNext()) {
            final String type = field.getType().getSimpleName().toLowerCase();
            throw new RuntimeException("option '" + name + "' requires a " + type + " argument");
        }
        return args.next();
    }
    
    // Cache the available options and report any problems with the options themselves right away.
    private HashMap<String, Field> makeOptionMap() {
        final HashMap<String, Field> optionMap = new HashMap<String, Field>();
        final Class<?> optionClass = optionSource.getClass();
        for (Field field : optionClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Option.class)) {
                final Option option = field.getAnnotation(Option.class);
                final String[] names = option.names();
                if (names.length == 0) {
                    throw new RuntimeException("found an @Option with no name!");
                }
                for (String name : names) {
                    if (optionMap.put(name, field) != null) {
                        throw new RuntimeException("found multiple @Options sharing the name '" + name + "'");
                    }
                }
                if (handlers.get(field.getType()) == null) {
                    throw new RuntimeException("unsupported @Option field type '" + field.getType() + "'");
                }
            }
        }
        return optionMap;
    }
    
    static abstract class Handler {
        // Only BooleanHandler should ever override this.
        boolean isBoolean() {
            return false;
        }
        
        void setValue(Object object, Field field, String name, String valueText) {
            final Object value = translate(valueText);
            if (value == null) {
                final String type = field.getType().getSimpleName().toLowerCase();
                throw new RuntimeException("couldn't convert '" + valueText + "' to a " + type + " for option '" + name + "'");
            }
            try {
                field.setAccessible(true);
                field.set(object, value);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("internal error", ex);
            }
        }
        
        /**
         * Returns an object of appropriate type for the given Handle, corresponding to 'valueText'.
         * Returns null on failure.
         */
        abstract Object translate(String valueText);
    }
    
    static class BooleanHandler extends Handler {
        @Override boolean isBoolean() {
            return true;
        }
        
        Object translate(String valueText) {
            if (valueText.equalsIgnoreCase("true") || valueText.equalsIgnoreCase("yes")) {
                return Boolean.TRUE;
            } else if (valueText.equalsIgnoreCase("false") || valueText.equalsIgnoreCase("no")) {
                return Boolean.FALSE;
            }
            return null;
        }
    }
    
    static class IntegerHandler extends Handler {
        Object translate(String valueText) {
            try {
                return Integer.parseInt(valueText);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
    
    static class DoubleHandler extends Handler {
        Object translate(String valueText) {
            try {
                return Double.parseDouble(valueText);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
    
    static class StringHandler extends Handler {
        Object translate(String valueText) {
            return valueText;
        }
    }
    
    @TestHelper private static class TestOptions {
        @Option(names = {"-a"}) boolean a = false;
        @Option(names = {"-b"}) boolean b = false;
        @Option(names = {"-c"}) boolean c = false;
        @Option(names = {"--flag"}) boolean flag = true;
        @Option(names = {"-n", "--name"}) String name;
        @Option(names = {"-d"}) double d = 0.0;
        @Option(names = {"-i"}) int i = 0;
    }
    
    @Test private static void testOptionParser() {
        TestOptions o;
        
        Assert.equals(new OptionParser(new TestOptions()).parse(new String[] { "hello", "world" }), Arrays.asList("hello", "world"));
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--no-flag" }), Collections.emptyList());
        Assert.equals(o.flag, false);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--no-flag", "--flag" }), Collections.emptyList());
        Assert.equals(o.flag, true);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--flag=true" }), Collections.emptyList());
        Assert.equals(o.flag, true);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--flag=false" }), Collections.emptyList());
        Assert.equals(o.flag, false);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--flag=yes" }), Collections.emptyList());
        Assert.equals(o.flag, true);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--flag=no" }), Collections.emptyList());
        Assert.equals(o.flag, false);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-n", "hello", "world" }), Arrays.asList("world"));
        Assert.equals(o.name, "hello");
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-nhello", "world" }), Arrays.asList("world"));
        Assert.equals(o.name, "hello");
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--name", "hello", "world" }), Arrays.asList("world"));
        Assert.equals(o.name, "hello");
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "--name=hello", "world" }), Arrays.asList("world"));
        Assert.equals(o.name, "hello");
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-a", "--", "-a" }), Arrays.asList("-a"));
        Assert.equals(o.a, true);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-a", "-b", "hello" }), Arrays.asList("hello"));
        Assert.equals(o.a, true);
        Assert.equals(o.b, true);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-ab", "hello" }), Arrays.asList("hello"));
        Assert.equals(o.a, true);
        Assert.equals(o.b, true);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-abnhello", "world" }), Arrays.asList("world"));
        Assert.equals(o.a, true);
        Assert.equals(o.b, true);
        Assert.equals(o.name, "hello");
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-abn", "hello", "world" }), Arrays.asList("world"));
        Assert.equals(o.a, true);
        Assert.equals(o.b, true);
        Assert.equals(o.name, "hello");
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-i", "123" }), Collections.emptyList());
        Assert.equals(o.i, 123);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-i", "-123" }), Collections.emptyList());
        Assert.equals(o.i, -123);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-d", "123.0" }), Collections.emptyList());
        Assert.equals(o.d, 123.0);
        
        o = new TestOptions();
        Assert.equals(new OptionParser(o).parse(new String[] { "-d", "-123.0" }), Collections.emptyList());
        Assert.equals(o.d, -123.0);
    }
}
