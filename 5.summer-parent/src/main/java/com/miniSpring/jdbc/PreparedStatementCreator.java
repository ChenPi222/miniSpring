package com.miniSpring.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ClassName: PreparedStatementCreator
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/28 19:19
 * @Version 1.0
 */
@FunctionalInterface
public interface PreparedStatementCreator {
    PreparedStatement createPreparedStatement(Connection con) throws SQLException;
}
