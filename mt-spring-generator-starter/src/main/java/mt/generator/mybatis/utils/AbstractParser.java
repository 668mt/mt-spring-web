package mt.generator.mybatis.utils;

import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.core.dialect.Dialect;
import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.JavaTypeResolver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 数据库解析器公共实现。
 *
 * @author Martin
 * @Date 2021/1/6
 * @deprecated 请改用 {@link mt.generator.mybatis.core.dialect.Dialect}。
 * 该类保留是为了兼容旧代码，构造时传入方言后，所有 SQL 都会交给方言生成。
 */
@Deprecated
public abstract class AbstractParser implements IParser {

	/**
	 * 方言，没有方言时退回到本类的老实现
	 */
	private final Dialect dialect;

	protected AbstractParser() {
		this.dialect = null;
	}

	protected AbstractParser(Dialect dialect) {
		this.dialect = dialect;
	}

	/**
	 * 获取方言
	 *
	 * @return 方言
	 */
	protected Dialect getDialect() {
		return dialect;
	}

	@Override
	public String getForeignKeySql(@NotNull String tableName, @NotNull String referenceTableName,
									@NotNull String columnName, @NotNull ForeignKey foreignKey) {
		if (dialect != null) {
			String referencedColumnName = foreignKey.referencedColumnName();
			ForeignKeyDefinition definition = ForeignKeyDefinition.builder()
					.name("FK_" + tableName + "_" + columnName + "_" + referenceTableName + "_" + referencedColumnName)
					.tableName(tableName)
					.columnName(columnName)
					.referenceTableName(referenceTableName)
					.referencedColumnName(referencedColumnName)
					.cascadeType(foreignKey.casecadeType())
					.build();
			return dialect.renderForeignKey(definition);
		}
		String referencedColumnName = foreignKey.referencedColumnName();
		ForeignKey.CascadeType casecadeType = foreignKey.casecadeType();
		String casecade = "";
		switch (casecadeType) {
			case ALL:
				casecade = " ON DELETE CASCADE ON UPDATE CASCADE ";
				break;
			case DELETE:
				casecade = " ON DELETE CASCADE ";
				break;
			case UPDATE:
				casecade = " ON UPDATE CASCADE ";
				break;
			case DETACH:
				casecade = " ON UPDATE SET NULL ON DELETE SET NULL ";
				break;
			case DEFAULT:
				casecade = "";
				break;
		}
		String fkName = "FK_" + tableName + "_" + columnName + "_" + referenceTableName + "_" + referencedColumnName;
		if (fkName.length() > 64) {
			fkName = fkName.substring(fkName.length() - 64);
		}
		return "ALTER TABLE " + tableName + " ADD CONSTRAINT  " + fkName + " FOREIGN KEY(" + columnName + ") REFERENCES " + referenceTableName + "(" + referencedColumnName + ") " + casecade;
	}

	@Override
	public String getPrimaryKeySql(@Nullable List<String> columns) {
		if (CollectionUtils.isEmpty(columns)) {
			return null;
		}
		if (dialect != null) {
			return dialect.renderPrimaryKey(columns);
		}
		StringBuilder sql = new StringBuilder();
		for (String name : columns) {
			sql.append(name).append(",");
		}
		sql = new StringBuilder("primary key(" + sql.substring(0, sql.length() - 1) + ")");
		return sql.toString();
	}

	@Override
	public String getColumnDefinition(@Nullable Column column, @NotNull Class<?> type) {
		if (dialect != null) {
			return dialect.renderColumnType(buildColumnDefinition(column, type));
		}
		String columnDefinition;
		if (type.isAssignableFrom(Integer.class)) {
			columnDefinition = "int";
		} else if (type.isAssignableFrom(Boolean.class)) {
			columnDefinition = "int";
		} else if (type.isAssignableFrom(Date.class)) {
			columnDefinition = "datetime";
		} else if (type.isAssignableFrom(Long.class)) {
			columnDefinition = "bigint";
		} else if (type.isAssignableFrom(BigDecimal.class)) {
			int precision = 21;
			int scale = 6;
			if (column != null) {
				if (column.precision() > 0) {
					precision = column.precision();
				}
				if (column.scale() > 0) {
					scale = column.scale();
				}
			}
			columnDefinition = "numeric(" + precision + "," + scale + ")";
		} else if (type.isAssignableFrom(String.class)) {
			if (column != null) {
				columnDefinition = "varchar(" + column.length() + ")";
			} else {
				columnDefinition = "varchar(100)";
			}
		} else if (type.isAssignableFrom(Enum.class)) {
			columnDefinition = "varchar(100)";
		} else {
			columnDefinition = "varchar(100)";
		}
		return columnDefinition;
	}

	/**
	 * 把注解转换成列定义模型
	 *
	 * @param column 列定义注解
	 * @param type   java 类型
	 * @return 列定义
	 */
	protected ColumnDefinition buildColumnDefinition(@Nullable Column column, @NotNull Class<?> type) {
		return ColumnDefinition.builder()
				.javaType(type)
				.columnType(JavaTypeResolver.resolve(type))
				.columnDefinition(column == null ? null : column.columnDefinition())
				.length(column == null ? 100 : column.length())
				.precision(column == null ? 0 : column.precision())
				.scale(column == null ? 0 : column.scale())
				.build();
	}

	/**
	 * 把字段转换成列定义模型
	 *
	 * @param field 字段
	 * @return 列定义
	 */
	protected ColumnDefinition buildColumnDefinition(@NotNull Field field) {
		Column column = field.getAnnotation(Column.class);
		boolean autoIncrement = false;
		KeySql keySql = field.getAnnotation(KeySql.class);
		if (keySql != null && keySql.useGeneratedKeys()) {
			autoIncrement = true;
		}
		ColumnDefinition definition = buildColumnDefinition(column, field.getType());
		definition.setAutoIncrement(autoIncrement);
		definition.setField(field);
		return definition;
	}

	@Nullable
	@Override
	public String getIdentitySql(@NotNull Field idField) {
		if (dialect != null) {
			return dialect.renderIdentityClause(buildColumnDefinition(idField));
		}
		KeySql keySql = idField.getAnnotation(KeySql.class);
		if (keySql != null && keySql.useGeneratedKeys()) {
			return "AUTO_INCREMENT";
		}
		return null;
	}

	private String getIndexName(String name, List<String> columns) {
		if (StringUtils.isBlank(name)) {
			name = "INDEX_" + StringUtils.join(columns, "_");
		}
		if (name.length() > 64) {
			name = name.substring(name.length() - 64);
		}
		return name;
	}

	@Override
	public String getIndexSql(@Nullable String name, @NotNull List<String> columns) {
		String indexName = getIndexName(name, columns);
		return "KEY " + indexName + "(" + StringUtils.join(columns, ",") + ")";
	}

	@Override
	public String getUniqueIndexSql(@Nullable String name, @NotNull List<String> columns) {
		String indexName = getIndexName(name, columns);
		return "UNIQUE KEY " + indexName + "(" + StringUtils.join(columns, ",") + ")";
	}

	@Override
	public String getFullTextIndexSql(@Nullable String name, @NotNull List<String> columns) {
		String indexName = getIndexName(name, columns);
		return "FULLTEXT KEY " + indexName + " (" + StringUtils.join(columns, ",") + ") WITH PARSER ngram";
	}

	@Override
	public String getHashIndexSql(@Nullable String name, @NotNull List<String> columns) {
		String indexName = getIndexName(name, columns);
		return "KEY " + indexName + " (" + StringUtils.join(columns, ",") + ") using hash";
	}
}
