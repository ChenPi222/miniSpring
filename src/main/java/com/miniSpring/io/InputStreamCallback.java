package com.miniSpring.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * ClassName: InputStreamCallback
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/17 16:52
 * @Version 1.0
 */

@FunctionalInterface
public interface InputStreamCallback<T> {
    T doWithInputStream(InputStream stream) throws IOException;
}
