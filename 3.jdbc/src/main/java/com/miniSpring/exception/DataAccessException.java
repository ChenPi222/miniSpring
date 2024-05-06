package com.miniSpring.exception;

/**
 * ClassName: DataAccessException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/28 18:04
 * @Version 1.0
 */
public class DataAccessException extends NestedRuntimeException{
    public DataAccessException() {
    }

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
