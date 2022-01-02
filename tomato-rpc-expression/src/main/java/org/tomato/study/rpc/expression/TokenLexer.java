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

    /**
     * 对输入的字符做词法分析
     * @param code 代码
     * @return 词法分析后的token流
     */
    public TokenStream tokenize(String code) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("code is empty");
        }
        List<Token> tokens = new ArrayList<>(0);
        StringBuilder buffer = new StringBuilder(0);
        State state = State.INIT;
        for (char c : code.toCharArray()) {
            state = state.transfer(c, tokens, buffer);
            if (state == null) {
                throw new IllegalStateException("illegal state");
            }
        }

        // 最后一次需做特殊处理，防止遍历到末尾结束循环的情况但却没清除缓冲区token的情况
        if (state != State.INIT && buffer.length() > 0) {
            tokens.add(new Token(buffer.toString(), state.getMatchType()));
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
                if (c >= '0' && c <= '9') {
                    buffer.append(c);
                    return INT_LITERAL;
                } else if ((c >= 'a' && c <='z') || (c >= 'A' && c <= 'Z') || c == '_') {
                    buffer.append(c);
                    return IDENTIFIER;
                } else if (c == '"') {
                    buffer.append(c);
                    return STR_LITERAL;
                } else if (c == '-') {
                    buffer.append(c);
                    return MINUS;
                } else if (c == '&') {
                    buffer.append(c);
                    return AND;
                } else if (c == '|') {
                    buffer.append(c);
                    return OR;
                } else if (c == '=') {
                    buffer.append(c);
                    return EQ;
                } else if (c == '>') {
                    buffer.append(c);
                    return GT;
                } else if (c == '<') {
                    buffer.append(c);
                    return LT;
                } else if (c == '(') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.LEFT_PAREN));
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                } else if (c == ')') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.RIGHT_PAREN));
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                } else if (c == '+') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.PLUS));
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                } else if (c == '*') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.MUL));
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                } else if (c == '/') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.DIV));
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                } else if (c == '%') {
                    buffer.append(c);
                    tokens.add(new Token(buffer.toString(), TokenType.MOD));
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                }
                return null;
            }

            @Override
            public TokenType getMatchType() {
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

            @Override
            public TokenType getMatchType() {
                return TokenType.INT_LITERAL;
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
                    buffer.delete(0, buffer.length());
                    return State.INIT;
                }
                if (!State.isBlank(c)) {
                    buffer.append(c);
                    return State.STR_LITERAL;
                }
                return null;
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.STR_LITERAL;
            }
        },
        /**
         * 箭头 ->
         */
        ARROW {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                tokens.add(new Token(buffer.toString(), TokenType.ARROW));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.ARROW;
            }
        },

        /**
         * 逻辑与 &&
         */
        AND {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c != '&') {
                    return null;
                }
                buffer.append(c);
                tokens.add(new Token(buffer.toString(), TokenType.AND));
                buffer.delete(0, buffer.length());
                return State.INIT;
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.AND;
            }
        },

        /**
         * 逻辑或 ||
         */
        OR {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c != '|') {
                    return null;
                }
                buffer.append(c);
                tokens.add(new Token(buffer.toString(), TokenType.OR));
                buffer.delete(0, buffer.length());
                return State.INIT;
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.OR;
            }
        },

        /**
         * 表达式变量
         */
        IDENTIFIER {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (State.isAlpha(c) || c == '_') {
                    buffer.append(c);
                    return State.IDENTIFIER;
                }
                tokens.add(new Token(buffer.toString(), TokenType.IDENTIFIER));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.IDENTIFIER;
            }
        },

        /**
         * ==
         */
        EQ {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c != '=') {
                    return null;
                }
                buffer.append(c);
                tokens.add(new Token(buffer.toString(), TokenType.EQ));
                buffer.delete(0, buffer.length());
                return State.INIT;
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.EQ;
            }
        },

        /**
         * >=
         */
        GE {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                tokens.add(new Token(buffer.toString(), TokenType.GE));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.GE;
            }
        },

        /**
         * <=
         */
        LE {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                tokens.add(new Token(buffer.toString(), TokenType.LE));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.LE;
            }
        },

        /**
         * >
         */
        GT {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c == '=') {
                    buffer.append(c);
                    return State.GE;
                }
                tokens.add(new Token(buffer.toString(), TokenType.GT));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.GT;
            }
        },

        /**
         * <
         */
        LT {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c == '=') {
                    buffer.append(c);
                    return State.LE;
                }
                tokens.add(new Token(buffer.toString(), TokenType.LT));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.LT;
            }
        },

        /**
         * 减法
         */
        MINUS {
            @Override
            public State transfer(char c, List<Token> tokens, StringBuilder buffer) {
                if (c == '>') {
                    buffer.append(c);
                    return State.ARROW;
                }
                tokens.add(new Token(buffer.toString(), TokenType.MINUS));
                buffer.delete(0, buffer.length());
                return State.INIT.transfer(c, tokens, buffer);
            }

            @Override
            public TokenType getMatchType() {
                return TokenType.MINUS;
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

        /**
         * 获取当前状态对应的token type
         * @return token type
         */
        public abstract TokenType getMatchType();

        private static boolean isBlank(char c) {
            return c == ' ' || c == '\t' || c == '\n';
        }

        private static boolean isAlpha(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }
    }

}
