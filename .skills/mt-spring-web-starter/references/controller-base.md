# Controller / Service 基类

## 一、四元组模式

最标准的写法：

```java
// Service
public class NoticeService extends AbstractEntityService<Notice, NoticeVO, NoticeDTO, NoticeCondition> {
    public NoticeService(ApplicationEventPublisher publisher) {
        super(publisher);
    }
    @Override public BaseRepository<Notice> getBaseService() {
        return baseService;
    }
    @Autowired private BaseRepositoryImpl<Notice> baseService;
}

// Controller
@RestController
@RequestMapping("/notice")
public class NoticeController
        extends BaseController<Notice, NoticeVO, NoticeDTO, NoticeCondition> {

    public NoticeController(NoticeService service) {
        super(service);
    }
}
```

`AbstractEntityService<EntityDO, EntityVO, EntityDTO, EntityCondition>` 会自动：
- `addOrUpdate`：DTO → DO 转换；id 为空则 `save`，否则 `updateByIdSelective`。
- `findPage`：DO → VO 转换，分页参数透传。
- `deletes`：按 id 查出 → 发布 `EntityDeleteCheckEvent`（外部可监听做级联校验）→ 物理删除。
- `findById`：DO → VO 转换。

`BaseController` 自动暴露 5 个 REST 接口：

| Method | Path | 说明 |
| --- | --- | --- |
| `GET` | `/` | 列表分页 → `PageInfo<EntityVO>` |
| `POST` | `/` | 新增/修改（按 id 判断） |
| `GET` | `/{id}` | 详情 |
| `DELETE` | `/{id}` | 删除 |
| `DELETE` | `/` | 批量删除（body 传 ids） |

## 二、简化版（DO == VO）

如果 DO 和 VO 是同一个类，用简化基类省去一个泛型：

```java
public class NoticeService
        extends AbstractSimpleEntityService<Notice, NoticeDTO, NoticeCondition> {
    public NoticeService(ApplicationEventPublisher publisher) { super(publisher); }
    @Override public BaseRepository<Notice> getBaseService() { return baseService; }
    @Autowired private BaseRepositoryImpl<Notice> baseService;
}

@RestController
@RequestMapping("/notice")
public class NoticeController
        extends BaseSimpleController<Notice, NoticeDTO, NoticeCondition> {
    public NoticeController(NoticeService service) { super(service); }
}
```

## 三、EntityService / EntityId

```java
public interface EntityService<EntityDO, EntityVO, EntityDTO, EntityCondition> {
    @Transactional(rollbackFor = Exception.class)
    EntityVO addOrUpdate(EntityDTO dto);

    PageInfo<EntityVO> findPage(EntityCondition condition);

    EntityVO findById(Long id);

    @Transactional(rollbackFor = Exception.class)
    void deletes(List<Long> ids);
}

public interface EntityId<T> {
    T getId();
    void setId(T id);
}
```

## 四、统一返回 ResResult

`mt.common.entity.ResResult<T>`：

```java
{ status: "ok"|"error", message: "...", result: T, code: "..." }

ResResult.success(data)
ResResult.success()
ResResult.error("msg")
ResResult.error("code", "msg")
```

字段 `status == "ok"` 即成功；`code` 用于自定义业务码。

## 五、删除前事件

```java
@EventListener
public void onDeleteCheck(EntityDeleteCheckEvent<Notice> e) {
    Notice notice = e.getEntity();
    // 业务校验：被引用、状态不允许等，抛异常即可阻止删除
}
```

事件源：`mt.common.biz.EntityDeleteCheckEvent`。

## 六、VO/DO 转换工具

```java
// 列表转换
List<NoticeVO> voList = BeanUtils.batchTransform(NoticeVO.class, doList);

// PageInfo 转换
PageInfo<NoticeVO> voPage = TransformUtils.transformPageInfo(doPage, NoticeVO.class);

// 自定义转换
PageInfo<NoticeVO> voPage = TransformUtils.transformPageInfo(doPage,
    srcList -> srcList.stream().map(this::toVO).collect(toList()));
```

工具类：`mt.common.utils.BeanUtils`、`mt.common.biz.TransformUtils`。

## 七、关键类路径

- `mt.common.biz.AbstractEntityService`
- `mt.common.biz.AbstractSimpleEntityService`
- `mt.common.biz.BaseController`
- `mt.common.biz.BaseSimpleController`
- `mt.common.biz.EntityService`
- `mt.common.biz.EntityId`
- `mt.common.biz.EntityDeleteCheckEvent`
- `mt.common.biz.TransformUtils`
- `mt.common.entity.ResResult`
