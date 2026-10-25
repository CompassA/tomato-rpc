# 表达式引擎（expression）

路由、灰度、流量染色本质上都是「**按条件挑实例**」。如果把这个条件写死在代码里，每加一条规则就要改代码、发版；写进配置又需要一门能被安全执行的"小语言"。

`tomato-rpc-expression` 就是这个语言：**一个零依赖（只用 lombok + commons 工具类）的微型表达式引擎**，包含词法分析、递归下降语法分析、AST 求值三段，与 RPC 框架本身完全解耦（core 反向依赖 expression，而不是反过来）。

> 语法灵感来自 C 系语言：`group=="gray" && weight>=5`。它的作用域只有「路由规则」，不是通用脚本引擎——没有变量声明、没有函数、没有循环。

## 模块结构

```text
tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/
├── token/
│   ├── Token.java        // (value, type) 二元组，value 保留原始文本
│   ├── TokenType.java    // 18 种终结符
│   ├── TokenStream.java  // Token[] + next 游标，current() / pop()
│   └── TokenLexer.java   // 手写状态机，static tokenize(String) → TokenStream
└── ast/
    ├── ExpressionParser.java            // parse(TokenStream) → ASTNode，注释里写着完整 EBNF
    ├── RootExpressionParser.java        // 门面，代理给 LogicParser
    ├── LogicParser.java   LogicNode.java
    ├── CmpParser.java     CmpNode.java
    ├── AddAndSubParser.java  ┐
    ├── MulAndDivAndModParser.java ├ NumCalcNode.java
    ├── PrimaryTokenParser.java    PrimaryNode.java
    ├── RouterExpressionParser.java  RouterExpressionNode.java   // 处理 `->`
    ├── ASTNode.java / AbstractASTNode.java
    ├── ExpressionCalcContext.java       // 求值上下文（就是一张 Map）
    └── ExpressionConstant.java          // TRUE / FALSE / NULL
```

两级结构一目了然：**token 包只认字符，ast 包只认 token**，两边都不知道 RPC 的存在。

## 词法分析

入口是一个静态方法，没有实例、没有状态泄漏：

```java
public static TokenStream tokenize(String code) {
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
    // 最后一个字符可能还在 buffer 里（例如 "age>=3" 的 "3"），补一个 token
    if (state != State.INIT && buffer.length() > 0) {
        tokens.add(new Token(buffer.toString(), state.getMatchType()));
    }
    return new TokenStream(tokens);
}
```

`State` 是一个**枚举形式的状态机**，每个枚举常量实现 `transfer(char, tokens, buffer)` 返回下一个状态；返回 `null` 就是非法字符。状态集：

| 状态          | 含义          | 转入条件                                                                                  | 转出行为                                                 |
| ------------- | ------------- | ----------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `INIT`        | 空闲          | 空白 / 数字 / 字母下划线 / `"` / `-` / `&` / `\|` / `=` / `>` / `<` / `(` `)` / `+ * / %` | 单字符运算符当场出 token 并留在 `INIT`                   |
| `INT_LITERAL` | 数字          | 读到 `0-9`                                                                                | 读到非数字 → 出 `INT_LITERAL` 并把该字符重新交给 `INIT`  |
| `IDENTIFIER`  | 变量名        | 字母或 `_`                                                                                | 读到非「字母/下划线」→ 出 `IDENTIFIER` 并重新交给 `INIT` |
| `STR_LITERAL` | 字符串        | 读到 `"`                                                                                  | 再读到 `"` → 出 `STR_LITERAL`（**value 含首尾引号**）    |
| `MINUS`       | `-`           | 读到 `-`                                                                                  | 下一个是 `>` → 转 `ARROW`；否则出 `MINUS`                |
| `ARROW`       | `->`          | `-` 后跟 `>`                                                                              | 出 `ARROW`                                               |
| `AND` / `OR`  | `&&` / `\|\|` | 单个 `&` / `\|`                                                                           | 必须再读到同样的符号，否则抛「illegal state」            |
| `EQ`          | `==`          | 单个 `=`                                                                                  | 必须再读到 `=`                                           |
| `GT` / `LT`   | `>` / `<`     | 读到 `>` / `<`                                                                            | 下一个是 `=` → `GE` / `LE`，否则出单字符 token           |
| `GE` / `LE`   | `>=` / `<=`   | `>` `<` 后跟 `=`                                                                          | 下一个字符无条件出 token（立即结束）                     |

