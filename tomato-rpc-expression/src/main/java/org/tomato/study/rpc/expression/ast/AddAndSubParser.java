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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.expression.token.Token;
import org.tomato.study.rpc.expression.token.TokenStream;
import org.tomato.study.rpc.expression.token.TokenType;

/**
 * 解析[ADD_SUB]符号
 * @author Tomato
 * Created on 2023.02.03
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddAndSubParser implements ExpressionParser {

    private MulAndDivAndModParser lowerParser;

    @Override
    public ASTNode parse(TokenStream tokenStream) {
        ASTNode node = lowerParser.parse(tokenStream);
        if (node == null) {
            return null;
        }

        while (canContinue(tokenStream)) {
            Token opToken = tokenStream.pop();
            ASTNode right = lowerParser.parse(tokenStream);
            if (right == null) {
                throw new IllegalStateException("illegal [ADD_SUB] expression, missing right node after " + opToken.getType());
            }
            node = new NumCalcNode(opToken, new ASTNode[]{node, right});
        }

        return node;
    }

    private boolean canContinue(TokenStream tokenStream) {
        Token topToken = tokenStream.current();
        if (topToken == null) {
            return false;
        }
        TokenType type = topToken.getType();
        return TokenType.PLUS == type || TokenType.MINUS == type;
    }

}
