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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.expression.TestCode;

/**
 * @author Tomato
 * Created on 2022.01.02
 */
public class TokenLexerTest {

    @Test
    public void tokenizeTest0() {
        Token[] tokensRes = TokenLexer.tokenize(TestCode.CODES.get(0)).getTokens();
        Assert.assertTrue(tokensRes != null && tokensRes.length == 11);

        Assert.assertTrue(StringUtils.equals(tokensRes[0].getValue(), "group"));
        Assert.assertSame(tokensRes[0].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[1].getValue(), "=="));
        Assert.assertSame(tokensRes[1].getType(), TokenType.EQ);

        Assert.assertTrue(StringUtils.equals(tokensRes[2].getValue(), "\"dev\""));
        Assert.assertSame(tokensRes[2].getType(), TokenType.STR_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[3].getValue(), "&&"));
        Assert.assertSame(tokensRes[3].getType(), TokenType.AND);

        Assert.assertTrue(StringUtils.equals(tokensRes[4].getValue(), "host"));
        Assert.assertSame(tokensRes[4].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[5].getValue(), "=="));
        Assert.assertSame(tokensRes[5].getType(), TokenType.EQ);

        Assert.assertTrue(StringUtils.equals(tokensRes[6].getValue(), "\"101.1.1.1\""));
        Assert.assertSame(tokensRes[6].getType(), TokenType.STR_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[7].getValue(), "->"));
        Assert.assertSame(tokensRes[7].getType(), TokenType.ARROW);

        Assert.assertTrue(StringUtils.equals(tokensRes[8].getValue(), "group"));
        Assert.assertSame(tokensRes[8].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[9].getValue(), "=="));
        Assert.assertSame(tokensRes[9].getType(), TokenType.EQ);

        Assert.assertTrue(StringUtils.equals(tokensRes[10].getValue(), "\"perf\""));
        Assert.assertSame(tokensRes[10].getType(), TokenType.STR_LITERAL);
    }

    @Test
    public void tokenizeTest1() {
        TokenStream tokenStream = TokenLexer.tokenize(TestCode.CODES.get(1));
        Token[] tokensRes = tokenStream.getTokens();

        Assert.assertTrue(tokensRes != null && tokensRes.length == 9);

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "id"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.IDENTIFIER);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "%"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.MOD);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "10"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.INT_LITERAL);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "=="));
        Assert.assertSame(tokenStream.current().getType(), TokenType.EQ);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "0"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.INT_LITERAL);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "->"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.ARROW);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "group"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.IDENTIFIER);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "=="));
        Assert.assertSame(tokenStream.current().getType(), TokenType.EQ);
        tokenStream.pop();

        Assert.assertTrue(StringUtils.equals(tokenStream.current().getValue(), "101"));
        Assert.assertSame(tokenStream.current().getType(), TokenType.INT_LITERAL);
        tokenStream.pop();

        Assert.assertEquals(tokensRes.length, tokenStream.getNext());
    }

    @Test
    public void tokenizeTest2() {
        Token[] tokensRes = TokenLexer.tokenize(TestCode.CODES.get(2)).getTokens();
        Assert.assertTrue(tokensRes != null && tokensRes.length == 25);

        Assert.assertTrue(StringUtils.equals(tokensRes[0].getValue(), "("));
        Assert.assertSame(tokensRes[0].getType(), TokenType.LEFT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[1].getValue(), "id"));
        Assert.assertSame(tokensRes[1].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[2].getValue(), "-"));
        Assert.assertSame(tokensRes[2].getType(), TokenType.MINUS);

        Assert.assertTrue(StringUtils.equals(tokensRes[3].getValue(), "100"));
        Assert.assertSame(tokensRes[3].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[4].getValue(), "<="));
        Assert.assertSame(tokensRes[4].getType(), TokenType.LE);

        Assert.assertTrue(StringUtils.equals(tokensRes[5].getValue(), "50"));
        Assert.assertSame(tokensRes[5].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[6].getValue(), ")"));
        Assert.assertSame(tokensRes[6].getType(), TokenType.RIGHT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[7].getValue(), "&&"));
        Assert.assertSame(tokensRes[7].getType(), TokenType.AND);

        Assert.assertTrue(StringUtils.equals(tokensRes[8].getValue(), "("));
        Assert.assertSame(tokensRes[8].getType(), TokenType.LEFT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[9].getValue(), "id"));
        Assert.assertSame(tokensRes[9].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[10].getValue(), "*"));
        Assert.assertSame(tokensRes[10].getType(), TokenType.MUL);

        Assert.assertTrue(StringUtils.equals(tokensRes[11].getValue(), "100"));
        Assert.assertSame(tokensRes[11].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[12].getValue(), ">="));
        Assert.assertSame(tokensRes[12].getType(), TokenType.GE);

        Assert.assertTrue(StringUtils.equals(tokensRes[13].getValue(), "30"));
        Assert.assertSame(tokensRes[13].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[14].getValue(), ")"));
        Assert.assertSame(tokensRes[14].getType(), TokenType.RIGHT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[15].getValue(), "||"));
        Assert.assertSame(tokensRes[15].getType(), TokenType.OR);

        Assert.assertTrue(StringUtils.equals(tokensRes[16].getValue(), "("));
        Assert.assertSame(tokensRes[16].getType(), TokenType.LEFT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[17].getValue(), "group"));
        Assert.assertSame(tokensRes[17].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[18].getValue(), "=="));
        Assert.assertSame(tokensRes[18].getType(), TokenType.EQ);

        Assert.assertTrue(StringUtils.equals(tokensRes[19].getValue(), "\"grey\""));
        Assert.assertSame(tokensRes[19].getType(), TokenType.STR_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[20].getValue(), ")"));
        Assert.assertSame(tokensRes[20].getType(), TokenType.RIGHT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[21].getValue(), "->"));
        Assert.assertSame(tokensRes[21].getType(), TokenType.ARROW);

        Assert.assertTrue(StringUtils.equals(tokensRes[22].getValue(), "group"));
        Assert.assertSame(tokensRes[22].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[23].getValue(), "=="));
        Assert.assertSame(tokensRes[23].getType(), TokenType.EQ);

        Assert.assertTrue(StringUtils.equals(tokensRes[24].getValue(), "\"grey\""));
        Assert.assertSame(tokensRes[24].getType(), TokenType.STR_LITERAL);
    }

    @Test
    public void tokenizeTest3() {
        Token[] tokensRes = TokenLexer.tokenize(TestCode.CODES.get(3)).getTokens();
        Assert.assertTrue(tokensRes != null && tokensRes.length == 17);

        Assert.assertTrue(StringUtils.equals(tokensRes[0].getValue(), "id"));
        Assert.assertSame(tokensRes[0].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[1].getValue(), "+"));
        Assert.assertSame(tokensRes[1].getType(), TokenType.PLUS);

        Assert.assertTrue(StringUtils.equals(tokensRes[2].getValue(), "10"));
        Assert.assertSame(tokensRes[2].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[3].getValue(), ">"));
        Assert.assertSame(tokensRes[3].getType(), TokenType.GT);

        Assert.assertTrue(StringUtils.equals(tokensRes[4].getValue(), "1"));
        Assert.assertSame(tokensRes[4].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[5].getValue(), "&&"));
        Assert.assertSame(tokensRes[5].getType(), TokenType.AND);

        Assert.assertTrue(StringUtils.equals(tokensRes[6].getValue(), "id"));
        Assert.assertSame(tokensRes[6].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[7].getValue(), "/"));
        Assert.assertSame(tokensRes[7].getType(), TokenType.DIV);

        Assert.assertTrue(StringUtils.equals(tokensRes[8].getValue(), "10"));
        Assert.assertSame(tokensRes[8].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[9].getValue(), "<"));
        Assert.assertSame(tokensRes[9].getType(), TokenType.LT);

        Assert.assertTrue(StringUtils.equals(tokensRes[10].getValue(), "2"));
        Assert.assertSame(tokensRes[10].getType(), TokenType.INT_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[11].getValue(), "->"));
        Assert.assertSame(tokensRes[11].getType(), TokenType.ARROW);

        Assert.assertTrue(StringUtils.equals(tokensRes[12].getValue(), "("));
        Assert.assertSame(tokensRes[12].getType(), TokenType.LEFT_PAREN);

        Assert.assertTrue(StringUtils.equals(tokensRes[13].getValue(), "group"));
        Assert.assertSame(tokensRes[13].getType(), TokenType.IDENTIFIER);

        Assert.assertTrue(StringUtils.equals(tokensRes[14].getValue(), "=="));
        Assert.assertSame(tokensRes[14].getType(), TokenType.EQ);

        Assert.assertTrue(StringUtils.equals(tokensRes[15].getValue(), "\"grey\""));
        Assert.assertSame(tokensRes[15].getType(), TokenType.STR_LITERAL);

        Assert.assertTrue(StringUtils.equals(tokensRes[16].getValue(), ")"));
        Assert.assertSame(tokensRes[16].getType(), TokenType.RIGHT_PAREN);
    }

    @Test
    public void tokenizeTest4() {
        boolean hasError = false;
        try {
            TokenLexer.tokenize(TestCode.CODES.get(4));
        } catch (IllegalStateException ex) {
            hasError = true;
        }

        Assert.assertTrue(hasError);
    }
}