「出 token 后把当前字符**重新交给 `INIT`**」是这个状态机最关键的一行：它让 `"3+4"`、`"a==1"` 这类**没有空格分隔**的表达式也能正确切分。

### 词法层的边界

| 输入      | 结果                                           | 说明                                                |
| --------- | ---------------------------------------------- | --------------------------------------------------- |
| `age>=3`  | `IDENTIFIER(age) GE(>=) INT_LITERAL(3)`        | 无空格也能切                                        |
| `1.5`     | **抛异常**                                     | 没有 `.` 状态；`INT_LITERAL` 只吃 `0-9`             |
| `-1`      | `MINUS INT_LITERAL(1)` → 语法阶段返回 null     | 不支持负数字面量                                    |
| `a1`      | `IDENTIFIER(a) INT_LITERAL(1)`                 | **标识符不能含数字**（`IDENTIFIER` 只吃字母和 `_`） |
| `"a b"`   | **抛异常**                                     | `STR_LITERAL` 遇到空白返回 null                     |
| `!`、`!=` | **抛异常**                                     | 没有 `!` 状态，也没有 NE 运算符                     |
| `a & b`   | **抛异常**                                     | 单个 `&` 不是合法运算符                             |
| `"" `     | 抛 `IllegalArgumentException("code is empty")` | 全空白入口即拦                                      |

## 语法与优先级

`ExpressionParser` 的类注释就是一份完整 EBNF，八层从上到下优先级递增：

```text
ROUTER_EXPR ::= EXPR -> EXPR
EXPR        ::= LOGIC
LOGIC       ::= CMP | LOGIC && CMP | LOGIC || CMP
CMP         ::= ADD_SUB | CMP > ADD_SUB | CMP >= ADD_SUB
                      | CMP < ADD_SUB | CMP <= ADD_SUB | CMP == ADD_SUB
ADD_SUB     ::= MUL_DIV_MOD | ADD_SUB + MUL_DIV_MOD | ADD_SUB - MUL_DIV_MOD
MUL_DIV_MOD ::= PRIMARY | MUL_DIV_MOD * PRIMARY
                        | MUL_DIV_MOD / PRIMARY | MUL_DIV_MOD % PRIMARY
PRIMARY     ::= INT_LITERAL | STR_LITERAL | IDENTIFIER | (EXPR)
```

| 优先级 | 运算符                 | 结合性 | 结果          |
| ------ | ---------------------- | ------ | ------------- |
| 最低   | `\|\|` `&&`            | 左     | `"1"` / `"0"` |
| ↑      | `==` `>=` `<=` `>` `<` | 左     | `"1"` / `"0"` |
| ↑      | `+` `-`                | 左     | 数字字符串    |
| ↑      | `*` `/` `%`            | 左     | 数字字符串    |
| 最高   | `(` `)`                | —      | 子表达式      |

注意 `&&` 与 `||` **同级**（不像 C 里 `&&` 高于 `||`），所以 `a && b || c` 按 `(a && b) || c` 解析；想要别的组合必须加括号。

## 递归下降：一条手写的责任链

没有用 parser generator，而是每个优先级一个类，互相持有下一层。装配发生在 core 的静态块里（[BaseMicroServiceSpace](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/router/BaseMicroServiceSpace.java)）：

