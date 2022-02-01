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

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * @author Tomato
 * Created on 2022.01.31
 */
public class FenwickTreeTest {

    @Test
    public void fenwickTreeTest() {
        int length = 1000;
        FenwickTree tree = new FenwickTree(length);
        Assert.assertEquals(tree.length(), length);
        int[] array = new int[length];

        Random r = new Random();
        for (int i = 0; i < array.length; ++i) {
            int nextVal = r.nextInt(100);
            array[i] = nextVal;
            tree.update(i+1, nextVal);
        }

        Assert.assertEquals(sum(array, 0, array.length-1), tree.search(array.length));
        Assert.assertEquals(
                sum(array, array.length / 3, array.length / 2),
                tree.search(array.length/2+1) - tree.search(array.length/3));

        int deltaV = 200;
        array[length/2] += deltaV;
        tree.update(length/2 + 1, deltaV);
        Assert.assertEquals(sum(array, 0, array.length-1), tree.search(array.length));
        Assert.assertEquals(
                sum(array, array.length / 3, array.length / 2),
                tree.search(array.length/2+1) - tree.search(array.length/3));

        for (int i = 0; i < array.length; ++i) {
            deltaV = r.nextInt(100);
            array[i] += deltaV;
            tree.update(i+1, deltaV);
            Assert.assertEquals(sum(array, 0, i), tree.search(i+1));
        }

    }

    private int sum(int[] array, int i, int j) {
        int res = 0;
        for (int k = i; k <=j; ++k) {
            res += array[k];
        }
        return res;
    }
}
