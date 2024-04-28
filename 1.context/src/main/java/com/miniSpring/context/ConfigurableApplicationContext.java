package com.miniSpring.context;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * ClassName: ConfigurableApplicationContext
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/24 16:54
 * @Version 1.0
 */
public interface ConfigurableApplicationContext extends ApplicationContext{
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    @Nullable
    BeanDefinition findBeanDefinition(String name);

    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    Object createBeanAsEarlySingleton(BeanDefinition def);
}
