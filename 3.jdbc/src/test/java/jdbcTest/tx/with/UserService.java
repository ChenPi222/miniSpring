package jdbcTest.tx.with;

import com.miniSpring.annotation.Autowired;
import com.miniSpring.annotation.Component;
import com.miniSpring.annotation.Transactional;
import com.miniSpring.jdbc.JdbcTemplate;
import jdbcTest.JdbcTestBase;

/**
 * ClassName: UserService
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/30 15:31
 * @Version 1.0
 */
@Component
@Transactional
public class UserService {
    @Autowired
    AddressService addressService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public User createUser(String name, int age) {
        Number id = jdbcTemplate.updateAndReturnGeneratedKey(JdbcTestBase.INSERT_USER, name, age);
        User user = new User();
        user.id = id.intValue();
        user.name = name;
        user.theAge = age;
        return user;
    }

    public User getUser(int userId) {
        return jdbcTemplate.queryForObject(JdbcTestBase.SELECT_USER, User.class, userId);
    }

    public void updateUser(User user) {
        jdbcTemplate.update(JdbcTestBase.UPDATE_USER, user.name, user.theAge, user.id);
    }

    public void deleteUser(User user) {
        jdbcTemplate.update(JdbcTestBase.DELETE_USER, user.id);
        addressService.deleteAddress(user.id);
    }
}
