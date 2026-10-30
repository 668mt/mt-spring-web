package mt.generator.mybatis.utils;

import mt.generator.mybatis.core.dialect.PostgreSqlDialect;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * PostgreSQL 解析器。
 *
 * @author Martin
 * @Date 2021/1/6
 * @deprecated 请改用 {@link mt.generator.mybatis.core.dialect.PostgreSqlDialect}。
 */
@Deprecated
public class PostgresqlParser extends AbstractParser {

	private final PostgreSqlDialect dialect = new PostgreSqlDialect();

	public PostgresqlParser() {
		super(new PostgreSqlDialect());
	}

	@Override
	public boolean support(String driverClassName) {
		return dialect.supports(driverClassName);
	}

	@Override
	public String parseDatabaseName(String jdbcUrl) {
		return dialect.getDatabaseName(jdbcUrl);
	}

	@Override
	public String getMasterJdbcUrl(String jdbcUrl) {
		return dialect.getMaintenanceJdbcUrl(jdbcUrl);
	}

	@Override
	public boolean isExistDatabase(@NotNull String databaseName, Jdbc masterJdbc) {
		Map<String, Object> select = masterJdbc.selectOne("SELECT (CASE WHEN EXISTS (SELECT 1 FROM pg_database WHERE datname = '"
				+ databaseName + "') THEN 1 ELSE 0 END) as result;");
		return isTrue(select);
	}

	@Override
	public boolean isExistTable(@NotNull String tableName, @NotNull String databaseName, Jdbc jdbc) {
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables "
				+ "WHERE table_schema = 'public' AND lower(table_name) = lower('" + tableName + "')) THEN 1 ELSE 0 END) as result;");
		return isTrue(select);
	}

	/**
	 * 解析标志位
	 *
	 * @param select 查询结果
	 * @return 是否为真
	 */
	private boolean isTrue(Map<String, Object> select) {
		if (select == null) {
			return false;
		}
		for (Map.Entry<String, Object> entry : select.entrySet()) {
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			if ("1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString())) {
				return true;
			}
		}
		return false;
	}
}
