package com.miniSpring.aop;

import com.miniSpring.annotation.Around;
import com.miniSpring.context.ApplicationContextUtils;
import com.miniSpring.context.BeanDefinition;
import com.miniSpring.context.BeanPostProcessor;
import com.miniSpring.context.ConfigurableApplicationContext;
import com.miniSpring.exception.AopConfigException;
import com.miniSpring.exception.BeansException;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: AroundProxyBeanPostProcessor
 * Description:
 * 为@Around注解创建Proxy，其他注解也类似
 * @Author Jeffer Chen
 * @Create 2024/4/25 20:35
 * @Version 1.0
 */
public class AroundProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Around> {

}
