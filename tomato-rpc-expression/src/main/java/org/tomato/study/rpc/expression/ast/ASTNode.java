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

package org.tomato.study.rpc.expression.ast;

import org.tomato.study.rpc.expression.token.Token;

/**
 * AST抽象语法树节点
 * @author Tomato
 * Created on 2023.02.03
 */
public interface ASTNode {

    /**
     * 获取节点token
     * @return token
     */
    Token getToken();

    /**
     * 获取子节点
     * @return 子节点
     */
    ASTNode[] getChildren();

    /**
     * 节点求值
     * @param context 求值依赖的上下文
     * @return 节点值
     */
    String calc(ExpressionCalcContext context);
}
