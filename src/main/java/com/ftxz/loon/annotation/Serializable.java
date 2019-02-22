package com.ftxz.loon.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * process class with annotation to implementing {@link java.io.Serializable} interface
 *
 * @author ftxz
 *
 * @since 1.0.0
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface Serializable {

    long value() default 1L;
}
