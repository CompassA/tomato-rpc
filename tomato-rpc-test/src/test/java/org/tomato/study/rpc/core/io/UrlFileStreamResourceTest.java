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

package org.tomato.study.rpc.core.io;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author Tomato
 * Created on 2021.11.15
 */
@SuppressWarnings("all")
public class UrlFileStreamResourceTest {

    /**
     * 常规路径测试
     * 1.通过类加载器load一个url
     * 2.通过{@link UrlFileStreamResource}封装
     * 3.获取流，打开文件，读取内容
     */
    @Test
    public void fileResourceTest() {
        URL resourceURL = UrlFileStreamResourceTest.class.getClassLoader().getResource("test.txt");
        FileStreamResource urlFileStreamResource = new UrlFileStreamResource(resourceURL);
        Assert.assertTrue(urlFileStreamResource.exists());
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(urlFileStreamResource.openNewStream()))) {
            String line = reader.readLine();
            while (StringUtils.isNotBlank(line)) {
                builder.append(line).append(" ");
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertTrue(builder.toString().startsWith("hello world !"));
    }
}
