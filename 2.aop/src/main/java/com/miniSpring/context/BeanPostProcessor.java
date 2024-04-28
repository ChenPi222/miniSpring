package com.miniSpring.context;

/**
 * ClassName: BeanPostProcessor
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/23 14:48
 * @Version 1.0
 */
public interface BeanPostProcessor {
    /**
     * 新建完Bean之后调用
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName){
        return bean;
    }

    /**
     * 初始化方法之后调用
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * set属性注入时调用，返回原始的bean，用于注入依赖
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
