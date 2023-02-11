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

import lombok.Getter;
import lombok.Setter;
import org.tomato.study.rpc.expression.token.Token;

import static org.tomato.study.rpc.expression.ast.ExpressionConstant.FALSE;
import static org.tomato.study.rpc.expression.ast.ExpressionConstant.TRUE;

/**
 * @author Tomato
 * Created on 2023.02.11
 */
@Getter
@Setter
public class LogicNode extends AbstractASTNode {

    public LogicNode(Token token, ASTNode[] children) {
        super(token, children);
    }

    @Override
    public String calc(ExpressionCalcContext context) {
        ASTNode[] children = getChildren();
        Token opToken = getToken();
        String left = children[0].calc(context);
        switch (opToken.getType()) {
            case AND: {
                if (left.equals(FALSE)) {
                    return FALSE;
                }
                String right = children[1].calc(context);
                if (right.equals(FALSE)) {
                    return FALSE;
                }
                return TRUE;
            }
            case OR: {
                if (left.equals(TRUE)) {
                    return TRUE;
                }
                String right = children[1].calc(context);
                if (right.equals(TRUE)) {
                    return TRUE;
                }
                return FALSE;
            }
            default:
                throw new IllegalStateException("illegal operation type: " + opToken.getType());
        }
    }
}
