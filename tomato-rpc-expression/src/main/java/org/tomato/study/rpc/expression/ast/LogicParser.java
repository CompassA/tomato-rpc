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
 * LOGIC ::= CMP | LOGIC && CMP | LOGIC || CMP
 * @author Tomato
 * Created on 2023.02.11
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogicParser implements ExpressionParser {

    private CmpParser cmpParser;

    @Override
    public ASTNode parse(TokenStream tokenStream) {
        ASTNode node = cmpParser.parse(tokenStream);
        if (node == null) {
            return null;
        }
        while (continueParse(tokenStream)) {
            Token opToken = tokenStream.pop();
            ASTNode right = cmpParser.parse(tokenStream);
            if (right == null) {
                throw new IllegalStateException("illegal [Logic] expression, missing right node after " + opToken.getType());
            }
            node = new LogicNode(opToken, new ASTNode[] {node, right});
        }
        return node;
    }

    private boolean continueParse(TokenStream tokenStream) {
        Token current = tokenStream.current();
        if (current == null) {
            return false;
        }
        TokenType type = current.getType();
        return type == TokenType.AND || type == TokenType.OR;
    }
}
