package mt.generator.mybatis.utils;

import mt.common.annotation.ForeignKey;
import mt.generator.mybatis.core.dialect.AbstractDialect;
import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ColumnType;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.IndexDefinition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;

import javax.persistence.Column;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 把旧的 {@link IParser} 适配成新的 {@link mt.generator.mybatis.core.dialect.Dialect}。
 * <p>
 * 用户自己实现的 {@link IParser} 依然可以继续使用，内部会被包装成方言参与建表流程。
 *
 * @author Martin
 */
public class IParserDialectAdapter extends AbstractDialect {

	private final IParser parser;

	public IParserDialectAdapter(IParser parser) {
		super(parser.getClass().getSimpleName(), 64, "", "");
		this.parser = parser;
	}

	/**
	 * 获取被适配的解析器
	 *
	 * @return 解析器
	 */
	public IParser getParser() {
		return parser;
	}

	@Override
	public boolean supports(String driverClassName) {
		return parser.support(driverClassName);
	}

	@Override
	public String getDatabaseName(String jdbcUrl) {
		return parser.parseDatabaseName(jdbcUrl);
	}

	@Override
	public String getMaintenanceJdbcUrl(String jdbcUrl) {
		return parser.getMasterJdbcUrl(jdbcUrl);
	}

	@Override
	public String renderCreateDatabase(String databaseName) {
		return "CREATE DATABASE " + databaseName;
	}

	@Override
	public String renderColumnType(ColumnDefinition column) {
		if (column == null) {
			return null;
		}
		return parser.getColumnDefinition(synthesizeColumn(column), column.getJavaType());
	}

	@Override
	public String renderIdentityClause(ColumnDefinition column) {
		if (column == null || column.getField() == null) {
			return null;
		}
		return parser.getIdentitySql(column.getField());
	}

	@Override
	public String renderPrimaryKey(List<String> columns) {
		if (columns == null || columns.isEmpty()) {
			return null;
		}
		String sql = parser.getPrimaryKeySql(columns);
		if (StringUtils.isBlank(sql)) {
			return null;
		}
		return sql.replaceFirst("(?i)primary\\s*key\\s*\\(", "PRIMARY KEY (");
	}

	@Override
	public String renderIndex(String tableName, IndexDefinition index) {
		List<String> columns = index.getColumns();
		String legacySql;
		switch (index.getType()) {
			case unique:
				legacySql = parser.getUniqueIndexSql(index.getName(), columns);
				break;
			case fulltext:
				legacySql = parser.getFullTextIndexSql(index.getName(), columns);
				break;
			case hash:
				legacySql = parser.getHashIndexSql(index.getName(), columns);
				break;
			case normal:
			default:
				legacySql = parser.getIndexSql(index.getName(), columns);
				break;
		}
		// 旧接口返回的是表内约束片段，不能直接执行，只有本身是完整语句时才使用
		if (legacySql != null && legacySql.trim().toUpperCase(Locale.ROOT).startsWith("CREATE")) {
			return legacySql;
		}
		return super.renderIndex(tableName, index);
	}

	@Override
	public String renderForeignKey(ForeignKeyDefinition foreignKey) {
		return parser.getForeignKeySql(foreignKey.getTableName(), foreignKey.getReferenceTableName(),
				foreignKey.getColumnName(), synthesizeForeignKey(foreignKey));
	}

	@Override
	protected String getIdentityClause(ColumnDefinition column) {
		return null;
	}

	@Override
	protected String getSimpleType(ColumnType type) {
		return parser.getColumnDefinition(null, javaTypeOf(type));
	}

	/**
	 * 列类型对应的 java 类型
	 *
	 * @param type 列类型
	 * @return java 类型
	 */
	private static Class<?> javaTypeOf(ColumnType type) {
		switch (type) {
			case INTEGER:
				return Integer.class;
			case LONG:
				return Long.class;
			case SHORT:
				return Short.class;
			case BYTE:
				return Byte.class;
			case BOOLEAN:
				return Boolean.class;
			case DOUBLE:
				return Double.class;
			case FLOAT:
				return Float.class;
			case DECIMAL:
				return java.math.BigDecimal.class;
			case DATE:
				return java.util.Date.class;
			case TIME:
				return java.util.Date.class;
			case TIMESTAMP:
				return java.util.Date.class;
			case BINARY:
				return byte[].class;
			default:
				return String.class;
		}
	}

	/**
	 * 把列定义还原成 {@link Column} 注解
	 *
	 * @param column 列定义
	 * @return 注解
	 */
	private Column synthesizeColumn(ColumnDefinition column) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("name", column.getName() == null ? "" : column.getName());
		attributes.put("length", column.getLength());
		attributes.put("precision", column.getPrecision());
		attributes.put("scale", column.getScale());
		attributes.put("nullable", column.isNullable());
		attributes.put("unique", column.isUnique());
		attributes.put("insertable", true);
		attributes.put("updatable", true);
		attributes.put("columnDefinition", column.getColumnDefinition() == null ? "" : column.getColumnDefinition());
		attributes.put("table", "");
		return AnnotationUtils.synthesizeAnnotation(attributes, Column.class, ColumnDefinition.class);
	}

	/**
	 * 把外键定义还原成 {@link ForeignKey} 注解
	 *
	 * @param foreignKey 外键定义
	 * @return 注解
	 */
	private ForeignKey synthesizeForeignKey(ForeignKeyDefinition foreignKey) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("table", foreignKey.getReferenceTableName());
		attributes.put("tableEntity", Object.class);
		attributes.put("referencedColumnName", foreignKey.getReferencedColumnName());
		attributes.put("casecadeType", foreignKey.getCascadeType());
		return AnnotationUtils.synthesizeAnnotation(attributes, ForeignKey.class, ColumnDefinition.class);
	}
}
