/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.router;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.circuit.CircuitRpcInvoker;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.loadbalance.LoadBalance;
import org.tomato.study.rpc.expression.ast.ASTNode;
import org.tomato.study.rpc.expression.ast.AddAndSubParser;
import org.tomato.study.rpc.expression.ast.CmpParser;
import org.tomato.study.rpc.expression.ast.LogicParser;
import org.tomato.study.rpc.expression.ast.MulAndDivAndModParser;
import org.tomato.study.rpc.expression.ast.PrimaryTokenParser;
import org.tomato.study.rpc.expression.ast.RootExpressionParser;
import org.tomato.study.rpc.expression.ast.RouterExpressionParser;
import org.tomato.study.rpc.expression.token.TokenLexer;
import org.tomato.study.rpc.expression.token.TokenStream;
import org.tomato.study.rpc.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 一个RPC服务，管理服务的多个实例
 * @author Tomato
 * Created on 2021.07.10
 */
@Getter
public abstract class BaseMicroServiceSpace implements MicroServiceSpace {

    /**
     * 订阅的服务的唯一标识 {@link MetaData#getMicroServiceId()}
     */
    private final String microServiceId;

    /**
     * 当前服务RPC配置
     */
    private final RpcConfig rpcConfig;

    /**
     * 一个RPC服务的所有实例节点，一个RpcInvoker持有一个与服务实例节点的连接并与其通信
     * 实例节点ip端口等元数据信息 -> 对应的Invoker
     */
    private final ConcurrentMap<MetaData, RpcInvoker> invokerMap = new ConcurrentHashMap<>(0);

    /**
     * 均衡负载器
     */
    private final LoadBalance loadBalance;

    /**
     * 路由规则词法分析器
     */
    private final TokenLexer lexer = new TokenLexer();

    /**
     * 路由规则语法分析器
     */
    private RouterExpressionParser parser;

    /**
     * 兜底路由规则
     */
    private List<FullRequestRouter> defaultRouters;

    /**
     * 路由规则
     */
    private List<Router> routers;

    public BaseMicroServiceSpace(String microServiceId, RpcConfig rpcConfig, LoadBalance loadBalance) {
        this.microServiceId = microServiceId;
        this.rpcConfig = rpcConfig;
        this.loadBalance = loadBalance;
        initParser();
        initDefaultRouters(rpcConfig);
    }

    private void initParser() {
        PrimaryTokenParser primaryTokenParser = new PrimaryTokenParser();
        RootExpressionParser rootExpressionParser =
                new RootExpressionParser(
                        new LogicParser(
                                new CmpParser(
                                        new AddAndSubParser(
                                                new MulAndDivAndModParser(primaryTokenParser)))));
        primaryTokenParser.setTopExpressionParser(rootExpressionParser);
        this.parser = new RouterExpressionParser(rootExpressionParser);
    }

    private void initDefaultRouters(RpcConfig rpcConfig) {
        // 设置兜底路由规则, 将rpc请求发送给通组的节点
        String finalGroup = getJvmConfigGroup(microServiceId).orElse(rpcConfig.getGroup());
        ASTNode node = parser.getRootExpressionParser().parse(lexer.tokenize(
                String.format("%s==\"%s\"", MetaData.GROUP_PARAM_NAME, finalGroup)));
        this.defaultRouters = Collections.singletonList(new FullRequestRouter(node));
        this.routers = new ArrayList<>(this.defaultRouters);
    }

    /**
     * 查找服务有无jvm配置的group
     * @param serviceId 服务id
     * @return jvm配置的group
     */
    private Optional<String> getJvmConfigGroup(String serviceId) {
        Map<String, String> jvmConfigGroup = RpcJvmConfigKey.parseMultiKeyValue(
                System.getProperty(RpcJvmConfigKey.MICRO_SUBSCRIBE_GROUP));
        return Optional.ofNullable(jvmConfigGroup.get(serviceId));
    }

    @Override
    public List<RpcInvoker> getAllInvokers() {
        return new ArrayList<>(invokerMap.values());
    }

    @Override
    public String getMicroServiceId() {
        return microServiceId;
    }

