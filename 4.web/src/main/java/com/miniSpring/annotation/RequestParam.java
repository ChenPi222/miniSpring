package com.miniSpring.annotation;

import com.miniSpring.web.utils.WebUtils;

import java.lang.annotation.*;

/**
 * ClassName: RequestParam
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 17:03
 * @Version 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value();

    String defaultValue() default WebUtils.DEFAULT_PARAM_VALUE;
}
