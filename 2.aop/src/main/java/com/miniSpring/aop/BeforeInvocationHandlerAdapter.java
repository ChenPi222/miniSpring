package com.miniSpring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * ClassName: BeforeInvocationHandlerAdapter
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/26 21:36
 * @Version 1.0
 */
public abstract class BeforeInvocationHandlerAdapter implements InvocationHandler {
    public abstract void before(Object proxy, Method method, Object[] args);

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before(proxy, method, args);
        return method.invoke(proxy, args);
    }
}
