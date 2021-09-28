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

package org.tomato.study.rpc.core.error;

/**
 * rpc异常
 * @author Tomato
 * Created on 2021.06.19
 */
public class TomatoRpcRuntimeException extends RuntimeException {

    private final TomatoRpcErrorInfo errorInfo;

    public TomatoRpcRuntimeException(TomatoRpcErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
    }
    public TomatoRpcRuntimeException(TomatoRpcErrorInfo errorInfo, Throwable throwable) {
        super(errorInfo.getMessage(), throwable);
        this.errorInfo = errorInfo;
    }

    @Override
    public String getMessage() {
        return errorInfo.getMessage();
    }
}
