package com.miniSpring.exception;

/**
 * ClassName: BeanDefinitionException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:44
 * @Version 1.0
 */
public class BeanDefinitionException extends BeansException{
    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }
}
