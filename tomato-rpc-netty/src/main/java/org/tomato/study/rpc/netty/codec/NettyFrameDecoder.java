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

package org.tomato.study.rpc.netty.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 解析二进制数据帧
 * @author Tomato
 * Created on 2021.04.16
 */
public class NettyFrameDecoder extends LengthFieldBasedFrameDecoder {

    public NettyFrameDecoder() {
        super(  // 数据帧最大长度
                Integer.MAX_VALUE,
                // 记录数据长度的字段 相对数据帧启始位置 的偏移
                1,
                // 记录数据长度的字段的字段大小
                4,
                // 可以配置一个增量字节，Netty将[数据长度字段存储的长度 + 增量字节]作为最终数据帧长度
                0,
                // 收到数据帧后跳过多少字节
                0);
    }
}
