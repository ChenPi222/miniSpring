package com.miniSpring.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * ClassName: ApplicationContextUtils
 * Description:
 * 用来设置和获取ApplicationContext实例
 * @Author Jeffer Chen
 * @Create 2024/4/25 14:30
 * @Version 1.0
 */
public class ApplicationContextUtils {
    private static ApplicationContext applicationContext = null;

    /**
     * 返回ApplicationContext，如果为空则抛NullPointerException(message)异常
     * @return
     */
    @Nonnull
    public static ApplicationContext getRequiredApplicationContext(){
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set.");
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    static void setApplicationContext(ApplicationContext ctx){
        applicationContext = ctx;
    }
}
