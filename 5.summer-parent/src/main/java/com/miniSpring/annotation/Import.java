package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Import
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:23
 * @Version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {
    Class<?>[] value();
}
