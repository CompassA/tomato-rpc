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

package org.tomato.study.rpc.core.circuit;

import org.tomato.study.rpc.common.structure.FenwickTree;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 成功率环状计数器
 * 内部通过一个环状数组记录成功与失败
 * 数组每个位置代表一次统计，成功值为1，失败值0
 * 通过数组求和获取成功/失败次数
 * @author Tomato
 * Created on 2022.01.31
 */
public class SuccessFailureRingCounter {

    /**
     * 环的下一个位置
     */
    private final AtomicInteger offset;

    /**
     * bitset
     */
    private final BitSet bitSet;

    /**
     * 环的尺寸
     */
    private final int size;

    /**
     * 前缀索引
     */
    private final FenwickTree fenwickTree;

    private final Object successLock;
    private final Object failureLock;

    public SuccessFailureRingCounter(int size) {
        this.size = size;
        this.bitSet = new BitSet(size);
        this.offset = new AtomicInteger(0);
        this.fenwickTree = new FenwickTree(size);
        this.successLock = new Object();
        this.failureLock = new Object();
    }

    public int length() {
        return fenwickTree.length();
    }

    /**
     * 增加成功次数
     */
    public void addSuccess() {
        int index = next();
        if (!bitSet.get(index)) {
            synchronized (successLock) {
                if (!bitSet.get(index)) {
                    bitSet.set(index, true);
                    fenwickTree.update(index + 1, 1);
                }
            }
        }
    }

    /**
     * 增加失败次数
     */
    public void addFailure() {
        int index = next();
        if (bitSet.get(index)) {
            synchronized (failureLock) {
                if (bitSet.get(index)) {
                    bitSet.set(index, false);
                    fenwickTree.update(index + 1, -1);
                }
            }
        }

    }

    /**
     * 统计成功次数
     * @return 成功次数
     */
    public int successSum() {
        return fenwickTree.search(size);
    }

    /**
     * 统计失败次数
     * @return 失败此时
     */
    public int failureSum() {
        return size - successSum();
    }

    private int next() {
        int res = offset.getAndAdd(1);
        // 如果超过环形数组下表界限，争抢锁，抢到锁的线程将offset重置为0
        if (res >= size) {
            synchronized (this) {
                if (offset.get() >= size) {
                    offset.set(0);
                }
                return offset.getAndAdd(1);
            }
        }
        return res;
    }
}
