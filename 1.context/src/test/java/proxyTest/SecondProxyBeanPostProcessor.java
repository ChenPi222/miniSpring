package proxyTest;

import com.miniSpring.annotation.Component;
import com.miniSpring.annotation.Order;
import com.miniSpring.context.BeanPostProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: SecondProxyBeanPostProcessor
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/23 15:52
 * @Version 1.0
 */
@Order(200)
@Component
public class SecondProxyBeanPostProcessor implements BeanPostProcessor {
    // 保存原始Bean:
    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            // 检测到OriginBean,创建SecondProxyBean:
            var proxy = new SecondProxyBean((OriginBean) bean);
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
