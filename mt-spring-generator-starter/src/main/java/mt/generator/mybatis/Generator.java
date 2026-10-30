package mt.generator.mybatis;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.CommonProperties;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.event.BeforeInitEvent;
import mt.generator.mybatis.core.dialect.Dialect;
import mt.generator.mybatis.utils.GenerateHelper;
import mt.generator.mybatis.utils.IParser;
import mt.generator.mybatis.utils.IParserDialectAdapter;
import mt.generator.mybatis.utils.MysqlParser;
import mt.generator.mybatis.utils.PostgresqlParser;
import mt.generator.mybatis.utils.SqlServerParser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.autoconfigure.MybatisProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据库和表格自动生成器。
 * <p>
 * 启动时扫描实体类，把实体翻译成表结构模型，再由对应的数据库方言渲染成 DDL 执行。
 *
 * @author Martin
 * @date 2017-10-23 下午10:10:04
 */
@Slf4j
public class Generator {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private CommonProperties commonProperties;

	@Autowired(required = false)
	private MybatisProperties mybatisProperties;

	@Autowired
	private DataSourceProperties dataSourceProperties;

	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * 数据库方言
	 */
	@Autowired(required = false)
	private List<Dialect> dialects = Collections.emptyList();

	/**
	 * 兼容旧的解析器扩展点
	 */
	@Autowired(required = false)
	private List<IParser> parsers = Collections.emptyList();

	private final AtomicBoolean loaded = new AtomicBoolean(false);

	/**
	 * 建表
	 * <p>
	 * 这里必须用最高优先级：{@code ScheduledAnnotationBeanPostProcessor} 是在
	 * {@code prepareRefresh()} 阶段就注册成事件监听器的，而 {@code @EventListener} 方法
	 * 要等到所有单例创建完才注册，两者默认都是 {@code LOWEST_PRECEDENCE}，
	 * 排序后定时任务会先启动，导致定时任务查还没建出来的表。
	 *
	 * @param contextRefreshedEvent 上下文刷新事件
	 */
	@EventListener
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public void listener(ContextRefreshedEvent contextRefreshedEvent) {
		//只处理自己的上下文，Spring Cloud 下 bootstrap 上下文刷新时不会重复执行
		if (contextRefreshedEvent.getApplicationContext() != applicationContext) {
			return;
		}
		if (!Boolean.TRUE.equals(commonProperties.getGenerator().getEnabled())) {
			return;
		}
		if (!loaded.compareAndSet(false, true)) {
			return;
		}
		try {
			generate();
		} catch (Exception e) {
			log.error("自动建表失败", e);
			//建表失败必须让应用启动失败，否则会出现"启动了但表没建"的假象
			throw new IllegalStateException("自动建表失败：" + e.getMessage(), e);
		}
	}

	/**
	 * 执行建表
	 */
	private void generate() {
		String jdbcUrl = dataSourceProperties.getUrl();
		if (StringUtils.isBlank(jdbcUrl)) {
			log.warn("没有配置数据库连接串，跳过自动建表");
			return;
		}
		String driverClass;
		try {
			driverClass = dataSourceProperties.determineDriverClassName();
		} catch (Exception e) {
			log.warn("无法确定数据库驱动，跳过自动建表：{}", e.getMessage());
			return;
		}
		String user = dataSourceProperties.determineUsername();
		String password = dataSourceProperties.determinePassword();
		String[] entityPackages = resolveEntityPackages();
		if (entityPackages.length == 0) {
			log.warn("没有配置实体所在的包（mt.config.generator.packages），只会创建框架自带的表，"
					+ "请检查 mybatis 的 type-aliases-package 配置");
		}
		Dialect dialect = resolveDialect(driverClass);
		if (dialect == null) {
			log.warn("没有找到合适的表生成器：{}", driverClass);
			return;
		}

		log.info("Generator初始化，使用方言[{}]，创建表的包名：{}", dialect.getName(), Arrays.toString(entityPackages));

		applicationEventPublisher.publishEvent(new BeforeInitEvent(this));
		AfterInitEvent afterInitEvent = new AfterInitEvent(this);
		GenerateHelper generateHelper = new GenerateHelper(jdbcUrl, driverClass, user, password, dialect, commonProperties);
		generateHelper.init(entityPackages, afterInitEvent);
		//注册初始化完成事件
		applicationEventPublisher.publishEvent(afterInitEvent);
	}

	/**
	 * 解析需要扫描的实体包名
	 *
	 * @return 包名
	 */
	private String[] resolveEntityPackages() {
		List<String> packages = commonProperties.getGenerator().getPackages();
		if (CollectionUtils.isEmpty(packages)) {
			if (mybatisProperties == null) {
				return new String[0];
			}
			String typeAliasesPackage = mybatisProperties.getTypeAliasesPackage();
			return StringUtils.isBlank(typeAliasesPackage) ? new String[0] : new String[]{typeAliasesPackage};
		}
		return packages.stream().filter(StringUtils::isNotBlank).toArray(String[]::new);
	}

	/**
	 * 解析数据库方言
	 *
	 * @param driverClassName 驱动类名
	 * @return 方言
	 */
	private Dialect resolveDialect(String driverClassName) {
		//用户自定义的解析器优先
		for (IParser parser : parsers) {
			if (isBuiltInParser(parser)) {
				continue;
			}
			if (parser.support(driverClassName)) {
				log.info("使用自定义解析器：{}", parser.getClass().getName());
				return new IParserDialectAdapter(parser);
			}
		}
		for (Dialect dialect : dialects) {
			if (dialect.supports(driverClassName)) {
				return dialect;
			}
		}
		//内置的解析器兜底
		for (IParser parser : parsers) {
			if (parser.support(driverClassName)) {
				return new IParserDialectAdapter(parser);
			}
		}
		return null;
	}

	/**
	 * 是否是内置的解析器
	 *
	 * @param parser 解析器
	 * @return 是否内置
	 */
	private boolean isBuiltInParser(IParser parser) {
		Class<?> clazz = parser.getClass();
		return clazz == MysqlParser.class || clazz == SqlServerParser.class || clazz == PostgresqlParser.class;
	}
}
