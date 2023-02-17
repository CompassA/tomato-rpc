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
 * ROUTER_EXPR ::= EXPR -> EXPR
 * @author Tomato
 * Created on 2023.02.11
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouterExpressionParser implements ExpressionParser {

    private RootExpressionParser rootExpressionParser;

    @Override
    public ASTNode parse(TokenStream tokenStream) {
        ASTNode left = rootExpressionParser.parse(tokenStream);
        if (left == null) {
            return null;
        }
        Token opToken = tokenStream.current();
        if (opToken == null || opToken.getType() != TokenType.ARROW) {
            throw new IllegalStateException("illegal [ROUTER_EXPR] expression, missing \"->\"");
        }
        tokenStream.pop();
        ASTNode right = rootExpressionParser.parse(tokenStream);
        if (right == null) {
            throw new IllegalStateException("illegal [ROUTER_EXPR] expression, missing right node after \"->\"");
        }
        return new RouterExpressionNode(opToken, new ASTNode[] {left, right});
    }
}
