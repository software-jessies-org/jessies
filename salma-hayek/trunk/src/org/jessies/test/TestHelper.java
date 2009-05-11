package org.jessies.test;

import java.lang.annotation.*;

/**
 * Indicates that the annotated method is a helper for a test method.
 * This is intended as a self-documenting convenience for humans.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestHelper {
}
