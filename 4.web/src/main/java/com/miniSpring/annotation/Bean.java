package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Bean
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:20
 * @Version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    /**
     * Bean name. default to method name.
     */
    String value() default "";

    String initMethod() default "";

    String destroyMethod() default "";
}
