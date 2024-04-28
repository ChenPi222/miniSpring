package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Component
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/18 21:47
 * @Version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented //有该注解的注解，才能在生成文档时显示出来
public @interface Component {
    /**
     * Bean name. Default to simple class name with first-letter-lowercase.
     */
    String value() default "";
}
