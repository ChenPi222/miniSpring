package proxyTest;

import com.miniSpring.annotation.Autowired;
import com.miniSpring.annotation.Component;

/**
 * ClassName: InjectProxyOnPropertyBean
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/23 16:02
 * @Version 1.0
 */
@Component
public class InjectProxyOnPropertyBean {
    @Autowired
    public OriginBean injected;
}
