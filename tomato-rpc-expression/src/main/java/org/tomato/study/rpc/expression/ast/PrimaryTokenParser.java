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
 * 解析AST终结符
 * @author Tomato
 * Created on 2023.02.03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrimaryTokenParser implements ExpressionParser {

    private static final ASTNode[] EMPTY_CHILDREN = new ASTNode[0];

    private RootExpressionParser topExpressionParser;

    @Override
    public ASTNode parse(TokenStream tokenStream) {
        Token current = tokenStream.current();
        if (current == null) {
            return null;
        }
        switch (current.getType()) {
            case IDENTIFIER:
            case STR_LITERAL:
            case INT_LITERAL: {
                tokenStream.pop();
                return new PrimaryNode(current, EMPTY_CHILDREN);
            }
            case LEFT_PAREN: {
                tokenStream.pop();
                ASTNode subExpression = topExpressionParser.parse(tokenStream);
                if (subExpression == null) {
                    throw new IllegalStateException("parse sub expression error");
                }
                Token rightParen = tokenStream.current();
                if (rightParen == null || TokenType.RIGHT_PAREN != rightParen.getType()) {
                    throw new IllegalStateException("paren not match");
                }
                tokenStream.pop();
                return subExpression;
            }
            default:
                return null;
        }
    }
}
