package com.miniSpring.exception;

/**
 * ClassName: NoUniqueBeanDefinitionException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:46
 * @Version 1.0
 */
public class NoUniqueBeanDefinitionException extends BeanDefinitionException{
    public NoUniqueBeanDefinitionException() {
    }

    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}
