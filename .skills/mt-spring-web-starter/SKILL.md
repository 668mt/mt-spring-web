---
name: mt-spring-web-starter
description: mt-spring-web-starter 是一个基于 Spring Boot + tk.mybatis + pagehelper 的快速开发启动器。提供零 Mapper.xml 的单表 CRUD、分页查询、当前用户注入、参数校验、VO 字段转换（Message/BatchMessage）、上下文过滤等开箱即用功能。当用户在基于 mt-spring-web-starter 的项目里写 Entity、Service、Controller、Mapper，或想避免手写 XML 时使用本 skill。
---

# mt-spring-web-starter 使用指南

mt-spring-web-starter 基于 **tk.mybatis + pagehelper**，封装了"零 Mapper.xml"的持久层。
**目标：普通单表 CRUD 只需要写实体类 + Service/Repository，不需要任何 Mapper.java 和 Mapper.xml。**

## 一、5 分钟入门（最小完整示例）

### 1. 启动类
```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;   // 必须是 tk.mybatis，不要用 mybatis-spring

@MapperScan("com.xxx.biz.dao")
@SpringBootApplication(scanBasePackages = "com.xxx.biz")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. 实体（继承 BaseEntity 自动获得 id + 审计字段）
```java
@Data
@Table(name = "notice")
@EqualsAndHashCode(callSuper = true)
public class Notice extends BaseEntity {
    @Column(nullable = false) private String title;
    @Column(nullable = false) private Integer noticeType;
    private Long userId;
}
```

### 3. Service（继承 BaseRepositoryImpl，自动获得全部 CRUD）
```java
@Service
public class NoticeService extends BaseRepositoryImpl<Notice> { }
```

### 4. Controller（分页查询）
```java
@Data
@EqualsAndHashCode(callSuper = true)
public static class NoticeCondition extends PageCondition {
    @Filter private Long userId;
    @Filter(column = "id", operator = Filter.Operator.in) private List<Long> ids;
    @Filter(operator = Filter.Operator.like) private String title;
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

> 上面 4 段代码就构成一个完整接口，**没有写一行 Mapper.xml**。

---

## 二、依赖与配置

```xml
<dependency>
    <groupId>io.github.668mt</groupId>
    <artifactId>mt-spring-web-starter</artifactId>
    <version>${mt-spring-web.version}</version>
</dependency>
```

```properties
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://host:3306/demo?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8&useSSL=false
spring.datasource.username=root
spring.datasource.password=******
```

可选配置（按需）：
```properties
project.assert-package-name=com.xxx.biz.service        # 启用参数校验 AOP
mt.config.messager.auto-message=true                   # 默认 true，自动 VO 字段转换
```

---

## 三、能力地图（按需查阅 references）

| 能力 | 参考文档 |
| --- | --- |
| BaseRepository 全部 CRUD 方法清单、Filter/Operator 体系 | [references/repository-crud.md](references/repository-crud.md) |
| 分页查询、`@Filter` 注解、`PageCondition`、Converter、Parser、AdvancedQuery | [references/filter-pagination.md](references/filter-pagination.md) |
| 当前用户：`UserContext` + `@CurrentUser/@CurrentUserId/@CurrentUserName` | [references/current-user.md](references/current-user.md) |
| 参数校验：`AssertAspectConfiguration` + `@NotNull` + 自定义 `ParameterChecker` | [references/param-validation.md](references/param-validation.md) |
| VO 字段转换：`@Message` / `@BatchMessage` / `@BatchMultipleMessage` | [references/message-conversion.md](references/message-conversion.md) |
| 上下文过滤：`FilterContext` + `@UseFilterContext` + `@UseFilterContextField` | [references/filter-context.md](references/filter-context.md) |
| 实体基类 + 审计注解 + 乐观锁版本号 | [references/entity-base.md](references/entity-base.md) |
| Controller / Service 基类、四元组模式、`ResResult` | [references/controller-base.md](references/controller-base.md) |
| ID 生成器：`@EnableIdGenerator` + `@IdGenerator` | [references/id-generator.md](references/id-generator.md) |
| 常用工具类：`BeanUtils / SpringUtils / WebUtils / FiltersBuilder / MessageUtils …` | [references/utils.md](references/utils.md) |
| 核心源码索引（按关注点列出所有关键类路径） | [references/source-index.md](references/source-index.md) |

---

## 四、遇到问题先看这里

- **`@Table` 必须用 `javax.persistence.Table`**（与 tk.mybatis 兼容），不要用 `com.baomidou` 那一套。
- **MapperScan 必须 `tk.mybatis.spring.annotation.MapperScan`**，否则会报错。
- **审计字段自动赋值**需要实现 `UserContext` Bean，否则 createdBy/updatedBy 字段一直是 null（时间字段不需要）。
- **pageSize 上限 5000**，超过会被截断。需要全部请重写 `PageCondition.isAllowSelectAll()` 返回 true。
- **`@Message` 默认是开**（`mt.config.messager.auto-message=true`），所有 Controller 返回值都会被过一遍转换。要在某个方法关闭用 `@IgnoreMessage`。
