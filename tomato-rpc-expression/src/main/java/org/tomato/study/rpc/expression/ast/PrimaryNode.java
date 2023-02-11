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

import lombok.NoArgsConstructor;
import org.tomato.study.rpc.expression.token.Token;

/**
 * 终结符节点
 * @author Tomato
 * Created on 2023.02.03
 */
@NoArgsConstructor
public class PrimaryNode extends AbstractASTNode {

    public PrimaryNode(Token token, ASTNode[] children) {
        super(token, children);
    }

    @Override
    public String calc(ExpressionCalcContext context) {
        return getToken().getValue();
    }
}
