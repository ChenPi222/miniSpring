package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: RestController
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 16:57
 * @Version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RestController {
    String value() default "";
}
