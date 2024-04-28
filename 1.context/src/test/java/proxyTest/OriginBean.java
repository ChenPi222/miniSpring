package proxyTest;

import com.miniSpring.annotation.Component;
import com.miniSpring.annotation.Value;

/**
 * ClassName: OriginBean
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/23 15:48
 * @Version 1.0
 */
@Component
public class OriginBean {
    @Value("${app.title}")
    public String name;

    public String version;

    @Value("${app.version}")
    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return this.version;
    }
}