```java
static {
    PrimaryTokenParser primaryTokenParser = new PrimaryTokenParser();

    RootExpressionParser rootExpressionParser =
        new RootExpressionParser(
            new LogicParser(
                new CmpParser(
                    new AddAndSubParser(
                        new MulAndDivAndModParser(primaryTokenParser)))));

    primaryTokenParser.setTopExpressionParser(rootExpressionParser);  // 括号回环

    PARSER = new RouterExpressionParser(rootExpressionParser);
}
```

三件事值得注意：

1. **单向链 + 一个回边**。普通优先级靠「上层调下层」；`PRIMARY ::= (EXPR)` 是唯一需要往回跳的地方，所以 `PrimaryTokenParser` 额外持有 `RootExpressionParser` 引用（`setTopExpressionParser`）。装配顺序必须是「先建链、后补回边」。
2. **每层的循环模式完全一样**：先解析左操作数，再看当前 token 是否属于本层运算符，是则 `pop()` 后解析右操作数，把左右包成节点，继续循环。这就是**左结合**的来源——`1-2-3` 变成 `(1-2)-3`。
3. **静态单例、无状态**。解析器只有 `lowerParser` 这类不可变字段，`TokenStream` 是每次调用新建的，所以 `PARSER` 可被多线程共享（`refreshRouter` 本身还是 `synchronized`）。

解析失败的三种方式：

| 情况                   | 行为                                                                |
| ---------------------- | ------------------------------------------------------------------- |
| 最左就解析不出来       | `parse` 返回 `null`（调用方判断后抛业务异常）                       |
| 运算符后面没有右操作数 | `throw new IllegalStateException("...missing right node after...")` |
| 括号没配对             | `throw new IllegalStateException("paren not match")`                |

## AST 与求值

所有节点实现同一个接口，求值就是**后序遍历 + 字符串返回**：

```java
public interface ASTNode {
    Token getToken();
    ASTNode[] getChildren();
    String calc(ExpressionCalcContext context);
}
```

`ExpressionCalcContext` 里只有一张 `Map<String, String> valMap`——没有类型系统，所有值都是字符串。这让实现极简，也带来了下文那些语义陷阱。

### 三个特殊常量

```java
public static final String TRUE  = "1";            // 逻辑/关系运算为真
public static final String FALSE = "0";            // 逻辑/关系运算为假
public static final String NULL  = "$__NULL__$";   // 变量不存在
```

**"真"就是字符串 `"1"`**。整条链路上没有 boolean，判断真值的地方一律写 `ExpressionConstant.TRUE.equals(res)` 或 `.equals(FALSE)`。

### 节点语义

