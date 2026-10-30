package mt.generator.mybatis.utils;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.CommonProperties;
import mt.common.mybatis.event.AfterInitEvent;
import mt.generator.mybatis.core.dialect.Dialect;
import mt.generator.mybatis.core.meta.SchemaInspector;
import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.TableDefinition;
import mt.generator.mybatis.core.scanner.EntityScanner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 建表辅助类。
 * <p>
 * 负责把实体类翻译成表结构模型，再交给方言渲染成 DDL 并执行。
 *
 * @author Martin
 * @date 2017-10-22 下午3:37:37
 */
@Slf4j
public class GenerateHelper {

	/**
	 * 外键关系
	 */
	public final List<String> foreightKeys = new ArrayList<>();

	private final String jdbcUrl;

	private final String driverClass;

	private final String user;

	private final String password;

	private final Dialect dialect;

	private final CommonProperties commonProperties;

	private final EntityScanner scanner;

	private final SchemaInspector inspector;

	private final Jdbc jdbc;

	private final Jdbc maintenanceJdbc;

	private final String databaseName;

	/**
	 * 使用方言创建
	 *
	 * @param jdbcUrl         连接串
	 * @param driverClass     驱动
	 * @param user            用户名
	 * @param password        密码
	 * @param dialect         方言
	 * @param commonProperties 公共配置
	 */
	public GenerateHelper(String jdbcUrl, String driverClass, String user, String password, Dialect dialect,
						  CommonProperties commonProperties) {
		Assert.notNull(dialect, "dialect不能为空");
		this.jdbcUrl = jdbcUrl;
		this.driverClass = driverClass;
		this.user = user;
		this.password = password;
		this.dialect = dialect;
		this.commonProperties = commonProperties;
		this.scanner = new EntityScanner(commonProperties);
		this.inspector = new SchemaInspector(dialect);
		this.jdbc = new Jdbc(driverClass, jdbcUrl, user, password);
		this.maintenanceJdbc = new Jdbc(driverClass, dialect.getMaintenanceJdbcUrl(jdbcUrl), user, password);
		this.databaseName = dialect.getDatabaseName(jdbcUrl);
	}

	/**
	 * 兼容旧的构造方式
	 *
	 * @param jdbcUrl     连接串
	 * @param driverClass 驱动
	 * @param user        用户名
	 * @param password    密码
	 * @param parser      解析器
	 */
	public GenerateHelper(String jdbcUrl, String driverClass, String user, String password, IParser parser) {
		this(jdbcUrl, driverClass, user, password, new IParserDialectAdapter(parser), null);
	}

	/**
	 * 创建数据库，数据库已存在时返回 false
	 *
	 * @return 是否创建了数据库
	 */
	public boolean createDatabase() {
		if (StringUtils.isBlank(databaseName)) {
			log.info("无法从连接串中解析出数据库名，跳过自动创建数据库：{}", jdbcUrl);
			return false;
		}
		boolean exists;
		try (Connection connection = maintenanceJdbc.getConnection()) {
			exists = inspector.databaseExists(connection, databaseName);
		} catch (Exception e) {
			// 连不上管理库时没法判断，交由后面的建表流程报错，提示更明确
			log.warn("连接管理库失败，跳过自动创建数据库：{}", e.getMessage());
			return false;
		}
		if (!exists) {
			log.info("初始化自动创建数据库...");
			String sql = dialect.renderCreateDatabase(databaseName);
			//建库失败必须抛出去，否则会出现"表没建"却以为建好了
			maintenanceJdbc.execute(sql);
			log.info("创建数据库：{}", databaseName);
			return true;
		}
		return false;
	}

	/**
	 * 初始化自动创建表
	 *
	 * @param entityPackages  实体所在的包名
	 * @param afterInitEvent  初始化完成事件
	 */
	public void init(String[] entityPackages, AfterInitEvent afterInitEvent) {
		Assert.notNull(afterInitEvent, "afterInitEvent事件不能为空");
		//检查数据库
		afterInitEvent.setCreateDatabase(createDatabase());

		List<Class<?>> entitys = scanner.scan(entityPackages);
		List<String> newTables = new ArrayList<>();
		List<String> newIndexes = new ArrayList<>();
		List<String> newColumns = new ArrayList<>();
		//外键要等所有表都建好之后再建，否则引用的表可能还不存在
		List<ForeignKeyDefinition> pendingForeignKeys = new ArrayList<>();

		for (Class<?> entityClass : entitys) {
			TableDefinition table = scanner.toTableDefinition(entityClass);
			String existingTable = findTable(table.getName());
			if (existingTable == null) {
				for (String sql : dialect.renderCreateTable(table)) {
					jdbc.execute(sql);
				}
				newTables.add(table.getName());
				for (String sql : dialect.renderIndexes(table)) {
					jdbc.execute(sql);
					newIndexes.add(sql);
				}
				pendingForeignKeys.addAll(table.getForeignKeys());
			} else {
				newColumns.addAll(addMissingColumns(existingTable, table));
			}
		}

		List<String> foreightSuccess = new ArrayList<>();
		for (ForeignKeyDefinition foreignKey : pendingForeignKeys) {
			String sql = dialect.renderForeignKey(foreignKey);
			jdbc.execute(sql);
			foreightSuccess.add(sql);
			foreightKeys.add(sql);
		}

		//创建的表
		afterInitEvent.setNewTables(newTables);
		//创建的主外键关系
		afterInitEvent.setNewForeightKeys(foreightSuccess);
		afterInitEvent.setNewIndexes(newIndexes);
		afterInitEvent.setNewColumns(newColumns);
		afterInitEvent.setCreateTable(!CollectionUtils.isEmpty(newTables));
		afterInitEvent.setCreateForeightKey(!CollectionUtils.isEmpty(foreightSuccess));

		log.info("新建表数量：{}", newTables.size());
		for (String table : newTables) {
			log.info(">>>>>>>>>{}", table);
		}
		log.info("新建索引数量：{}", newIndexes.size());
		for (String sql : newIndexes) {
			log.info(">>>>>>>>>{}", sql);
		}
		log.info("新建字段数量：{}", newColumns.size());
		for (String sql : newColumns) {
			log.info(">>>>>>>>>{}", sql);
		}
		log.info("新建外键数量：{}", foreightSuccess.size());
		for (String sql : foreightSuccess) {
			log.info(">>>>>>>>>{}", sql);
		}
	}

