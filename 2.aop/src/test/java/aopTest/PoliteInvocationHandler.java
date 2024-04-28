package aopTest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * ClassName: PoliteInvocationHandler
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 19:35
 * @Version 1.0
 */
public class PoliteInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object bean, Method method, Object[] args) throws Throwable {
        if(method.getAnnotation(Polite.class) != null) {
            String ret = (String) method.invoke(bean, args);
            if(ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!";
            }
            return ret;
        }
        return method.invoke(bean, args);
    }
}
