package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Configuration
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:22
 * @Version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    /**
     * Bean name. Default to simple class name with first-letter-lower-case.
     */
    String value() default "";
}
