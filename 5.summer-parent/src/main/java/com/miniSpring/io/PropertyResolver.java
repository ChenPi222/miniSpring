package com.miniSpring.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * ClassName: io.PropertyResolver
 * Description:
 * 解析Properties文件
 * @Author Jeffer Chen
 * @Create 2024/4/16 18:33
 * @Version 1.0
 */
public class PropertyResolver {
    Logger logger = LoggerFactory.getLogger(getClass());

    Map<String, String> properties = new HashMap<>();

    //存储Class->Function
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();


    public PropertyResolver(Properties props) {
        //存入环境变量
        this.properties.putAll(System.getenv());
        //存入Properties文件中的key-value值
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
         //Debug模式下输出日志内容
        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("io.PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }

        //将各种要转换的类型放到Map里
        converters.put(String.class, s -> s);
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }

    @Nullable //该注解表示方法返回值可以为空
    public String getProperty(String key) {
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if(keyExpr != null){
            //不为空代表为${}格式
            if(keyExpr.defaultValue() != null){
                //带默认值查询,如果该key的Value为空则赋予默认值
                return getProperty(keyExpr.key(), keyExpr.defaultValue()); //key()为record类自带的get方法
            } else {
                //不带默认值查询
                return getRequiredProperty(keyExpr.key());
            }
        }
        //普通key查询
        String value = this.properties.get(key);
        if(value != null){
            return parseValue(value);
        }
        return value;
    }



    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    /**
     * 某些value可能也是引用值，需要解析后寻找真实值
     * @param value
     * @return
     */
    private String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        if(expr == null) {
            //表示value不需要解析
            return value;
        }
        if(expr.defaultValue() != null) {
            return getProperty(expr.key(), expr.defaultValue());
        }else {
            return getRequiredProperty(expr.key());
        }
    }

    /**
     * 存储解析后的key和defaultValue
     * @param key
     * @param defaultValue
     */

    /**
     * 解析${key:defaultValue}这样格式的输入，若无value则使用defaultValue
     * @param key
     * @return 如果不是这种格式，返回null，如果是，返回解析后的key-defaultValue
     */
    PropertyExpr parsePropertyExpr(String key){
        if(key.startsWith("${") && key.endsWith("}")) {
            //是否存在defaultValue？
            int n = key.indexOf(":");
            if(n == -1){
                //代表没有defaultValue  ${key}
                String k = notEmpty(key.substring(2, key.length()-1));
                return new PropertyExpr(k, null);
            } else {
                //有defaultValue
                String k = notEmpty(key.substring(2, n));
                String v = key.substring(n + 1, key.length() - 1);
                return new PropertyExpr(k, v);
            }
        }
        return null;
    }

    /**
     * 获取指定类型的value
     * @param key
     * @param targetType
     * @return
     * @param <T>
     */
    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        //将Value转换为指定类型
        return convert(targetType, value);
    }

    /**
     * 获取指定类型的Value，若为空则返回默认值
     * @param key
     * @param targetType
     * @param defaultValue
     * @return
     * @param <T>
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        //将Value转换为指定类型
        return convert(targetType, value);
    }

    /**
     * 将value转换到指定Class类型
     * @param clazz
     * @param value
     * @return
     * @param <T>
     */
    @SuppressWarnings("unchecked") //告诉编译器忽略unchecked警告信息
    <T> T convert(Class<?> clazz, String value){
        Function<String, Object> fn = this.converters.get(clazz);
        if(fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /**
     * 如果value为空则报指定message错误
     * @param key
     * @return
     */
    public String getRequiredProperty(String key){
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property'" + key + "' not found");
    }

    /**
     * 如果value为空则报指定message错误
     * @param key
     * @param targetType
     * @return
     * @param <T>
     */
    public <T> T getRequiredProperty(String key, Class<T> targetType){
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property'" + key + "' not found");
    }

    /**
     * 若key为空则报错
     * @param key
     * @return 不为空就返回原值
     */
    String notEmpty(String key) {
        if(key.isEmpty()){
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }
}
record PropertyExpr(String key, String defaultValue){}
