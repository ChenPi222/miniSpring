package com.miniSpring.exception;

/**
 * ClassName: ServerWebInputException
 * Description:
 * 400 client error
 * @Author Jeffer Chen
 * @Create 2024/5/7 17:12
 * @Version 1.0
 */
public class ServerWebInputException extends ErrorResponseException{
    private static final Integer SERVER_ERROR_CODE = 400;
    public ServerWebInputException() {
        super(SERVER_ERROR_CODE);
    }

    public ServerWebInputException(String message) {
        super(message, SERVER_ERROR_CODE);
    }

    public ServerWebInputException(String message, Throwable cause) {
        super(message, cause, SERVER_ERROR_CODE);
    }

    public ServerWebInputException(Throwable cause) {
        super(cause, SERVER_ERROR_CODE);
    }
}
