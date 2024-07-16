# Web Starter

封装了常用的web功能，包括mybatis单表快速操作，无需写Mapper.java和Mapper.xml文件即可实现数据库操作，配合mt-spring-generator-starter还可实现自动创建数据库表和索引。

其它的功能：

* 用户信息注解：@CurrentUser、@CurrentUserId、@CurrentUserName
* 分页查询，查询条件@Filter注解
* 控制层字段转换：@Message
* 参数检查拦截器：@NotNull、@Nullable等
* 排行榜
* 进度条
* 访问量统计
* 任务分片
* 延迟执行
* 上下文Filter：@ContextFilter，使用场景：例如大部分操作都是在某个项目空间中进行，那么可无需频繁校验空间权限，降低业务复杂度。

详细的说明请查看：[使用文档](https://github.com/668mt/mt-spring-web/tree/master/mt-spring-web-starter/README.md)

# JWT Starter

JWT的封装、拦截

# Redis Starter

RedisService、LockService

# RocketMQ Starter

RocketMQ的封装

# 表生成器

自动生成表和索引。
主要会用到如下索引：
`@Table`
`@Column`
`@ColumnType`
`@ForeignKey`
`@Index`
`@Indexs`
`@IdGenerator`
`@GenerateOrder`

如何开启：

- 引入mt-spring-generator-starter依赖
- 配置开启

```properties
project.generatorEnable=true
project.generateEntityPackages=mt.spring.biz.entity.po
```

使用示例：

```java
package mt.spring.biz.entity.po;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import mt.common.entity.po.BaseEntity;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.Indexs;
import mt.spring.biz.entity.enums.NoticeContentType;
import mt.spring.biz.entity.enums.NoticeLevel;
import mt.spring.biz.entity.handlers.JSONObjectToStringTypeHandler;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.Date;

/**
 * 公告
 */
@Data
@Table(name = "notice")
@EqualsAndHashCode(callSuper = false)
@Indexs({
	@Index(columns = "notice_type"),
	@Index(columns = "user_id"),
	@Index(columns = "biz_key"),
})
public class Notice extends BaseEntity {
	/**
	 * 公共通知
	 */
	public static final Integer NOTICE_TYPE_PUBLIC = 1;
	/**
	 * 用户通知
	 */
	public static final Integer NOTICE_TYPE_USER = 2;
	
	
	@Column(nullable = false)
	private String title;
	@Column(columnDefinition = "longtext", nullable = false)
	private String content;
	@Column(nullable = false)
	private Boolean isTop;
	@Column(nullable = false)
	private Integer noticeType;
	private Long userId;
	/**
	 * 公共事件开始时间
	 */
	private Date startDate;
	/**
	 * 公共事件结束时间
	 */
	private Date endDate;
	/**
	 * 是否弹窗提示
	 */
	private Boolean isDialog;
	private NoticeLevel noticeLevel;
	private NoticeContentType contentType;
	private Boolean isRead;
	/**
	 * 通知关联的key
	 */
	private String bizKey;
	
	@Column(columnDefinition = "text")
	@ColumnType(typeHandler = JSONObjectToStringTypeHandler.class)
	private JSONObject params;
}

```

# 工具类

## 视频处理

FfmpegUtils

* 视频连续截图
* 图片、视频压缩
* 视频转码
* 文件下载
* m3u8视频生成、合并

## 常用工具类

- RegexUtils
- ReflectUtils
- JsonUtils
- Assert
- RetryUtils
- CollectionUtils
- BeanUtils
- TimeUtils
- FtpUtils
- ServiceClient