package com.miniSpring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ClassName: ResultSetExtractor
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 10:21
 * @Version 1.0
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {

    @Nullable
    T extractData(ResultSet rs) throws SQLException;
}
