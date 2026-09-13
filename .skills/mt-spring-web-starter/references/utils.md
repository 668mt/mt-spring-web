# 常用工具类

## 一、BeanUtils（`mt.common.utils.BeanUtils`）

```java
// 单个转换
UserVO vo = BeanUtils.transform(UserVO.class, userDo);

// 列表转换
List<UserVO> voList = BeanUtils.batchTransform(UserVO.class, doList);

// 按 key 聚合
Map<Long, UserVO> map = BeanUtils.mapByKey("id", voList);

// 按 key 分组到 List
Map<Long, List<UserVO>> grouped = BeanUtils.aggByKeyToList("deptId", voList);

// 取某个字段集合
Set<Long> ids = BeanUtils.toPropertySet("id", voList);

// 拷贝
BeanUtils.copyProperties(src, tgt, "ignoreField1", "ignoreField2");
BeanUtils.copyEntityProperties(src, tgt);   // 自动忽略 BaseEntity 字段
```

注意：null 字段不会拷贝到目标对象（默认过滤）。

## 二、SpringUtils（`mt.common.utils.SpringUtils`）

```java
T bean = SpringUtils.getBean(Class<T>);
T bean = SpringUtils.getBean("beanName", Class<T>);
T bean = SpringUtils.getOptionalBean(Class<T>);   // 没注册返回 null
Map<String, T> beans = SpringUtils.getBeansOfType(Class<T>);

String prop = SpringUtils.getProperty("key");
T prop = SpringUtils.getProperty("key", Class<T>);

String msg = SpringUtils.getMessage("i18n.code", arg1, arg2);   // 需要 localeResolver
```

## 三、WebUtils（`mt.common.utils.WebUtils`）

```java
HttpServletRequest  req = WebUtils.getRequest();
HttpServletResponse resp = WebUtils.getResponse();
String ip = WebUtils.getRemoteIpAddr();
boolean ajax = WebUtils.isAjaxRequest(req);

// Cookie
WebUtils.addCookie(req, resp, "name", "value", maxAge);
String v = WebUtils.getCookie(req, "name");
WebUtils.removeCookie(req, resp, "name");

// HTTP 客户端（同步）
String resp = WebUtils.post(url, paramMap);
String resp = WebUtils.get(url, paramMap);
String resp = WebUtils.post(url, xml);
```

## 四、HostChooseUtils（`mt.common.utils.HostChooseUtils`）

带权重 + 健康检查的 host 选择器：

```java
// url 写法：host(weight)
List<String> hosts = List.of("http://10.0.0.1:8080(50)", "http://10.0.0.2:8080(100)");

// 被动（按需检查）
String host = HostChooseUtils.getAvailableHostByWeight(hosts, "/health");

// 主动（周期检查）
HostChooseUtils.registerHostCheck("biz", () -> hosts, "/health", 30000L,
    healthHosts -> log.info("可用: {}", healthHosts));
```

## 五、FiltersBuilder / OrGroupBuilder（`mt.common.tkmapper.builder`）

```java
List<Filter> filters = Filter.builds()
    .add("status", Operator.eq, 1)
    .addIfNotNull(() -> keyword != null ? new Filter("title", Operator.like, "%" + keyword + "%") : null)
    .addIf(pageNum != null, "user_id", Operator.eq, userId)
    .addOrGroup()
        .addOr("type", Operator.eq, 1)
        .addOr("level", Operator.eq, 2)
        .endOrGroup()
    .build();
```

## 六、MessageUtils（`mt.common.starter.message.utils.MessageUtils`）

手动触发转换：

```java
@Autowired MessageUtils messageUtils;

NoticeVO vo = ...;
vo = (NoticeVO) messageUtils.message(vo);   // 走 @Message
vo = (NoticeVO) messageUtils.messageWithGroup(vo, new String[]{"list"});   // 带分组
```

变量替换：
```java
String sql = MessageUtils.replaceVariable("user_id = #userId and type = #type", dto);
// 自动把 #fieldName 替换成 dto 字段值（带 SQL 转义）
```

## 七、EntityUtils（`mt.common.utils.EntityUtils`）

```java
List<Filter> idFilters = EntityUtils.getIdFilters(Notice.class, notice);   // 取出 id 的 Filter 列表
```

## 八、MyBatisUtils / SqlProviderUtils（`mt.common.mybatis.utils`）

```java
Example example = MyBatisUtils.createExample(Notice.class, filters);
example.setForUpdate(true);
```

`MyBatisUtils` 内部把 `Filter` 列表翻译为 tk.mybatis 的 `Example.Criteria`，处理 OR / AND / in / between / 自定义 condition。

## 九、MapperColumnUtils（`mt.common.mybatis.utils`）

```java
String column = MapperColumnUtils.parseColumn("userId");   // user_id（按 mapper.style 配置）
String column = MapperColumnUtils.parseColumn("userId", Notice.class);   // 优先看 @Column(name)
```

## 十、ClassTypeUtils（`mt.common.utils.ClassTypeUtils`）

```java
Type t = ClassTypeUtils.findGenericInterface(MyClass.class, ParentInterface.class);
ParameterizedType pt = ClassTypeUtils.findGenericSuperclass(MyClass.class, ParentClass.class);
```

## 十一、其他

- `mt.common.utils.PageUtils`（pagehelper 包装，注释保留）
- `mt.common.utils.HostChooseUtils`（见上文）
- `mt.common.utils.ClassTypeUtils`
- `mt.common.utils.EntityUtils`
- `mt.common.utils.BeanUtilsException`
