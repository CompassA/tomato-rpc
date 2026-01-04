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

package org.tomato.study.rpc.dashboard.advice;

import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.core.serializer.JsonSerializer;
import org.tomato.study.rpc.dashboard.exception.AbstractDashboardException;
import org.tomato.study.rpc.dashboard.exception.DashboardParamException;
import org.tomato.study.rpc.dashboard.exception.DashboardRuntimeException;
import org.tomato.study.rpc.dashboard.web.view.DashboardResponse;
import org.tomato.study.rpc.dashboard.web.view.ResponseCodeEnum;

/**
 * @author Tomato
 * Created on 2026.01.02
 * @param <Q> 请求
 * @param <R> 响应
 */
@Slf4j
public abstract class ServiceTemplate<Q, R> {

    private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();

    public DashboardResponse process(String module, String method, Q req) {
        String resMark = Logger.SUCCESS_MARK;
        long start = System.currentTimeMillis();
        DashboardResponse resp = null;
        try {
            log.info("|req|{}|{}|{}|", module, method, convertToString(req));

            validate(req);

            R res = doProcess(req);

            resp = DashboardResponse.success(res);
            return resp;
        } catch (AbstractDashboardException e) {
            resMark = Logger.FAILURE_MARK;
            if (e instanceof DashboardRuntimeException runtimeException) {
                log.warn("|{}|{}|DashboardRuntimeException|", method, method, runtimeException);
                resp = DashboardResponse.failed(runtimeException.getErrCode(), runtimeException.getErrMsg());
            } else {
                log.error("|{}|{}|DashboardSystemException|", method, method, e);
                resp = DashboardResponse.failed(e.getErrCode(), "system error");
            }
            return resp;
        } catch (Throwable e) {
            resMark = Logger.FAILURE_MARK;
            log.error("|{}|{}|{}|", method, method,  e.getClass().getSimpleName(), e);
            resp = DashboardResponse.failed(ResponseCodeEnum.RPC_UNKNOWN_EXCEPTION);
            return resp;
        } finally {
            log.info("|resp|{}|{}|{}|{}|{}|", module, method, resMark, System.currentTimeMillis() - start, convertToString(resp));
        }
    }

    protected void validate(Q q) throws DashboardParamException {
    }

    protected abstract R doProcess(Q q) throws Throwable;

    private static String convertToString(Object object) {
        return new String(JSON_SERIALIZER.serialize(object));
    }
}
