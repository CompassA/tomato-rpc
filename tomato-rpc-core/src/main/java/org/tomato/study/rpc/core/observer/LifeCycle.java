/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.observer;

import org.tomato.study.rpc.core.error.TomatoRpcException;

import java.util.List;

/**
 * tomato-rpc所有组件的通用生命周期
 * @author Tomato
 * Created on 2021.09.23
 */
public interface LifeCycle {

    /**
     * 创建
     */
    int CREATED = 0;

    /**
     * 初始化
     */
    int INIT = 1;

    /**
     * 启动
     */
    int START = 2;

    /**
     * 停止
     */
    int STOP = 3;

    /**
     * 组件初始化方法
     * @throws TomatoRpcException 初始化时抛出异常
     */
    void init() throws TomatoRpcException;

    /**
     * 组件启动方法
     * @throws TomatoRpcException 启动时抛出异常
     */
    void start() throws TomatoRpcException;

    /**
     * 组件关闭方法
     * @throws TomatoRpcException 停止时时抛出异常
     */
    void stop() throws TomatoRpcException;

    /**
     * 获取组件的所有监听者
     * @return 监听者
     */
    List<LifeCycleListener> getListeners();

    /**
     * 增加监听者
     * @param lifeCycleListener 要添加的监听对象
     */
    void addListener(LifeCycleListener lifeCycleListener);

    /**
     * 删除监听者
     * @param lifeCycleListener 要删除的监听对象
     */
    void removeListener(LifeCycleListener lifeCycleListener);
}
