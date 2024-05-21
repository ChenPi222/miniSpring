package com.miniSpring.exception;

/**
 * ClassName: ErrorResponseException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 17:05
 * @Version 1.0
 */
public class ErrorResponseException extends NestedRuntimeException{
    public final int statusCode;

    public ErrorResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    public ErrorResponseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ErrorResponseException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public ErrorResponseException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }
}
