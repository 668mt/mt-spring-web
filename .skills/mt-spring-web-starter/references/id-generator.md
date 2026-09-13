# ID 生成器

可选组件，配合 `@IdGenerator` 实现自定义 ID（自增 / 补零 / DIY）。

## 一、启动启用

```java
@EnableIdGenerator   // 启动类上
@SpringBootApplication
public class Application {}
```

`@EnableIdGenerator` 内部 `@Import(IdGeneratorConfiguration.class)`，且 `@EnableDataLock`（分布式锁）。

需要建两张表：
```sql
CREATE TABLE idGenerate (
  tableName VARCHAR(64) PRIMARY KEY,
  nextValue BIGINT
);

CREATE TABLE dataLock (
  id VARCHAR(64) PRIMARY KEY,
  useKey VARCHAR(128)
);
```

可在 `application.properties` 自定义表名：
```properties
mt.config.id-generate-table-name=my_id_table
mt.config.data-lock-table-name=my_lock_table
```

## 二、在实体上声明

```java
public class Notice extends BaseEntity {
    @Id
    @KeySql(useGeneratedKeys = true)
    @Column(updatable = false)
    @IdGenerator(generator = IdGenerateService.Generator.IDENTITY)
    private Long id;
}
```

`IdGenerateService.Generator` 枚举：

| 枚举 | 行为 |
| --- | --- |
| `IDENTITY` | 默认。从 `idGenerate` 表读 nextValue，自增。 |
| `FILLZERO` | 自增 + 补零（最大长度由 `@IdGenerator(maxLength=16)` 控制）。 |
| `DIY` | 自定义。通过 `@GenerateClass(value = MyIdGen.class)` 指定实现 `BaseIdGenerator` 的类。 |

不写 `@IdGenerator` 时仍走数据库自增（AUTO_INCREMENT）。

## 三、自定义生成器

```java
public class MySnowflakeIdGen implements BaseIdGenerator {
    @Override
    public Object generate(String tableName, IdGenerator idGenerator) {
        // 返回自定义 id
        return snowflake.nextId();
    }
}

public class Notice extends BaseEntity {
    @Id
    @IdGenerator(generator = Generator.DIY, maxLength = 16)
    @GenerateClass(MySnowflakeIdGen.class)
    @Column(updatable = false)
    private Long id;
}
```

## 四、相关类

- `mt.common.annotation.EnableIdGenerator`（启动注解）
- `mt.common.annotation.EnableDataLock`（分布式锁，可单独启用）
- `mt.common.annotation.IdGenerator`（字段注解）
- `mt.common.annotation.GenerateClass`（DIY 时指定生成器类）
- `mt.common.annotation.BaseIdGenerator`（自定义生成器接口）
- `mt.common.annotation.GenerateOrder`（生成顺序，调试用）
- `mt.common.service.IdGenerateService`
- `mt.common.service.DataLockService`
- `mt.common.config.IdGeneratorConfiguration`
- `mt.common.config.DataLockConfiguration`
- `mt.common.entity.IdGenerate`（idGenerate 表映射）
- `mt.common.entity.DataLock`（dataLock 表映射）
