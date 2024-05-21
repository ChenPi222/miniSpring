package com.miniSpring.exception;

/**
 * ClassName: UnsatisfiedDependencyException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:47
 * @Version 1.0
 */
public class UnsatisfiedDependencyException extends BeanCreationException{
    public UnsatisfiedDependencyException() {
    }

    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsatisfiedDependencyException(Throwable cause) {
        super(cause);
    }
}
