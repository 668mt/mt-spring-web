package mt.generator.mybatis.utils;

import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.UniqueIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/6
 */
public interface IParser {
	
	/**
	 * 是否支持
	 *
	 * @param driverClassName driverClass
	 * @return 是否支持
	 */
	boolean support(String driverClassName);
	
	String parseDatabaseName(String jdbcUrl);
	
	String getMasterJdbcUrl(String jdbcUrl);
	
	/**
	 * 是否存在数据库
	 *
	 * @param databaseName 是否存在数据库
	 * @return 是否存在
	 */
	boolean isExistDatabase(@NotNull String databaseName, Jdbc masterJdbc);
	
	/**
	 * 是否存在表
	 *
	 * @param tableName 表名
	 * @return 是否存在
	 */
	boolean isExistTable(@NotNull String tableName, @NotNull String databaseName, Jdbc jdbc);
	
	/**
	 * 获取默认列定义
	 *
	 * @param type   字段类型
	 * @param column 列定义注解
	 * @return jdbc类型
	 */
	String getColumnDefinition(@Nullable Column column, @NotNull Class<?> type);
	
	/**
	 * 获取自增Sql
	 *
	 * @param idField id字段
	 * @return sql
	 */
	@Nullable
	String getIdentitySql(@NotNull Field idField);
	
	/**
	 * 获取主键sql
	 *
	 * @param columns ids
	 * @return sql
	 */
	String getPrimaryKeySql(@Nullable List<String> columns);
	
	/**
	 * 获取外键sql
	 *
	 * @param tableName  表名
	 * @param foreignKey 外键注解
	 * @return sql
	 */
	String getForeignKeySql(@NotNull String tableName, @NotNull String referenceTableName, @NotNull String columnName, @NotNull ForeignKey foreignKey);
	
	String getIndexSql(@Nullable String name, @NotNull List<String> columns);
	
	String getUniqueIndexSql(@Nullable String name, @NotNull List<String> columns);
}
