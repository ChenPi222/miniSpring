package com.miniSpring.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * ClassName: View
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/9 14:52
 * @Version 1.0
 */

//TODO 这个接口的实现在哪里？
public interface View {

    @Nullable
    default String getContentType() {
        return null;
    }

    void render(@Nullable Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
