# 分页查询 / Filter / @Filter 注解

## 一、最推荐的写法

```java
@Data
@EqualsAndHashCode(callSuper = true)
public static class NoticeCondition extends PageCondition {
    @Filter
    private Long userId;

    @Filter(column = "id", operator = Filter.Operator.in)
    private List<Long> ids;

    @Filter(operator = Filter.Operator.like)
    private String title;
}

@RestController
@RequestMapping("/notice")
public class NoticeController {
    @Autowired NoticeService noticeService;

    @GetMapping("/list")
    public PageInfo<Notice> list(NoticeCondition condition) {
        return noticeService.findPage(condition);
    }
}
```

`PageCondition`（`mt.common.entity.PageCondition`）带：
- `pageNum`、`pageSize`、`orderType`、`orderBy`
- 默认按 `id desc` 排序
- `isAllowSelectAll()` 默认 false（pageSize 上限 5000）

## 二、@Filter 注解

来自 `mt.common.annotation.Filter`：

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `column` | "" 为空时取字段名（驼峰转下划线或 `@Column.name`） | 数据库列名 |
| `operator` | `eq` | Filter.Operator |
| `prefix / suffix` | "" | 在值前后拼接（常用于 like：`%xxx%`） |
| `converter` | `DefaultConverter` | 值转换器（见下文） |
| `sql` | "" | 当 operator=condition 时使用，支持 `#fieldName` 占位符 |
| `parserClass` | `DefaultConditionFilterParser` | 自定义过滤器解析器 |
| `parserParams` | {} | 自定义解析器参数 |

## 三、自定义排序 + orderType 映射

```java
@Data
public class NoticeCondition extends PageCondition {
    @Filter private Long userId;

    @Override
    public Map<String, String> getOrderTypeMapping() {
        return Map.of("hot", "view_count desc", "new", "created_date desc");
    }
}
```

请求：`?orderType=hot` 即按 view_count desc。

## 四、自定义 Converter（值转换）

```java
public class BooleanConverter implements Converter<Object> {
    @Override public Object convert(Object value) {
        return Boolean.valueOf(value.toString());
    }
}

public class NoticeCondition extends PageCondition {
    @Filter(converter = BooleanConverter.class)
    private String isRead;       // "true"/"1" 字符串 → Boolean
}
```

框架提供：`DefaultConverter / BooleanConverter / DateConverter / HttpDateConverter`。

## 五、自定义 ConditionFilterParser

需要把单个字段展开成多个 Filter 时使用：

```java
public class DateRangeParser implements ConditionFilterParser<String> {
    @Override
    public List<Filter> parseFilters(Object condition, String fieldValue, String[] params) {
        return List.of(
            new Filter(params[0], Operator.ge, fieldValue),
            new Filter(params[1], Operator.le, fieldValue)
        );
    }
}

@Filter(parserClass = DateRangeParser.class, parserParams = {"startDate", "endDate"})
private String dateRange;
```

## 六、高级查询 AdvancedQuery（任意 SQL + 任意返回类）

需要一个不在主表上的字段？用 AdvancedQuery：

```java
@From("user u LEFT JOIN dept d ON u.dept_id = d.id")
@Data
public class UserDeptVO {
    private Long userId;
    private String userName;
    @Column(name = "dept_name")
    private String deptName;
}
```

```java
List<UserDeptVO> list = userService.findAdvancedList(UserDeptVO.class, filters);
PageInfo<UserDeptVO> page = userService.findAdvancedPage(UserDeptVO.class, condition);
```

要点：
- `@From` 必须填完整的 FROM 子句（含 join）。
- VO 字段上 `@Column(name=...)` 指明数据库列；不写则按字段名驼峰转换。
- 仍然复用 `List<Filter>` 作为 WHERE 条件。

## 七、分页工具

- `mt.common.entity.Pageable` 接口：`getPageNum/getPageSize/getOrderBy/isAllowSelectAll`。
- `mt.common.entity.PageDTO`：通用 Pageable 实现。
- `BaseRepository.doPage(QueryHandler, Pageable)`：把任意 `List<T2> doQuery()` 包成 PageInfo。

```java
PageInfo<NoticeVO> page = noticeService.doPage(
    () -> noticeService.findList(condition).stream().map(this::toVO).collect(toList()),
    condition
);
```
