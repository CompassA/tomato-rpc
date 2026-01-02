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

import lombok.Getter;
import org.tomato.study.rpc.dashboard.web.view.ResponseCodeEnum;

/**
 * @author Tomato
 * Created on 2026.01.02
 */
@Getter
public abstract class AbstractDashboardException extends RuntimeException {

    private final ResponseCodeEnum errCode;
    private final String errMsg;

    public AbstractDashboardException(ResponseCodeEnum errCode) {
        this(errCode, errCode.getMessage());
    }

    public AbstractDashboardException(ResponseCodeEnum errCode, String errMsg) {
        super(errMsg);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public AbstractDashboardException(Throwable e, ResponseCodeEnum errCode) {
        this(e, errCode, errCode.getMessage());
    }

    public AbstractDashboardException(Throwable e, ResponseCodeEnum errCode, String errMsg) {
        super(e);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
}
