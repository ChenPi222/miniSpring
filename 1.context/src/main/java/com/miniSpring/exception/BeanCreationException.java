package com.miniSpring.exception;

/**
 * ClassName: BeanCreationException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:43
 * @Version 1.0
 */
public class BeanCreationException extends BeansException{
    public BeanCreationException() {
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}
