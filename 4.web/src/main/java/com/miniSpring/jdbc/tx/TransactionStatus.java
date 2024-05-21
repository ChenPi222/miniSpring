package com.miniSpring.jdbc.tx;

import java.sql.Connection;

/**
 * ClassName: TransactionStatus
 * Description:
 * 表示当前事务状态，目前仅封装了一个Connection，将来如果扩展，则可以将事务的传播模式存储在里面
 * @Author Jeffer Chen
 * @Create 2024/4/29 21:22
 * @Version 1.0
 */
public class TransactionStatus {
    final Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }
}
