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

import lombok.Getter;

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
     * {@link LifeCycle#START}
     * {@link LifeCycle#STOP}
     */
    private static final AtomicIntegerFieldUpdater<BaseLifeCycleComponent> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(BaseLifeCycleComponent.class, "state");

    /**
     * component listener
     */
    @Getter
    private final List<LifeCycleListener> listeners;

    /**
     * component state
     */
    private volatile int state = CREATED;

    public BaseLifeCycleComponent(List<LifeCycleListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void init() {
        if (!STATE_UPDATER.compareAndSet(this, CREATED, INIT)) {
            return;
        }
        doInit();
    }

    @Override
    public void start() {
        if (!STATE_UPDATER.compareAndSet(this, INIT, START)) {
            return;
        }
        doStart();
    }

    @Override
    public void destroy() {
        if (!STATE_UPDATER.compareAndSet(this, START, START)) {
            return;
        }
        doDestroy();
    }

    /**
     * abstract method for initializing
     */
    abstract protected void doInit();

    /**
     * abstract method for starting
     */
    abstract protected void doStart();

    /**
     * abstract method for destroying
     */
    abstract protected void doDestroy();
}