	/**
	 * 查询表是否存在
	 *
	 * @param tableName 表名
	 * @return 数据库中真实的表名，不存在返回 null
	 */
	private String findTable(String tableName) {
		try (Connection connection = jdbc.getConnection()) {
			return inspector.findTable(connection, databaseName, tableName);
		} catch (SQLException e) {
			throw new IllegalStateException("检查表是否存在失败：" + tableName, e);
		}
	}

	/**
	 * 给已有的表补齐缺失的字段
	 *
	 * @param existingTable 数据库中真实的表名
	 * @param table         表定义
	 * @return 执行的 sql
	 */
	private List<String> addMissingColumns(String existingTable, TableDefinition table) {
		if (commonProperties == null || !Boolean.TRUE.equals(commonProperties.getGenerator().getAddMissingColumn())) {
			return Collections.emptyList();
		}
		Set<String> existingColumns;
		try (Connection connection = jdbc.getConnection()) {
			existingColumns = inspector.listColumns(connection, databaseName, existingTable);
		} catch (SQLException e) {
			throw new IllegalStateException("查询表字段失败：" + existingTable, e);
		}
		if (CollectionUtils.isEmpty(existingColumns)) {
			return Collections.emptyList();
		}
		List<String> sqls = new ArrayList<>();
		for (ColumnDefinition column : table.getColumns()) {
			boolean exists = existingColumns.stream().anyMatch(name -> name.equalsIgnoreCase(column.getName()));
			if (exists) {
				continue;
			}
			String sql = dialect.renderAddColumn(existingTable, column);
			jdbc.execute(sql);
			sqls.add(sql);
		}
		return sqls;
	}

	/**
	 * 获取表名
	 *
	 * @param entityClass 实体类
	 * @return 表名
	 */
	public String getTableName(Class<?> entityClass) {
		return scanner.resolveTableName(entityClass);
	}

	/**
	 * 拼接sql语句
	 *
	 * @param field 字段
	 * @return sql
	 */
	public String getJdbcTypeSql(Field field) {
		ColumnDefinition column = scanner.toColumnDefinition(field);
		StringBuilder sb = new StringBuilder();
		sb.append(dialect.quoteIdentifier(column.getName())).append(" ").append(dialect.renderColumnType(column));
		String identity = dialect.renderIdentityClause(column);
		if (StringUtils.isNotBlank(identity)) {
			sb.append(" ").append(identity);
		}
		if (!column.isNullable()) {
			sb.append(" NOT NULL");
		}
		if (column.isUnique()) {
			sb.append(" UNIQUE");
		}
		return sb.toString();
	}

	public String getColumnName(Field field) {
		return scanner.resolveColumnName(field);
	}

	/**
	 * 获取数据库类型  例：varchar(100)
	 *
	 * @param field 字段
	 * @return 数据库类型
	 */
	public String getColumnDefinition(Field field) {
		ColumnDefinition column = scanner.toColumnDefinition(field);
		String definition = dialect.renderColumnType(column);
		String identity = dialect.renderIdentityClause(column);
		return StringUtils.isBlank(identity) ? definition : definition + " " + identity;
	}

	/**
	 * 拼接创建表语句
	 *
	 * @param entityClass 实体类
	 * @return sql
	 */
	public String getCreateTableSql(Class<?> entityClass) {
		TableDefinition table = scanner.toTableDefinition(entityClass);
		List<String> sqls = dialect.renderCreateTable(table);
		return String.join(";\r\n", sqls);
	}

	/**
	 * 获取所有带@Table注解的实体
	 *
	 * @param entityPackage 包名
	 * @return 实体类列表
	 */
	public List<Class<?>> getAllEntitys(String entityPackage) {
		return scanner.findEntities(entityPackage);
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public Dialect getDialect() {
		return dialect;
	}

}
