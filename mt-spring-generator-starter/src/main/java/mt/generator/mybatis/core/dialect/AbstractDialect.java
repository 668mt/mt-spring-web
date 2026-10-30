package mt.generator.mybatis.core.dialect;

import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ColumnType;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.IndexDefinition;
import mt.generator.mybatis.core.model.TableDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * 方言公共实现。
 * <p>
 * 建表、索引、外键的语句结构在各类数据库中是一致的，差异集中在：
 * <ul>
 *     <li>Java 类型 -&gt; SQL 类型的映射</li>
 *     <li>自增列的写法</li>
 *     <li>标识符是否加引号</li>
 *     <li>索引的具体语法</li>
 * </ul>
 * 这些差异由子类覆盖。
 *
 * @author Martin
 */
public abstract class AbstractDialect implements Dialect {

	/**
	 * 默认字符串长度
	 */
	protected static final int DEFAULT_STRING_LENGTH = 100;

	/**
	 * 默认精度
	 */
	protected static final int DEFAULT_PRECISION = 21;

	/**
	 * 默认小数位
	 */
	protected static final int DEFAULT_SCALE = 6;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 方言名称
	 */
	private final String name;

	/**
	 * 标识符最大长度
	 */
	private final int maxIdentifierLength;

	/**
	 * 标识符左引号
	 */
	private final String quotePrefix;

	/**
	 * 标识符右引号
	 */
	private final String quoteSuffix;

