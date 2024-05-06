package com.miniSpring.aop;

import com.miniSpring.context.ApplicationContextUtils;
import com.miniSpring.context.BeanDefinition;
import com.miniSpring.context.BeanPostProcessor;
import com.miniSpring.context.ConfigurableApplicationContext;
import com.miniSpring.exception.AopConfigException;
import com.miniSpring.exception.BeansException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: AnnotationProxyBeanPostProcessor
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 21:01
 * @Version 1.0
 */
public abstract class AnnotationProxyBeanPostProcessor<A extends Annotation> implements BeanPostProcessor {
    //存储代理前的原始bean
    Map<String, Object> originBeans = new HashMap<>();
    Class<A> annotationClass;
    
    public AnnotationProxyBeanPostProcessor(){
        //泛型即为注解的class
        this.annotationClass = getParameterizedType();
    }

    /**
     * 普通Beans实例化过程中会依次调用所有BeanPostProcessor的该方法，如果命中条件则会执行代理步骤
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        //判断当前Bean是否含有相应注解
        A anno = beanClass.getAnnotation(annotationClass);
        if(anno != null) {
            String handlerName;
            try {
                //获取注解上标明的handler Name
                handlerName = (String) anno.annotationType().getMethod("value").invoke(anno);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(String.format("@%s must have value() returned String type.",
                        this.annotationClass.getSimpleName()), e);
            }
            //调用createProxy方法获取代理对象
            Object proxy = createProxy(beanClass, bean, handlerName);
            //将代理前的原始Bean存入map，方便之后获取
            originBeans.put(beanName, bean);
            return proxy;
        }else {
            return bean;
        }
    }

    /**
     * 根据handlerName从AnnotationConfigApplicationContext对象的beans Map中获取handler实例，再将bean和Handler传入ProxyResolver
     * 生成相应的代理对象
     * @param beanClass 这里的beanClass是否多余了？
     * @param bean
     * @param handlerName
     * @return 返回生成的代理对象
     */
    Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        var ctx = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        //获取handler的BeanDefinition
        BeanDefinition def = ctx.findBeanDefinition(handlerName);
        if (def == null) {
            throw new AopConfigException();
        }
        //获取handler的实例
        Object handlerBean = def.getInstance();
        //如果handlerBean还没创建Instance（原始bean与Handler bean都是在同一批生成Instance），则调用createBean方法生成
        if (handlerBean == null) {
            handlerBean = ctx.createBeanAsEarlySingleton(def);
        }
        if (handlerBean instanceof InvocationHandler handler) {
            //调用ProxyResolver中创建代理对象的方法（根据Bean和拦截器方法）
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException(String.format("@%s proxy handler '%s' is not type of %s.",
                    this.annotationClass.getSimpleName(), handlerName, InvocationHandler.class.getName()));
        }
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = this.originBeans.get(beanName);
        return origin != null ? origin : bean;
    }

    /**
     * 获取当前类的直接父类中泛型的类，即要知道当前类需要解析Bean中的什么注解，例如@Around、@Transactional等
     * @return
     */
    @SuppressWarnings("unchecked")
    private Class<A> getParameterizedType() {
        //getGenericSuperclass()返回表示直接父类的ParameterizedType对象，比如AroundProxyBeanPostProcessor对象调用该方法得到的结果
        //是父类AnnotationProxyBeanPostProcessor.class<Around.class>
        Type type = getClass().getGenericSuperclass();
        //ParameterizedType表示带有泛型参数的类型
        if(!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + "does not have parameterized type.");
        }
        ParameterizedType pt = (ParameterizedType) type;
        //获取pt的泛型类，即<>中的内容
        Type[] types = pt.getActualTypeArguments();
        if(types.length != 1){
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type r = types[0];
        if(!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }
}
