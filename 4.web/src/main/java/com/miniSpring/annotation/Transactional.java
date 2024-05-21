package com.miniSpring.annotation;

import java.lang.annotation.*;

/**
 * ClassName: Transactional
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 21:13
 * @Version 1.0
 */
@Target(ElementType.TYPE)//注意这里只允许定义在类上，与SpringBoot不同
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {
    String value() default "platformTransactionManager"; //默认用这个Bean来管理事务
}
