package com.miniSpring.exception;

/**
 * ClassName: BeansException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:40
 * @Version 1.0
 */
public class BeansException extends NestedRuntimeException{
    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException() {
    }
}
