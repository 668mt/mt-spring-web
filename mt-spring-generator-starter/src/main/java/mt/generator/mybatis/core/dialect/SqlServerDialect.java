package mt.generator.mybatis.core.dialect;

import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ColumnType;
import mt.generator.mybatis.core.model.IndexDefinition;
import mt.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * SQL Server 方言。
 *
 * @author Martin
 */
public class SqlServerDialect extends AbstractDialect {

	public SqlServerDialect() {
		super("SQL Server", 128, "[", "]");
	}

	@Override
	public boolean supports(String driverClassName) {
		if (StringUtils.isBlank(driverClassName)) {
			return false;
		}
		return "com.microsoft.sqlserver.jdbc.SQLServerDriver".equalsIgnoreCase(driverClassName.trim());
	}

	@Override
	public String getSchemaPattern() {
		return "dbo";
	}

	@Override
	public String getDatabaseName(String jdbcUrl) {
		return RegexUtils.findFirst(jdbcUrl, "database(name){0,1}=(\\w+)", 2);
	}

	@Override
	public String getMaintenanceJdbcUrl(String jdbcUrl) {
		if (jdbcUrl == null) {
			return null;
		}
		return jdbcUrl.replaceAll("database(name)?=(\\w+)", "databasename=master");
	}

	@Override
	protected String getSimpleType(ColumnType type) {
		switch (type) {
			case INTEGER:
				return "int";
			case LONG:
				return "bigint";
			case SHORT:
				return "smallint";
			case BYTE:
				return "tinyint";
			case BOOLEAN:
				return "bit";
			case DOUBLE:
				return "float";
			case FLOAT:
				return "real";
			case BINARY:
				return "varbinary(max)";
			case DATE:
				return "date";
			case TIME:
				return "time";
			case TIMESTAMP:
				return "datetime2";
			case JSON:
			case TEXT:
				return "nvarchar(max)";
			default:
				return "nvarchar(" + DEFAULT_STRING_LENGTH + ")";
		}
	}

	@Override
	protected String getIdentityClause(ColumnDefinition column) {
		return "IDENTITY(1,1)";
	}

	@Override
	protected String getStringType(ColumnDefinition column) {
		int length = column.getLength() > 0 ? column.getLength() : DEFAULT_STRING_LENGTH;
		return "nvarchar(" + length + ")";
	}

	@Override
	protected String getUuidType() {
		return "uniqueidentifier";
	}

	@Override
	protected boolean isIndexNameIncludeTable() {
		return true;
	}

	@Override
	protected String normalizeColumnDefinition(String columnDefinition) {
		String definition = columnDefinition.trim();
		if ("text".equalsIgnoreCase(definition)) {
			return "nvarchar(max)";
		}
		if ("blob".equalsIgnoreCase(definition)) {
			return "image";
		}
		if ("datetime".equalsIgnoreCase(definition) || "datetime2".equalsIgnoreCase(definition)) {
			return "datetime2";
		}
		if ("boolean".equalsIgnoreCase(definition) || "bool".equalsIgnoreCase(definition)) {
			return "bit";
		}
		if ("text".equalsIgnoreCase(definition)) {
			return "nvarchar(max)";
		}
		return definition;
	}

	@Override
	public String renderIndex(String tableName, IndexDefinition index) {
		String indexName = quoteIdentifier(resolveIndexName(tableName, index));
		String table = quoteIdentifier(tableName);
		String columns = joinColumns(index.getColumns());
		switch (index.getType()) {
			case unique:
				return "CREATE UNIQUE INDEX " + indexName + " ON " + table + " (" + columns + ")";
			case fulltext:
				return "CREATE FULLTEXT INDEX " + indexName + " ON " + table + " (" + columns + ")";
			case hash:
			case normal:
			default:
				// SQL Server 没有 hash 索引，退化成普通索引
				return "CREATE INDEX " + indexName + " ON " + table + " (" + columns + ")";
		}
	}
}
