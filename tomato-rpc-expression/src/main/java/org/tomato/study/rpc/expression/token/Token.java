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

package org.tomato.study.rpc.expression.token;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 表达式词法分析元素
 * ex: val1 == "abc" && val2 != 2 -> b == 3
 * @author Tomato
 * Created on 2021.12.30
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class Token {

    /**
     * 词法分析提取出的文本
     */
    private final String value;

    /**
     * 文本类型
     */
    private final TokenType type;
}
