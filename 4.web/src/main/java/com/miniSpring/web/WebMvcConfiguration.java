package com.miniSpring.web;

import com.miniSpring.annotation.Autowired;
import com.miniSpring.annotation.Bean;
import com.miniSpring.annotation.Configuration;
import com.miniSpring.annotation.Value;
import jakarta.servlet.ServletContext;

import java.util.Objects;

/**
 * ClassName: WebMvcConfiguration
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/9 14:39
 * @Version 1.0
 */
@Configuration
public class WebMvcConfiguration {
    private static ServletContext servletContext = null;

    //set by web listener TODO 这里修饰符为public，源代码为空
    public static void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    @Bean(initMethod = "init") //这里的init方法调用的是对应类里的
    ViewResolver viewResolver(
            @Autowired ServletContext servletContext,
            @Value("${miniSpring.web.freemarker.template-path:/WEB-INF/templates}") String templatePath,
            @Value("${miniSpring.web.freemarker.template-encoding:UTF-8}") String templateEncoding) {
        return new FreeMarkerViewResolver(templatePath, templateEncoding, servletContext);
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set");
    }
}
