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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.expression.token.Token;
import org.tomato.study.rpc.expression.token.TokenStream;
import org.tomato.study.rpc.expression.token.TokenType;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tomato
 * Created on 2023.02.03
 */
public class ASTNodeParserTest {

    private RootExpressionParser expressionParser;

    @Before
    public void init() {
        PrimaryTokenParser primaryTokenParser = new PrimaryTokenParser();
        expressionParser =
                new RootExpressionParser(
                        new AddAndSubParser(
                                new MulAndDivAndModParser(primaryTokenParser)
                        )
                );
        primaryTokenParser.setTopExpressionParser(expressionParser);
    }

    @Test
    public void addSubNodeTest() {
        List<Token> tokens = Arrays.asList(
                /* 0 */ new Token("2", TokenType.INT_LITERAL),
                /* 1 */ new Token("+", TokenType.PLUS),
                /* 2 */ new Token("3", TokenType.INT_LITERAL),
                /* 3 */ new Token("-", TokenType.MINUS),
                /* 4 */ new Token("1", TokenType.INT_LITERAL)
        );
        TokenStream tokenStream = new TokenStream(tokens);

        ASTNode root = expressionParser.parse(tokenStream);

        Assert.assertNull(tokenStream.current());
        Assert.assertEquals(root.getToken(), tokens.get(3));

        ASTNode[] rootChildren = root.getChildren();
        Assert.assertEquals(rootChildren.length, 2);

        ASTNode rootLeft = rootChildren[0];
        ASTNode rootRight = rootChildren[1];
        Assert.assertEquals(rootLeft.getToken(), tokens.get(1));
        Assert.assertEquals(rootRight.getToken(), tokens.get(4));


        ASTNode[] rootLeftChildren = rootLeft.getChildren();
        Assert.assertEquals(rootLeftChildren.length, 2);
        Assert.assertEquals(rootLeftChildren[0].getToken(), tokens.get(0));
        Assert.assertEquals(rootLeftChildren[1].getToken(), tokens.get(2));

        Assert.assertEquals(Integer.parseInt(root.calc(new ExpressionCalcContext())), 4);
    }

    @Test
    public void mulDivModeNodeTest() {
        List<Token> tokens = Arrays.asList(
                /* 0 */ new Token("2", TokenType.INT_LITERAL),
                /* 1 */ new Token("*", TokenType.MUL),
                /* 2 */ new Token("30", TokenType.INT_LITERAL),
                /* 3 */ new Token("/", TokenType.DIV),
                /* 4 */ new Token("2", TokenType.INT_LITERAL),
                /* 5 */ new Token("%", TokenType.MOD),
                /* 6 */ new Token("4", TokenType.INT_LITERAL)
        );
        TokenStream tokenStream = new TokenStream(tokens);

        ASTNode root = expressionParser.parse(tokenStream);

        Assert.assertNull(tokenStream.current());
        Assert.assertEquals(root.getToken(), tokens.get(5));

        ASTNode[] rootChildren = root.getChildren();
        Assert.assertEquals(rootChildren.length, 2);

        ASTNode rootLeft = rootChildren[0];
        ASTNode rootRight = rootChildren[1];
        Assert.assertEquals(rootLeft.getToken(), tokens.get(3));
        Assert.assertEquals(rootRight.getToken(), tokens.get(6));


        ASTNode[] rootLeftChildren = rootLeft.getChildren();
        Assert.assertEquals(rootLeftChildren.length, 2);
        Assert.assertEquals(rootLeftChildren[0].getToken(), tokens.get(1));
        Assert.assertEquals(rootLeftChildren[1].getToken(), tokens.get(4));

        ASTNode[] leftLeftSubNodes = rootLeftChildren[0].getChildren();
        Assert.assertEquals(leftLeftSubNodes.length, 2);
        Assert.assertEquals(leftLeftSubNodes[0].getToken(), tokens.get(0));
        Assert.assertEquals(leftLeftSubNodes[1].getToken(), tokens.get(2));

        Assert.assertEquals(2, Integer.parseInt(root.calc(new ExpressionCalcContext())));
    }

    @Test
    public void parenTest() {
        /* (2+30)/(2*(9-1))*/
        List<Token> tokens = Arrays.asList(
                /* 0 */ new Token("(", TokenType.LEFT_PAREN),
                /* 1 */ new Token("2", TokenType.INT_LITERAL),
                /* 2 */ new Token("+", TokenType.PLUS),
                /* 3 */ new Token("30", TokenType.INT_LITERAL),
                /* 4 */ new Token(")", TokenType.RIGHT_PAREN),
                /* 5 */ new Token("/", TokenType.DIV),
                /* 6 */ new Token("(", TokenType.LEFT_PAREN),
                /* 7 */ new Token("2", TokenType.INT_LITERAL),
                /* 8 */ new Token("*", TokenType.MUL),
                /* 9 */ new Token("(", TokenType.LEFT_PAREN),
                /*10 */ new Token("9", TokenType.INT_LITERAL),
                /*11 */ new Token("-", TokenType.MINUS),
                /*12 */ new Token("1", TokenType.INT_LITERAL),
                /*13 */ new Token(")", TokenType.RIGHT_PAREN),
                /*14 */ new Token(")", TokenType.RIGHT_PAREN)
        );
        TokenStream tokenStream = new TokenStream(tokens);

        ASTNode root = expressionParser.parse(tokenStream);
        Assert.assertNull(tokenStream.current());
        Assert.assertEquals(tokens.get(5), root.getToken());
        Assert.assertEquals(tokens.get(2), root.getChildren()[0].getToken());
        Assert.assertEquals(tokens.get(8), root.getChildren()[1].getToken());
        Assert.assertEquals(tokens.get(11), root.getChildren()[1].getChildren()[1].getToken());

        Assert.assertEquals(2, Integer.parseInt(root.calc(new ExpressionCalcContext())));
    }
}
