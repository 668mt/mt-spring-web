package mt.generator.mybatis.utils;

import mt.common.annotation.ForeignKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/6
 */
public abstract class AbstractParser implements IParser {
	@Override
	public String getForeignKeySql(@NotNull String tableName, @NotNull String referenceTableName, @NotNull String columnName, @NotNull ForeignKey foreignKey) {
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
		StringBuilder sql = new StringBuilder();
		for (String name : columns) {
			sql.append(name).append(",");
		}
		sql = new StringBuilder("primary key(" + sql.substring(0, sql.length() - 1) + ")");
		return sql.toString();
	}
	
	@Override
	public String getColumnDefinition(@Nullable Column column, @NotNull Class<?> type) {
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
