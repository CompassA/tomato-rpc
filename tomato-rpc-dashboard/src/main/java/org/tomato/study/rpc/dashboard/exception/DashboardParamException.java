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

package org.tomato.study.rpc.dashboard.exception;

import org.tomato.study.rpc.dashboard.web.view.ResponseCodeEnum;

/**
 * @author Tomato
 * Created on 2026.01.02
 */
public class DashboardParamException extends DashboardRuntimeException {

    public DashboardParamException() {
        super(ResponseCodeEnum.PARAMETER_INVALID);
    }

    public DashboardParamException(String errMsg) {
        super(ResponseCodeEnum.PARAMETER_INVALID, errMsg);
    }
}
