package org.jessies.cli;

import java.lang.annotation.*;

/**
 * Annotates a field as representing a command-line option for OptionParser.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
    /**
     * The names for this option, such as { "-h", "--help" }.
     * Names must start with one or two '-'s.
     * An option must have at least one name.
     */
    String[] names();
    
    /**
     * Help text to print for this option when listing options.
     * The default is to list the option but to have no help text.
     * Setting the help to null will prevent the option from even being listed.
     */
    String help() default "";
}
