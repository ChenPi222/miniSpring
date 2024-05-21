package com.miniSpring.web;

import com.miniSpring.context.AnnotationConfigApplicationContext;
import com.miniSpring.context.ApplicationContext;
import com.miniSpring.exception.NestedRuntimeException;
import com.miniSpring.io.PropertyResolver;
import com.miniSpring.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClassName: ContextLoaderListener
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 11:39
 * @Version 1.0
 */
public class ContextLoaderListener implements ServletContextListener { //监听ServletContext的创建与销毁，调用相应方法
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}.", getClass().getName());
        //从事件中获取ServletContext
        ServletContext servletContext = sce.getServletContext();
        //将servletContext设置到WebMvcConfiguration中，后续创建IoC容器的时候该配置类会生成ServletContext Bean
        WebMvcConfiguration.setServletContext(servletContext);
        //从yaml或Properties文件中读取配置信息
        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${miniSpring.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);
        //从web.xml文件中通过“Configuration”变量名读取配置类的完整类名，并由此创建IoC容器
        var applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        // 实例化DispatcherServlet并注册
        WebUtils.registerDispatchServlet(servletContext, propertyResolver);
        //将IoC容器与servletContext关联
        servletContext.setAttribute("applicationContext", applicationContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }

    /**
     * 通过传入的配置类全类名获取Class对象，与propertyResolver一起传入AnnotationConfigApplicationContext构造器中
     * @param configClassName
     * @param propertyResolver
     * @return
     */
    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        if(configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration':" + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }
}
