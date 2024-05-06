package aopTest;

import java.lang.annotation.*;

/**
 * ClassName: Polite
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/25 19:34
 * @Version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Polite {
}
