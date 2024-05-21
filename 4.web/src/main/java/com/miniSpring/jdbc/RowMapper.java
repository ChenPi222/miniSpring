package com.miniSpring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ClassName: RowMapper
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 10:01
 * @Version 1.0
 */
@FunctionalInterface
public interface RowMapper<T>{

    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
