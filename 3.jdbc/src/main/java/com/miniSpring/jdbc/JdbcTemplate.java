package com.miniSpring.jdbc;

import com.miniSpring.exception.DataAccessException;
import com.miniSpring.jdbc.tx.TransactionalUtils;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: JdbcTemplate
 * Description:
 * 基于Template设计模式，提供了大量以回调作为参数的模板方法
 * @Author Jeffer Chen
 * @Create 2024/4/28 17:42
 * @Version 1.0
 */
public class JdbcTemplate {
    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Number queryForNumber(String sql, Object... args) throws DataAccessException {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) throws DataAccessException{
        if (clazz == String.class) {
            return (T) queryForObject(sql, StringRowMapper.instance, args);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) queryForObject(sql, BooleanRowMapper.instance, args);
        }
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            return (T) queryForObject(sql, NumberRowMapper.instance, args);
        }
        return queryForObject(sql, new BeanRowMapper<>(clazz), args);
    }

    /**
     * 查询数据，并将结果中的数据映射至RowMapper对应的T泛型类
     * @param sql
     * @param rowMapper
     * @param args
     * @return
     * @param <T>
     * @throws DataAccessException
     */
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return execute(
                //参数1：PreparedStatementCreator函数式接口的实现（不用考虑Connection，交由execute方法处理，
                //该实现将sql字段和args拼接为PreparedStatement
                preparedStatementCreator(sql, args),
                //参数2：PreparedStatementCallback函数式接口的实现，该实现返回查询语句的查询结果（若不是1条结果则报错）
                (PreparedStatement ps) -> {
                    T t = null;
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (t == null) {
                                t = rowMapper.mapRow(rs, rs.getRow());
                            } else {
                                throw new DataAccessException("Multiple rows found");
                            }
                        }
                    }
                    if (t == null) {
                        throw new DataAccessException("Empty result set.");
                    }
                    return t;
                });
    }

    /**
     * 传入sql语句、想要将结果封装的目标类、sql语句参数，返回查询到的结果集List<T>
     * @param sql
     * @param clazz
     * @param args
     * @return
     * @param <T>
     * @throws DataAccessException
     */
    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> List<T> queryForList(String sql, BeanRowMapper<T> rowMapper, Object[] args) throws DataAccessException{
        return execute(
                //参数1：PreparedStatementCreator函数式接口的实现（不用考虑Connection，交由execute方法处理，
                //该实现将sql字段和args拼接为PreparedStatement
                preparedStatementCreator(sql, args),
                //参数2：PreparedStatementCallback函数式接口的实现，该实现返回查询语句的查询结果list
                //此时该方法返回的是List<T>,则execute方法返回的也是List<T>
                (PreparedStatement ps) -> {
                    List<T> list = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            //将每一行结果都封装为目标类对象
                            list.add(rowMapper.mapRow(rs, rs.getRow()));
                        }
                    }
                    return list;
                });
    }

    /**
     * 插入一条数据，并返回该数据的自增key值
     * @param sql
     * @param args
     * @return
     * @throws DataAccessException
     */
    public Number updateAndReturnGeneratedKey(String sql, Object... args) throws DataAccessException {
        return execute(
                (Connection con) -> {
                    //这里与其他方法的区别是多传入了一个参数，要求返回自增Key的值
                    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    bindArgs(ps, args);
                    return ps;
                },
                (PreparedStatement ps) -> {
                    //这里返回的是update语句影响的行数，对于需要反馈自增key的情况，应该只插入1条
                    int n = ps.executeUpdate();
                    if(n == 0) {
                        throw new DataAccessException("0 rows inserted.");
                    }
                    if(n > 1) {
                        throw new DataAccessException("Multiple rows inserted.");
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        while(keys.next()) {
                            //返回自增key
                            return (Number) keys.getObject(1);
                        }
                    }
                    throw new DataAccessException("Should not reach here.");
                });
    }

    /**
     * Update操作，返回影响行数
     * @param sql
     * @param args
     * @return
     * @throws DataAccessException
     */
    public int update(String sql, Object... args) throws DataAccessException {
        return execute(preparedStatementCreator(sql, args),
                (PreparedStatement ps) -> {
                    return ps.executeUpdate();
                });
    }


    /**
     * 调用execute(ConnectionCallback<T> action)方法，括号内的lamda表达式是对ConnectionCallback函数式接口的实现，
     * 调用psc中的方法生成PreparedStatement，并将其传入到action中
     * @param psc
     * @param action
     * @return
     * @param <T>
     */
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        //函数式接口只有一个抽象方法，因此不用写方法名，直接写参数即对应ConnectionCallback<T>中的doInConnection方法
        return execute((Connection con) -> {
            try (PreparedStatement ps = psc.createPreparedStatement(con)) {//由psc生成ps，生成方式取决于传入的psc
                return action.doInPreparedStatement(ps); //在传入的PreparedStatementCallback中调用相应方法
            }
        });
    }

    /**
     * 在本方法中获取真正的connection，在action（ConnectionCallback的实现）的doInConnection方法中传入connection，获取执行后结果并返回
     * 本方法集中处理获取连接、释放连接、捕获SQLException，让上层代码专注于使用Connection
     * @param action
     * @return
     * @param <T>
     */
    public <T> T execute(ConnectionCallback<T> action) {
        //尝试获取当前事务连接
        Connection current = TransactionalUtils.getCurrentConnection();
        if(current != null){
            try {
                return action.doInConnection(current);
            }catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }
        //当前无事务连接，从dataSource获取新连接
        try(Connection newConn = dataSource.getConnection()) {
            //将autoCommit置为true
            boolean autoCommit = newConn.getAutoCommit();
            if(!autoCommit) {
                newConn.setAutoCommit(true);
            }
            //这里调用的doInconnection方法取决于传入的action是怎么实现的
            T result = action.doInConnection(newConn);
            //TODO 这里为何要设置两次？
            if(!autoCommit) {
                newConn.setAutoCommit(true);
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * 将sql字段和args拼接为PreparedStatement
     * @param sql
     * @param args
     * @return
     */
    private PreparedStatementCreator preparedStatementCreator(String sql, Object... args) {
        return (Connection con) -> {
            PreparedStatement ps = con.prepareStatement(sql);
            bindArgs(ps, args);
            return ps;
        };
    }
    /**
     * 将传入的参数绑定到PreparedStatement上,这种方式通常用于防止SQL注入攻击，因为PreparedStatement会自动处理特殊字符和转义序列，
     * 确保传入的参数被正确地解释为数据，而不是SQL代码的一部分
     * @param ps
     * @param args
     * @throws SQLException
     */
    private void bindArgs(PreparedStatement ps, Object[] args) throws SQLException{
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);//这里i + 1是因为在JDBC中，参数的索引是从1开始的，而不是从0开始
        }
    }
}
class StringRowMapper implements RowMapper<String> {
    //饿汉式单例模式？
    static StringRowMapper instance = new StringRowMapper();

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(1);//为何是1？ 因为这些情况下查询结果应该只能有1行1列
    }
}
class BooleanRowMapper implements RowMapper<Boolean> {
    static BooleanRowMapper instance = new BooleanRowMapper();

    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(1);
    }
}
class NumberRowMapper implements RowMapper<Number> {
    static NumberRowMapper instance = new NumberRowMapper();

    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(1);
    }
}