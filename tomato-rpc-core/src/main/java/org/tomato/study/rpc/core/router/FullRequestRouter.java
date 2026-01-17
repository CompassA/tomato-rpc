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

package org.tomato.study.rpc.core.router;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.expression.ast.ASTNode;
import org.tomato.study.rpc.expression.ast.ExpressionCalcContext;
import org.tomato.study.rpc.expression.ast.ExpressionConstant;

/**
 * 所有请求都能匹配的路由
 * @author Tomato
 * Created on 2023.02.12
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FullRequestRouter implements Router {

    /**
     * 原始表达式
     */
    private String expression;

    /**
     * 右表达式, 筛选Invoker
     */
    private ASTNode rightExpr;

    @Override
    public boolean matchRequest(ExpressionCalcContext context) {
        return true;
    }

    @Override
    public boolean matchInvoker(RpcInvoker rpcInvoker) {
        ExpressionCalcContext context = new ExpressionCalcContext();
        context.setValMap(rpcInvoker.getInvokerPropertyMap());

        String res = rightExpr.calc(context);

        return ExpressionConstant.TRUE.equals(res);
    }
}
