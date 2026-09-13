# 当前用户

## 一、实现 UserContext

继承 `mt.common.currentUser.UserContext<ENTITY, ID>`，把"从 token / session 中取用户"的逻辑写进去：

```java
@Component
public class MyUserContext implements UserContext<MyUser, Long> {

    @Override
    public MyUser getCurrentUser() {
        HttpServletRequest req = WebUtils.getRequest();
        String token = req.getHeader("Authorization");
        return token == null ? null : userService.findByToken(token);
    }

    @Override
    public Long getCurrentUserId() {
        MyUser u = getCurrentUser();
        return u == null ? null : u.getId();
    }

    @Override
    public String getCurrentUserName() {
        MyUser u = getCurrentUser();
        return u == null ? null : u.getName();
    }
}
```

接口位于 `mt.common.currentUser.UserContext`：

```text
ENTITY getCurrentUser()
ID     getCurrentUserId()
String getCurrentUserName()
```

## 二、Controller 注入注解

位于 `mt.common.annotation`：

| 注解 | 作用 |
| --- | --- |
| `@CurrentUser` | 注入完整用户对象 |
| `@CurrentUserId` | 注入当前用户 id |
| `@CurrentUserName` | 注入当前用户名称 |

```java
@PostMapping("/save")
public ResResult<Void> save(@RequestBody NoticeDTO dto,
                            @CurrentUser MyUser user,
                            @CurrentUserId Long userId,
                            @CurrentUserName String userName) {
    ...
}
```

> 必须先实现 `UserContext` Bean，否则这些参数都为 null。

## 三、自动写入审计字段

只要 `UserContext` 实现存在，Mybatis 拦截器会自动把：
- `@CreatedByUserName` / `@CreatedByUserId` 在 insert 时赋值
- `@UpdatedByUserName` / `@UpdatedByUserId` 在 update 时赋值

详见 `references/entity-base.md`。
