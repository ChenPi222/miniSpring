package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: ComponentScan
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:21
 * @Version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ComponentScan {
    /**
     * Package names to scan. Default to current package.
     */
    String[] value() default {};
}
