package com.miniSpring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * ClassName: AfterInvocationHandlerAdapter
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/26 21:34
 * @Version 1.0
 */
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {

    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);
    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = method.invoke(proxy,args);
        return after(proxy, ret, method, args);
    }
}
