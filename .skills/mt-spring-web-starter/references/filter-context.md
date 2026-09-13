# 上下文过滤 FilterContext

业务上经常会希望"自动注入当前用户相关过滤条件"（如只查自己部门的数据）。
`FilterContext` + `@UseFilterContext` 就是为这个场景设计。

## 一、实现 FilterContext

```java
@Component
public class DeptFilterContext implements FilterContext {
    private static final ThreadLocal<Long> CURRENT_DEPT_ID = new ThreadLocal<>();

    @Override public String name() { return "dept"; }

    @Override
    public void prepareContext(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long deptId = token == null ? null : userService.findDeptIdByToken(token);
        CURRENT_DEPT_ID.set(deptId);
    }

    @Override
    public void addContextFilter(Class<?> entityClass, List<Filter> filters,
                                 List<FieldInfo> fieldInfos) {
        Long deptId = CURRENT_DEPT_ID.get();
        if (deptId != null) {
            filters.add(new Filter("dept_id", Operator.eq, deptId));
        }
    }

    @Override public void clearContext() { CURRENT_DEPT_ID.remove(); }
}
```

接口位于 `mt.common.context.FilterContext`：

```text
String name()
void prepareContext(HttpServletRequest request)
void addContextFilter(Class<?> entityClass, List<Filter> filters, List<FieldInfo> fieldInfos)
void clearContext()
```

## 二、在 Entity 上声明

```java
public class Notice extends BaseEntity {
    @UseFilterContextField(contextName = "dept")
    private Long deptId;
}
```

`@UseFilterContextField` 标注在哪个字段，框架就会把 context 算出的过滤条件**对应到这个字段**（一般同名）。

## 三、在 Controller 启用

```java
@UseFilterContext(contextNames = "dept")
@RestController
@RequestMapping("/notice")
public class NoticeController { ... }
```

请求到来时 AOP（`FilterContextAspect`）会：
1. 调 `prepareContext(request)` 把上下文塞进 ThreadLocal。
2. 执行业务方法。
3. finally 调 `clearContext()` 清理。

期间所有 `BaseRepositoryImpl.findPage / findList / count` 都会自动把 dept 过滤加上。

## 四、关闭单个方法

```java
@IgnoreFilterContext
@GetMapping("/all")
public List<Notice> all() { ... }
```

## 五、注解汇总

位于 `mt.common.context.annotation`：

| 注解 | 作用 |
| --- | --- |
| `@UseFilterContext(contextNames = {"a","b"})` | 类级，启用一个或多个 FilterContext |
| `@UseFilterContextField(contextName = "x")` | 字段级，标记参与哪个 context 过滤 |
| `@IgnoreFilterContext` | 方法级，关闭 context 过滤 |

## 六、AOP 拦截范围

`FilterContextAspect` 默认拦截所有：
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@PatchMapping`

即只要 Controller 方法上用了这些注解、且类上有 `@UseFilterContext`，context 就会生效。

## 七、相关类

- `mt.common.context.FilterContext`（接口）
- `mt.common.context.FilterContextAspect`（AOP）
- `mt.common.context.FilterContextHolder`（ThreadLocal 持有当前请求的 contexts）
- `mt.common.context.FilterContextUtils`（在 MyBatisUtils.createExample 中被调用，自动追加 filters）
- `mt.common.context.FieldInfo`（context 拿到的字段信息）
