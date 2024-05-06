package jdbcTest.tx.with;

import com.miniSpring.annotation.ComponentScan;
import com.miniSpring.annotation.Configuration;
import com.miniSpring.annotation.Import;
import com.miniSpring.jdbc.JdbcConfiguration;

/**
 * ClassName: JdbcWithTxApplication
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/30 15:32
 * @Version 1.0
 */
@ComponentScan
@Configuration
@Import(JdbcConfiguration.class)
public class JdbcWithTxApplication {
}
