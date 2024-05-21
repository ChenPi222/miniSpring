package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Controller
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 16:56
 * @Version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
}
