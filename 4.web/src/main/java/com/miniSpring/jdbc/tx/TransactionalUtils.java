package com.miniSpring.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

/**
 * ClassName: TransactionalUtils
 * Description:
 * 获取当前事务连接的工具类
 * @Author Jeffer Chen
 * @Create 2024/4/30 15:13
 * @Version 1.0
 */
public class TransactionalUtils {
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.connection;
    }
}
