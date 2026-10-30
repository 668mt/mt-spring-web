package mt.generator.mybatis.utils;

import mt.generator.mybatis.core.dialect.SqlServerDialect;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * SQL Server 解析器。
 *
 * @author Martin
 * @Date 2021/1/6
 * @deprecated 请改用 {@link mt.generator.mybatis.core.dialect.SqlServerDialect}。
 */
@Deprecated
public class SqlServerParser extends AbstractParser {

	private final SqlServerDialect dialect = new SqlServerDialect();

	public SqlServerParser() {
		super(new SqlServerDialect());
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
		Map<String, Object> select = masterJdbc.selectOne("SELECT (CASE WHEN EXISTS (select 1 From master.dbo.sysdatabases where name = '" + databaseName + "') THEN 1 ELSE 0 END) as result");
		return "1".equals(select.get("result").toString());
	}

	@Override
	public boolean isExistTable(@NotNull String tableName, @NotNull String databaseName, Jdbc jdbc) {
		Map<String, Object> select = jdbc.selectOne("SELECT (CASE WHEN EXISTS (select 1 from dbo.sysobjects where xtype='U' and Name = '" + tableName + "') THEN 1 ELSE 0 END) as result");
		return "1".equals(select.get("result").toString());
	}
}
