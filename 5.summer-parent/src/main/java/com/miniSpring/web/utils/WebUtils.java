package com.miniSpring.web.utils;

import com.miniSpring.context.ApplicationContext;
import com.miniSpring.context.ApplicationContextUtils;
import com.miniSpring.io.PropertyResolver;
import com.miniSpring.utils.ClassPathUtils;
import com.miniSpring.utils.YamlUtils;
import com.miniSpring.web.DispatcherServlet;
import com.miniSpring.web.FilterRegistrationBean;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * ClassName: WebUtils
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/7 11:47
 * @Version 1.0
 */
public class WebUtils {
    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";
    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    static final String CONFIG_APP_YAML = "/application.yml";
    static final String CONFIG_APP_PROP = "/application.properties";

    /**
     * 创建DispatcherServlet实例，并向Servlet容器注册
     * @param servletContext
     * @param propertyResolver
     */
    public static void registerDispatchServlet(ServletContext servletContext, PropertyResolver propertyResolver) {
        //这里ApplicationContextUtils.getRequiredApplicationContext()获取的实例是什么时候放入的？ 答：IoC容器构造器里将this注入的
        //因此是先有IoC容器再有DispatcherServlet
        var dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), propertyResolver);
        logger.info("register servlet {} for URL '/", dispatcherServlet.getClass().getName());
        var dispatcherReg = servletContext.addServlet("dispatchServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }


    public static void registerFilters(ServletContext ctx) {
        var applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        for(var filterRegBean : applicationContext.getBeans(FilterRegistrationBean.class)) {
            List<String> urlPatterns = filterRegBean.getUrlPatterns();
            if(urlPatterns == null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("No url patterns for {}" + filterRegBean.getClass().getName());
            }
            var filter = Objects.requireNonNull(filterRegBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null.");
            logger.info("register filter '{}' {} for URLs: {}", filterRegBean.getName(), filter.getClass().getName(), String.join(", ", urlPatterns));
            var filterReg = ctx.addFilter(filterRegBean.getName(), filter);
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns.toArray(String[]::new));
        }
    }


    /**
     * 读取目录下的Application配置文件（先尝试yaml文件，若无则读取Properties文件）
     * @return
     */
    public static PropertyResolver createPropertyResolver() {
        final Properties props = new Properties();
        //尝试加载yaml文件
        try {
            //利用YamlUtils中的方法将yaml文件“拍平”为key-value map
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            //前一步如果报错则不会执行后续步骤
            logger.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if(value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e){
            if(e.getCause() instanceof FileNotFoundException) {
                //尝试加载Properties文件
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, (input) -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }
}
