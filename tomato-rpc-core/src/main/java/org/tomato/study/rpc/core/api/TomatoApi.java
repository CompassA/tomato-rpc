package org.tomato.study.rpc.core.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在接口上，表示这是一个RPC接口
 * @author Tomato
 * Created on 2021.09.29
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TomatoApi {

    /**
     * 用于设置服务的唯一标识
     * @return 服务唯一标识
     */
    String serviceVIP();
}
