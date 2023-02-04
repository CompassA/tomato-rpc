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
        String right = children[1].calc(context);

        Token token = getToken();
        BigDecimal leftNum = new BigDecimal(left);
        BigDecimal rightNum = new BigDecimal(right);
        switch (token.getType()) {
            case PLUS:
                return  leftNum.add(rightNum).toPlainString();
            case MINUS:
                return leftNum.subtract(rightNum).toPlainString();
            case MUL:
                return leftNum.multiply(rightNum).toPlainString();
            case DIV:
                return leftNum.divideAndRemainder(rightNum)[0].toPlainString();
            case MOD:
                return leftNum.divideAndRemainder(rightNum)[1].toPlainString();
            default:
                throw new IllegalStateException("illegal operation type: " + token.getType());
        }
    }
}
