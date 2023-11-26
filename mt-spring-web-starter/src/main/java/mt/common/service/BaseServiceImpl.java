package mt.common.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import mt.common.annotation.Filter;
import mt.common.converter.Converter;
import mt.common.entity.BaseCondition;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.mybatis.utils.MyBatisUtils;
import mt.common.starter.message.utils.MessageUtils;
import mt.common.tkmapper.CustomConditionFilterParser;
import mt.common.tkmapper.DefaultCustomConditionFilterParser;
import mt.common.tkmapper.Filter.Operator;
import mt.common.utils.SpringUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import tk.mybatis.mapper.common.BaseMapper;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Martin
 */
public abstract class BaseServiceImpl<T> implements BaseService<T> {
	private mt.common.mybatis.mapper.BaseMapper<T> baseMapper;
	@Autowired
	private ApplicationContext applicationContext;
	
	@Override
	public boolean notExists(String columnName, Object value) {
		return !exists(columnName, value);
	}
	
	/**
	 * 获取数据库Dao
	 *
	 * @return
	 */
	public mt.common.mybatis.mapper.BaseMapper<T> getBaseMapper() {
		if (baseMapper == null) {
			synchronized (this) {
				if (baseMapper == null) {
					Assert.notNull(applicationContext, "applicationContext还未初始化完成");
					Class<T> entityClass = getEntityClass();
					Map<String, BaseMapper> beansOfType = applicationContext.getBeansOfType(BaseMapper.class);
					BaseMapper baseMapper = beansOfType.values()
						.stream()
						.filter(baseMapper1 -> entityClass.equals(BaseMapperHelper.getBaseMapperGenericType(baseMapper1)))
						.findFirst()
						.orElse(null);
					Assert.notNull(baseMapper, entityClass.getSimpleName() + "的BaseMapper还未初始化完成，请避免在init方法调用jdbc操作或复写getBaseMapper方法");
					this.baseMapper = (mt.common.mybatis.mapper.BaseMapper<T>) baseMapper;
				}
			}
		}
		return baseMapper;
	}
	
