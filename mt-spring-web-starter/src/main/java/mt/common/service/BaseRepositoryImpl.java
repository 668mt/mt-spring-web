package mt.common.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.SneakyThrows;
import mt.common.annotation.Filter;
import mt.common.converter.Converter;
import mt.common.entity.PageCondition;
import mt.common.entity.Pageable;
import mt.common.mybatis.advanced.AdvancedQuery;
import mt.common.mybatis.entity.GroupCount;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.mybatis.utils.MyBatisUtils;
import mt.common.starter.message.utils.MessageUtils;
import mt.common.tkmapper.ConditionFilterParser;
import mt.common.tkmapper.DefaultConditionFilterParser;
import mt.common.tkmapper.Operator;
import mt.common.utils.SpringUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import tk.mybatis.mapper.common.BaseMapper;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Consumer;

import static mt.common.utils.EntityUtils.getIdFilters;

/**
 * @author Martin
 */
public abstract class BaseRepositoryImpl<T> implements BaseRepository<T>, ApplicationContextAware {
	private volatile mt.common.mybatis.mapper.BaseMapper<T> baseMapper;
	private ApplicationContext applicationContext;
	
	@Override
	public boolean notExists(@NotNull String columnName, @NotNull Object value) {
		return !exists(columnName, value);
	}
	
