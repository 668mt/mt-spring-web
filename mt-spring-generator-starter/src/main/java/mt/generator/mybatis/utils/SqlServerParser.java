package mt.generator.mybatis.utils;

import mt.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2021/1/6
 */
public class SqlServerParser extends AbstractParser {
	@Override
	public boolean support(String driverClassName) {
		return StringUtils.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver", driverClassName.trim());
	}
	
	@Override
	public String parseDatabaseName(String jdbcUrl) {
		return RegexUtils.findFirst(jdbcUrl, "database(name){0,1}=(\\w+)", 2);
	}
	
	@Override
	public String getMasterJdbcUrl(String jdbcUrl) {
		return jdbcUrl.replaceAll("database(name)?=(\\w+)", "databasename=master");
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
	
	@Nullable
	@Override
	public String getIdentitySql(@NotNull Field idField) {
		return null;
	}
	
	@Override
	public String getColumnDefinition(@Nullable Column column, @NotNull Class<?> type) {
		if (column != null && StringUtils.isNotBlank(column.columnDefinition())) {
			String c = column.columnDefinition();
			if ("text".equalsIgnoreCase(c)) {
				return "varchar(max)";
			} else if ("blob".equalsIgnoreCase(c)) {
				return "image";
			}
			return c;
		}
		return super.getColumnDefinition(column, type);
	}
}