    /**
     * 根据客户端订阅的该RPC服务的版本，寻找匹配的服务实例节点
     * @param invocation rpc request data
     * @return 可与实例节点通信的RpcInvoker
     */
    @Override
    public Optional<RpcInvoker> lookUp(Invocation invocation) {
        // 路由筛选
        List<RpcInvoker> invokers = new ArrayList<>(0);
        List<Router> routers = this.routers;
        List<RpcInvoker> allInvokers = getAllInvokers();
        for (Router router : routers) {
            if (!router.matchRequest(invocation)) {
                continue;
            }

            for (RpcInvoker invoker : allInvokers) {
                if (router.matchInvoker(invoker)) {
                    invokers.add(invoker);
                }
            }

            // 请求匹配当前路由规则后, 不再往后匹配
            break;
        }


        // 均衡负载
        RpcInvoker invoker = loadBalance.select(invocation, invokers);

        return Optional.ofNullable(invoker);
    }

    /**
     * 更新RPC服务的实例节点信息
     * @param newRpcInstanceInfoSet 相同micro-service-id、相同Stage的所有实例节点
     * @throws TomatoRpcException 更新异常
     */
    @Override
    public void refresh(Set<MetaData> newRpcInstanceInfoSet) throws TomatoRpcException {
        // 若实例节点为空，表明这个服务已经没有节点了，清空Invoker
        if (CollectionUtils.isEmpty(newRpcInstanceInfoSet)) {
            Logger.DEFAULT.info("clean {} invokers", microServiceId);
            cleanInvoker();
            return;
        }

        // 创建Invoker
        Map<MetaData, RpcInvoker> newInvokerMap = new HashMap<>(newRpcInstanceInfoSet.size());
        for (MetaData instanceMeta : newRpcInstanceInfoSet) {
            RpcInvoker rpcInvoker = invokerMap.computeIfAbsent(
                    instanceMeta,
                    meta -> createInvoker(instanceMeta));
            newInvokerMap.put(instanceMeta, rpcInvoker);
        }

        // 关闭旧的Invoker
        List<MetaData> needRemoveList = new ArrayList<>(0);
        invokerMap.forEach((k, v) -> {
            if (!newInvokerMap.containsKey(k)) {
                needRemoveList.add(k);
            }
        });
        for (MetaData metaData : needRemoveList) {
            RpcInvoker oldInvoker = invokerMap.remove(metaData);
            // 关闭invoker
            try {
                Logger.DEFAULT.info("close invoker{}", oldInvoker.getMetadata());
                oldInvoker.destroy();
            } catch (TomatoRpcException e) {
                // 仅打日志, 什么都不做
                Logger.DEFAULT.error("close invoker{} failed", oldInvoker.getMetadata(), e);
            }
        }

    }

    @Override
    public void refreshRouter(List<String> routers) throws TomatoRpcException {
        List<Router> newRouters = new ArrayList<>();
        for (String router : routers) {
            TokenStream tokenStream = lexer.tokenize(router);
            ASTNode node = parser.parse(tokenStream);
            if (node != null) {
                ASTNode[] children = node.getChildren();
                ExprRouter expr = new ExprRouter();
                expr.setLeftExpr(children[0]);
                expr.setRightExpr(children[1]);
                newRouters.add(expr);
            }
        }
        newRouters.addAll(defaultRouters);
        this.routers = newRouters;
    }

    @Override
    public synchronized void close() throws TomatoRpcException {
        cleanInvoker();
    }

    private RpcInvoker createInvoker(MetaData metaData) {
        Logger.DEFAULT.info("create invoker {}", metaData);
        RpcInvoker rpcInvoker = doCreateInvoker(metaData);
        if (!rpcConfig.isEnableCircuit()) {
            return rpcInvoker;
        }
        return doCreateCircuitBreaker(rpcInvoker);
    }

    /**
     * 创建熔断包装器
     * @param invoker 原始invoker
     * @return 被熔断包装器包装的invoker
     */
    protected abstract CircuitRpcInvoker doCreateCircuitBreaker(RpcInvoker invoker);

    /**
     * 根据rpc节点信息创建invoker
     * @param metaData rpc节点信息
     * @return 可与rpc节点通信的invoker
     */
    protected abstract RpcInvoker doCreateInvoker(MetaData metaData);

    private void cleanInvoker() throws TomatoRpcException {
        if (CollectionUtils.isNotEmpty(invokerMap.values())) {
            for (RpcInvoker value : invokerMap.values()) {
                value.destroy();
            }
        }
        invokerMap.clear();
    }
}
