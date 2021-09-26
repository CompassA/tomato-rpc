package org.tomato.study.rpc.registry.zookeeper.impl;

import org.tomato.study.rpc.core.NameServerFactory;
import org.tomato.study.rpc.core.base.BaseNameService;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.registry.zookeeper.ZookeeperNameService;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
public class ZookeeperNameServerFactory implements NameServerFactory {

    @Override
    public BaseNameService createNameService(NameServerConfig config) {
        return new ZookeeperNameService(config);
    }
}
