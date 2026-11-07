package mt.generator.mybatis;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.CommonProperties;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.event.BeforeInitEvent;
import mt.generator.mybatis.core.dialect.Dialect;
import mt.generator.mybatis.status.TableInitResult;
import mt.generator.mybatis.utils.GenerateHelper;
import mt.generator.mybatis.utils.IParser;
import mt.generator.mybatis.utils.IParserDialectAdapter;
import mt.generator.mybatis.utils.MysqlParser;
import mt.generator.mybatis.utils.PostgresqlParser;
import mt.generator.mybatis.utils.SqlServerParser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationEventPublisher;
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
public class Generator implements SmartInitializingSingleton {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private CommonProperties commonProperties;

	@Autowired(required = false)
	private MybatisProperties mybatisProperties;

	@Autowired
	private DataSourceProperties dataSourceProperties;

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
	 * 建表结果
	 */
	private static volatile TableInitResult initResult = TableInitResult.empty();

	/**
	 * 建表结果
	 * <p>
	 * 建表流程没有执行过（比如自动建表开关关闭）时，返回的是空结果
	 *
	 * @return 建表结果
	 */
	public static TableInitResult getInitResult() {
		return initResult;
	}

	/**
	 * 建表
	 * <p>
	 * 用 {@link SmartInitializingSingleton} 而不是监听 {@code ContextRefreshedEvent}：
	 * 它由 {@code finishBeanFactoryInitialization} 在所有单例创建完之后、上下文刷新事件发布之前回调，
	 * 此时数据源配置、方言、{@code @EventListener} 监听器都已就绪，但定时任务还没启动，
	 * 既比刷新事件更早，又不会出现"定时任务或刷新事件监听器先跑、查到还没建出来的表"的问题。
	 */
	@Override
	public void afterSingletonsInstantiated() {
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
		//记录建表结果，方便业务代码查询有没有建表、是不是第一次初始化
		record(afterInitEvent, generateHelper.getExistingTables());
		//注册初始化完成事件
		applicationEventPublisher.publishEvent(afterInitEvent);
	}

	/**
	 * 记录建表结果
	 *
	 * @param event          建表完成事件
	 * @param existedTables  建表之前就已经存在的表
	 */
	static void record(AfterInitEvent event, List<String> existedTables) {
		initResult = new TableInitResult(event.isCreateDatabase(), event.isCreateTable(), event.getNewTables(),
				existedTables, event.getNewColumns(), event.getNewIndexes(), event.getNewForeightKeys());
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