| 节点                   | 对应语法       | `calc` 行为                                                                                                                                           |
| ---------------------- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PrimaryNode`          | `PRIMARY`      | `IDENTIFIER` → `valMap.getOrDefault(name, NULL)`；`INT_LITERAL` → 原文本；`STR_LITERAL` → **去掉首尾引号**                                            |
| `NumCalcNode`          | `+ - * / %`    | 一侧为 `NULL` 则返回 `NULL`；否则 `new BigDecimal(...)` 运算。`/` 取 `divideAndRemainder()[0]`（**截断取商**），`%` 取 `[1]`。结果为 0 时返回 `FALSE` |
| `CmpNode`              | `== > < >= <=` | 任一侧 `NULL` → `FALSE`；**任一侧 token 是 `STR_LITERAL`** → 走 `String.compareTo`，否则 `new BigDecimal(...)` 比较。真返 `TRUE` 假返 `FALSE`         |
| `LogicNode`            | `&& \|\|`      | 与 `FALSE` 比较决定短路；结果只有 `TRUE` / `FALSE`                                                                                                    |
| `RouterExpressionNode` | `->`           | 返回 `null`——它只是**分叉容器**，从不直接求值                                                                                                         |

`CmpNode` 那句"任一侧是字符串字面量就走字符串比较"是刻意的：`group=="gray"` 显然不能转 BigDecimal。反过来 `weight>="5"` 这种写法会退化成字符串比较（`"10" < "5"`），是一个容易踩的坑。

### 真值判定链路上的三个坑

这是整套引擎最需要注意的部分，它们都是**真实行为**而非笔误：

1. **`&&` 把不认识的变量当"真"**。`LogicNode` 的 `AND` 分支只在 `left.equals(FALSE)` 时短路；`NULL`（`"$__NULL__$"`）不等于 `"0"`，于是继续算右侧，右侧非 `"0"` 就返回 `TRUE`。所以 `notExist && notExist2` 的结果是**真**。
2. **顶层必须是精确的 `"1"`**。`ExprRouter.matchRequest` 用 `TRUE.equals(res)` 判定，因此裸的 `group`（值 `"gray"`）→ 假；`weight`（值 `"5"`）→ 假；只有 `weight==1`、`1`（字面量恰好为 `"1"`）这类结果才是真。
3. **数字运算结果 `0` 与 `FALSE` 同形**。`NumCalcNode` 把 0 归一成 `"0"`，语义上等价于 `FALSE`；好在后续若进 `CmpNode` 会再转 `BigDecimal("0")`，不影响比较。

## 路由中的用法：`->`

路由规则是**两段表达式**：左边筛请求，右边筛实例，中间用 `->` 连接。

```text
group=="gray" && weight>=5 -> group=="gray"
└──────── 请求侧 ────────┘    └──── 实例侧 ────┘
```

`RouterExpressionParser` 强制要求这个箭头，缺了就抛 `missing "->"`：

```java
@Override
public ASTNode parse(TokenStream tokenStream) {
    ASTNode left = rootExpressionParser.parse(tokenStream);
    if (left == null) { return null; }
    Token opToken = tokenStream.current();
    if (opToken == null || opToken.getType() != TokenType.ARROW) {
        throw new IllegalStateException("illegal [ROUTER_EXPR] expression, missing \"->\"");
    }
    tokenStream.pop();
    ASTNode right = rootExpressionParser.parse(tokenStream);
    if (right == null) {
        throw new IllegalStateException("illegal [ROUTER_EXPR] expression, missing right node after \"->\"");
    }
    return new RouterExpressionNode(opToken, new ASTNode[] {left, right});
}
```

拆包发生在 `BaseMicroServiceSpace#refreshRouter`，直接取 `children[0]` / `children[1]` 装进 `ExprRouter`：

```java
TokenStream tokenStream = TokenLexer.tokenize(expression);
ASTNode node = PARSER.parse(tokenStream);
if (node != null) {
    ASTNode[] children = node.getChildren();
    newRouters.add(new ExprRouter(i + 1, expression, children[0], children[1]));
} else {
    throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_ROUTER_REFRESH_ERROR,
        String.format("invalid expression: %s", expression));
}
```

`ExprRouter` 自己就是两次求值的薄壳：

```java
public boolean matchRequest(ExpressionCalcContext context) {
    return ExpressionConstant.TRUE.equals(leftExpr.calc(context));
}

public boolean matchInvoker(RpcInvoker rpcInvoker) {
    ExpressionCalcContext context = new ExpressionCalcContext();
    context.setValMap(rpcInvoker.getInvokerPropertyMap());
    return ExpressionConstant.TRUE.equals(rightExpr.calc(context));
}
```

### 两侧的变量从哪来

| 侧     | 上下文来源                                                    | 可用变量                                                     | 备注                                                                                                                               |
| ------ | ------------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------- |
| 请求侧 | `InvocationContext.get()`（ThreadLocal `Map<String,String>`） | 用户自己 `InvocationContext.put("userId", "123")` 塞进去的键 | 内置的 `timeout`/`compress`/`traceId` 用的是 key `"1"`/`"2"`/`"3"`，以数字开头，**无法写成标识符**，因此请求侧实际只有自定义键可用 |
| 实例侧 | `RpcInvoker#getInvokerPropertyMap()` → `MetaData#toMap()`     | 只有两个：`group`、`weight`                                  | `weight` 来自 `NodeProperty.WEIGHT_KEY`                                                                                            |