	protected AbstractDialect(String name, int maxIdentifierLength, String quotePrefix, String quoteSuffix) {
		this.name = name;
		this.maxIdentifierLength = maxIdentifierLength;
		this.quotePrefix = quotePrefix;
		this.quoteSuffix = quoteSuffix;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getMaxIdentifierLength() {
		return maxIdentifierLength;
	}

	@Override
	public String getSchemaPattern() {
		return null;
	}

	@Override
	public boolean databaseExists(Connection connection, String databaseName) {
		if (StringUtils.isBlank(databaseName)) {
			// 拿不到库名时没法判断，直接认为存在，避免误创建
			return true;
		}
		try (ResultSet rs = connection.getMetaData().getCatalogs()) {
			while (rs.next()) {
				String name = rs.getString("TABLE_CAT");
				if (databaseName.equalsIgnoreCase(name)) {
					return true;
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("检查数据库是否存在失败：" + databaseName, e);
		}
		return false;
	}

	@Override
	public String quoteIdentifier(String identifier) {
		if (StringUtils.isEmpty(identifier)) {
			return identifier;
		}
		if (!isQuoteAlways() && !requiresQuoting(identifier)) {
			return identifier;
		}
		String escaped = identifier.replace(quoteSuffix, quoteSuffix + quoteSuffix);
		return quotePrefix + escaped + quoteSuffix;
	}

	/**
	 * 是否需要给标识符加引号，仅在 {@link #isQuoteAlways()} 为 false 时生效
	 *
	 * @param identifier 标识符
	 * @return 是否需要
	 */
	protected boolean requiresQuoting(String identifier) {
		return true;
	}

	@Override
	public String renderCreateDatabase(String databaseName) {
		return "CREATE DATABASE " + quoteIdentifier(databaseName);
	}

	@Override
	public String renderColumnType(ColumnDefinition column) {
		if (column == null) {
			return "varchar(" + DEFAULT_STRING_LENGTH + ")";
		}
		if (StringUtils.isNotBlank(column.getColumnDefinition())) {
			return normalizeColumnDefinition(column.getColumnDefinition());
		}
		switch (column.getColumnType()) {
			case DECIMAL:
				return getDecimalType(column);
			case STRING:
				return getStringType(column);
			case UUID:
				return getUuidType();
			case ENUM:
				return getEnumType();
			default:
				return getSimpleType(column.getColumnType());
		}
	}

	/**
	 * 归一化显式指定的列定义，把其它数据库的写法转换成本数据库的写法
	 *
	 * @param columnDefinition 原始列定义
	 * @return 列定义
	 */
	protected String normalizeColumnDefinition(String columnDefinition) {
		return columnDefinition;
	}

	/**
	 * 获取简单类型对应的 SQL 类型
	 *
	 * @param type 列类型
	 * @return SQL 类型
	 */
	protected abstract String getSimpleType(ColumnType type);

	/**
	 * 获取高精度数值类型
	 *
	 * @param column 列定义
	 * @return SQL 类型
	 */
	protected String getDecimalType(ColumnDefinition column) {
		int precision = column.getPrecision() > 0 ? column.getPrecision() : DEFAULT_PRECISION;
		int scale = column.getScale() > 0 ? column.getScale() : DEFAULT_SCALE;
		return "numeric(" + precision + "," + scale + ")";
	}

	/**
	 * 获取字符串类型
	 *
	 * @param column 列定义
	 * @return SQL 类型
	 */
	protected String getStringType(ColumnDefinition column) {
		int length = column.getLength() > 0 ? column.getLength() : DEFAULT_STRING_LENGTH;
		return "varchar(" + length + ")";
	}

	/**
	 * 获取 UUID 类型
	 *
	 * @return SQL 类型
	 */
	protected String getUuidType() {
		return "varchar(36)";
	}

	/**
	 * 获取枚举类型
	 *
	 * @return SQL 类型
	 */
	protected String getEnumType() {
		return "varchar(" + DEFAULT_STRING_LENGTH + ")";
	}

	@Override
	public String renderIdentityClause(ColumnDefinition column) {
		if (column == null || !column.isAutoIncrement()) {
			return null;
		}
		return getIdentityClause(column);
	}

	/**
	 * 获取自增子句
	 *
	 * @param column 列定义
	 * @return 自增子句
	 */
	protected abstract String getIdentityClause(ColumnDefinition column);

	@Override
	public List<String> renderCreateTable(TableDefinition table) {
		List<String> lines = new ArrayList<>();
		for (ColumnDefinition column : table.getColumns()) {
			lines.add(renderColumn(column));
		}
		String primaryKey = renderPrimaryKey(table.getPrimaryKeyColumns());
		if (StringUtils.isNotBlank(primaryKey)) {
			lines.add(primaryKey);
		}
		StringBuilder sb = new StringBuilder("CREATE TABLE ").append(quoteIdentifier(table.getName())).append(" (\n");
		sb.append(String.join(",\n", lines));
		sb.append("\n)");
		return Collections.singletonList(sb.toString());
	}

	/**
	 * 渲染单列定义
	 *
	 * @param column 列定义
	 * @return sql
	 */
	protected String renderColumn(ColumnDefinition column) {
		StringBuilder sb = new StringBuilder();
		sb.append(quoteIdentifier(column.getName())).append(" ").append(renderColumnType(column));
		String identity = renderIdentityClause(column);
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

	@Override
	public List<String> renderIndexes(TableDefinition table) {
		List<String> list = new ArrayList<>();
		if (table.getIndexes() == null) {
			return list;
		}
		for (IndexDefinition index : table.getIndexes()) {
			String sql = renderIndex(table.getName(), index);
			if (StringUtils.isNotBlank(sql)) {
				list.add(sql);
			}
		}
		return list;
	}

	@Override
	public List<String> renderForeignKeys(TableDefinition table) {
		List<String> list = new ArrayList<>();
		if (table.getForeignKeys() == null) {
			return list;
		}
		for (ForeignKeyDefinition foreignKey : table.getForeignKeys()) {
			String sql = renderForeignKey(foreignKey);
			if (StringUtils.isNotBlank(sql)) {
				list.add(sql);
			}
		}
		return list;
	}

	@Override
	public String renderAddColumn(String tableName, ColumnDefinition column) {
		return "ALTER TABLE " + quoteIdentifier(tableName) + " ADD COLUMN " + renderColumn(column);
	}

	@Override
	public String renderPrimaryKey(List<String> columns) {
		if (columns == null || columns.isEmpty()) {
			return null;
		}
		StringJoiner joiner = new StringJoiner(", ");
		for (String column : columns) {
			joiner.add(quoteIdentifier(column));
		}
		return "PRIMARY KEY (" + joiner + ")";
	}

	@Override
	public String renderIndex(String tableName, IndexDefinition index) {
		String indexName = quoteIdentifier(resolveIndexName(tableName, index));
		String columns = joinColumns(index.getColumns());
		switch (index.getType()) {
			case unique:
				return "CREATE UNIQUE INDEX " + indexName + " ON " + quoteIdentifier(tableName) + " (" + columns + ")";
			case hash:
				return "CREATE INDEX " + indexName + " ON " + quoteIdentifier(tableName) + " (" + columns + ") USING HASH";
			case fulltext:
			case normal:
			default:
				return "CREATE INDEX " + indexName + " ON " + quoteIdentifier(tableName) + " (" + columns + ")";
		}
	}

	@Override
	public String renderForeignKey(ForeignKeyDefinition foreignKey) {
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE ").append(quoteIdentifier(foreignKey.getTableName()));
		sb.append(" ADD CONSTRAINT ").append(quoteIdentifier(truncateIdentifier(foreignKey.getName())));
		sb.append(" FOREIGN KEY (").append(quoteIdentifier(foreignKey.getColumnName())).append(")");
		sb.append(" REFERENCES ").append(quoteIdentifier(foreignKey.getReferenceTableName()));
		sb.append(" (").append(quoteIdentifier(foreignKey.getReferencedColumnName())).append(")");
		String cascade = getCascadeClause(foreignKey.getCascadeType());
		if (StringUtils.isNotBlank(cascade)) {
			sb.append(cascade);
		}
		return sb.toString();
	}

	/**
	 * 拼接索引列
	 *
	 * @param columns 列
	 * @return 拼接结果
	 */
	protected String joinColumns(List<String> columns) {
		if (columns == null || columns.isEmpty()) {
			return "";
		}
		StringJoiner joiner = new StringJoiner(", ");
		for (String column : columns) {
			joiner.add(quoteIdentifier(column));
		}
		return joiner.toString();
	}

	/**
	 * 获取级联子句
	 *
	 * @param cascadeType 级联类型
	 * @return sql
	 */
	protected String getCascadeClause(ForeignKey.CascadeType cascadeType) {
		if (cascadeType == null) {
			return "";
		}
		switch (cascadeType) {
			case ALL:
				return " ON DELETE CASCADE ON UPDATE CASCADE";
			case DELETE:
				return " ON DELETE CASCADE";
			case UPDATE:
				return " ON UPDATE CASCADE";
			case DETACH:
				return " ON UPDATE SET NULL ON DELETE SET NULL";
			case DEFAULT:
			default:
				return "";
		}
	}

	/**
	 * 生成索引名
	 *
	 * @param tableName 表名
	 * @param index     索引定义
	 * @return 索引名
	 */
	protected String resolveIndexName(String tableName, IndexDefinition index) {
		String name = index.getName();
		if (StringUtils.isBlank(name)) {
			StringBuilder sb = new StringBuilder(getIndexNamePrefix());
			if (isIndexNameIncludeTable()) {
				sb.append(tableName).append('_');
			}
			if (index.getColumns() != null) {
				sb.append(String.join("_", index.getColumns()));
			}
			name = sb.toString();
		}
		return truncateIdentifier(name);
	}

	/**
	 * 索引名前缀
	 *
	 * @return 前缀
	 */
	protected String getIndexNamePrefix() {
		return "INDEX_";
	}

	/**
	 * 索引名是否带上表名
	 * <p>
	 * MySQL 的索引名只需要表内唯一，PostgreSQL、SQL Server 是库内唯一，需要带表名避免冲突。
	 *
	 * @return 是否带表名
	 */
	protected boolean isIndexNameIncludeTable() {
		return false;
	}

	/**
	 * 截断标识符
	 *
	 * @param name 标识符
	 * @return 截断后的标识符
	 */
	protected String truncateIdentifier(String name) {
		if (name == null) {
			return null;
		}
		int max = getMaxIdentifierLength();
		if (name.length() <= max) {
			return name;
		}
		String truncated = name.substring(0, max);
		if (Character.isDigit(truncated.charAt(0))) {
			truncated = "_" + truncated.substring(1);
		}
		return truncated;
	}
}
