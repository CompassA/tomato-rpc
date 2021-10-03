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

package org.tomato.study.rpc.registry.zookeeper.error;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.error.TomatoRpcErrorInfo;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@AllArgsConstructor
public enum TomatoRegistryErrorEnum {

    RPC_REGISTRY_CLOSE_ERROR(10000, "rpc registry close failed"),
    ;

    private int code;
    private String message;

    public TomatoRpcErrorInfo create() {
        return new TomatoRpcErrorInfo(this.code, this.message);
    }

    public TomatoRpcErrorInfo create(String customMessage) {
        return new TomatoRpcErrorInfo(this.code, customMessage);
    }
}
