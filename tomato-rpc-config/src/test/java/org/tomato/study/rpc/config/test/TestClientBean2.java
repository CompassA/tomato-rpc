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

package org.tomato.study.rpc.config.test;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.tomato.study.rpc.config.annotation.RpcClientStub;

/**
 * @author Tomato
 * Created on 2022.12.04
 */
@Getter
@Component
public class TestClientBean2 {

    @RpcClientStub
    private TestApi testApi;

    @RpcClientStub(timeout = 200)
    private TestTimeoutApi timeoutApi;

}