也就是说，**实例侧表达式只能围绕 `group` 和 `weight` 做文章**，`micro-service-id`、`stage`、`host` 都不在 map 里。想看更多维度必须扩 `MetaData#toMap`。

### 一次完整走查

取规则 `weight>=5 -> group=="gray"`，`InvocationContext` 为空，实例 `{group=gray, weight=10}`：

```text
词法   IDENTIFIER(weight) GE(>=) INT_LITERAL(5) ARROW(->) IDENTIFIER(group) EQ(==) STR_LITERAL("gray")

AST    RouterExpressionNode
       ├── CmpNode(GE)
       │   ├── PrimaryNode(IDENTIFIER weight)
       │   └── PrimaryNode(INT_LITERAL 5)
       └── CmpNode(EQ)
           ├── PrimaryNode(IDENTIFIER group)
           └── PrimaryNode(STR_LITERAL "gray")

请求侧 calc(∅)          weight → NULL  ⇒ CmpNode 见 NULL 返 "0"  ⇒ matchRequest=false
实例侧 calc({gray,10})  CmpNode(EQ) 走字符串比较 "gray".compareTo("gray")=0 ⇒ "1" ⇒ matchInvoker=true
```

结果：**该实例不会被选中**，因为请求侧返回了假。想让这条规则对所有请求生效，左边必须换成恒真式——`1`（字面量 `calc` 出 `"1"`）或 `1==1`；兜底路由 `FullRequestRouter` 就是靠 `matchRequest` 直接返 `true` 实现「所有请求都匹配」。

## 规则上线后的错误路径

`refreshRouter` 把「版本号推进」放在「解析规则」之前，这是一处必须知道的顺序：

```java
if (routerOpsGlobalId <= this.routerOpsGlobalId) { ... return; }   // 拒绝回退
this.routerOpsGlobalId = routerOpsGlobalId;                        // 先推进版本号

List<Router> newRouters = new ArrayList<>();
for (String expression : routers) { /* 解析，可能抛异常 */ }
newRouters.addAll(defaultRouters);
this.routers = newRouters;                                         // 最后才替换引用
```

于是：

| 场景                         | 现象                                                                                                                                                    |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 一条表达式非法               | 整批规则**全部不生效**（引用没替换），旧规则继续用；异常被 catch 后只打 error 日志                                                                      |
| 非法规则之后想重推同一份配置 | 若 opsId 相同 → 被 `<=` 拒绝；必须用**更大**的 opsId（dashboard 每次生成新时间戳，所以再点一次就够）                                                    |
| 启动时 ZK 里的规则非法       | 订阅阶段用 `INITIAL_ROUTER_OPS_ID=0` 调用，内存初值是 `-1`，所以版本号推进到 0 但 `routers` 仍是空列表 → **所有调用都路由失败**，直到有人推一条合法规则 |
| 求值期抛异常                 | 无兜底。`group > 3`（字符串转 BigDecimal）、除零、空指针都会把异常抛到调用链上                                                                          |

## 性能

- **解析只在刷新时发生**：每条规则一次 `tokenize` + 一次 `parse`，结果常驻内存。
- **求值在每次调用、每个实例上发生**：`lookUp` 对每条命中请求的规则遍历所有 invoker，每次 `matchInvoker` 都新建 `ExpressionCalcContext` 并做（可能多次）`BigDecimal` 构造。实例多、规则多时这是热点。
- 没有结果缓存、没有编译成字节码，也没有把 `calc` 结果放在 `Invocation` 上复用。

## 已知限制

