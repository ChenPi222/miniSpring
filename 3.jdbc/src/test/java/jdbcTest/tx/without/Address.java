package jdbcTest.tx.without;

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
    public int zipcode;

    public void setZip(Integer zip) {
        this.zipcode = zip == null ? 0 : zip.intValue();
    }
}
