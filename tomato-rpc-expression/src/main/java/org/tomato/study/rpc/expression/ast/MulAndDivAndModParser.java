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
 * 解析[MUL_DIV_MOD]符号
 * @author Tomato
 * Created on 2023.02.03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MulAndDivAndModParser implements ExpressionParser {

    private PrimaryTokenParser lowerParser;

    @Override
    public ASTNode parse(TokenStream tokenStream) {
        ASTNode node = lowerParser.parse(tokenStream);
        if (node == null) {
            return null;
        }

        while (canContinueParse(tokenStream)) {
            Token opToken = tokenStream.pop();
            ASTNode right = lowerParser.parse(tokenStream);
            if (right == null) {
                throw new IllegalStateException("illegal [MUL_DIV_MOD] expression, missing right node after " + opToken.getType());
            }
            node = new NumCalcNode(opToken, new ASTNode[]{node, right});
        }
        return node;
    }

    private boolean canContinueParse(TokenStream tokenStream) {
        Token current = tokenStream.current();
        if (current == null) {
            return false;
        }
        TokenType type = current.getType();
        return TokenType.MUL == type || TokenType.DIV == type || TokenType.MOD == type;
    }
}
