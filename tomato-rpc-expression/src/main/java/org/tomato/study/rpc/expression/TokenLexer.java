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

package org.tomato.study.rpc.expression;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 词法分析
 * @author Tomato
 * Created on 2021.12.30
 */
public class TokenLexer {

    public TokenStream tokenize(String code) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("code is empty");
        }
        List<Token> tokens = new ArrayList<>(0);
        StringBuilder current = new StringBuilder(0);
        State state = State.INIT;
        for (char c : code.toCharArray()) {
            state = state.transfer(c, tokens, current);
            if (state == null) {
                throw new IllegalStateException("illegal state");
            }
        }
        return new TokenStream(tokens);
    }

    private enum State {
        /**
         * 初始状态
         */
        INIT {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (State.isBlank(c)) {
                    return State.INIT;
                }
                buffer.append(c);
                if (c >= '0' && c <= '9') {
                    return INT_LITERAL;
                }
                if ((c >= 'a' && c <='z') || (c >= 'A' && c <= 'Z') || c == '_') {
                    return IDENTIFIER;
                }
                if (c == '"') {
                    return STR_LITERAL;
                }
                if (c == '-') {
                    return MINUS;
                }
                if (c == '&') {
                    return AND1;
                }
                if (c == '|') {
                    return OR1;
                }
                if (c == '=') {
                    return EQ1;
                }
                if (c == '>') {
                    return GT;
                }
                if (c == '<') {
                    return LT;
                }
                if (c == '(') {
                    return LEFT_PAREN;
                }
                if (c == ')') {
                    return RIGHT_PAREN;
                }
                if (c == '+') {
                    return PLUS;
                }
                if (c == '*') {
                    return MUL;
                }
                if (c == '/') {
                    return DIV;
                }
                return null;
            }
        },
        
        /**
         * 数字字面量 12323
         */
        INT_LITERAL {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c >= '0' && c <= '9') {
                    buffer.append(c);
                    return INT_LITERAL;
                }
                tokens.add(new Token(buffer.toString(), TokenType.INT_LITERAL));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }
        },

        /**
         * 字符字面量 "abc"  "23"
         */
        STR_LITERAL {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c == '"') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.STR_LITERAL));
                    return State.INIT;
                }
                if (!State.isBlank(c)) {
                    buffer.append(c);
                    return State.STR_LITERAL;
                }
                return null;
            }
        },
        /**
         * 箭头 ->
         */
        ARROW {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 逻辑与 &&
         */
        AND1 {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },
        /**
         * 逻辑与 &&
         */
        AND2 {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 逻辑或 ||
         */
        OR1 {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },
        /**
         * 逻辑或 ||
         */
        OR2 {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 左括号(
         */
        LEFT_PAREN {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 右括号)
         */
        RIGHT_PAREN {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 表达式变量
         */
        IDENTIFIER {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * ==
         */
        EQ1 {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },
        /**
         * ==
         */
        EQ2 {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * >=
         */
        GE {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * =<
         */
        LE {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * >
         */
        GT {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * <
         */
        LT {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 加法 +
         */
        PLUS {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 减法
         */
        MINUS {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 乘法 *
         */
        MUL {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                return null;
            }
        },

        /**
         * 除法 /
         */
        DIV {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {

                return null;
            }
        },
        ;

        /**
         * 根据当前的字符做状态转移
         * @param c 当前字符
         * @param tokens 已经生成的token
         * @param buffer 当前正在解析的token
         * @return 下一个状态
         */
        public abstract State transfer(char c,
                                       List<Token> tokens,
                                       StringBuilder buffer);

        private static boolean isBlank(char c) {
            return c == ' ' || c == '\t' || c == '\n';
        }
    }

}
