package mt.common.mybatis;

import lombok.extern.slf4j.Slf4j;
import mt.common.annotation.GenerateClass;
import mt.common.annotation.IdGenerator;
import mt.common.service.IdGenerateService;
import mt.common.utils.SpringUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * @Author Martin
 * @Date 2018/6/30
 */
@Slf4j
public class InterceptorHelper {
	/**
	 * 数据源绑定主键生成器
	 */
	private static Map<DataSource, IdGenerateService> idGenerateServiceMaps = new HashMap<>();
	/**
	 * 默认主键生成器
	 */
	private static IdGenerateService primaryIdGenerateService;
	/**
	 * jdbcTemplate缓存
	 */
	private static Map<DataSource, JdbcTemplate> jdbcTemplateMap = new HashMap<>();
	
	/**
	 * 绑定主键生成器
	 *
	 * @param dataSource        数据源
	 * @param idGenerateService 主键生成器
	 */
	public static void registIdGenerateService(DataSource dataSource, IdGenerateService idGenerateService) {
		idGenerateServiceMaps.put(dataSource, idGenerateService);
	}
	
	/**
	 * 获取主键生成器
	 *
	 * @param dataSource
	 * @return
	 */
	private static IdGenerateService getIdGenerateService(DataSource dataSource) {
		if (primaryIdGenerateService == null) {
			//注册默认的主键生成器
			primaryIdGenerateService = SpringUtils.getBean(IdGenerateService.class);
			registIdGenerateService(SpringUtils.getBean(DataSource.class), primaryIdGenerateService);
		}
		//获取数据源对应的主键生成器
		IdGenerateService idGenerateService = idGenerateServiceMaps.get(dataSource);
		Assert.notNull(idGenerateService, "请为第二数据源设置idGenerateService");
		return idGenerateService;
	}

//	/**
//	 * 获取参数
//	 *
//	 * @return
//	 */
//	private static CommonProperties getCommonProperties() {
//		if (commonProperties == null) {
//			commonProperties = SpringUtils.getBean(CommonProperties.class);
//		}
//		return commonProperties;
//	}
	
	/**
	 * 是否是保存操作
	 *
	 * @param ms
	 * @return
	 */
	public static boolean isSave(MappedStatement ms, Object parameters) {
		SqlCommandType sqlCommandType = ms.getSqlCommandType();
		return SqlCommandType.INSERT == sqlCommandType;
//		BoundSql boundSql = ms.getBoundSql(parameters);
//		String sql = boundSql.getSql();
//		return sql.toLowerCase().trim().startsWith("insert");
	}
	
	/**
	 * 是否是更新操作
	 *
	 * @param ms
	 * @return
	 */
	public static boolean isUpdate(MappedStatement ms, Object parameters) {
		SqlCommandType sqlCommandType = ms.getSqlCommandType();
		return SqlCommandType.UPDATE == sqlCommandType;
//		BoundSql boundSql = ms.getBoundSql(parameters);
//		String sql = boundSql.getSql();
//		return sql.toLowerCase().trim().startsWith("update");
	}
	
	/**
	 * 生成主键
	 *
	 * @param tableName     表名
	 * @param idGenerator   生成器注解
	 * @param generateClass 使用的生成类
	 * @param type          生成主键类型
	 * @param invocation
	 * @param <T>           主键类型
	 * @return 主键
	 * @throws Exception
	 */
	public static <T> T generateId(@NotNull String tableName, @NotNull IdGenerator idGenerator, @Nullable GenerateClass generateClass, @NotNull Class<T> type, @NotNull Invocation invocation) throws Exception {
		DataSource dataSource = getDataSource(invocation);
		JdbcTemplate jdbcTemplate = jdbcTemplateMap.get(dataSource);
		if (jdbcTemplate == null) {
			jdbcTemplate = new JdbcTemplate(dataSource);
			jdbcTemplateMap.put(dataSource, jdbcTemplate);
		}
		
		return getIdGenerateService(dataSource).generate(tableName, idGenerator, generateClass, type, jdbcTemplate);
	}
	
	public static String getTableName(Object parameter) {
		Table annotation = AnnotatedElementUtils.getMergedAnnotation(parameter.getClass(), Table.class);
		return annotation != null && StringUtils.isNotBlank(annotation.name()) ? annotation.name() : parameter.getClass().getSimpleName();
	}
	
