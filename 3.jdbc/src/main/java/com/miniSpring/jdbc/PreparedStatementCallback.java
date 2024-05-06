package com.miniSpring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ClassName: PreparedStatementCallback
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/28 19:21
 * @Version 1.0
 */
@FunctionalInterface
public interface PreparedStatementCallback<T>{

    @Nullable
    T doInPreparedStatement(PreparedStatement ps) throws SQLException;
}
