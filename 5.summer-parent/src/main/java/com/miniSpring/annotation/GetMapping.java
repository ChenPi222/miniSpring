package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: GetMapping
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 16:59
 * @Version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GetMapping {
    /**
     * URL mapping
     */
    String value();
}