	public static Map<String, Object> findByPrimaryKey(Object parameter, Invocation invocation) throws Exception {
		DataSource dataSource = getDataSource(invocation);
		JdbcTemplate jdbcTemplate = jdbcTemplateMap.get(dataSource);
		if (jdbcTemplate == null) {
			jdbcTemplate = new JdbcTemplate(dataSource);
			jdbcTemplateMap.put(dataSource, jdbcTemplate);
		}
		
		//获取主键字段
		List<Field> idFields = ReflectUtils.findAllFields(parameter.getClass(), Id.class);
		if (CollectionUtils.isEmpty(idFields)) {
			return new HashMap<>();
		}
		try {
			Map<String, Object> primaryKeys = new HashMap<>();
			for (Field idField : idFields) {
				idField.setAccessible(true);
				Object idValue = idField.get(parameter);
				//主键为空，直接返回空数据
				if (idValue == null) {
					return new HashMap<>();
				}
				primaryKeys.put(idField.getName(), idValue);
			}
			String tableName = getTableName(parameter);
			StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1=1");
			List<Object> args = new ArrayList<>();
			for (Map.Entry<String, Object> entry : primaryKeys.entrySet()) {
				sql.append(" and ").append(entry.getKey()).append(" = ?");
				args.add(entry.getValue());
			}
			return jdbcTemplate.queryForMap(sql.toString(), args.toArray());
		} catch (EmptyResultDataAccessException e2) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static DataSource getDataSource(Invocation invocation) throws Exception {
		Executor executor = (Executor) invocation.getTarget();
		SpringManagedTransaction springManagedTransaction = (SpringManagedTransaction) executor.getTransaction();
		Field dataSource1 = SpringManagedTransaction.class.getDeclaredField("dataSource");
		dataSource1.setAccessible(true);
		return (DataSource) dataSource1.get(springManagedTransaction);
	}
	
	public static abstract class AbstractValueGenerator<T> {
		@SuppressWarnings("unchecked")
		public final Class<T> getTClass() {
			return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}
		
		public abstract T getValue(Field field);
	}
	
	@SuppressWarnings("rawtypes")
	public static void setFieldsValue(Object entity, Class<? extends Annotation> annotation, boolean force, AbstractValueGenerator<?> valueGenerator) throws IllegalAccessException {
		
		if (entity instanceof DefaultSqlSession.StrictMap<?>) {
			DefaultSqlSession.StrictMap<?> strictMap = (DefaultSqlSession.StrictMap<?>) entity;
			Object o = strictMap.get("collection");
			if (o instanceof Collection<?>) {
				Collection collection = (Collection) o;
				for (Object o1 : collection) {
					setFieldsValue(o1, annotation, force, valueGenerator);
				}
			}
			Object l = strictMap.get("list");
			if (l instanceof List) {
				List list = (List) l;
				for (Object o1 : list) {
					setFieldsValue(o1, annotation, force, valueGenerator);
				}
			}
			return;
		}
		
		if (entity instanceof Collection) {
			Collection collection = (Collection) entity;
			for (Object o : collection) {
				setFieldsValue(o, annotation, force, valueGenerator);
			}
			return;
		}
		
		if (entity instanceof MapperMethod.ParamMap<?>) {
			MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) entity;
			if (paramMap.containsKey("list")) {
				Object list = paramMap.get("list");
				if (list instanceof Collection<?>) {
					Collection collection = (Collection) list;
					for (Object o : collection) {
						setFieldsValue(o, annotation, force, valueGenerator);
					}
				}
			} else if (paramMap.containsKey("record")) {
				Object record = paramMap.get("record");
				setFieldsValue(record, annotation, force, valueGenerator);
			}
			return;
		}
		
		//单个对象
		List<Field> createdByFields = ReflectUtils.findAllFields(entity.getClass(), annotation);
		if (CollectionUtils.isEmpty(createdByFields)) {
			return;
		}
		//数据库对象
		for (Field field : createdByFields) {
			if (!field.getType().isAssignableFrom(valueGenerator.getTClass())) {
				continue;
			}
			field.setAccessible(true);
			Object fieldValue = field.get(entity);
			if (!force && fieldValue != null) {
				continue;
			}
			Object value = valueGenerator.getValue(field);
			if (value == null) {
				continue;
			}
			try {
				field.set(entity, ConvertUtils.convert(value, field.getType()));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