	@Override
	public PageInfo<T> findPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, @Nullable Object condition) {
		if (condition == null) {
			condition = new BaseCondition();
		}
		Class<T> entityClass = getEntityClass();
		List<mt.common.tkmapper.Filter> filters = parseCondition(condition);
		return doPage(pageNum, pageSize, orderBy, () -> getBaseMapper().selectByExample(MyBatisUtils.createExample(entityClass, filters)));
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List<mt.common.tkmapper.Filter> parseCondition(Object condition) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		List<String> condition2 = ReflectUtils.getValue(condition, "condition", List.class);
		if (CollectionUtils.isNotEmpty(condition2)) {
			for (String sql : condition2) {
				filters.add(new mt.common.tkmapper.Filter(sql, Operator.condition));
			}
		}
		if (condition != null) {
			List<Field> fields = ReflectUtils.findAllFields(condition.getClass(), Filter.class);
			for (Field field : fields) {
				field.setAccessible(true);
				//http参数值
				Object value;
				try {
					value = field.get(condition);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				if (value instanceof List) {
					if (CollectionUtils.isEmpty((List<?>) value)) {
						continue;
					}
				}
				if (value instanceof Object[]) {
					if (ArrayUtils.isEmpty((Object[]) value)) {
						continue;
					}
				}
				Filter annotation = AnnotatedElementUtils.getMergedAnnotation(field, Filter.class);
				Assert.notNull(annotation, "filter注解不能为空");
				Class<? extends CustomConditionFilterParser<?, ?>> customParserClass = annotation.customParserClass();
				if (!DefaultCustomConditionFilterParser.class.equals(customParserClass)) {
					CustomConditionFilterParser parser = SpringUtils.getBean(customParserClass);
					List<mt.common.tkmapper.Filter> list = parser.parseFilters(condition, value);
					if (CollectionUtils.isNotEmpty(list)) {
						filters.addAll(list);
					}
					continue;
				}
				
				if (value != null && !"".equals((value + "").trim())) {
					//获取注解
					String column = StringUtils.isNotBlank(annotation.column()) ? annotation.column() : MapperColumnUtils.parseColumn(field.getName());
					Operator operator = annotation.operator();
					String prefix = annotation.prefix();
					String suffix = annotation.suffix();
					Class<? extends Converter<?>> converterClass = annotation.converter();
//					String conditionScript = annotation.condition();
//					conditionScript = MessageUtils.replaceVariable(conditionScript, condition, false);
//					try {
//						Boolean conditionResult = JsUtils.eval(conditionScript);
//						if (!conditionResult) {
//							continue;
//						}
//					} catch (ScriptException e) {
//						throw new RuntimeException(e);
//					}
					
					switch (operator) {
						case condition:
							String sql = annotation.sql();
							if (StringUtils.isNotBlank(sql)) {
								filters.add(new mt.common.tkmapper.Filter(MessageUtils.replaceVariable(sql, condition), operator));
							} else {
								//替换变量
								filters.add(new mt.common.tkmapper.Filter(MessageUtils.replaceVariable(column, condition), operator, value));
							}
							break;
						default:
							Map<String, ? extends Converter<?>> beansOfType = SpringUtils.getBeansOfType(converterClass);
							Converter converter;
							if (beansOfType.size() > 0) {
								converter = beansOfType.values().iterator().next();
							} else {
								try {
									converter = converterClass.newInstance();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}
							value = converter.convert(value);
							if (StringUtils.isNotBlank(prefix)) {
								value = prefix + value;
							}
							if (StringUtils.isNotBlank(suffix)) {
								value = value + suffix;
							}
							filters.add(new mt.common.tkmapper.Filter(column, operator, value));
							break;
					}
				}
			}
		}
		return filters;
	}
	
	
	@Override
	public <T2> PageInfo<T2> doPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, GetList<T2> getList) {
		Class<T> entityClass = getEntityClass();
		//分页
		Page<T2> page = null;
		try {
			if (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0) {
				page = PageHelper.startPage(pageNum, pageSize);
			}
			//排序
			String realOrderBy = null;
			if (orderBy != null) {
				realOrderBy = orderBy;
			} else {
				List<Field> ids = ReflectUtils.findFields(entityClass, Id.class);
				if (CollectionUtils.isNotEmpty(ids)) {
					List<String> orderBys = new ArrayList<>();
					for (Field id : ids) {
						Column column = id.getAnnotation(Column.class);
						String columnName = id.getName();
						if (column != null && StringUtils.isNotBlank(column.name())) {
							columnName = column.name();
						}
						orderBys.add(MapperColumnUtils.parseColumn(columnName) + " desc");
					}
					realOrderBy = StringUtils.join(orderBys, ",");
				}
			}
			if (StringUtils.isNotBlank(realOrderBy) && page != null) {
				page.setUnsafeOrderBy(realOrderBy);
			}
			return new PageInfo<>(getList.getList());
		} finally {
			if (page != null) {
				page.close();
			}
		}
	}
	
	@Override
	public List<T> findAll() {
		return getBaseMapper().selectAll();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int count(List<mt.common.tkmapper.Filter> filters) {
		Assert.notNull(filters);
		return getBaseMapper().selectCountByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@SuppressWarnings("unchecked")
	public Class<T> getEntityClass() {
		return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	@SuppressWarnings("unchecked")
	public <T2> Class<T2> getEntityClass(Class<? extends BaseMapper<T2>> class1) {
		return (Class<T2>) ((ParameterizedType) class1.getGenericInterfaces()[0]).getActualTypeArguments()[0];
	}
	
	@Override
	public boolean exists(String columnName, Object value) {
		List<T> list = findList(columnName, value);
		return CollectionUtils.isNotEmpty(list);
	}
	
	@Override
	public boolean existsId(Object record) {
		return getBaseMapper().existsWithPrimaryKey(record);
	}
	
	@Override
	public List<T> findByFilter(mt.common.tkmapper.Filter filter) {
		return findByFilter(filter, false);
	}
	
	@Override
	public List<T> findByFilter(mt.common.tkmapper.Filter filter, boolean forUpdate) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(filter);
		return findByFilters(filters, forUpdate);
	}
	
	@Override
	public T findById(Object record) {
		return getBaseMapper().selectByPrimaryKey(record);
	}
	
	@Override
	public T findOne(String column, Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(column, Operator.eq, value));
		return findOneByFilters(filters);
	}
	
	@Override
	public List<T> findList(String column, Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(column, Operator.eq, value));
		return findByFilters(filters);
	}
	
	@Override
	public List<T> findByFilters(List<mt.common.tkmapper.Filter> filters) {
		return findByFilters(filters, false);
	}
	
	@Override
	public List<T> findByFilters(List<mt.common.tkmapper.Filter> filters, boolean forUpdate) {
		return getBaseMapper().selectByExample(MyBatisUtils.createExample(getEntityClass(), filters, forUpdate));
	}
	
	
	@Override
	public T findOneByFilters(List<mt.common.tkmapper.Filter> filters, boolean forUpdate) {
		List<T> findByFilters = findByFilters(filters, forUpdate);
		if (CollectionUtils.isEmpty(findByFilters)) {
			return null;
		}
		if (findByFilters.size() > 1) {
			throw new RuntimeException("findOneByFilters 查询出多个结果！");
		}
		return findByFilters.get(0);
	}
	
	@Override
	public T findOneByFilters(List<mt.common.tkmapper.Filter> filters) {
		return findOneByFilters(filters, false);
	}
	
	@Override
	public T findOneByFilter(mt.common.tkmapper.Filter filter, boolean forUpdate) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(filter);
		return findOneByFilters(filters, forUpdate);
	}
	
	@Override
	public T findOneByFilter(mt.common.tkmapper.Filter filter) {
		return findOneByFilter(filter, false);
	}
	
	@Override
	public int save(T record) {
		return getBaseMapper().insert(record);
	}
	
	@Override
	public int saveSelective(T record) {
		return getBaseMapper().insert(record);
	}
	
	@Override
	public int updateById(T record) {
		return getBaseMapper().updateByPrimaryKey(record);
	}
	
	@Override
	public int updateByIdSelective(T record) {
		return getBaseMapper().updateByPrimaryKeySelective(record);
	}
	
	@Override
	public int updateByFilter(T record, mt.common.tkmapper.Filter filter) {
		return updateByFilters(record, Collections.singletonList(filter));
	}
	
	@Override
	public int updateByFilterSelective(T record, mt.common.tkmapper.Filter filter) {
		return updateByFiltersSelective(record, Collections.singletonList(filter));
	}
	
	@Override
	public int updateByFilters(T record, List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().updateByExample(record, MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	public int updateByFiltersSelective(T record, List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().updateByExampleSelective(record, MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	public int deleteById(Object record) {
		return getBaseMapper().deleteByPrimaryKey(record);
	}
	
	@Override
	public int deleteByIds(Object[] records) {
		int update = 0;
		for (Object id : records) {
			update += getBaseMapper().deleteByPrimaryKey(id);
		}
		return update;
	}
	
	@Override
	public int deleteByFilters(List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().deleteByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	public int deleteByFilter(mt.common.tkmapper.Filter filter) {
		return deleteByFilters(Collections.singletonList(filter));
	}
	
	@Override
	public int delete(String columnName, Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(columnName, Operator.eq, value));
		return deleteByFilters(filters);
	}
	
	@Override
	public boolean existsByFilters(List<mt.common.tkmapper.Filter> filters) {
		List<T> byFilters = findByFilters(filters);
		return CollectionUtils.isNotEmpty(byFilters);
	}
	
	@Override
	public boolean existsByFilter(mt.common.tkmapper.Filter filter) {
		return CollectionUtils.isNotEmpty(findByFilter(filter));
	}
	
	@Override
	public boolean notExistsId(Object record) {
		return !existsId(record);
	}
	
	@Override
	public boolean notExistsByFilters(List<mt.common.tkmapper.Filter> filters) {
		return !existsByFilters(filters);
	}
	
	@Override
	public boolean notExistsByFilter(mt.common.tkmapper.Filter filter) {
		return !existsByFilter(filter);
	}
}
