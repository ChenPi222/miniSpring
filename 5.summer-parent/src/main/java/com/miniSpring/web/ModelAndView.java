package com.miniSpring.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: ModelAndView
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/9 15:00
 * @Version 1.0
 */
public class ModelAndView {
    private String view;
    private Map<String, Object> model;
    int status;

    public ModelAndView(String view, int status, Map<String, Object> model) {
        this.view = view;
        this.status = status;
        if (model != null) {
            addModel(model);
        }
    }

    public ModelAndView(String viewName, String modelName, Object modelObject) {
        this(viewName, HttpServletResponse.SC_OK, null);
        addModel(modelName, modelObject);
    }

    public ModelAndView(String viewName) {
        this(viewName, HttpServletResponse.SC_OK, null);
    }

    public ModelAndView(String viewName, @Nullable Map<String, Object> model) {
        this(viewName, HttpServletResponse.SC_OK, model);
    }

    public ModelAndView(String viewName, int status) {
        this(viewName, status, null);
    }

    public String getViewName() {
        return this.view;
    }

    public void addModel(Map<String, Object> map) {
        if(this.model == null) {
            this.model = new HashMap<>();
        }
        this.model.putAll(map);
    }

    public void addModel(String key, Object value) {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        this.model.put(key, value);
    }

    public Map<String, Object> getModel() {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        return this.model;
    }

    public int getStatus() {
        return this.status;
    }
}
