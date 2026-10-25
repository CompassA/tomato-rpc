# tomato-rpc-expression

> 一个独立的、零框架依赖的表达式引擎：手写词法分析 + 递归下降语法分析 + AST 求值。它只干「把一段字符串变成可反复求值的对象」，不知道 RPC、不知道路由、也不知道 ZK。

语法细节、真值语义、坑位清单在 [表达式引擎](./../12-expression.md) 里逐条展开；本文只讲**模块怎么组织、类怎么协作**。

## 职责

| 它做什么                                       | 它不做什么                                             |
| ---------------------------------------------- | ------------------------------------------------------ |
| 词法分析：`String` → `TokenStream`             | 决定表达式从哪来（dashboard / ZK）                     |
| 语法分析：`TokenStream` → `ASTNode`            | 决定判定为真之后选哪个实例                             |
| 求值：`ASTNode#calc(ctx)` → `"1"` / `"0"`      | 提供变量（`ExpressionCalcContext` 由调用方填）         |
| 支持八层优先级与括号                           | 短路求值引发的副作用控制                               |
| 路由语法的 `->` 分叉（`RouterExpressionNode`） | 把 AST 和 `MetaData` 绑定（那是 core 的 `ExprRouter`） |

## 依赖

```text
tomato-rpc-expression
├── lombok（provided）
├── commons-lang3
└── commons-collections4
```

**不依赖 core、不依赖 common**。反过来 `tomato-rpc-core` 依赖它，所以它在这个仓库里是「最底层的可复用件」。

## 包结构

```text
org.tomato.study.rpc.expression
├── token/   Token、TokenType、TokenStream、TokenLexer
└── ast/     ASTNode 接口、AbstractASTNode、8 层 Parser、4 种 Node
```

## 类清单

### token

| 类                                                                                                              | 职责                                                                     |
| --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `Token`                                                                                                         | `(value, type)` 二元组，`@EqualsAndHashCode`                             |
| `TokenType`                                                                                                     | 18 种终结符，见下表                                                      |
| [TokenStream](../../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/token/TokenStream.java) | 包住 `Token[]` + 游标，只有 `current()` / `pop()` 两个动作，**没有回退** |
| [TokenLexer](../../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/token/TokenLexer.java)   | 手写状态机，`tokenize(String)` 入口                                      |

| TokenType             | 文本        | TokenType                    | 文本              |
| --------------------- | ----------- | ---------------------------- | ----------------- |
| `INT_LITERAL`         | `123`       | `IDENTIFIER`                 | `userId`、`group` |
| `STR_LITERAL`         | `"abc"`     | `LEFT_PAREN` / `RIGHT_PAREN` | `(` `)`           |
| `ARROW`               | `->`        | `AND` / `OR`                 | `&&` `\|\|`       |
| `EQ`                  | `==`        | `GE` / `LE`                  | `>=` `<=`         |
| `GT` / `LT`           | `>` `<`     | `PLUS` / `MINUS`             | `+` `-`           |
| `MUL` / `DIV` / `MOD` | `*` `/` `%` |                              |                   |

### ast

| 类                                      | 职责                                                                                                                                                                    |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ASTNode`                               | 三方法接口：`getToken()` / `getChildren()` / `calc(ExpressionCalcContext)`                                                                                              |
| `AbstractASTNode`                       | 存 `token` + `children`，不含求值逻辑                                                                                                                                   |
| `PrimaryNode`                           | 叶子：标识符、数字、字符串                                                                                                                                              |
| `CmpNode` / `LogicNode` / `NumCalcNode` | 比较、逻辑、算术三类内部节点                                                                                                                                            |
| `RouterExpressionNode`                  | 分叉节点：`children[0]` 是条件，`children[1]` 是动作                                                                                                                    |
| `ExpressionConstant`                    | `TRUE = "1"`、`FALSE = "0"`、`NULL = "$__NULL__$"`                                                                                                                      |
| `ExpressionCalcContext`                 | 求值上下文，本质是一个 `Map<String, String>` 的包装                                                                                                                     |
| 8 个 Parser                             | `RouterExpressionParser`、`RootExpressionParser`、`LogicParser`、`CmpParser`、`AddAndSubParser`、`MulAndDivAndModParser`、`PrimaryTokenParser`、`ExpressionParser` 接口 |

## 责任链怎么装起来

整条链只在 `RouterExpressionParser` 的静态初始化块里装配一次，之后全局共享（无状态，线程安全）：

```java
static {
    PrimaryTokenParser primaryTokenParser = new PrimaryTokenParser();
    RootExpressionParser rootExpressionParser =
        new RootExpressionParser(
            new LogicParser(new CmpParser(new AddAndSubParser(
                new MulAndDivAndModParser(primaryTokenParser)))));
    primaryTokenParser.setTopExpressionParser(rootExpressionParser);  // 括号回边
    PARSER = new RouterExpressionParser(rootExpressionParser);
}
```

```text
RouterExpressionParser        A -> B
└── RootExpressionParser      转发
    └── LogicParser           && ||
        └── CmpParser         == >= <= > <
            └── AddAndSubParser        + -
                └── MulAndDivAndModParser   * / %
                    └── PrimaryTokenParser   字面量 / 标识符 / ( ... ) → 回到 Root
