package mt.generator.mybatis.utils;

import lombok.extern.slf4j.Slf4j;
import mt.common.annotation.ForeignKey;
import mt.common.annotation.GenerateOrder;
import mt.common.config.CommonProperties;
import mt.common.config.SystemEntity;
import mt.common.entity.DataLock;
import mt.common.entity.IdGenerate;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.utils.SpringUtils;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.Indexs;
import mt.generator.mybatis.annotation.UniqueIndex;
import mt.generator.mybatis.annotation.UniqueIndexs;
import mt.utils.ClassUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实体工具类
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2017-10-22 下午3:37:37
 */
@Slf4j
public class GenerateHelper {
	/**
	 * 外键关系
	 */
	public List<String> foreightKeys = new ArrayList<>();
	
	public boolean createDatabase() {
		String databaseName = iParser.parseDatabaseName(jdbcUrl);
		String masterJdbcUrl = iParser.getMasterJdbcUrl(jdbcUrl);
		Jdbc masterJdbc = new Jdbc(driverClass, masterJdbcUrl, user, password);
		if (!iParser.isExistDatabase(databaseName, masterJdbc)) {
			log.info("初始化自动创建数据库...");
			masterJdbc.execute("create database " + databaseName);
			log.info("创建数据库：" + databaseName);
			return true;
		}
		return false;
	}
	
	/**
	 * 初始化自动创建表
	 *
	 * @param entityPackages
	 * @param afterInitEvent
	 */
	public void init(String[] entityPackages, AfterInitEvent afterInitEvent) {
		Assert.notNull(afterInitEvent, "afterInitEvent事件不能为空");
		//检查数据库
		afterInitEvent.setCreateDatabase(createDatabase());
		
		Set<Class<?>> allEntitys = new HashSet<>();
		if (entityPackages != null) {
			for (String entityPackage : entityPackages) {
				allEntitys.addAll(getAllEntitys(entityPackage));
			}
		}
		allEntitys.addAll(getAllEntitys("mt.common.entity"));
		allEntitys.addAll(SystemEntity.getEntities());
		
		List<String> list = new ArrayList<>();
		for (Class<?> entityClass : allEntitys) {
			String tableName = getTableName(entityClass);
			if (!iParser.isExistTable(tableName, iParser.parseDatabaseName(jdbcUrl), jdbc)) {
				//建表
				jdbc.execute(getCreateTableSql(entityClass));
				list.add(tableName);
			}
		}
		List<String> foreightSuccess = new ArrayList<>();
		for (String sql : foreightKeys) {
			//建外键
			jdbc.execute(sql);
			foreightSuccess.add(sql);
		}
		
		//创建的表
		afterInitEvent.setNewTables(list);
		//创建的主外键关系
		afterInitEvent.setNewForeightKeys(foreightSuccess);
		afterInitEvent.setCreateTable(CollectionUtils.isNotEmpty(list));
		afterInitEvent.setCreateForeightKey(CollectionUtils.isNotEmpty(foreightSuccess));
		
		
		log.info("新建表数量：" + list.size());
		for (String table : list) {
			log.info(">>>>>>>>>" + table);
		}
		log.info("新建外键数量：" + foreightSuccess.size());
		for (String sql : foreightSuccess) {
			log.info(">>>>>>>>>" + sql);
		}
	}
	
	/**
	 * 获取表名
	 *
	 * @param entityClass
	 * @return
	 */
	public String getTableName(Class<?> entityClass) {
		if (entityClass.equals(IdGenerate.class)) {
			return SpringUtils.getBean(CommonProperties.class).getIdGenerateTableName();
		}
		if (entityClass.equals(DataLock.class)) {
			return SpringUtils.getBean(CommonProperties.class).getDataLockTableName();
		}
		Table table = entityClass.getAnnotation(Table.class);
		String tableName = entityClass.getSimpleName();
		if (table != null) {
			if (StringUtils.isNotBlank(table.name())) {
				tableName = table.name();
			}
		}
		return tableName;
	}
	
	/**
	 * 拼接sql语句
	 *
	 * @param field
	 * @return
	 */
	public String getJdbcTypeSql(Field field) {
		Column column = field.getAnnotation(Column.class);
		boolean nullable = true;
		String name = getColumnName(field);
		boolean unique = false;
		String columnDefinition = getColumnDefinition(field);
		
		if (column != null) {
			nullable = column.nullable();
			unique = column.unique();
		}
		String nullSql = nullable ? "" : "not null";
		String uniqueSql = unique ? "unique" : "";
		return " " + name + " " + " " + columnDefinition + " " + nullSql + " " + uniqueSql;
	}
	
	public String getColumnName(Field field) {
		Column column = field.getAnnotation(Column.class);
		String name = field.getName();
		if (column != null && StringUtils.isNotBlank(column.name())) {
			return column.name();
		}
		return MapperColumnUtils.parseColumn(name);
	}
	
	/**
	 * 获取数据库类型  例：varchar(100)
	 *
	 * @param field
	 * @return
	 */
	public String getColumnDefinition(Field field) {
		Column column = field.getAnnotation(Column.class);
		String defaultColumnDefinition = iParser.getColumnDefinition(column, field.getType());
		String identitySql = iParser.getIdentitySql(field);
		if (identitySql == null) {
			identitySql = "";
		}
		return defaultColumnDefinition + " " + identitySql;
	}
	
