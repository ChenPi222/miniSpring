package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: PathVariable
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 17:00
 * @Version 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    String value();
}
