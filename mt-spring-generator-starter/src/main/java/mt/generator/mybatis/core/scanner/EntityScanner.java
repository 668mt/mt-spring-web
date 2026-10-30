package mt.generator.mybatis.core.scanner;

import mt.common.annotation.ForeignKey;
import mt.common.annotation.GenerateOrder;
import mt.common.config.CommonProperties;
import mt.common.config.SystemEntity;
import mt.common.entity.DataLock;
import mt.common.entity.IdGenerate;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.IndexType;
import mt.generator.mybatis.annotation.Indexs;
import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.IndexDefinition;
import mt.generator.mybatis.core.model.JavaTypeResolver;
import mt.generator.mybatis.core.model.TableDefinition;
import mt.utils.ClassUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.lang3.StringUtils;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实体扫描器。
 * <p>
 * 负责把实体类翻译成与数据库无关的 {@link TableDefinition}，不涉及任何 SQL 生成。
 *
 * @author Martin
 */
public class EntityScanner {

	/**
	 * 框架内置实体的包名
	 */
	private static final String COMMON_ENTITY_PACKAGE = "mt.common.entity";

	/**
	 * 默认字符串长度
	 */
	private static final int DEFAULT_STRING_LENGTH = 100;

	private final CommonProperties commonProperties;

	public EntityScanner(CommonProperties commonProperties) {
		this.commonProperties = commonProperties;
	}

	/**
	 * 扫描实体类
	 *
	 * @param packages 实体所在的包名
	 * @return 实体类列表
	 */
	public List<Class<?>> scan(String[] packages) {
		Set<Class<?>> entities = new LinkedHashSet<>();
		if (packages != null) {
			for (String entityPackage : packages) {
				if (StringUtils.isBlank(entityPackage)) {
					continue;
				}
				entities.addAll(findEntities(entityPackage));
			}
		}
		entities.addAll(findEntities(COMMON_ENTITY_PACKAGE));
		entities.addAll(SystemEntity.getEntities());
		return new ArrayList<>(entities);
	}

	/**
	 * 查找包下带 {@link Table} 注解的实体
	 *
	 * @param entityPackage 包名
	 * @return 实体类列表
	 */
	public List<Class<?>> findEntities(String entityPackage) {
		List<Class<?>> classes = ClassUtils.getClasses(entityPackage);
		List<Class<?>> entities = new ArrayList<>();
		for (Class<?> entityClass : classes) {
			if (entityClass != null && entityClass.getAnnotation(Table.class) != null) {
				entities.add(entityClass);
			}
		}
		return entities;
	}

	/**
	 * 获取表名
	 *
	 * @param entityClass 实体类
	 * @return 表名
	 */
	public String resolveTableName(Class<?> entityClass) {
		if (entityClass.equals(IdGenerate.class)) {
			return commonProperties == null ? "idGenerate" : commonProperties.getIdGenerateTableName();
		}
		if (entityClass.equals(DataLock.class)) {
			return commonProperties == null ? "dataLock" : commonProperties.getDataLockTableName();
		}
		Table table = entityClass.getAnnotation(Table.class);
		if (table != null && StringUtils.isNotBlank(table.name())) {
			return table.name();
		}
		return entityClass.getSimpleName();
	}

	/**
	 * 实体类 -&gt; 表定义
	 *
	 * @param entityClass 实体类
	 * @return 表定义
	 */
	public TableDefinition toTableDefinition(Class<?> entityClass) {
		String tableName = resolveTableName(entityClass);
		List<Field> fields = ReflectUtils.findAllFieldsIgnore(entityClass, Transient.class);
		fields.sort(Comparator.comparingInt(EntityScanner::getGenerateOrder).reversed());

		List<ColumnDefinition> columns = new ArrayList<>();
		List<String> primaryKeyColumns = new ArrayList<>();
		List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
		for (Field field : fields) {
			int modifiers = field.getModifiers();
			if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
				continue;
			}
			ColumnDefinition column = toColumnDefinition(field);
			columns.add(column);
			if (field.getAnnotation(Id.class) != null) {
				primaryKeyColumns.add(column.getName());
			}
			ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
			if (foreignKey != null) {
				foreignKeys.add(toForeignKeyDefinition(tableName, field, column, foreignKey));
			}
		}