| #   | 限制                                              | 影响                                                     |
| --- | ------------------------------------------------- | -------------------------------------------------------- |
| 1   | 标识符不能含数字                                  | `userId2` 会被切成 `IDENTIFIER(userId) INT_LITERAL(2)`   |
| 2   | 无小数点、无负数字面量                            | `1.5`、`-1` 无法表达                                     |
| 3   | 无 `!=`、无 `!`                                   | 只能用 `==` + 反向规则表达                               |
| 4   | 字符串不能含空格、不能转义                        | `"gray zone"` 直接崩                                     |
| 5   | `NULL` 在 `&&`/`\|\|` 中被当作真                  | 变量名写错不会"变严"，而是"变松"                         |
| 6   | 顶层真值必须是精确 `"1"`                          | 裸变量、裸算术都判假，需显式比较                         |
| 7   | 数字比较统一走 `BigDecimal`，但字符串一侧走字典序 | `weight>="5"` 与 `weight>=5` 语义不同                    |
| 8   | 求值期异常无捕获                                  | 非法比较/除零会让调用失败                                |
| 9   | 实例侧只有 `group`、`weight`                      | 想按 host/stage/id 路由需改 `MetaData#toMap`             |
| 10  | 无注释、无变量引用校验                            | 写错变量名只在运行期体现（且表现为"永远不匹配"或陷阱 5） |
| 11  | 非法规则会推进 opsId                              | 失败后必须用更大 opsId 重推                              |
| 12  | `RouterExpressionNode#calc` 返回 `null`           | 直接调 `calc` 会 NPE，必须先拆 children                  |

## 测试

`tomato-rpc-test` 里有两层覆盖：

| 测试                                                                                                                | 覆盖点                                                            |
| ------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| [TokenLexerTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/expression/token/TokenLexerTest.java)        | 状态机切分：多字符运算符、无空格表达式、末尾 token 补齐           |
| [ASTNodeParserTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/expression/ast/ASTNodeParserTest.java)    | 优先级、括号、`->` 拆分、求值结果                                 |
| [TestCode](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/expression/TestCode.java)                          | 手工试跑用的 main/样例代码                                        |
| [BaseMicroServiceTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/router/BaseMicroServiceTest.java) | `refreshRouter` + `lookUp` 的端到端行为（含版本号拒绝、兜底路由） |

它们都是纯内存测试，不需要 ZooKeeper，也不启 Netty——这正是把表达式拆成独立模块的收益。

## 关键类速查

| 类                       | 位置                                                                                                                     | 职责                                  |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------ | ------------------------------------- |
| `TokenLexer`             | [expression/token](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/token/TokenLexer.java)         | 手写状态机，字符 → token              |
| `TokenStream`            | [expression/token](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/token/TokenStream.java)        | token 游标（`current` / `pop`）       |
| `ExpressionParser`       | [expression/ast](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/ExpressionParser.java)       | 解析器接口 + 完整 EBNF 注释           |
| `RootExpressionParser`   | [expression/ast](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/RootExpressionParser.java)   | 门面                                  |
| `PrimaryTokenParser`     | [expression/ast](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/PrimaryTokenParser.java)     | 终结符 + 括号回环                     |
| `RouterExpressionParser` | [expression/ast](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/RouterExpressionParser.java) | 拆分 `->`                             |
| `ExpressionConstant`     | [expression/ast](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/ExpressionConstant.java)     | `TRUE`/`FALSE`/`NULL` 三个字面量      |
| `ExpressionCalcContext`  | [expression/ast](../tomato-rpc-expression/src/main/java/org/tomato/study/rpc/expression/ast/ExpressionCalcContext.java)  | 求值上下文（`Map<String,String>`）    |
| `ExprRouter`             | [core/router](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/router/ExprRouter.java)                         | 把左右表达式包装成 `Router`           |
| `FullRequestRouter`      | [core/router](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/router/FullRequestRouter.java)                  | 兜底路由（请求侧恒真，只筛 group）    |
| `BaseMicroServiceSpace`  | [core/router](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/router/BaseMicroServiceSpace.java)              | 装配解析器、编译规则、`lookUp` 时求值 |

相关章节：[服务治理](./08-governance.md)（规则怎么用）、[监控与运营](./11-dashboard.md)（规则怎么下发）。
