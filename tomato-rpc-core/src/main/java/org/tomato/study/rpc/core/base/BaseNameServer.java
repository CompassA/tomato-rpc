package org.tomato.study.rpc.core.base;

import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RefreshInvokerTask;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@Slf4j
public abstract class BaseNameServer extends BaseLifeCycleComponent implements NameServer {

    private final NameServerConfig nameServerConfig;

    /**
     * invoker更新队列
     */
    private final BlockingQueue<RefreshInvokerTask> refreshTaskQueue = new ArrayBlockingQueue<>(1000);

    /**
     * 更新invoker的后台线程
     */
    private Thread refreshInvokerThread;

    /**
     * 用户通过jvm参数配置的要订阅的微服务的group
     * service-id -> jvm配置的group
     */
    private final Map<String, String> jvmConfigGroup;

    public BaseNameServer(NameServerConfig nameServerConfig) {
        this.nameServerConfig = nameServerConfig;
        String groupJvmConfigs = System.getProperty(RpcJvmConfigKey.MICRO_SUBSCRIBE_GROUP);
        this.jvmConfigGroup = RpcJvmConfigKey.parseMultiKeyValue(groupJvmConfigs);
    }

    /**
     * 查找服务有无jvm配置的group
     * @param serviceId 服务id
     * @return jvm配置的group
     */
    public Optional<String> getJvmConfigGroup(String serviceId) {
        return Optional.ofNullable(jvmConfigGroup.get(serviceId));
    }

    public String getConnString() {
        return nameServerConfig.getConnString();
    }

    public Charset getCharset() {
        return nameServerConfig.getCharset();
    }

    @Override
    public void submitInvokerRefreshTask(RefreshInvokerTask task) throws InterruptedException {
        refreshTaskQueue.put(task);
    }

    @Override
    protected void doInit() throws TomatoRpcException {
        this.refreshInvokerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RefreshInvokerTask task = refreshTaskQueue.take();
                    task.getMicroServiceSpace().refresh(task.getInvokerInfoSet());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (TomatoRpcException e) {
                    log.error("refresh error", e);
                }
            }
        }, "invoker-refresh-thread");
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        refreshInvokerThread.start();
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        refreshInvokerThread.interrupt();
    }
}
