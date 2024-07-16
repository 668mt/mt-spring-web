# mt-spring-web
封装了常用工具类、mybatis单表快速操作、自动生成表等功能

# Web Starter
## 添加依赖
```xml
<dependency>
    <groupId>com.github.668mt.web</groupId>
    <artifactId>mt-spring-web-starter</artifactId>
    <version>${latest.version}</version>
</dependency>
```

## 配置
```properties
#数据库配置
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://192.168.0.100:3306/demo?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8&useSSL=false&autoReconnect=true&failOverReadOnly=false
spring.datasource.username=root
spring.datasource.password=******
```

## 使用
### 启动类
```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

//注意这里的MapperScan导入的包是tk.mybatis开头的
@MapperScan("mt.spring.biz.dao")
@SpringBootApplication(scanBasePackages = "mt.spring.biz")
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
```

### 数据库实体类
```java
/**
 * 基础实体类，带有id和审计信息
 */
@Data
public class BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	@Column(updatable = false)
	private Long id;
	@CreatedByUserName
	private String createdBy;
	@UpdatedByUserName
	private String updatedBy;
	@CreatedDate
	private Date createdDate;
	@UpdatedDate
	private Date updatedDate;
}

```
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

### 数据库操作
#### 继承BaseServiceImpl
继承BaseServiceImpl之后，就自动拥有了单表的增删改查功能。无需再写Mapper.java和Mapper.xml文件。
#### 条件查询
```java
@Service
@Slf4j
public class NoticeService extends BaseServiceImpl<Notice> {
	
	/**
	 * 获取未读消息数量
	 *
	 * @param userId
	 * @return
	 */
	public long getUnReadCount(@NotNull Long userId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("userId", Filter.Operator.eq, userId));
		filters.add(new Filter("isRead", Filter.Operator.eq, false));
		return count(filters);
	}
}
```
#### 分页查询

```java
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notice")
public class NoticeController {
	@Autowired
	private NoticeService noticeService;
	
	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class NoticeCondition extends PageCondition {
		@Filter
		private Long userId;
		
		@Filter(column = "id", operator = Filter.Operator.in)
		private List<Long> ids;
	}
	
	@GetMapping("/list")
	public PageInfo<Notice> list(NoticeCondition condition) {
		return noticeService.findPage(condition);
	}
}
```
### 上下文条件Context
### 用户信息
### 控制层字段转换
### 参数检查拦截器
### 高级功能
#### 排行榜
#### 进度条
#### 点击率
#### 任务分片
#### 延迟执行
# JWT
# Redis
# RocketMQ
# 表生成器
# 视频处理
# 常用工具类