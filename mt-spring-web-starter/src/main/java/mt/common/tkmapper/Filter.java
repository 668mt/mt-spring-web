package mt.common.tkmapper;

import lombok.Data;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.tkmapper.builder.FiltersBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Mybatis判断
 *
 * @author Martin
 * @ClassName: Filter
 * @Description:
 * @date 2017-10-18 下午2:02:58
 */
@Data
public class Filter {
	public Filter() {
	}
	
	public static FiltersBuilder builds() {
		return new FiltersBuilder();
	}
	
	private String property;
	private Object value;
	private Object value2;
	private Operator operator;
	
	
	/**
	 * 把运算符转换为sql
	 *
	 * @param operator
	 * @return
	 */
	public static String toSql(Operator operator) {
		if (operator != null) {
			switch (operator) {
				case eq:
					return " = ";
				case eqn:
					throw new RuntimeException("不支持eqn");
				case ge:
					return " >= ";
				case gt:
					return " > ";
				case in:
					return " in ";
				case isNotNull:
					return " is not null ";
				case isNull:
					return " is null ";
				case le:
					return " <= ";
				case like:
					return " like ";
				case lt:
					return " < ";
				case ne:
					return " != ";
				case between:
					return " between ";
				case notBetween:
					return " not between ";
				case notIn:
					return " not in ";
				case notLike:
					return " not like ";
			}
		}
		
		return "";
	}
	
	private static String inValueSql(String paramName, Object value) {
		String sql = " (";
		if (value instanceof Collection) {
			List<String> list = new ArrayList<>();
			Collection c = (Collection) value;
			Iterator iterator = c.iterator();
			int index = 1;
			while (iterator.hasNext()) {
				Object next = iterator.next();
				list.add("#{" + paramName + "_" + index + "}");
				index++;
			}
			sql += StringUtils.join(list, ",");
		} else if (value instanceof Object[]) {
			List<String> list = new ArrayList<>();
			int index = 1;
			Object[] c = (Object[]) value;
			for (Object o : c) {
				list.add("#{" + paramName + "_" + index + "}");
				index++;
			}
			sql += StringUtils.join(list, ",");
		} else {
			List<Object> list = new ArrayList<>();
			list.add(value);
			return inValueSql(paramName, list);
		}
		sql += ") ";
		return sql;
	}
	
	public String toMyBatisSql(@NotNull String paramName, @Nullable String alias) {
		Filter filter = this;
		Assert.notNull(filter, "参数不能为空");
		Object value = "#{" + paramName + "}";
		alias = StringUtils.isBlank(alias) ? "" : alias + ".";
		String column;
		String filterProperty = filter.getProperty();
		Assert.notNull(filterProperty, "filter property不能为空");
		if (filter.getOperator() == Operator.condition) {
			column = filterProperty;
		} else {
			column = MapperColumnUtils.parseColumn(filterProperty);
		}
		String sql = alias + column;
		switch (filter.getOperator()) {
			case eq:
				return sql += " = " + value + " ";
			case eqn:
				return sql += " = " + value + " ";
			case condition:
				if (filter.getValue() != null) {
					return column + " " + value;
				} else {
					return column;
				}
			case between:
				return sql += " between #{" + paramName + "_1} and #{" + paramName + "_2} ";
			case ge:
				return sql += " >= " + value + " ";
			case gt:
				return sql += " > " + value + " ";
			case in:
				return sql += " in " + inValueSql(paramName, filter.getValue());
			case isNotNull:
				return sql += " is not null ";
			case isNull:
				return sql += " is null ";
			case le:
				return sql += " <= " + value + " ";
			case like:
				return sql += " like " + value + " ";
			case lt:
				return sql += " < " + value + " ";
			case ne:
				return sql += " != " + value + " ";
			case notBetween:
				return sql += " not between #{" + paramName + "_1} and #{" + paramName + "_2} ";
			case notIn:
				return sql += " not in " + inValueSql(paramName, filter.getValue());
			case notLike:
				return sql += " not like " + value + " ";
		}
		
		return sql;
	}
	
	public static String filtersToMybatisSql(@NotNull String parameterMapName, @NotNull List<Filter> filters, @Nullable String alias) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < filters.size(); i++) {
			Filter filter = filters.get(i);
			sb.append(" and ");
			sb.append(filter.toMyBatisSql(parameterMapName + "." + "p" + (i + 1), alias));
		}
		return sb.toString();
	}
	
	public void addToParameterMap(@NotNull Map<String, Object> parameterMap, @NotNull String parameterName) {
		switch (operator) {
			case in:
			case notIn:
				if (value instanceof Collection) {
					Collection collection = (Collection) value;
					Iterator iterator = collection.iterator();
					int index = 1;
					while (iterator.hasNext()) {
						parameterMap.put(parameterName + "_" + index, iterator.next());
						index++;
					}
				} else if (value instanceof Object[]) {
					Object[] array = (Object[]) value;
					int index = 1;
					for (Object o : array) {
						parameterMap.put(parameterName + "_" + index, o);
						index++;
					}
				} else {
					parameterMap.put(parameterName + "_" + 1, value);
				}
				break;
			case between:
			case notBetween:
				parameterMap.put(parameterName + "_" + 1, value);
				parameterMap.put(parameterName + "_" + 2, value2);
				break;
			default:
				parameterMap.put(parameterName, value);
				break;
		}
	}
	
	/**
	 * 将过滤条件转化为Mybatis参数map
	 *
	 * @param filters
	 * @return
	 */
	public static Map<String, Object> filtersToParameterMap(@NotNull List<Filter> filters) {
		Map<String, Object> parameterMap = new LinkedHashMap<>();
		for (int i = 0; i < filters.size(); i++) {
			Filter filter = filters.get(i);
			String parameterName = "p" + (i + 1);
			filter.addToParameterMap(parameterMap, parameterName);
		}
		return parameterMap;
	}
	
	public Filter(String property, Operator operator, Object value) {
		this.property = property;
		this.operator = operator;
		this.value = value;
	}
	
	public Filter(String property, Operator operator) {
		this.property = property;
		this.operator = operator;
	}
	
	public Filter(String property, Operator operator, Object value, Object value2) {
		this.property = property;
		this.value = value;
		this.value2 = value2;
		this.operator = operator;
	}
	
}
