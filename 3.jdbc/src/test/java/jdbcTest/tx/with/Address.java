package jdbcTest.tx.with;

/**
 * ClassName: Address
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 16:02
 * @Version 1.0
 */
public class Address {
    public int id;
    public int userId;
    public String address;
    public int zip;

    public Address() {
    }

    public Address(int userId, String address, int zip) {
        this.userId = userId;
        this.address = address;
        this.zip = zip;
    }
}
