# 实体基类与审计注解

## 一、BaseEntity

继承 `mt.common.entity.po.BaseEntity` 自动获得 id + 4 个审计字段：

```java
@Data
@Table(name = "notice")
@EqualsAndHashCode(callSuper = true)
public class Notice extends BaseEntity {
    @Column(nullable = false) private String title;
    ...
}

public class BaseEntity implements EntityId<Long> {
    @Id
    @KeySql(useGeneratedKeys = true)
    @Column(updatable = false)
    private Long id;

    @CreatedByUserName    private String createdBy;
    @UpdatedByUserName    private String updatedBy;
    @CreatedDate          private Date   createdDate;
    @UpdatedDate          private Date   updatedDate;
}
```

> 不需要 `BaseEntity` 也可以，但建议继承以获得自动审计。
> `@Table/@Column` 用 `javax.persistence` 的注解，与 tk.mybatis 兼容。

## 二、审计注解（位于 mt.common.annotation）

| 字段 | 注解 | 拦截器 | 自动填充时机 |
| --- | --- | --- | --- |
| String createdBy | `@CreatedByUserName` | CreatedByInterceptor | insert 时（需 UserContext） |
| Long   createdBy | `@CreatedByUserId`   | CreatedByInterceptor | insert 时（需 UserContext） |
| String updatedBy | `@UpdatedByUserName` | UpdatedByInterceptor | update 时（需 UserContext） |
| Long   updatedBy | `@UpdatedByUserId`   | UpdatedByInterceptor | update 时（需 UserContext） |
| Date   createdDate | `@CreatedDate` | CreatedDateInterceptor | insert 时 |
| Date   updatedDate | `@UpdatedDate` | UpdatedDateInterceptor | insert + update 时 |

`@CreatedByUserName/@CreatedByUserId/@UpdatedByUserName/@UpdatedByUserId` 需要 `UserContext` Bean 才能填值；时间字段不需要。

## 三、乐观锁版本号

```java
@Version
private Long version;
```

由 `VersionInterceptor` 处理（基于 tk.mybatis 的 `@Version`），自动 +1 / 检查冲突。

## 四、ID 生成器

```java
@Id
@KeySql(useGeneratedKeys = true)
@Column(updatable = false)
@IdGenerator(generator = IdGenerateService.Generator.IDENTITY)
private Long id;
```

详见 `references/id-generator.md`。

## 五、EntityId 标识接口

`mt.common.biz.EntityId<T>`：

```text
T getId();
void setId(T id);
```

`BaseEntity`、`BaseDTO` 都已实现。框架很多地方（`AbstractEntityService.addOrUpdate`、`BaseRepository.findById`）都依赖此接口。

## 六、其他可复用基类

### `BaseFields`（`mt.common.entity.BaseFields`）
不带 id，但带 createdDate / version / lastModifiedDate，适合用作"嵌入字段"。

### `BaseDTO`（`mt.common.entity.dto.BaseDTO`）
只带 `Long id`，用于 DTO 基类。

### `DataLock`（`mt.common.entity.DataLock`）
分布式锁表（`@EnableDataLock` 启用）。

### `IdGenerate`（`mt.common.entity.IdGenerate`）
ID 生成表（`@EnableIdGenerator` 启用）。

## 七、表名/列名映射

`mt.common.mybatis.utils.MapperColumnUtils.parseColumn(name)` 支持 5 种风格，通过属性配置：

```properties
mapper.style=camelhumpAndLowercase    # 驼峰转下划线并小写（默认）
# 可选：camelhump / camelhumpAndLowercase / camelhumpAndUppercase / lowercase / uppercase / normal
```

类级别优先使用 `@Table(name)` / 字段级 `@Column(name)`，未标注则按此规则转换。
