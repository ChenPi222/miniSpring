package com.miniSpring.exception;

/**
 * ClassName: NoSuchBeanDefinitionException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:45
 * @Version 1.0
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException{
    public NoSuchBeanDefinitionException() {
    }

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}