		return TableDefinition.builder()
				.name(tableName)
				.columns(columns)
				.primaryKeyColumns(primaryKeyColumns)
				.indexes(toIndexDefinitions(entityClass))
				.foreignKeys(foreignKeys)
				.build();
	}

	/**
	 * 字段 -&gt; 列定义
	 *
	 * @param field 字段
	 * @return 列定义
	 */
	public ColumnDefinition toColumnDefinition(Field field) {
		Column column = field.getAnnotation(Column.class);
		Class<?> type = field.getType();
		boolean nullable = true;
		boolean unique = false;
		String columnDefinition = null;
		int length = DEFAULT_STRING_LENGTH;
		int precision = 0;
		int scale = 0;
		if (column != null) {
			nullable = column.nullable();
			unique = column.unique();
			columnDefinition = column.columnDefinition();
			length = column.length() > 0 ? column.length() : DEFAULT_STRING_LENGTH;
			precision = column.precision();
			scale = column.scale();
		}
		boolean autoIncrement = false;
		KeySql keySql = field.getAnnotation(KeySql.class);
		if (keySql != null && keySql.useGeneratedKeys()) {
			autoIncrement = true;
		}
		return ColumnDefinition.builder()
				.name(resolveColumnName(field))
				.javaType(type)
				.columnType(JavaTypeResolver.resolve(type))
				.columnDefinition(columnDefinition)
				.nullable(nullable)
				.unique(unique)
				.autoIncrement(autoIncrement)
				.length(length)
				.precision(precision)
				.scale(scale)
				.field(field)
				.build();
	}

	/**
	 * 获取列名
	 *
	 * @param field 字段
	 * @return 列名
	 */
	public String resolveColumnName(Field field) {
		Column column = field.getAnnotation(Column.class);
		if (column != null && StringUtils.isNotBlank(column.name())) {
			return column.name();
		}
		return MapperColumnUtils.parseColumn(field.getName());
	}

	/**
	 * 生成顺序，值大的排前面
	 *
	 * @param field 字段
	 * @return 顺序
	 */
	private static int getGenerateOrder(Field field) {
		GenerateOrder annotation = field.getAnnotation(GenerateOrder.class);
		return annotation == null ? 0 : annotation.value();
	}

	/**
	 * 解析索引注解
	 *
	 * @param entityClass 实体类
	 * @return 索引定义列表
	 */
	private List<IndexDefinition> toIndexDefinitions(Class<?> entityClass) {
		List<Index> annotations = new ArrayList<>();
		Indexs indexs = entityClass.getAnnotation(Indexs.class);
		if (indexs != null) {
			annotations.addAll(Arrays.asList(indexs.value()));
		}
		Index index = entityClass.getAnnotation(Index.class);
		if (index != null) {
			annotations.add(index);
		}
		List<IndexDefinition> definitions = new ArrayList<>();
		for (Index annotation : annotations) {
			List<String> columns = Arrays.stream(annotation.columns())
					.map(column -> MapperColumnUtils.parseColumn(column, entityClass))
					.collect(Collectors.toList());
			definitions.add(IndexDefinition.builder()
					.name(annotation.name())
					.columns(columns)
					.type(annotation.type() == null ? IndexType.normal : annotation.type())
					.build());
		}
		return definitions;
	}

	/**
	 * 解析外键注解
	 *
	 * @param tableName  从表名
	 * @param field      字段
	 * @param column     列定义
	 * @param foreignKey 外键注解
	 * @return 外键定义
	 */
	private ForeignKeyDefinition toForeignKeyDefinition(String tableName, Field field, ColumnDefinition column,
														ForeignKey foreignKey) {
		Class<?> tableEntity = foreignKey.tableEntity();
		if (tableEntity == null || Object.class.equals(tableEntity)) {
			tableEntity = null;
		}
		String referenceTableName = foreignKey.table();
		if (StringUtils.isBlank(referenceTableName)) {
			if (tableEntity == null) {
				throw new IllegalStateException("@ForeignKey 的 table 和 tableEntity 不能同时为空："
						+ field.getDeclaringClass().getName() + "#" + field.getName());
			}
			referenceTableName = resolveTableName(tableEntity);
		}
		String referencedColumnName = foreignKey.referencedColumnName();
		String name = "FK_" + tableName + "_" + column.getName() + "_" + referenceTableName + "_" + referencedColumnName;
		return ForeignKeyDefinition.builder()
				.name(name)
				.tableName(tableName)
				.columnName(column.getName())
				.referenceTableName(referenceTableName)
				.referencedColumnName(referencedColumnName)
				.cascadeType(foreignKey.casecadeType())
				.build();
	}
}
