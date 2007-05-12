package org.jessies.blindvnc;

import java.util.*;

public class Options {
    private HashMap<String, Option> options = new HashMap<String, Option>();
    
    public Options(Option[] optionsList) {
        for (Option opt : optionsList) {
            options.put(opt.getName(), opt);
        }
    }
    
    public void dump() {
        for (String key : options.keySet()) {
            System.err.println("Key " + key + " has value " + options.get(key).getValue());
        }
    }
    
    public static class Option {
        private String name;
        private Class type;
        private boolean hasDefaultValue;
        private String defaultValue;
        private String currentValue;
        
        public Option(String name, Class type, boolean hasDefaultValue, String defaultValue) {
            this.name = name;
            this.type = type;
            this.hasDefaultValue = hasDefaultValue;
            this.defaultValue = defaultValue;
        }
        
        public Option(String name, String defaultValue) {
            this(name, String.class, true, defaultValue);
        }
        
        public Option(String name, boolean defaultValue) {
            this(name, Boolean.TYPE, true, String.valueOf(defaultValue));
        }
        
        public Option(String name, int defaultValue) {
            this(name, Integer.TYPE, true, String.valueOf(defaultValue));
        }
        
        public String getName() { return name; }
        public String getDefaultValue() { return defaultValue; }
        public boolean hasDefaultValue() { return hasDefaultValue; }
        public Class getType() { return type; }
        
        public void setValue(String value) {
            currentValue = value;
        }
        
        public void resetToDefault() {
            currentValue = defaultValue;
        }
        
        public String getValue() {
            return (currentValue == null) ? defaultValue : currentValue;
        }
    }
    
    /**
     * Parses the set of arguments, decoding all those with a '--' prefix, and returning the list of
     * arguments which were left over.  This stops parsing after the string '--' is seen.
     */
    public List<String> parseDashDashArguments(String[] arguments) {
        boolean seenDashDash = false;
        List<String> result = new ArrayList<String>();
        for (String arg : arguments) {
            if (seenDashDash == false) {
                if (arg.equals("--")) {
                    seenDashDash = true;
                } else if (arg.startsWith("--")) {
                    String key = arg.substring(2);  // Trim the dash-dash.
                    int equalsIndex = key.indexOf("=");
                    if (equalsIndex == -1) {
                        if (getOptionType(key) != Boolean.TYPE) {
                            System.err.println("Option " + arg + " must be given an argument.");
                        } else {
                            setBooleanOption(key, true);
                        }
                    } else {
                        setOption(key.substring(0, equalsIndex), key.substring(equalsIndex + 1));
                    }
                } else {
                    result.add(arg);
                }
            } else {
                result.add(arg);
            }
        }
        return result;
    }
    
    private void setOption(String key, String value) {
        Class type = getOptionType(key);
        if (type == Boolean.TYPE) {
            if (value.equalsIgnoreCase("true")) {
                setBooleanOption(key, true);
            } else if (value.equalsIgnoreCase("false")) {
                setBooleanOption(key, false);
            } else {
                System.err.println("Option --" + key + " requires either 'true' or 'false' as argument.");
            }
        } else if (type == Integer.TYPE) {
            try {
                setIntOption(key, Integer.parseInt(value));
            } catch (NumberFormatException ex) {
                System.err.println("Option --" + key + " requires an integer as argument.");
            }
        } else if (type == String.class) {
            setStringOption(key, value);
        } else {
            throw new IllegalArgumentException("Option --" + key + " of unsupported type " + type);
        }
    }
    
    private void setBooleanOption(String key, boolean value) {
        setStringOption(key, String.valueOf(value));
    }
    
    private void setIntOption(String key, int value) {
        setStringOption(key, String.valueOf(value));
    }
    
    private void setStringOption(String key, String value) {
        options.get(key).setValue(value);
    }
    
    public Class getOptionType(String key) {
        if (options.containsKey(key)) {
            return options.get(key).getType();
        } else {
            return null;
        }
    }
    
    public int getIntOption(String key) {
        if (getOptionType(key) == Integer.TYPE) {
            return Integer.parseInt(options.get(key).getValue());
        } else {
            throw new IllegalArgumentException("Option --" + key + " is not an integer option.");
        }
    }
    
    public boolean getBooleanOption(String key) {
        if (getOptionType(key) == Boolean.TYPE) {
            return Boolean.parseBoolean(options.get(key).getValue());
        } else {
            throw new IllegalArgumentException("Option --" + key + " is not an integer option.");
        }
    }
    
    public String getStringOption(String key) {
        if (getOptionType(key) != null) {
            return options.get(key).getValue();
        } else {
            throw new IllegalArgumentException("Option --" + key + " is simply not an option.");
        }
    }
}
