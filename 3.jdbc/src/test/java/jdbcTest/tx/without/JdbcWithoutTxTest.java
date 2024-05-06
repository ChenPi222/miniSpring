package jdbcTest.tx.without;

import com.miniSpring.context.AnnotationConfigApplicationContext;
import com.miniSpring.exception.DataAccessException;
import com.miniSpring.jdbc.JdbcTemplate;
import jdbcTest.JdbcTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClassName: JdbcWithoutTxTest
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 16:04
 * @Version 1.0
 */
public class JdbcWithoutTxTest extends JdbcTestBase {
    @Test
    public void testJdbcWithoutTx() {
        try (var ctx = new AnnotationConfigApplicationContext(JdbcWithoutTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            jdbcTemplate.update(CREATE_USER);
            jdbcTemplate.update(CREATE_ADDRESS);
            // insert user:
            int userId1 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, "Bob", 12).intValue();
            int userId2 = jdbcTemplate.updateAndReturnGeneratedKey(INSERT_USER, "Alice", null).intValue();
            assertEquals(1, userId1);
            assertEquals(2, userId2);
            // query user:
            User bob = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId1);
            User alice = jdbcTemplate.queryForObject(SELECT_USER, User.class, userId2);
            assertEquals(1, bob.id);
            assertEquals("Bob", bob.name);
            assertEquals(12, bob.theAge);
            assertEquals(2, alice.id);
            assertEquals("Alice", alice.name);
            assertNull(alice.theAge);
            // query name:
            assertEquals("Bob", jdbcTemplate.queryForObject(SELECT_USER_NAME, String.class, userId1));
            assertEquals(12, jdbcTemplate.queryForObject(SELECT_USER_AGE, int.class, userId1));
            // update user:
            int n1 = jdbcTemplate.update(UPDATE_USER, "Bob Jones", 18, bob.id);
            assertEquals(1, n1);
            //queryForList
            List<User> users = jdbcTemplate.queryForList(SELECT_USER_LIST, User.class);
            // delete user:
            int n2 = jdbcTemplate.update(DELETE_USER, alice.id);
            assertEquals(1, n2);


        }
        // re-open db and query:
        try (var ctx = new AnnotationConfigApplicationContext(JdbcWithoutTxApplication.class, createPropertyResolver())) {
            JdbcTemplate jdbcTemplate = ctx.getBean(JdbcTemplate.class);
            User bob = jdbcTemplate.queryForObject(SELECT_USER, User.class, 1);
            assertEquals("Bob Jones", bob.name);
            assertEquals(18, bob.theAge);
            assertThrows(DataAccessException.class, () -> {
                // alice was deleted:
                jdbcTemplate.queryForObject(SELECT_USER, User.class, 2);
            });
        }
    }
}
