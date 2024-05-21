package com.miniSpring.jdbc;

import com.miniSpring.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: BeanRowMapper
 * Description:
 * 封装每一列数据库查询结果的bean
 * @Author Jeffer Chen
 * @Create 2024/4/29 10:03
 * @Version 1.0
 */
public class BeanRowMapper<T> implements RowMapper<T>{

    final Logger logger = LoggerFactory.getLogger(getClass());

    Class<T> clazz;
    Constructor<T> constructor;
    Map<String, Field> fields = new HashMap<>();
    Map<String, Method> methods = new HashMap<>();

    /**
     * 构造器，传入要封装的目标类，获取其所有属性和set方法，并分别存入fields和methods Map中
     * @param clazz
     */
    public BeanRowMapper(Class<T> clazz) {
        this.clazz = clazz;
        try {
            //必须有无参public构造器
            this.constructor = clazz.getConstructor();
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("No public default constructor found for class %s when build " +
                    "BeanRowMapper." , clazz.getName()), e);
        }
        for (Field f : clazz.getFields()) {
            String name = f.getName();
            this.fields.put(name, f);
            logger.atDebug().log("Add row mapping: {} to field {}", name, name);
        }
        //获取Setter方法，并将属性名和方法存入methods Map中
        for (Method m : clazz.getMethods()) {
            Parameter[] ps = m.getParameters();
            if(ps.length == 1){
                String name = m.getName();
                if(name.length() >= 4 && name.startsWith("set")) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    this.methods.put(prop, m);
                    logger.atDebug().log("Add row mapping: {} to {}({})", prop, name, ps[0].getType().getSimpleName());
                }
            }
        }
    }

    /**
     * 将结果集中的一行数据封装至目标类T
     * @param rs
     * @param rowNum 这里传入rowNum是否有些多余？
     * @return
     * @throws SQLException
     */
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean;
        try {
            bean = this.constructor.newInstance();
            //获取ResultSet对象的列的数量、类型和属性
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            //遍历每一列，寻找与列名相对应的set方法，若没有则寻找对应属性，并将对应数据值传入，没有对应则跳过
            for (int i = 1; i <= columns; i++) {
                String label = meta.getColumnLabel(i);
                Method method = this.methods.get(label);
                if(method != null) {
                    method.invoke(bean, rs.getObject(label));
                } else {
                    Field field = this.fields.get(label);
                    if(field != null) {
                        field.set(bean, rs.getObject(label));
                    }
                }

            }
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("Could not map result set to class %s", this.clazz.getName()), e);
        }
        return bean;
    }
}
