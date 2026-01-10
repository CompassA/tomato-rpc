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

import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author Tomato
 * Created on 2021.09.23
 */
public abstract class BaseLifeCycleComponent implements LifeCycle {

    /**
     * cas updater {@link BaseLifeCycleComponent#state}
     * {@link LifeCycle#CREATED}
     * {@link LifeCycle#INIT}
     * {@link LifeCycle#INIT_FINISHED}
     * {@link LifeCycle#START}
     * {@link LifeCycle#START_FINISHED}
     * {@link LifeCycle#STOP}
     * {@link LifeCycle#STOP_FINISHED}
     */
    private static final AtomicIntegerFieldUpdater<BaseLifeCycleComponent> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(BaseLifeCycleComponent.class, "state");

    /**
     * component listener
     */
    private final List<LifeCycleListener> listeners = new ArrayList<>(0);

    /**
     * component state
     */
    private volatile int state = CREATED;

    @Override
    public void init() throws TomatoRpcException {
        if (!STATE_UPDATER.compareAndSet(this, CREATED, INIT)) {
            return;
        }
        doInit();
        if (!STATE_UPDATER.compareAndSet(this, INIT, INIT_FINISHED)) {
            throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_COMPONENT_LIFE_CYCLE_INVALID_STATE,
                "INIT -> INIT_FINISHED failed, current status: " + state);
        }
    }

    @Override
    public void start() throws TomatoRpcException {
        if (!STATE_UPDATER.compareAndSet(this, INIT_FINISHED, START)) {
            return;
        }
        doStart();
        if (!STATE_UPDATER.compareAndSet(this, START, START_FINISHED)) {
            throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_COMPONENT_LIFE_CYCLE_INVALID_STATE,
                "START -> START_FINISHED failed, current status: " + state);
        }
    }

    @Override
    public void stop() throws TomatoRpcException{
        if (!STATE_UPDATER.compareAndSet(this, START_FINISHED, STOP)) {
            return;
        }
        doStop();
        if (!STATE_UPDATER.compareAndSet(this, STOP, STOP_FINISHED)) {
            throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_COMPONENT_LIFE_CYCLE_INVALID_STATE,
                "STOP -> STOP_FINISHED failed, current status: " + state);
        }
    }

    @Override
    public void addListener(LifeCycleListener lifeCycleListener) {
        listeners.add(lifeCycleListener);
    }

    @Override
    public void removeListener(LifeCycleListener lifeCycleListener) {
        listeners.remove(lifeCycleListener);
    }

    @Override
    public List<LifeCycleListener> getListeners() {
        return listeners;
    }

    public int getState() {
        return state;
    }

    /**
     * abstract method for initializing
     */
    abstract protected void doInit() throws TomatoRpcException;

    /**
     * abstract method for starting
     */
    abstract protected void doStart() throws TomatoRpcException;

    /**
     * abstract method for stopping
     */
    abstract protected void doStop() throws TomatoRpcException;
}