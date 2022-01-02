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

/**
 * 表达式文本类型
 * @author Tomato
 * Created on 2021.12.30
 */
public enum TokenType {

    /**
     * 数字字面量 12323
     */
    INT_LITERAL,

    /**
     * 字符字面量 "abc"  "23"
     */
    STR_LITERAL,

    /**
     * 箭头 ->
     */
    ARROW,

    /**
     * 逻辑与 &&
     */
    AND,

    /**
     * 逻辑或 ||
     */
    OR,

    /**
     * 左括号(
     */
    LEFT_PAREN,

    /**
     * 右括号)
     */
    RIGHT_PAREN,

    /**
     * 表达式变量
     */
    IDENTIFIER,

    /**
     * ==
     */
    EQ,

    /**
     * >=
     */
    GE,

    /**
     * =<
     */
    LE,

    /**
     * >
     */
    GT,

    /**
     * <
     */
    LT,

    /**
     * 加法 +
     */
    PLUS,

    /**
     * 减法
     */
    MINUS,

    /**
     * 乘法 *
     */
    MUL,

    /**
     * 除法 /
     */
    DIV,

    /**
     * 取余
     */
    MOD,

}
