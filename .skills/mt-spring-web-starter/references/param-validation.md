# 参数校验（AssertAspectConfiguration）

## 一、Service 入参自动校验

在 Service 方法上加 `@NotNull` 等注解，调用前会自动校验：

```java
public long getUnReadCount(@NotNull Long userId) {
    return count(List.of(new Filter("userId", Operator.eq, userId)));
}
```

`userId == null` 时抛 `IllegalArgumentException`，不需要在每个方法里手写 `Assert.notNull`。

## 二、启用方式

在 `application.properties` 配置要拦截的包：

```properties
project.assert-package-name=com.xxx.biz.service
```

框架使用 AOP 拦截这个包下所有方法，对每个参数做注解校验。
**不配置则不会启用。**

## 三、内置校验器

`mt.common.paramcheck.ParameterCheckers.NotNullChecker`
（对应 `org.jetbrains.annotations.NotNull`）。

`@NotBlank/@Min/@Max` 等 Spring Validation 注解需要单独配置 `@Validated`（在 Controller / Service 类上）。

## 四、自定义校验器

```java
public class PhoneChecker implements ParameterChecker<Phone, String> {
    @Override public boolean isValid(String value) {
        return value != null && value.matches("1\\d{10}");
    }

    @Override
    public String errorMsg(Phone annotation, Method method, Parameter parameter,
                           String value, Object target) {
        return "手机号格式错误";
    }
}

// 启动时注册一次（在某 @Configuration 里）
AssertAspectConfiguration.register(new PhoneChecker());

// 业务方法入参使用
public NoticeVO create(@Phone String phone) { ... }
```

## 五、关键类

- `mt.common.paramcheck.AssertAspectConfiguration`
- `mt.common.paramcheck.ParameterChecker<AnnotationType, ParameterType>`
- `mt.common.paramcheck.ParameterCheckerWrapper`
- `mt.common.paramcheck.ParameterCheckers`（内置 NotNullChecker）

## 六、对 `@RequestBody` 的校验（推荐）

控制层入参推荐使用 Spring 标准方式，与本套件互补：

```java
@PostMapping
public ResResult<NoticeVO> addOrUpdate(@RequestBody @Validated NoticeDTO dto) {
    return ResResult.success(noticeService.addOrUpdate(dto));
}

@Data
public class NoticeDTO {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull
    @Min(1)
    private Integer noticeType;
}
```
