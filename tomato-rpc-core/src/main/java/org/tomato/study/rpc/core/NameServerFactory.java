package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * 创建注册中心
 * @author Tomato
 * Created on 2021.09.27
 */
@SpiInterface("zookeeper-factory")
public interface NameServerFactory {

    /**
     * 创建注册中心服务
     * @param config 注册中心配置
     * @return 注册中心
     */
    NameServer createNameService(NameServerConfig config);
}