```

`PrimaryTokenParser` 里的 `topExpressionParser` 是**唯一的回边**，它让括号里的子表达式能从最高优先级重新开始，这也是递归下降处理括号的标准写法。

## 三个阶段的数据形态

以 `userId % 10 == 2 -> group == "gray"` 为例（字符串只能用双引号）：

```text
        │
        ▼ TokenLexer.tokenize
[IDENTIFIER:userId] [MOD:%] [INT_LITERAL:10] [EQ:==] [INT_LITERAL:2] [ARROW:->]
[IDENTIFIER:group] [EQ:==] [STR_LITERAL:"gray"]
        │
        ▼ RouterExpressionParser.parse
RouterExpressionNode
├── children[0] = CmpNode(==, NumCalcNode(%, userId, 10), 2)
└── children[1] = CmpNode(==, PrimaryNode(group), PrimaryNode("gray"))
        │
        ▼ calc(context)
"1" 或 "0"
```

`calc` 的返回类型是 `String` 而不是 `boolean`，这是整个引擎最容易踩的地方：

- 逻辑运算里用 `"0"` 判假；
- 顶层只认**精确等于 `"1"`** 为真；
- 未知标识符求值成 `"$__NULL__$"`，行为视节点而定（见 [12-expression](./../12-expression.md) 的「真值三坑」）。

## 与 core 的衔接

模块本身没有 RPC 概念，衔接点只有两处：

| 位置                                       | 用法                                                                                                                                                                     |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `BaseMicroServiceSpace` 静态块             | `PARSER.parse(TokenLexer.tokenize(expr))`，拿到 `RouterExpressionNode` 后取 `children[0]` / `children[1]` 包成 `ExprRouter(i + 1, expression, children[0], children[1])` |
| `ExprRouter#matchRequest` / `matchInvoker` | 分别用 `InvocationContext` 和 `invoker.getMetadata().toMap()` 构造 `ExpressionCalcContext` 再 `calc`                                                                     |

也就是说：**语法层只保证结构合法，语义（谁能做变量、真值是啥）由 core 决定**。

## 已知限制

| #   | 限制                 | 说明                                                                                |
| --- | -------------------- | ----------------------------------------------------------------------------------- | ------ | ---------------------------------------------- |
| 1   | 无位置信息           | `Token` 只有 `(value, type)`，报错只知道「第几个表达式」，不知道第几列              |
| 2   | 空表达式直接抛异常   | `tokenize("")` 抛 `IllegalArgumentException`，调用方 `refreshRouter` 靠 catch 兜住  |
| 3   | 标识符不能含数字     | `user1` 会在 `1` 处被切成 `IDENTIFIER + INT_LITERAL`，随后解析失败                  |
| 4   | 没有小数与负数字面量 | `1.5`、`-1` 都非法（`-` 只能作为二元运算符）                                        |
| 5   | 没有 `!=`            | 想表达不等只能写 `a > b                                                             |        | a < b`                                         |
| 6   | 字符串不能含空格     | 词法在 `STR_LITERAL` 态遇到非字母数字即返回 `null` → `IllegalStateException`        |
| 7   | `&&` / `             |                                                                                     | ` 同级 | 没有 `&&` 优先于 `\|\|` 的约定，混用必须加括号 |
| 8   | 没有短路优化         | 两侧都会被求值；`LogicNode` 的短路只作用于「与 0 比较」                             |
| 9   | 无缓存               | 每条表达式每次刷新都重新词法+语法分析（`refreshRouter` 只在规则变更时跑，影响不大） |

## 测试

| 测试                | 覆盖                                           |
| ------------------- | ---------------------------------------------- |
| `TokenLexerTest`    | 各类合法/非法字符输入，断言 token 序列或抛异常 |
| `ASTNodeParserTest` | 八层优先级、括号、`->` 结构、错误表达式        |
| `TestCode`          | 手工调试用的 main 方法，不属于测试用例         |

详见 [测试](./../13-testing.md)。

## 阅读顺序

1. [TokenType](../../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/token/TokenType.java)：先看语言里有哪些符号。
2. [TokenLexer](../../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/token/TokenLexer.java)：状态机怎么切词。
3. [RouterExpressionParser](../../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/RouterExpressionParser.java) 的静态块：一眼看清整条链。
4. `LogicParser` → `CmpParser` → `PrimaryTokenParser`：跟着一个表达式走完递归下降。
5. `RouterExpressionNode` → `LogicNode` → `CmpNode` → `NumCalcNode`：求值语义。

配套章节：[表达式引擎](./../12-expression.md)、[服务治理](./../08-governance.md)。
