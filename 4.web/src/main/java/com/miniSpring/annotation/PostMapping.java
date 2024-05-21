package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: PostMapping
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 17:01
 * @Version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostMapping {
    /**
     * URL mapping
     */
    String value();
}
