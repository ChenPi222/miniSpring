package com.miniSpring.aop;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * ClassName: ProxyResolver
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 19:08
 * @Version 1.0
 */
public class ProxyResolver {
    final Logger logger = LoggerFactory.getLogger(getClass());
    //ByteBuddy实例
    final ByteBuddy byteBuddy = new ByteBuddy();
    private static ProxyResolver INSTANCE = null;

    /**
     * 懒汉单例模式？
     * @return
     */
    public static ProxyResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProxyResolver();
        }
        return INSTANCE;
    }

    private ProxyResolver() {
    }

    /**
     * 传入原始Bean、拦截器，返回代理后的实例
     * @param bean
     * @param handler
     * @return
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        //目标Bean的Class类型
        Class<?> targetClass = bean.getClass();
        //动态创建Proxy的Class
        Class<?> proxyClass = this.byteBuddy
                //子类用默认无参构造方法
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                //拦截所有public方法
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        //新的拦截器实例
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                //将方法调用代理至原始Bean
                                return handler.invoke(bean, method,args);
                            }
                        }))
                //生成字节码
                .make()
                //加载字节码
                .load(targetClass.getClassLoader()).getLoaded();
        //创建Proxy实例
        Object proxy;
        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }
}
