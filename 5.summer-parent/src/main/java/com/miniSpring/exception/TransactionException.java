package com.miniSpring.exception;

/**
 * ClassName: TransactionException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 21:16
 * @Version 1.0
 */
public class TransactionException extends DataAccessException{
    public TransactionException() {
    }

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
