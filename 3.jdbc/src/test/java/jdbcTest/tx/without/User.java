package jdbcTest.tx.without;

/**
 * ClassName: User
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/29 16:05
 * @Version 1.0
 */
public class User {
    public int id;
    public String name;
    public Integer theAge;

    public void setAge(Integer age) {
        this.theAge = age;
    }
}
