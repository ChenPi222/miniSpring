package com.miniSpring.context;

import java.util.List;

/**
 * ClassName: ApplicationContext
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/24 16:44
 * @Version 1.0
 */
public interface ApplicationContext extends AutoCloseable{ //autocloseable需要结合try with resource一起使用
    /**
     * 是否存在指定name的Bean？
     */
    boolean containsBean(String name);

    /**
     * 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     */
    <T> T getBean(String name);

    /**
     * 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException，找到但type不符抛出BeanNotOfRequiredTypeException
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 根据type返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 根据type返回一组Bean，未找到返回空List
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * 关闭并执行所有bean的destroy方法
     */
    void close();
}
