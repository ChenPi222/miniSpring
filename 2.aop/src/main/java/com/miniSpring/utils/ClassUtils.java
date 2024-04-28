package com.miniSpring.utils;

import com.miniSpring.annotation.Bean;
import com.miniSpring.annotation.Component;
import com.miniSpring.exception.BeanDefinitionException;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassName: ClassUtils
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/18 19:14
 * @Version 1.0
 */
public class ClassUtils {
    /**
     * 递归查找Annotation
     *
     * 示例：Annotation A可以直接标注在Class定义:
     * @A
     * public class Hello {}
     *
     * 或者Annotation B标注了A，Class标注了B:
     * <code>
     * &#64;A
     * public @interface B {}
     *
     * @B
     * public class Hello {}
     * </code>
     */

    /**
     * 递归查找注解的方法
     * 不但要在当前类查找目标注解，还要在当前类的所有注解上，查找该注解是否有目标注解（例如一个注解嵌套了两个注解）
     * @param target
     * @param annoClass
     * @return
     * @param <A>
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        //获取目标Class中的A注解
        A a = target.getAnnotation(annoClass);
        //遍历目标Class的注解，查看该注解是否包含目标注解
        for (Annotation anno : target.getAnnotations()){
            Class<? extends Annotation> annoType = anno.annotationType();
            if(!annoType.getPackageName().equals("java.lang.annotation")) {//lang包下的注解不用再看了
                A found = findAnnotation(annoType, annoClass);
                if(found != null) {
                    if (a != null) { //注解重复
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() +
                                " found on class " + target.getSimpleName());
                    }
                    a = found;
                }
            }
        }
        return a;
    }

    /**
     * 如果annos中有annoClass类的注解，则返回该注解
     * @param annos
     * @param annoClass
     * @return
     * @param <A>
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation[] annos, Class<A> annoClass) {
        for (Annotation anno : annos) {
            if (annoClass.isInstance(anno)) {
                return (A) anno;
            }
        }
        return null;
    }

    /**
     * 通过类名获取BeanName(不适用于Configuration类的Bean）
     * @param clazz
     * @return
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 查找Component
        Component component = clazz.getAnnotation(Component.class);
        if(component != null) {
            name = component.value();
        } else {
            //未在当前类找到@Component，继续在其他注解中查找@Component
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }
        if(name.isEmpty()) {
            //如果name为空，则默认使用Class名首字母小写
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    /**
     * 通过Bean注解中信息获取name，若没有就以方法名作为BeanName
     * @param method
     * @return
     */
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    /**
     * 用来获取目标类中的init/destroy方法
     * @param clazz
     * @param annoClass
     * @return
     */
    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
        //先获取类的所有包含目标注解的方法
        List<Method> ms = Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annoClass))
                .map(m -> {
                    if (m.getParameterCount() != 0) {
                        //init/destroy方法不能有参数
                        throw new BeanDefinitionException(String.format("Method '%s' with @%s must not have " +
                                "argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName()));
                    }
                    return m;
                }).collect(Collectors.toList());
        if(ms.isEmpty()) {
            return null;
        }
        if (ms.size() == 1) {
            return ms.get(0);
        }
        //如果有多个初始化、销毁方法，报错
        throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s",
                annoClass.getSimpleName(), clazz.getName()));
    }

    public static Method getNamedMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (ReflectiveOperationException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
