# 核心源码索引（按关注点）

## 启动配置
- `mt.common.config.MtWebAutoConfiguration` — 启动加载、扫描 basePackage、注册 PageHelper
- `mt.common.config.MybatisConfiguration` — PageHelper、CreatedBy/UpdatedBy 拦截器
- `mt.common.config.CommonEnvironmentPostProcessor` — 注入 `project.base-package`
- `mt.common.config.CommonPropertySource` — 自定义 PropertySource
- `mt.common.config.CommonProperties` — `mt.config.*` 绑定
- `mt.common.config.IdGeneratorConfiguration` — 启用 ID 生成
- `mt.common.config.DataLockConfiguration` — 启用分布式锁

## 持久层抽象（核心）
- `mt.common.service.BaseRepository` — CRUD 接口
- `mt.common.service.BaseRepositoryImpl` — 抽象实现（基于 tk.mybatis）
- `mt.common.service.QueryHandler` — `doPage()` 用
- `mt.common.service.BaseMapperHelper` — 从 BaseMapper 反射拿到泛型实体类
- `mt.common.service.BaseServiceBeanFactoryPostProcessor` — 扫描并增强 Service

## Mapper
- `mt.common.mybatis.mapper.BaseMapper` — tk.mybatis `Mapper<T>` 的扩展（findGroupCounts、findAdvancedList）
- `mt.common.mybatis.mapper.BaseUpdateMapper`
- `mt.common.mybatis.sqlProvider.BaseSelectProvider` — 动态 SQL Provider
- `mt.common.mybatis.sqlProvider.BaseUpdateProvider`

## 条件对象
- `mt.common.tkmapper.Filter`
- `mt.common.tkmapper.Operator`
- `mt.common.tkmapper.OrFilter`
- `mt.common.tkmapper.builder.FiltersBuilder`
- `mt.common.tkmapper.builder.OrGroupBuilder`
- `mt.common.tkmapper.ConditionFilterParser`
- `mt.common.tkmapper.DefaultConditionFilterParser`

## MyBatis 拦截器（自动填充审计字段、乐观锁、ID 生成）
- `mt.common.mybatis.BaseInterceptor`
- `mt.common.mybatis.CreatedByInterceptor`
- `mt.common.mybatis.CreatedDateInterceptor`
- `mt.common.mybatis.UpdatedByInterceptor`
- `mt.common.mybatis.UpdatedDateInterceptor`
- `mt.common.mybatis.VersionInterceptor`
- `mt.common.mybatis.IdGenerateInterceptor`
- `mt.common.mybatis.InterceptorHelper`

## MyBatis 工具
- `mt.common.mybatis.utils.MyBatisUtils` — Filter 列表 → Example
- `mt.common.mybatis.utils.SqlProviderUtils`
- `mt.common.mybatis.utils.MapperColumnUtils`
- `mt.common.mybatis.utils.CheckUtils`

## 高级查询
- `mt.common.mybatis.advanced.AdvancedQuery`
- `mt.common.mybatis.advanced.AdvancedResultSet`
- `mt.common.mybatis.advanced.ValueConverter`
- `mt.common.mybatis.advanced.DefaultValueConverter`
- `mt.common.mybatis.annotation.From`
- `mt.common.mybatis.entity.GroupCount`

## 事件 / 异常
- `mt.common.mybatis.event.AfterInitEvent`
- `mt.common.mybatis.event.BeforeInitEvent`
- `mt.common.mybatis.exception.MyBatisException`
- `mt.common.mybatis.exception.NotSupportException`

## 注解（mt.common.annotation）
- `Filter` — 字段映射过滤条件
- `CreatedByUserName / CreatedByUserId`
- `UpdatedByUserName / UpdatedByUserId`
- `CreatedDate / UpdatedDate`
- `Version` — 乐观锁
- `CurrentUser / CurrentUserId / CurrentUserName`
- `ForeignKey` — 级联管理
- `EnableDataLock / EnableIdGenerator`
- `IdGenerator / GenerateClass / GenerateOrder / BaseIdGenerator`
- `Datasource` — 多数据源

