package mt.generator.mybatis.core.dialect;

import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.IndexDefinition;
import mt.generator.mybatis.core.model.TableDefinition;

import java.util.List;

/**
 * 数据库方言。
 * <p>
 * 负责把 {@link TableDefinition}（与数据库无关的表结构模型）渲染成具体的 DDL，
 * 以及解析数据库连接信息。实体扫描与 SQL 渲染通过该接口解耦，
 * 新增一种数据库只需要新增一个实现类。
 *
 * @author Martin
 */
public interface Dialect {

	/**
	 * 方言名称
	 *
	 * @return 名称
	 */
	String getName();

	/**
	 * 是否支持该驱动
	 *
	 * @param driverClassName 驱动类名
	 * @return 是否支持
	 */
	boolean supports(String driverClassName);

	/**
	 * 从 JDBC URL 中解析数据库名
	 *
	 * @param jdbcUrl jdbc 连接串
	 * @return 数据库名
	 */
	String getDatabaseName(String jdbcUrl);

	/**
	 * 获取管理库的 JDBC URL，用于执行 create database
	 *
	 * @param jdbcUrl jdbc 连接串
	 * @return 管理库的 jdbc 连接串
	 */
	String getMaintenanceJdbcUrl(String jdbcUrl);

	/**
	 * schema 模式，MySQL 为 null，PostgreSQL 为 public，SQL Server 为 dbo
	 *
	 * @return schema 模式
	 */
	String getSchemaPattern();

	/**
	 * 数据库是否存在
	 * <p>
	 * PostgreSQL 的 JDBC 驱动 {@code getCatalogs()} 只会返回当前连接的库，
	 * 所以不能统一用 {@link java.sql.DatabaseMetaData} 判断，需要各方言自己实现。
	 *
	 * @param connection   连接
	 * @param databaseName 数据库名
	 * @return 是否存在
	 */
	boolean databaseExists(java.sql.Connection connection, String databaseName);

	/**
	 * 表名的候选写法
	 * <p>
	 * PostgreSQL 会把不加引号的标识符折叠成小写，所以驼峰的表名要同时用小写去找。
	 *
	 * @param tableName 表名
	 * @return 候选表名
	 */
	default java.util.List<String> getTableNameCandidates(String tableName) {
		java.util.List<String> candidates = new java.util.ArrayList<>(2);
		if (tableName == null) {
			return candidates;
		}
		candidates.add(tableName);
		String lower = tableName.toLowerCase(java.util.Locale.ROOT);
		if (!lower.equals(tableName)) {
			candidates.add(lower);
		}
		return candidates;
	}

	/**
	 * 标识符最大长度
	 *
	 * @return 最大长度
	 */
	int getMaxIdentifierLength();

	/**
	 * 是否总是给标识符加引号
	 * <p>
	 * PostgreSQL 不能总是加引号：不加引号时数据库会把标识符折叠成小写，
	 * 而 MyBatis 生成的 SQL 同样会被折叠成小写，两边才能对上；
	 * 一旦加了引号就会保留原始大小写，导致 MyBatis 查不到字段。
	 *
	 * @return 是否总是加引号
	 */
	default boolean isQuoteAlways() {
		return true;
	}

	/**
	 * 引用标识符
	 *
	 * @param identifier 标识符
	 * @return 引用后的标识符
	 */
	String quoteIdentifier(String identifier);

	/**
	 * 渲染创建数据库语句
	 *
	 * @param databaseName 数据库名
	 * @return sql
	 */
	String renderCreateDatabase(String databaseName);

	/**
	 * 渲染列类型
	 *
	 * @param column 列定义
	 * @return sql 类型
	 */
	String renderColumnType(ColumnDefinition column);

	/**
	 * 渲染自增子句
	 *
	 * @param column 列定义
	 * @return sql，为 null 表示不自增
	 */
	String renderIdentityClause(ColumnDefinition column);

	/**
	 * 渲染建表语句
	 *
	 * @param table 表定义
	 * @return sql 列表
	 */
	List<String> renderCreateTable(TableDefinition table);

	/**
	 * 渲染索引语句
	 *
	 * @param table 表定义
	 * @return sql 列表
	 */
	List<String> renderIndexes(TableDefinition table);

	/**
	 * 渲染外键语句
	 *
	 * @param table 表定义
	 * @return sql 列表
	 */
	List<String> renderForeignKeys(TableDefinition table);

	/**
	 * 渲染新增字段语句
	 *
	 * @param tableName 表名
	 * @param column    列定义
	 * @return sql
	 */
	String renderAddColumn(String tableName, ColumnDefinition column);

	/**
	 * 渲染主键约束
	 *
	 * @param columns 主键列
	 * @return sql，为 null 表示没有主键
	 */
	String renderPrimaryKey(List<String> columns);

	/**
	 * 渲染单个索引
	 *
	 * @param tableName 表名
	 * @param index     索引定义
	 * @return sql
	 */
	String renderIndex(String tableName, IndexDefinition index);

	/**
	 * 渲染单个外键
	 *
	 * @param foreignKey 外键定义
	 * @return sql
	 */
	String renderForeignKey(ForeignKeyDefinition foreignKey);
}
