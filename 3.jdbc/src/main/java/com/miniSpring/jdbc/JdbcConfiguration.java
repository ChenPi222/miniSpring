package com.miniSpring.jdbc;

import com.miniSpring.annotation.Autowired;
import com.miniSpring.annotation.Bean;
import com.miniSpring.annotation.Configuration;
import com.miniSpring.annotation.Value;
import com.miniSpring.jdbc.tx.DataSourceTransactionManager;
import com.miniSpring.jdbc.tx.PlatformTransactionManager;
import com.miniSpring.jdbc.tx.TransactionalBeanPostProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * ClassName: JdbcConfiguration
 * Description:
 * 数据源配置格式如下
 *      summer:
 *        datasource:
 *          url: jdbc:sqlite:test.db
 *          driver-class-name: org.sqlite.JDBC
 *          username: sa
 *          password:
 * @Author Jeffer Chen
 * @Create 2024/4/28 17:21
 * @Version 1.0
 */
@Configuration
public class JdbcConfiguration {

    /**
     * 从配置文件中读取数据库配置信息，创建HikariCP支持的DataSource
     * @param url
     * @param username
     * @param password
     * @param driver
     * @param maximumPoolSize
     * @param minimumPoolSize
     * @param connTimeout
     * @return
     */
    @Bean(destroyMethod = "close") //TODO 这里的destroyMethod执行流程是怎样的？
    DataSource dataSource(
            //从Properties文件中读取
            @Value("${summer.datasource.url}") String url,
            @Value("${summer.datasource.username}") String username,
            @Value("${summer.datasource.password}") String password,
            @Value("${summer.datasource.driver-class-name:}") String driver,
            @Value("${summer.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${summer.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${summer.datasource.connection-timeout:30000}") int connTimeout
    ) {
        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if(driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
