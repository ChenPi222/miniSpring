package com.miniSpring.jdbc.tx;

import com.miniSpring.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * ClassName: DataSourceTransactionManager
 * Description:
 * 是真正执行开启、提交、回归事务的地方；因为事务处理逻辑应该由框架确定而不是客户端，因此InvocationHandler需要框架写，@Around等注解的则由客户端自定义
 * @Author Jeffer Chen
 * @Create 2024/4/29 21:23
 * @Version 1.0
 */
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {
    //ThreadLocal存储的TransactionStatus
    static final ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();
    final Logger logger = LoggerFactory.getLogger(getClass());
    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TransactionStatus ts = transactionStatus.get();
        if(ts == null) {
            //开启新事务
            try (Connection connection = dataSource.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                if(autoCommit) {
                    connection.setAutoCommit(false);
                }
                try {
                    //设置ThreadLocal状态
                    transactionStatus.set(new TransactionStatus(connection));
                    //调用业务方法
                    Object r = method.invoke(proxy, args);
                    //提交事务
                    connection.commit();
                    //方法返回
                    return r;
                } catch (InvocationTargetException e) {
                    //回滚事务
                    logger.warn("will rollback transaction for caused exception: {}",
                            e.getCause() == null ? "null" : e.getCause().getClass().getName());
                    TransactionException te = new TransactionException(e.getCause());
                    //尝试回滚与connection关联的事务。但是，如果在回滚过程中发生SQLException，则捕获该异常并将其作为被抑制的异常
                    // 添加到TransactionException中
                    try {
                        connection.rollback();
                    } catch (SQLException sqle) {
                        te.addSuppressed(sqle);
                    }
                    throw te;
                }finally {
                    //移除ThreadLocal状态
                    transactionStatus.remove();
                    //TODO 为何这里autoCommit又会变为true？
                    if(autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        } else {
            //加入当前事务
            return method.invoke(proxy, args);
        }
    }
}
