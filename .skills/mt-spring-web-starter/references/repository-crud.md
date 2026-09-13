# BaseRepository 零 XML 单表 CRUD

`mt.common.service.BaseRepository<T>` 是框架的核心接口，由抽象实现 `BaseRepositoryImpl<T>` 完成。
**继承 BaseRepositoryImpl 后，CRUD 全部可用，不需要任何 Mapper.java 和 Mapper.xml。**

## 一、写一个 Service

```java
import mt.common.service.BaseRepositoryImpl;
import org.springframework.stereotype.Service;

@Service
public class NoticeService extends BaseRepositoryImpl<Notice> { }
```

`BaseRepositoryImpl` 内部会从 Spring 容器中找到与 `Notice` 类型匹配的
`mt.common.mybatis.mapper.BaseMapper<T>`（这是 tk.mybatis `Mapper<T>` 的扩展，已被框架自动注册）。
只要实体上有 `@Table(name="notice")`，即可直接 select/insert/update/delete。

## 二、BaseRepository 方法清单

### 查询

```text
List<T>   findAll()
List<T>   findList(Object condition)                      // condition 里的 @Filter 字段会被解析
List<T>   findList(String column, Object value)
List<T>   findByFilters(List<Filter> filters)
List<T>   findByFilters(List<Filter> filters, boolean forUpdate)
List<T>   findByFilter(Filter filter)
List<T>   findAdvancedList(Class<Result> resultClass, List<Filter> filters)
T         findById(Object id)
T         findById(Object id, boolean forUpdate)
T         findOne(String column, Object value)
T         findOneByFilters(List<Filter> filters)
T         findOneByFilter(Filter filter)
T         findFirstByFilter(Filter filter)
T         findFirstByFilters(List<Filter> filters)
boolean   exists(String column, Object value)
boolean   existsByFilter(Filter filter)
boolean   existsByFilters(List<Filter> filters)
boolean   existsId(Object id)
boolean   notExists(...)
int       count(List<Filter> filters)
```

### 分页（基于 pagehelper）

```text
PageInfo<T> findPage(Pageable pageable)
PageInfo<T> findPage(Integer pageNum, Integer pageSize, String orderBy, Object condition)
PageInfo<T> findPage(Integer pageNum, Integer pageSize, String orderBy, Object condition, boolean allowSelectAll)
<T2> PageInfo<T2> doPage(QueryHandler<T2> queryHandler, Pageable pageable)
PageInfo<Result> findAdvancedPage(Class<Result> resultClass, Pageable pageable)
```

`PageInfo<T>` 来自 `com.github.pagehelper.PageInfo`。

### 写入

```text
int save(T record)                                  // 全字段插入
int saveSelective(T record)                         // null 字段不写入，使用数据库默认值
int updateById(T record)                            // 全字段更新
int updateByIdSelective(T record)                   // null 字段不更新
int updateByFilter(T record, Filter filter)
int updateByFilterSelective(T record, Filter filter)
int updateByFilters(T record, List<Filter> filters)
int updateByFiltersSelective(T record, List<Filter> filters)
int add(String column, int value, List<Filter> filters)   // update set column = column + value
```

### 删除

```text
int deleteById(Object id)
int deleteByFilter(Filter filter)
int deleteByFilters(List<Filter> filters)
int delete(String column, Object value)
```

### 批处理 / 聚合

```text
void batchConsume(List<Filter> filters, int batchSize, String orderBy, Consumer<List<T>> consumer)
List<GroupCount> findGroupCounts(String groupField, List<Filter> filters)
ResultType findMax / findMin / findAvg(String fieldName, List<Filter> filters)
```

### 对象/条件互转

```text
List<Filter> parseCondition(Object condition)       // 把带 @Filter 的 DTO 转成 filters
```

## 三、条件构造 Filter

`mt.common.tkmapper.Filter` 是统一的查询条件对象，对应 SQL 片段。

### 方式 1：直接 new

```java
List<Filter> filters = new ArrayList<>();
filters.add(new Filter("userId", Filter.Operator.eq, 1001));
filters.add(new Filter("isRead", Filter.Operator.eq, false));
filters.add(new Filter("noticeType", Filter.Operator.in, Arrays.asList(1, 2)));
filters.add(new Filter("title", Filter.Operator.like, "%紧急%"));
filters.add(new Filter("createdDate", Filter.Operator.between, start, end));

long n = noticeService.count(filters);
List<Notice> list = noticeService.findByFilters(filters);
```

### 方式 2：FiltersBuilder 链式构造（支持 OR 分组）

```java
import static mt.common.tkmapper.Filter.builds;

List<Filter> filters = builds()
    .add("userId", Operator.eq, 1001)
    .addIf(keyword != null, "title", Operator.like, "%" + keyword + "%")
    .addOrGroup()
        .addOr("noticeType", Operator.eq, 1)
        .addOr("noticeLevel", Operator.eq, 2)
        .endOrGroup()
    .build();
```

工具类：
- `mt.common.tkmapper.builder.FiltersBuilder`
- `mt.common.tkmapper.builder.OrGroupBuilder`

### 方式 3：在 DTO 上用 @Filter（分页时最方便）

见 `references/filter-pagination.md`。

## 四、Operator 枚举

`mt.common.tkmapper.Operator`：

| Operator | SQL |
| --- | --- |
| `eq` | `=` |
| `eqn` | `=`，值 null 时退化为 `is null` |
| `ne` | `!=` |
| `gt / ge / lt / le` | `> >= < <=` |
| `like / notLike` | `like / not like` |
| `in / notIn` | `in / not in`（值传 Collection 或 Object[]） |
| `between / notBetween` | `between a and b` |
| `isNull / isNotNull` | `is null / is not null` |
| `condition` | 自定义 SQL 片段（property 即 SQL 片段；value 非空时使用 `${property} #{value}`） |
