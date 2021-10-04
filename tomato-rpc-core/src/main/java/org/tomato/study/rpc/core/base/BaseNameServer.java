package org.tomato.study.rpc.core.base;

import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;

import java.nio.charset.Charset;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
public abstract class BaseNameServer extends BaseLifeCycleComponent implements NameServer {

    private final NameServerConfig nameServerConfig;

    public BaseNameServer(NameServerConfig nameServerConfig) {
        this.nameServerConfig = nameServerConfig;
    }

    public String getConnString() {
        return nameServerConfig.getConnString();
    }

    public Charset getCharset() {
        return nameServerConfig.getCharset();
    }
}