	/**
	 * 拼接如果不存在创建表语句
	 *
	 * @param entityClass
	 * @return
	 */
	public String getCreateTableSql(Class<?> entityClass) {
		StringBuilder sb = new StringBuilder();
		String tableName = getTableName(entityClass);
		sb.append("create table ").append(tableName).append(" (\r\n");
		List<Field> findAllFields = ReflectUtils.findAllFieldsIgnore(entityClass, Transient.class);
		findAllFields.sort((o1, o2) -> {
			GenerateOrder annotation1 = o1.getAnnotation(GenerateOrder.class);
			int order1 = annotation1 == null ? 0 : annotation1.value();
			GenerateOrder annotation2 = o2.getAnnotation(GenerateOrder.class);
			int order2 = annotation2 == null ? 0 : annotation2.value();
			return order2 - order1;
		});
		for (Field field : findAllFields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				sb.append("\t").append(getJdbcTypeSql(field)).append(",\r\n");
			}
		}
		//添加主外键信息
		List<String> ids = findAllFields.stream().filter(field -> field.getAnnotation(Id.class) != null).map(this::getColumnName).collect(Collectors.toList());
		String primaryKeySql = iParser.getPrimaryKeySql(ids);
		if (StringUtils.isNotBlank(primaryKeySql)) {
			sb.append("\t").append(primaryKeySql).append(",\r\n");
		}
		List<UniqueIndex> uniqueIndexList = new ArrayList<>();
		UniqueIndexs uniqueIndexs = entityClass.getAnnotation(UniqueIndexs.class);
		if (uniqueIndexs != null) {
			UniqueIndex[] indexs = uniqueIndexs.value();
			uniqueIndexList.addAll(Arrays.asList(indexs));
		}
		UniqueIndex uniqueIndex = entityClass.getAnnotation(UniqueIndex.class);
		if (uniqueIndex != null) {
			uniqueIndexList.add(uniqueIndex);
		}
		uniqueIndexList.forEach(u -> {
			List<String> columns = Arrays.stream(u.columns()).map(s -> MapperColumnUtils.parseColumn(s, entityClass)).collect(Collectors.toList());
			String uniqueSql = iParser.getUniqueIndexSql(u.name(), columns);
			if (StringUtils.isNotBlank(uniqueSql)) {
				sb.append("\t").append(uniqueSql).append(",\r\n");
			}
		});
		List<Index> indexList = new ArrayList<>();
		Indexs indexs = entityClass.getAnnotation(Indexs.class);
		if (indexs != null) {
			indexList.addAll(Arrays.asList(indexs.value()));
		}
		Index index = entityClass.getAnnotation(Index.class);
		if (index != null) {
			indexList.add(index);
		}
		indexList.forEach(i -> {
			List<String> columns = Arrays.stream(i.columns()).map(s -> MapperColumnUtils.parseColumn(s, entityClass)).collect(Collectors.toList());
			String indexSql = iParser.getIndexSql(i.name(), columns);
			if (StringUtils.isNotBlank(indexSql)) {
				sb.append("\t").append(indexSql).append(",\r\n");
			}
		});
		
		//获取主外键信息
		for (Field field : findAllFields) {
			if (!Modifier.isFinal(field.getModifiers())) {
				ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
				if (foreignKey != null) {
					Class<?> aClass = foreignKey.tableEntity();
					String targetTable = foreignKey.table();
					if (StringUtils.isBlank(targetTable)) {
						Assert.notNull(aClass, "@ForeignKey的tableEntity和table不能同时为空");
						targetTable = getTableName(aClass);
					}
					String foreignKeySql = iParser.getForeignKeySql(tableName, targetTable, getColumnName(field), foreignKey);
					foreightKeys.add(foreignKeySql);
				}
			}
		}
		String sql = sb.toString().trim();
		if (sql.endsWith(",")) {
			sql = sql.substring(0, sql.length() - 1);
		}
		return sql + "\r\n)";
	}
	
	/**
	 * 获取所有带@Table注解的实体
	 *
	 * @return
	 */
	public List<Class<?>> getAllEntitys(String entityPackage) {
		List<Class<?>> classes = ClassUtils.getClasses(entityPackage);
		List<Class<?>> entitys = new ArrayList<>();
		CollectionUtils.select(classes, object -> {
			Class<?> class1 = (Class<?>) object;
			return class1 != null && class1.getAnnotation(Table.class) != null;
		}, entitys);
		return entitys;
	}
	
	private final String jdbcUrl;
	private final String driverClass;
	private final String user;
	private final String password;
	private final IParser iParser;
	private final Jdbc jdbc;
	
	public GenerateHelper(String jdbcUrl, String driverClass, String user,
						  String password, IParser iParser) {
		this.jdbcUrl = jdbcUrl;
		this.driverClass = driverClass;
		this.user = user;
		this.password = password;
		Assert.state(iParser.support(driverClass), "iParser[" + iParser + "] not support " + driverClass);
		this.iParser = iParser;
		this.jdbc = new Jdbc(driverClass, jdbcUrl, user, password);
	}
	
	
}
