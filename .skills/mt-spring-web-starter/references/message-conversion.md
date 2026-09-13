# VO 字段转换（Message / BatchMessage）

业务上经常需要"列表里的 userId 字段要返回 userName"。框架提供 **AOP 自动转换**：
在 VO 字段上加 `@Message` / `@BatchMessage` 即可，Controller 不用改。

**默认 autoMessage = true**，所有 Controller 返回值都会被过一遍 Message 转换。

## 一、@Message 单条转换

适用：当前对象上有 ID，需要调用某个方法查出 name 等信息。

```java
@Data
public class NoticeVO {
    private Long id;
    private String title;
    private Long userId;

    @Message(handlerClass = UserNameMessageHandler.class, params = "#userId")
    private String userName;
}

@Component
public class UserNameMessageHandler implements MessageHandler<NoticeVO, String> {
    @Autowired private UserService userService;

    @Override
    public String handle(NoticeVO vo, Object[] params, String mark) {
        Long userId = (Long) params[0];
        return userService.findNameById(userId);
    }
}
```

要点：
- `@Message` 必须有 `handlerClass`（或 `handlerBeanName`）。
- `params` 可以写 `"#userId"`，运行时会从当前对象读 userId 字段值；
  也可以写普通字符串做固定参数；空字符串 `""` 表示"取当前字段值"。
- `mark` 用于在 handler 里区分同一 handler 的多种场景。

### 简化版：DefaultMessageHandler
```java
@Message(params = "未知用户")
private String userName;   // 直接显示"未知用户"
```
`DefaultMessageHandler` 把 params 当成"原样赋值"。

### 注解
`mt.common.starter.message.annotation.Message`：

```text
Class<? extends MessageHandler> value()       默认 DefaultMessageHandler.class
Class<? extends MessageHandler> handlerClass() 默认同上
String                          handlerBeanName()
String[]                        params()
String                          mark()
String[]                        group()       // 分组
```

## 二、@BatchMessage 批量查询（性能更好）

适用：列表页里有 N 条 userId，要全部转成 userName。一次查出所有用户，再按 ID 映射回去。

```java
@Data
public class NoticeVO {
    @BatchMessage(column = "userId", handlerClass = UserBatchMessageHandler.class)
    private UserVO user;
}

@Component
public class UserBatchMessageHandler
        extends AbstractCodeNameBatchMessageHandler<Long, UserVO> {

    @Autowired private UserService userService;

    @Override
    protected List<UserVO> doQuery(Collection<Long> ids) {
        return userService.findByIds(ids);
    }
}
```

`AbstractCodeNameBatchMessageHandler<KEY, RESULT>` 是框架提供的基础抽象：
接收 Set<Long> ID，返回 Map<Long, RESULT>，运行时把 map 写回每个 vo 对应字段。

注解 `mt.common.starter.message.annotation.BatchMessage`：

```text
String column()                                       // 取值的源字段
Class<? extends BatchMessageHandler<?, ?>> handlerClass()
String[] params() default ""
```

## 三、@BatchMultipleMessage 多字段组合

@BatchMessage 只从一个字段取 key；如果要根据"两个字段组合"做批量查询，用 @BatchMultipleMessage：

```java
@BatchMultipleMessage(columns = {"userId", "orgId"}, handlerClass = RoleBatchHandler.class)
private Role role;

@Component
public class RoleBatchHandler implements BatchMultipleMessageHandler<Role> {
    @Override
    public Map<MultipleFieldValue, Role> handle(Collection<?> vos,
                                                Set<MultipleFieldValue> keys,
                                                String[] params) {
        return roleService.findByUserIdsAndOrgIds(keys);
    }
}
```

注解 `mt.common.starter.message.annotation.BatchMultipleMessage`：

```text
String[] columns()
Class<? extends BatchMultipleMessageHandler<?>> handlerClass()
String[] params() default ""
```

## 四、启用 / 关闭

```properties
mt.config.messager.auto-message=false
```

- `auto-message=true`（默认）：所有 Controller 方法都自动转换。
- `auto-message=false`：需要在类上加 `@EnableMessage` 才会转换。

关闭某个方法：

```java
@IgnoreMessage
public ResResult<NoticeVO> getRaw(...) { ... }
```

按 group 过滤：

```java
@MessageGroup({"list","detail"})
@GetMapping("/list")
public ResResult<PageInfo<NoticeVO>> list(...) { ... }

// VO 字段
@Message(handlerClass = XxxHandler.class, group = {"list"})
private String xxx;     // 只在 list 接口里转换
```

## 五、注解与方法汇总

位于 `mt.common.starter.message.annotation`：

| 注解 | 作用 |
| --- | --- |
| `@Message` | 单条转换，handler 接收单个对象，返回字段值 |
| `@BatchMessage` | 批量转换，从 `column` 字段取 key，一次查全部 |
| `@BatchMultipleMessage` | 多字段组合 key 批量转换 |
| `@MessageGroup` | 类/方法级，限定只转换 group 命中的字段 |
| `@IgnoreMessage` | 关闭自动转换 |
| `@EnableMessage` | autoMessage=false 时手动开启 |

Handler 接口位于 `mt.common.starter.message.messagehandler`：

| 类 / 接口 | 用途 |
| --- | --- |
| `MessageHandler<E, R>` | 单条转换，`R handle(E entity, Object[] params, String mark)` |
| `DefaultMessageHandler` | 默认实现，params[0] 即返回值 |
| `BatchMessageHandler<F, R>` | 批量转换，`Map<F, R> handle(Collection, Set<F>, String[])` |
| `BatchMultipleMessageHandler<R>` | 多字段批量 |
| `AbstractCodeNameBatchMessageHandler<K, R>` | 批量基础抽象，复写 `doQuery(Collection<K>)` 即可 |
| `MultipleFieldValue` | 多字段组合 key |
| `MessageHandler.init()` | 生命周期方法，可在 handler 启动时初始化缓存 |

## 六、处理过程

入口：`mt.common.starter.message.MessageAspectAdapter.doAround(...)`，
由 `DefaultMessageAspect`（`@Aspect` 拦截所有 `@GetMapping/@PostMapping/...`）调用，
最终委托 `MessageUtils.messageWithGroup(...)`。

递归处理 Collection / Object[] / Map / 嵌套对象 / @Message / @BatchMessage / @BatchMultipleMessage。
**批量字段先收集、再一次性查询、最后写回**。
