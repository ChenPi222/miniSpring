package aopTest;

import com.miniSpring.aop.AroundProxyBeanPostProcessor;
import com.miniSpring.aop.ProxyResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName: ProxyResolverTest
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 19:39
 * @Version 1.0
 */

public class ProxyResolverTest {
    @Test
    public void test(){
        OriginBean origin = new OriginBean();
        origin.name = "Bob";
        assertEquals("Hello, Bob.", origin.hello());

        OriginBean proxy = ProxyResolver.getInstance().createProxy(origin, new PoliteInvocationHandler());
        System.out.println(proxy.getClass().getName());

        // proxy class, not origin class:
        assertNotSame(OriginBean.class, proxy.getClass());
        // proxy.name is null:
        assertNull(proxy.name);

        // 带@Polite:
        assertEquals("Hello, Bob!", proxy.hello());
        // 不带@Polite:
        assertEquals("Morning, Bob.", proxy.morning());
    }
    
    @Test
    public void test2(){
        Type type = new AroundProxyBeanPostProcessor().getClass().getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType) type;
        Type[] types = pt.getActualTypeArguments();
        System.out.println(1);
    }
}
