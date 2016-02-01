package org.jessies.test;

import java.lang.annotation.*;

/**
 * Indicates that the annotated method is a test method for the simple Java unit testing framework.
 * For use only on zero-argument private static void methods. (The test runner checks this.)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}
