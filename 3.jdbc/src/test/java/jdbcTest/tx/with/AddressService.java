package jdbcTest.tx.with;

import com.miniSpring.annotation.Autowired;
import com.miniSpring.annotation.Component;
import com.miniSpring.annotation.Transactional;
import com.miniSpring.jdbc.JdbcTemplate;
import jdbcTest.JdbcTestBase;

import java.util.List;

/**
 * ClassName: AddressService
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/30 15:31
 * @Version 1.0
 */
@Component
@Transactional
public class AddressService {
    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public void addAddress(Address... addresses) {
        for (Address address : addresses) {
            // check if userId is exist:
            userService.getUser(address.userId);
            jdbcTemplate.update(JdbcTestBase.INSERT_ADDRESS, address.userId, address.address, address.zip);
        }
    }

    public List<Address> getAddresses(int userId) {
        return jdbcTemplate.queryForList(JdbcTestBase.SELECT_ADDRESS_BY_USERID, Address.class, userId);
    }

    public void deleteAddress(int userId) {
        jdbcTemplate.update(JdbcTestBase.DELETE_ADDRESS_BY_USERID, userId);
        if (userId == 1) {
            throw new RuntimeException("Rollback delete for user id = 1");
        }
    }

}
