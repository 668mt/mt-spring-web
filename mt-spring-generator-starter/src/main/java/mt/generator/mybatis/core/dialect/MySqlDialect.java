package mt.generator.mybatis.core.dialect;

import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ColumnType;
import mt.generator.mybatis.core.model.IndexDefinition;
import org.apache.commons.lang3.StringUtils;

/**
 * MySQL 方言。
 *
 * @author Martin
 */
public class MySqlDialect extends AbstractDialect {

	public MySqlDialect() {
		super("MySQL", 64, "`", "`");
	}

	@Override
	public boolean supports(String driverClassName) {
		if (StringUtils.isBlank(driverClassName)) {
			return false;
		}
		String driver = driverClassName.trim();
		return "com.mysql.cj.jdbc.Driver".equalsIgnoreCase(driver)
				|| "com.mysql.jdbc.Driver".equalsIgnoreCase(driver);
	}

	@Override
	public String getDatabaseName(String jdbcUrl) {
		return JdbcUrl.parse(jdbcUrl).getDatabase();
	}

	@Override
	public String getMaintenanceJdbcUrl(String jdbcUrl) {
		JdbcUrl url = JdbcUrl.parse(jdbcUrl);
		return JdbcUrl.build(url.getScheme(), url.getHosts(), "mysql", url.getParams());
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
				return "int";
			case DOUBLE:
				return "double";
			case FLOAT:
				return "float";
			case BINARY:
				return "blob";
			case DATE:
				return "date";
			case TIME:
				return "time";
			case TIMESTAMP:
				return "datetime";
			case JSON:
			case TEXT:
				return "text";
			default:
				return "varchar(" + DEFAULT_STRING_LENGTH + ")";
		}
	}

	@Override
	protected String getIdentityClause(ColumnDefinition column) {
		return "AUTO_INCREMENT";
	}

	@Override
	protected String normalizeColumnDefinition(String columnDefinition) {
		String definition = columnDefinition.trim();
		if ("varchar(max)".equalsIgnoreCase(definition) || "nvarchar(max)".equalsIgnoreCase(definition)) {
			return "text";
		}
		if ("image".equalsIgnoreCase(definition) || "varbinary(max)".equalsIgnoreCase(definition)) {
			return "blob";
		}
		if ("datetime2".equalsIgnoreCase(definition)) {
			return "datetime";
		}
		if ("bit".equalsIgnoreCase(definition)) {
			return "int";
		}
		if ("uniqueidentifier".equalsIgnoreCase(definition)) {
			return "varchar(36)";
		}
		if ("real".equalsIgnoreCase(definition)) {
			return "float";
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
				return "CREATE FULLTEXT INDEX " + indexName + " ON " + table + " (" + columns + ") WITH PARSER ngram";
			case hash:
				return "CREATE INDEX " + indexName + " ON " + table + " (" + columns + ") USING HASH";
			case normal:
			default:
				return "CREATE INDEX " + indexName + " ON " + table + " (" + columns + ")";
		}
	}
}
