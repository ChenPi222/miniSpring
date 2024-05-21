package com.miniSpring.jdbc;

import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ClassName: ConnectionCallback
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/28 17:52
 * @Version 1.0
 */
@FunctionalInterface //函数式接口，只能标记在“有且仅有一个抽象方法”的接口上（主要用于编译器检查该接口是否符合要求）
public interface ConnectionCallback<T>{
    @Nullable
    T doInConnection(Connection con) throws SQLException;
}
