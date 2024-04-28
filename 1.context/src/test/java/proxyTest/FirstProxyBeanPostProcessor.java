package proxyTest;

import com.miniSpring.annotation.Component;
import com.miniSpring.annotation.Order;
import com.miniSpring.context.BeanPostProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: FirstProxyBeanPostProcessor
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/23 15:49
 * @Version 1.0
 */
@Order(100)
@Component
public class FirstProxyBeanPostProcessor implements BeanPostProcessor {
    // 保存原始Bean:
    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            // 检测到OriginBean,创建FirstProxyBean:
            var proxy = new FirstProxyBean((OriginBean) bean);
            // 保存原始Bean:
            originBeans.put(beanName, bean);
            // 返回Proxy:
            return proxy;
        }
        return bean;
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = originBeans.get(beanName);
        if (origin != null) {
            // 存在原始Bean时,返回原始Bean:
            return origin;
        }
        return bean;
    }
}