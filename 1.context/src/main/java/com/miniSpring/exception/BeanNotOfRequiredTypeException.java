package com.miniSpring.exception;

/**
 * ClassName: BeanNotOfRequiredTypeException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:45
 * @Version 1.0
 */
public class BeanNotOfRequiredTypeException extends BeansException{
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
