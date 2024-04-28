package proxyTest;

import com.miniSpring.annotation.Autowired;
import com.miniSpring.annotation.Component;

/**
 * ClassName: InjectProxyOnConstructorBean
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/23 15:53
 * @Version 1.0
 */
@Component
public class InjectProxyOnConstructorBean {
    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}
