package mt.generator.mybatis.utils;

import mt.generator.mybatis.core.dialect.MySqlDialect;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * MySQL 解析器。
 *
 * @author Martin
 * @Date 2021/1/6
 * @deprecated 请改用 {@link mt.generator.mybatis.core.dialect.MySqlDialect}。
 */
@Deprecated
public class MysqlParser extends AbstractParser {

	private final MySqlDialect dialect = new MySqlDialect();

	public MysqlParser() {
		super(new MySqlDialect());
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
		Map<String, Object> select = masterJdbc.selectOne("SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + databaseName + "') THEN 1 ELSE 0 END) as result;");
		return "1".equals(select.get("result").toString());
	}

	@Override
	public boolean isExistTable(@NotNull String tableName, @NotNull String databaseName, Jdbc jdbc) {
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (SELECT table_name FROM information_schema.TABLES WHERE table_name = '" + tableName + "' AND table_schema = '" + databaseName + "') THEN 1 ELSE 0 END) as result;");
		return "1".equals(select.get("result").toString());
	}
}