	@Override
	public void setApplicationContext(@NotNull ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * 获取数据库Dao
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
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
						.filter(baseMapper1 -> baseMapper1 instanceof mt.common.mybatis.mapper.BaseMapper<?>)
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
	public PageInfo<T> findPage(@Nullable Pageable pageable) {
		if (pageable == null) {
			pageable = new PageCondition();
		}
		Class<T> entityClass = getEntityClass();
		List<mt.common.tkmapper.Filter> filters = parseCondition(pageable);
		return doPage(() -> getBaseMapper().selectByExample(MyBatisUtils.createExample(entityClass, filters)), pageable);
	}
	
	@SneakyThrows
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List<mt.common.tkmapper.Filter> parseCondition(Object condition) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		if (condition == null) {
			return filters;
		}
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
			
			//自定义解析器
			Class<? extends ConditionFilterParser<?>> parserClass = annotation.parserClass();
			if (!DefaultConditionFilterParser.class.equals(parserClass)) {
				ConditionFilterParser parser = SpringUtils.getOptionalBean(parserClass);
				if (parser == null) {
					Constructor<? extends ConditionFilterParser<?>> constructor = parserClass.getConstructor();
					parser = constructor.newInstance();
				}
				List<mt.common.tkmapper.Filter> list = parser.parseFilters(condition, value, annotation.parserParams());
				if (CollectionUtils.isNotEmpty(list)) {
					filters.addAll(list);
				}
				continue;
			}
			
			if (value != null && !(value + "").trim().isEmpty()) {
				//获取注解
				String column = StringUtils.isNotBlank(annotation.column()) ? annotation.column() : MapperColumnUtils.parseColumn(field.getName());
				Operator operator = annotation.operator();
				String prefix = annotation.prefix();
				String suffix = annotation.suffix();
				Class<? extends Converter<?>> converterClass = annotation.converter();
				
				if (operator == Operator.condition) {
					String sql = annotation.sql();
					if (StringUtils.isNotBlank(sql)) {
						filters.add(new mt.common.tkmapper.Filter(MessageUtils.replaceVariable(sql, condition), operator));
					} else {
						//替换变量
						filters.add(new mt.common.tkmapper.Filter(MessageUtils.replaceVariable(column, condition), operator, value));
					}
				} else {
					Converter converter = SpringUtils.getOptionalBean(converterClass);
					if (converter == null) {
						converter = converterClass.getConstructor().newInstance();
					}
					value = converter.convert(value);
					if (StringUtils.isNotBlank(prefix)) {
						value = prefix + value;
					}
					if (StringUtils.isNotBlank(suffix)) {
						value = value + suffix;
					}
					filters.add(new mt.common.tkmapper.Filter(column, operator, value));
				}
			}
		}
		return filters;
	}
	
	@Override
	public <T2> PageInfo<T2> doPage(@NotNull QueryHandler<T2> queryHandler, @Nullable Pageable pageable) {
		if (pageable == null) {
			pageable = new PageCondition();
		}
		Class<T> entityClass = getEntityClass();
		Integer pageNum = pageable.getPageNum();
		Integer pageSize = pageable.getPageSize();
		boolean allowSelectAll = pageable.isAllowSelectAll();
		String orderBy = pageable.getOrderBy();
		//分页
		Page<T2> page = null;
		try {
			if (!allowSelectAll) {
				if (pageNum == null || pageNum < 1) {
					pageNum = 1;
				}
				if (pageSize == null || pageSize < 1) {
					pageSize = 10;
				}
				pageSize = Math.min(pageSize, 5000);
			}
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
			if (StringUtils.isNotBlank(realOrderBy)) {
				if (page == null) {
					page = new Page<>();
					page.setOrderByOnly(true);
					PageHelper.setLocalPage(page);
				}
				page.setUnsafeOrderBy(realOrderBy);
			}
			return new PageInfo<>(queryHandler.doQuery());
		} finally {
			PageHelper.clearPage();
		}
	}
	
	@Override
	public List<T> findAll() {
		return findByFilters(new ArrayList<>());
	}
	
	@Override
	public int count(@Nullable List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().selectCountByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<T> getEntityClass() {
		return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	@SuppressWarnings("unchecked")
	public <T2> Class<T2> getEntityClass(Class<? extends BaseMapper<T2>> class1) {
		return (Class<T2>) ((ParameterizedType) class1.getGenericInterfaces()[0]).getActualTypeArguments()[0];
	}
	
	@Override
	public boolean exists(@NotNull String columnName, @NotNull Object value) {
		List<T> list = findList(columnName, value);
		return CollectionUtils.isNotEmpty(list);
	}
	
	@Override
	public boolean existsId(@NotNull Object record) {
		List<mt.common.tkmapper.Filter> idFilters = getIdFilters(getEntityClass(), record);
		return existsByFilters(idFilters);
	}
	
	@Override
	public List<T> findByFilter(@NotNull mt.common.tkmapper.Filter filter) {
		return findByFilter(filter, false);
	}
	
	@Override
	public List<T> findByFilter(@NotNull mt.common.tkmapper.Filter filter, boolean forUpdate) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(filter);
		return findByFilters(filters, forUpdate);
	}
	
	@Override
	public T findById(@NotNull Object record) {
		List<mt.common.tkmapper.Filter> idFilters = getIdFilters(getEntityClass(), record);
		return findOneByFilters(idFilters);
	}
	
	@Override
	public T findById(@NotNull Object record, boolean forUpdate) {
		List<mt.common.tkmapper.Filter> idFilters = getIdFilters(getEntityClass(), record);
		return findOneByFilters(idFilters, forUpdate);
	}
	
	@Override
	public T findOne(@NotNull String column, @NotNull Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(column, Operator.eq, value));
		return findOneByFilters(filters);
	}
	
	@Override
	public List<T> findList(@NotNull String column, @NotNull Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(column, Operator.eq, value));
		return findByFilters(filters);
	}
	
	@Override
	public List<T> findByFilters(@Nullable List<mt.common.tkmapper.Filter> filters) {
		return findByFilters(filters, false);
	}
	
	@Override
	public List<T> findByFilters(@Nullable List<mt.common.tkmapper.Filter> filters, boolean forUpdate) {
		return getBaseMapper().selectByExample(MyBatisUtils.createExample(getEntityClass(), filters, forUpdate));
	}
	
	@Override
	public T findOneByFilters(@Nullable List<mt.common.tkmapper.Filter> filters, boolean forUpdate) {
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
	public T findFirstByFilters(@Nullable List<mt.common.tkmapper.Filter> filters) {
		List<T> list = findByFilters(filters);
		if (CollectionUtils.isNotEmpty(list)) {
			return list.get(0);
		}
		return null;
	}
	
	@Override
	public T findFirstByFilter(@NotNull mt.common.tkmapper.Filter filter) {
		return findFirstByFilters(Collections.singletonList(filter));
	}
	
	@Override
	public T findOneByFilters(@Nullable List<mt.common.tkmapper.Filter> filters) {
		return findOneByFilters(filters, false);
	}
	
	@Override
	public T findOneByFilter(@NotNull mt.common.tkmapper.Filter filter, boolean forUpdate) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(filter);
		return findOneByFilters(filters, forUpdate);
	}
	
	@Override
	public T findOneByFilter(@NotNull mt.common.tkmapper.Filter filter) {
		return findOneByFilter(filter, false);
	}
	
	@Override
	public int save(@NotNull T record) {
		return getBaseMapper().insert(record);
	}
	
	@Override
	public int saveSelective(@NotNull T record) {
		return getBaseMapper().insert(record);
	}
	
	@Override
	public int updateById(@NotNull T record) {
		List<mt.common.tkmapper.Filter> idFilters = getIdFilters(getEntityClass(), record);
		return getBaseMapper().updateByExample(record, MyBatisUtils.createExample(getEntityClass(), idFilters));
	}
	
	@Override
	public int updateByIdSelective(@NotNull T record) {
		List<mt.common.tkmapper.Filter> idFilters = getIdFilters(getEntityClass(), record);
		return getBaseMapper().updateByExampleSelective(record, MyBatisUtils.createExample(getEntityClass(), idFilters));
	}
	
	@Override
	public int updateByFilter(@NotNull T record, @NotNull mt.common.tkmapper.Filter filter) {
		return updateByFilters(record, Collections.singletonList(filter));
	}
	
	@Override
	public int updateByFilterSelective(@NotNull T record, @NotNull mt.common.tkmapper.Filter filter) {
		return updateByFiltersSelective(record, Collections.singletonList(filter));
	}
	
	@Override
	public int updateByFilters(@NotNull T record, @Nullable List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().updateByExample(record, MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	public int updateByFiltersSelective(@NotNull T record, @Nullable List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().updateByExampleSelective(record, MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	public int deleteById(@NotNull Object record) {
		List<mt.common.tkmapper.Filter> idFilters = getIdFilters(getEntityClass(), record);
		return deleteByFilters(idFilters);
	}
	
	@Override
	public int deleteByFilters(@Nullable List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().deleteByExample(MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	@Override
	public int deleteByFilter(@NotNull mt.common.tkmapper.Filter filter) {
		return deleteByFilters(Collections.singletonList(filter));
	}
	
	@Override
	public int delete(@NotNull String columnName, @NotNull Object value) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		filters.add(new mt.common.tkmapper.Filter(columnName, Operator.eq, value));
		return deleteByFilters(filters);
	}
	
	@Override
	public boolean existsByFilters(@Nullable List<mt.common.tkmapper.Filter> filters) {
		List<T> byFilters = findByFilters(filters);
		return CollectionUtils.isNotEmpty(byFilters);
	}
	
	@Override
	public boolean existsByFilter(@NotNull mt.common.tkmapper.Filter filter) {
		return CollectionUtils.isNotEmpty(findByFilter(filter));
	}
	
	@Override
	public boolean notExistsId(@NotNull Object record) {
		return !existsId(record);
	}
	
	@Override
	public boolean notExistsByFilters(@Nullable List<mt.common.tkmapper.Filter> filters) {
		return !existsByFilters(filters);
	}
	
	@Override
	public boolean notExistsByFilter(@NotNull mt.common.tkmapper.Filter filter) {
		return !existsByFilter(filter);
	}
	
	@Override
	public void batchConsume(@NotNull List<mt.common.tkmapper.Filter> filters, int batchSize, @Nullable String orderBy, @NotNull Consumer<List<T>> consumer) {
		int pageNum = 1;
		while (true) {
			PageCondition pageCondition = new PageCondition() {
				@Override
				public String getOrderBy() {
					return orderBy;
				}
			};
			pageCondition.setPageNum(pageNum++);
			pageCondition.setPageSize(batchSize);
			PageInfo<T> pageInfo = doPage(() -> findByFilters(filters), pageCondition);
			List<T> list = pageInfo.getList();
			if (CollectionUtils.isEmpty(list)) {
				break;
			}
			consumer.accept(list);
			if (!pageInfo.isHasNextPage() || list.size() < batchSize) {
				break;
			}
		}
	}
	
	@Override
	public List<GroupCount> findGroupCounts(@NotNull String groupField, @Nullable List<mt.common.tkmapper.Filter> filters) {
		Example example = MyBatisUtils.createExample(getEntityClass(), filters);
		return getBaseMapper().findGroupCounts(example, groupField);
	}
	
	@Override
	public <ResultType> ResultType findMax(@NotNull String fieldName, @NotNull List<mt.common.tkmapper.Filter> filters) {
		return findAggregation(fieldName, filters, "max");
	}
	
	@Override
	public <ResultType> ResultType findMin(@NotNull String fieldName, @NotNull List<mt.common.tkmapper.Filter> filters) {
		return findAggregation(fieldName, filters, "min");
	}
	
	@Override
	public <ResultType> ResultType findAvg(@NotNull String fieldName, @NotNull List<mt.common.tkmapper.Filter> filters) {
		return findAggregation(fieldName, filters, "avg");
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	private <ResultType> ResultType findAggregation(@NotNull String fieldName, List<mt.common.tkmapper.Filter> filters, String aggregation) {
		Field field = assertFieldExists(fieldName);
		Example example = MyBatisUtils.createExample(getEntityClass(), filters);
		List<T> listWithFields = getBaseMapper().findListWithFields(Arrays.asList(aggregation + "(" + fieldName + ") as " + fieldName), example);
		if (CollectionUtils.isEmpty(listWithFields)) {
			return null;
		}
		T entity = listWithFields.get(0);
		field.setAccessible(true);
		return (ResultType) field.get(entity);
	}
	
	private Field assertFieldExists(@NotNull String fieldName) {
		Field field = ReflectUtils.findField(getEntityClass(), fieldName);
		Assert.notNull(field, "字段" + fieldName + "不存在");
		return field;
	}
	
	@Override
	public int add(@NotNull String column, int value, @NotNull List<mt.common.tkmapper.Filter> filters) {
		return getBaseMapper().addField(column, value, MyBatisUtils.createExample(getEntityClass(), filters));
	}
	
	public int add(@NotNull String column, int value, @NotNull mt.common.tkmapper.Filter filter) {
		return add(column, value, Collections.singletonList(filter));
	}
	
	@Override
	public <Result> List<Result> findAdvancedList(@NotNull Class<Result> resultClass, @Nullable List<mt.common.tkmapper.Filter> filters) {
		AdvancedQuery advancedQuery = AdvancedQuery.create(resultClass, filters);
		List<Map<String, Object>> list = getBaseMapper().findAdvancedList(advancedQuery);
		return advancedQuery.convert(list);
	}
	
	@Override
	public <Result> List<Result> findAdvancedList(@NotNull Class<Result> resultClass, @Nullable mt.common.tkmapper.Filter filter) {
		return findAdvancedList(resultClass, Arrays.asList(filter));
	}
	
	@Override
	public <Result> PageInfo<Result> findAdvancedPage(@NotNull Class<Result> resultClass, @Nullable Pageable pageable) {
		return doPage(() -> findAdvancedList(resultClass, parseCondition(pageable)), pageable);
	}
}
