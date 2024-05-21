package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Around
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 20:37
 * @Version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited //子类可以继承该注解
@Documented
public @interface Around {
    /**
     * Invocation handler bean name.
     */
    String value();
}
