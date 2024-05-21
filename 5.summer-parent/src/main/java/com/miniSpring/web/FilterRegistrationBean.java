package com.miniSpring.web;

import jakarta.servlet.Filter;

import java.util.List;

/**
 * ClassName: FilterRegistrationBean
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/5/9 17:28
 * @Version 1.0
 */
public abstract class FilterRegistrationBean {
    public abstract List<String> getUrlPatterns();

    /**
     * Get name by class name. Example:
     * ApiFilterRegistrationBean -> apiFilter
     * ApiFilterRegistration -> apiFilter
     * ApiFilterReg -> apiFilterReg
     */
    public String getName() {
        String name = getClass().getSimpleName();
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        if (name.endsWith("FilterRegistrationBean") && name.length() > "FilterRegistrationBean".length()) {
            return name.substring(0, name.length() - "FilterRegistrationBean".length());
        }
        if (name.endsWith("FilterRegistration") && name.length() > "FilterRegistration".length()) {
            return name.substring(0, name.length() - "FilterRegistration".length());
        }
        return name;
    };

    public abstract Filter getFilter();
}
