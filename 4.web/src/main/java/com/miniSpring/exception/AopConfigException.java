package com.miniSpring.exception;

/**
 * ClassName: AopConfigException
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 20:41
 * @Version 1.0
 */
public class AopConfigException extends NestedRuntimeException{
    public AopConfigException() {
    }

    public AopConfigException(String message) {
        super(message);
    }

    public AopConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public AopConfigException(Throwable cause) {
        super(cause);
    }
}