## 当前用户
- `mt.common.currentUser.UserContext` — 接口
- `mt.common.currentUser.CurrentUserHandlerInterceptor` — 拦截请求
- `mt.common.currentUser.CurrentUserMethodArgumentResolver` — `@CurrentUser` 解析
- `mt.common.currentUser.CurrentUserIdMethodArgumentResolver` — `@CurrentUserId`
- `mt.common.currentUser.CurrentUserNameMethodArgumentResolver` — `@CurrentUserName`
- `mt.common.currentUser.CurrentUserWebMvcConfiguration`

## 参数校验
- `mt.common.paramcheck.AssertAspectConfiguration` — AOP 拦截配置
- `mt.common.paramcheck.ParameterChecker` — 自定义校验器接口
- `mt.common.paramcheck.ParameterCheckers` — 内置 NotNullChecker
- `mt.common.paramcheck.ParameterCheckerWrapper`

## Message 字段转换
注解（`mt.common.starter.message.annotation`）：
- `Message / BatchMessage / BatchMultipleMessage`
- `MessageGroup / IgnoreMessage / EnableMessage`

Handler（`mt.common.starter.message.messagehandler`）：
- `MessageHandler`
- `DefaultMessageHandler`
- `BatchMessageHandler`
- `BatchMessageKey`
- `BatchMultipleMessageHandler`
- `MultipleFieldValue`
- `AbstractCacheMessageHandler`
- `AbstractCodeNameBatchMessageHandler`

AOP 与工具（`mt.common.starter.message`）：
- `MessageAspectAdapter`
- `DefaultMessageAspect`
- `MessageConfiguration`
- `utils.MessageUtils`
- `utils.MessageRecursiveParams`
- `utils.BatchHandleTarget`

异常：
- `mt.common.starter.message.exception.FieldNotFoundException`

## 上下文过滤
- `mt.common.context.FilterContext`
- `mt.common.context.FilterContextAspect`
- `mt.common.context.FilterContextHolder`
- `mt.common.context.FilterContextUtils`
- `mt.common.context.FieldInfo`
- `mt.common.context.annotation.UseFilterContext`
- `mt.common.context.annotation.UseFilterContextField`
- `mt.common.context.annotation.IgnoreFilterContext`

## 实体 / DTO / 分页
- `mt.common.entity.po.BaseEntity`
- `mt.common.entity.po.SystemEntity`
- `mt.common.entity.dto.BaseDTO`
- `mt.common.entity.BaseCondition`
- `mt.common.entity.BaseFields`
- `mt.common.entity.DataLock`
- `mt.common.entity.IdGenerate`
- `mt.common.entity.Pageable`
- `mt.common.entity.PageCondition`
- `mt.common.entity.PageDTO`
- `mt.common.entity.ResResult`

## Controller / Service 基类
- `mt.common.biz.AbstractEntityService`
- `mt.common.biz.AbstractSimpleEntityService`
- `mt.common.biz.BaseController`
- `mt.common.biz.BaseSimpleController`
- `mt.common.biz.EntityService`
- `mt.common.biz.EntityId`
- `mt.common.biz.EntityDeleteCheckEvent`
- `mt.common.biz.TransformUtils`

## 转换器
- `mt.common.converter.Converter`
- `mt.common.converter.DefaultConverter`
- `mt.common.converter.BooleanConverter`
- `mt.common.converter.DateConverter`
- `mt.common.converter.HttpDateConverter`

## 工具类（mt.common.utils）
- `BeanUtils` / `BeanUtilsException`
- `SpringUtils`
- `WebUtils`
- `HostChooseUtils`
- `EntityUtils`
- `PageUtils`
- `ClassTypeUtils`

## 业务接口
- `mt.common.service.BaseRepository` — 核心 CRUD
- `mt.common.service.IdGenerateService`
- `mt.common.service.DataLockService`
