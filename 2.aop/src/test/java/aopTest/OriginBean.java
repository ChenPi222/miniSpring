package aopTest;

/**
 * ClassName: OriginBean
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 19:34
 * @Version 1.0
 */
public class OriginBean {
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
