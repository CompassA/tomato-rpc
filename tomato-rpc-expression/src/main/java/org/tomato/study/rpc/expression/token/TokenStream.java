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

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.12.30
 */
public class TokenStream {

    /**
     * token列表
     */
    @Getter
    private final Token[] tokens;

    /**
     * 下一个要用到的token
     */
    @Getter
    @Setter
    private int next;

    public TokenStream(List<Token> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            throw new IllegalArgumentException("token list is empty");
        }
        this.tokens = tokens.toArray(Token[]::new);
        this.next = 0;
    }

    /**
     * 获取当前token，但不消耗流位置
     * @return token
     */
    public Token current() {
        if (next >= tokens.length) {
            return null;
        }
        return tokens[next];
    }

    /**
     * 获取当前token，并且消耗流位置
     * @return token
     */
    public Token pop() {
        if (next >= tokens.length) {
            return null;
        }
        return tokens[next++];
    }
}
