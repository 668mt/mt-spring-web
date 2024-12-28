package mt.common.mybatis.advanced;

import com.github.pagehelper.Page;
import lombok.Getter;
import lombok.SneakyThrows;
import mt.common.mybatis.annotation.From;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.tkmapper.Filter;
import mt.utils.ReflectUtils;
import mt.utils.common.Assert;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import java.beans.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2024/12/28
 */
@Getter
public class AdvancedQuery {
	
	private AdvancedQuery() {
	}
	
	private Map<String, Object> params;
	private String conditionSql;
	private String fromSql;
	private String fieldsSql;
	private Map<String, Field> columnFieldMappings;
	private Class<?> resultClass;
	
	public static AdvancedQuery create(@NotNull Class<?> resultClass, @Nullable List<Filter> filters) {
		if (filters == null) {
			filters = new ArrayList<>();
		}
		From from = AnnotatedElementUtils.getMergedAnnotation(resultClass, From.class);
		Assert.notNull(from, resultClass.getName() + "上未标注@From注解");
		String fromSql = from.value();
		fromSql = fromSql.trim();
		if (fromSql.endsWith(";")) {
			fromSql = fromSql.substring(0, fromSql.length() - 1);
		}
		List<Field> fields = ReflectUtils.findAllFieldsIgnore(resultClass, Transient.class);
		Map<String, Field> columnFieldMappings = new HashMap<>();
		List<String> columnNames = new ArrayList<>();
		for (Field field : fields) {
			String columnName;
			Column column = field.getAnnotation(Column.class);
			if (column != null && StringUtils.isNotBlank(column.name())) {
				columnName = column.name();
			} else {
				columnName = MapperColumnUtils.parseColumn(field.getName());
			}
			columnNames.add(columnName);
			columnFieldMappings.put(columnName, field);
		}
		AdvancedQuery advancedQuery = new AdvancedQuery();
		advancedQuery.resultClass = resultClass;
		advancedQuery.fromSql = fromSql;
		advancedQuery.fieldsSql = CollectionUtils.isNotEmpty(columnNames) ? StringUtils.join(columnNames, ",") : "*";
		advancedQuery.columnFieldMappings = columnFieldMappings;
		advancedQuery.params = Filter.filtersToParameterMap(filters);
		advancedQuery.conditionSql = Filter.filtersToMybatisSql("params", filters, "tmp");
		return advancedQuery;
	}
	
	@SuppressWarnings("resource")
	public <T> List<T> convert(@Nullable List<Map<String, Object>> list) {
		if (list == null) {
			list = new ArrayList<>();
		}
		List<T> resultList;
		if (list instanceof Page<Map<String, Object>> page) {
			Page<T> newPage = new Page<>();
			newPage.setPageNum(page.getPageNum());
			newPage.setPageSize(page.getPageSize());
			newPage.setOrderBy(page.getOrderBy());
			newPage.setTotal(page.getTotal());
			resultList = newPage;
		} else {
			resultList = new ArrayList<>();
		}
		for (Map<String, Object> item : list) {
			resultList.add(convert(item));
		}
		return resultList;
	}
	
	@SuppressWarnings({"unchecked"})
	@SneakyThrows
	private <T> T convert(Map<String, Object> data) {
		T instance = (T) resultClass.getConstructor().newInstance();
		Map<String, Object> typeHandlerCache = new HashMap<>();
		for (Map.Entry<String, Object> stringObjectEntry : data.entrySet()) {
			String column = stringObjectEntry.getKey();
			Object value = stringObjectEntry.getValue();
			Field field = columnFieldMappings.get(column);
			if (field == null) {
				continue;
			}
			ColumnType columnType = field.getAnnotation(ColumnType.class);
			if (columnType != null) {
				Class<?> typeHandler = columnType.typeHandler();
				Method method = typeHandler.getMethod("getResult", ResultSet.class, String.class);
				Object typeHandlerInstance = typeHandlerCache.computeIfAbsent(typeHandler.getName(), s -> {
					try {
						return typeHandler.getConstructor().newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				value = method.invoke(typeHandlerInstance, new AdvancedResultSet(value), column);
			} else {
				value = ConvertUtils.convert(value, field.getType());
			}
			try {
				resultClass.getMethod("set" + StringUtils.capitalize(field.getName()), field.getType()).invoke(instance, value);
			} catch (NoSuchMethodException noSuchMethodException) {
				field.setAccessible(true);
				field.set(instance, value);
			}
		}
		return instance;
	}
}
