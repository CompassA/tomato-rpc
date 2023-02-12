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
import org.tomato.study.rpc.expression.token.TokenType;

import java.math.BigDecimal;

import static org.tomato.study.rpc.expression.ast.ExpressionConstant.FALSE;
import static org.tomato.study.rpc.expression.ast.ExpressionConstant.NULL;
import static org.tomato.study.rpc.expression.ast.ExpressionConstant.TRUE;

/**
 * @author Tomato
 * Created on 2023.02.11
 */
@Getter
@Setter
public class CmpNode extends AbstractASTNode {

    public CmpNode(Token token, ASTNode[] children) {
        super(token, children);
    }

    @Override
    public String calc(ExpressionCalcContext context) {
        ASTNode[] children = getChildren();
        String leftStr = children[0].calc(context);
        if (NULL.equals(leftStr)) {
            return FALSE;
        }
        String rightStr = children[1].calc(context);
        if (NULL.equals(rightStr)) {
            return FALSE;
        }
        if (TokenType.STR_LITERAL.equals(children[1].getToken().getType()) ||
                TokenType.STR_LITERAL.equals(children[0].getToken().getType())) {
            return compare(leftStr, rightStr);
        }
        return compare(new BigDecimal(leftStr), new BigDecimal(rightStr));
    }

    private <T extends Comparable<T>> String compare(T left, T right) {
        TokenType type = getToken().getType();
        switch (type) {
            case EQ:
                return left.compareTo(right) == 0 ? TRUE : FALSE;
            case GT:
                return left.compareTo(right) > 0 ? TRUE : FALSE;
            case LT:
                return left.compareTo(right) < 0 ? TRUE : FALSE;
            case GE:
                return left.compareTo(right) >= 0 ? TRUE : FALSE;
            case LE:
                return left.compareTo(right) <= 0 ? TRUE : FALSE;
            default:
                throw new IllegalStateException("illegal operation type: " + type);
        }
    }
}
