package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Value
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:25
 * @Version 1.0
 */

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value();
}
