package com.miniSpring.exception;

/**
 * ClassName: ServerErrorException
 * Description:
 * 500 internal server error
 * @Author Jeffer Chen
 * @Create 2024/5/7 17:08
 * @Version 1.0
 */
public class ServerErrorException extends ErrorResponseException{
    private static final Integer SERVER_ERROR_CODE = 500;
    public ServerErrorException() {
        super(SERVER_ERROR_CODE);
    }

    public ServerErrorException(String message) {
        super(message, SERVER_ERROR_CODE);
    }

    public ServerErrorException(String message, Throwable cause) {
        super(message, cause, SERVER_ERROR_CODE);
    }

    public ServerErrorException(Throwable cause) {
        super(cause, SERVER_ERROR_CODE);
    }
}
