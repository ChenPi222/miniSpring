package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Primary
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:24
 * @Version 1.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
}
