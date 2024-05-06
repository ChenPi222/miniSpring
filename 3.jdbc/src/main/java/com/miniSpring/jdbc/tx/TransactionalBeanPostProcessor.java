package com.miniSpring.jdbc.tx;

import com.miniSpring.annotation.Transactional;
import com.miniSpring.aop.AnnotationProxyBeanPostProcessor;

/**
 * ClassName: TransactionalBeanPostProcessor
 * Description:
 *
 * @Author Jeffer Chen
 * @Create 2024/4/30 15:20
 * @Version 1.0
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {

}
