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

package org.tomato.study.rpc.structure;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Tomato
 * Created on 2022.01.30
 */
public class FenwickTree {

    /**
     * 记录前缀和
     */
    private final AtomicInteger[] value;

    public FenwickTree(int size) {
        this.value = new AtomicInteger[size+1];
        for (int i = 1; i <= size; ++i) {
            value[i] = new AtomicInteger(0);
        }
    }

    /**
     * 更新bitset index位置上的值
     * @param index index [1, size]
     * @param delta 增加的值
     */
    public void update(int index, int delta) {
        while (index < value.length) {
            value[index].addAndGet(delta);
            index += lowbit(index);
        }
    }

    /**
     * 查找[1, index]的和
     * @param index index[1, size]
     * @return sum
     */
    public int search(int index) {
        int res = 0;
        while (index > 0) {
            res += value[index].get();
            index -= lowbit(index);
        }
        return res;
    }

    public int length() {
        return value.length-1;
    }

    private int lowbit(int index) {
        return index & (-index);
    }
}
