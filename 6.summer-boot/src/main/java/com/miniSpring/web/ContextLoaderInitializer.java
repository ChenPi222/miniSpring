package com.miniSpring.web;

import com.miniSpring.context.AnnotationConfigApplicationContext;
import com.miniSpring.context.ApplicationContext;
import com.miniSpring.io.PropertyResolver;
import com.miniSpring.web.utils.WebUtils;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * ClassName: ContextLoaderInitializer
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/13 15:55
 * @Version 1.0
 */
public class ContextLoaderInitializer implements ServletContainerInitializer {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final Class<?> configClass;
    final PropertyResolver propertyResolver;

    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
        this.configClass = configClass;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext ctx) throws ServletException {
        logger.info("Servlet container start. ServletContext = {}", ctx);

        String encoding = propertyResolver.getProperty("${miniSpring.web.character-encoding:UTF-8}");
        ctx.setRequestCharacterEncoding(encoding);
        ctx.setResponseCharacterEncoding(encoding);

        //设置ServletContext
        WebMvcConfiguration.setServletContext(ctx);
        //启动IoC容器
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(this.configClass, this.propertyResolver);
        logger.info("Application context created: {}", applicationContext);

        //注册Filter与DispatcherServlet
        WebUtils.registerFilters(ctx);
        WebUtils.registerDispatchServlet(ctx, this.propertyResolver);
    }
}
