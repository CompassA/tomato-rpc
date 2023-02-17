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

import java.math.BigDecimal;

/**
 * 四则运算计算节点
 * @author Tomato
 * Created on 2023.02.03
 */
@NoArgsConstructor
public class NumCalcNode extends AbstractASTNode {

    public NumCalcNode(Token token, ASTNode[] children) {
        super(token, children);
    }

    @Override
    public String calc(ExpressionCalcContext context) {
        ASTNode[] children = getChildren();
        if (children.length != 2) {
            throw new IllegalStateException("missing children nodes");
        }
        String left = children[0].calc(context);
        if (ExpressionConstant.NULL.equals(left)) {
            return ExpressionConstant.NULL;
        }
        String right = children[1].calc(context);
        if (ExpressionConstant.NULL.equals(right)) {
            return ExpressionConstant.NULL;
        }

        Token token = getToken();
        BigDecimal leftNum = new BigDecimal(left);
        BigDecimal rightNum = new BigDecimal(right);

        BigDecimal res;
        switch (token.getType()) {
            case PLUS:
                res = leftNum.add(rightNum);
                break;
            case MINUS:
                res = leftNum.subtract(rightNum);
                break;
            case MUL:
                res = leftNum.multiply(rightNum);
                break;
            case DIV:
                res = leftNum.divideAndRemainder(rightNum)[0];
                break;
            case MOD:
                res = leftNum.divideAndRemainder(rightNum)[1];
                break;
            default:
                throw new IllegalStateException("illegal operation type: " + token.getType());
        }
        return (BigDecimal.ZERO.compareTo(res) == 0) ? ExpressionConstant.FALSE : res.toPlainString();
    }
}
