package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Autowired
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:15
 * @Version 1.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    /**
     * Is required.
     */
    boolean value() default true;

    /**
     * Bean name if set.
     */
    String name() default "";
}
