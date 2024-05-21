package com.miniSpring.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * ClassName: ViewResolver
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/8 12:33
 * @Version 1.0
 */
public interface ViewResolver {
    //初始化
    void init();

    //渲染
    void render(String viewName, Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp)
            throws IOException;
}
