package com.miniSpring.exception;

/**
 * ClassName: NestedRuntimeException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/19 9:41
 * @Version 1.0
 */
public class NestedRuntimeException extends RuntimeException{
    public NestedRuntimeException() {
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }
}
