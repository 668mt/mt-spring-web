package mt.generator.mybatis.utils;

import mt.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2021/1/6
 */
@Component
public class MysqlParser extends AbstractParser {
	@Override
	public boolean support(String driverClassName) {
		return "com.mysql.cj.jdbc.Driver".equals(driverClassName.trim()) || StringUtils.equals("com.mysql.jdbc.Driver", driverClassName.trim());
	}
	
	@Override
	public String parseDatabaseName(String jdbcUrl) {
		return RegexUtils.findFirst(jdbcUrl, "jdbc:mysql://(.+?)/(\\w+)(.*)", 2);
	}
	
	@Override
	public String getMasterJdbcUrl(String jdbcUrl) {
		return jdbcUrl.replaceAll("jdbc:mysql://(.+?)/(\\w+)(.*)", "jdbc:mysql://$1/mysql$3");
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
	
	@Nullable
	@Override
	public String getIdentitySql(@NotNull Field idField) {
		KeySql keySql = idField.getAnnotation(KeySql.class);
		if (keySql != null && keySql.useGeneratedKeys()) {
			return "AUTO_INCREMENT";
		}
		return null;
	}
	
	@Override
	public String getColumnDefinition(@Nullable Column column, @NotNull Class<?> type) {
		if (column != null && StringUtils.isNotBlank(column.columnDefinition())) {
			String c = column.columnDefinition();
			if ("varchar(max)".equalsIgnoreCase(c)) {
				return "text";
			} else if ("image".equalsIgnoreCase(c)) {
				return "blob";
			}
			return c;
		}
		return super.getColumnDefinition(column, type);
	}
}
