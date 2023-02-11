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

import org.tomato.study.rpc.expression.token.TokenStream;

/**
 * PRIMARY ::= INT_LITERAL | STR_LITERAL | IDENTIFIER | (EXPR)
 * MUL_DIV_MOD ::= PRIMARY | MUL_DIV_MOD * PRIMARY | MUL_DIV_MOD / PRIMARY | MUL_DIV_MOD % PRIMARY
 * ADD_SUB ::= MUL_DIV_MOD | ADD_SUB + MUL_DIV_MOD | ADD_SUB - MUL_DIV_MOD
 * CMP ::= ADD_SUB | CMP > ADD_SUB | CMP >= ADD_SUB | CMP < ADD_SUB | CMP <= ADD_SUB | CMP == ADD_SUB
 * LOGIC ::= CMP | LOGIC && CMP | LOGIC || CMP
 * EXPR ::= LOGIC
 * ROUTER_EXPR ::= EXPR -> EXPR
 * @author Tomato
 * Created on 2023.02.03
 */
public interface ExpressionParser {

    /**
     * 解析token流为表达式
     * @param tokenStream token流
     * @return AST节点
     */
    ASTNode parse(TokenStream tokenStream);
}
