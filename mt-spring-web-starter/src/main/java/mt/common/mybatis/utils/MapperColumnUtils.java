package mt.common.mybatis.utils;

import mt.common.utils.SpringUtils;
import mt.utils.ReflectUtils;
import mt.utils.RegexUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import tk.mybatis.mapper.code.Style;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Martin
 * @date 2020/4/16
 */
public class MapperColumnUtils {
	/**
	 * 驼峰转下划线
	 *
	 * @param property
	 * @return
	 */
	public static String camelhump(String property) {
		List<String[]> list = RegexUtils.findList(property, "([a-z])([A-Z])", new Integer[]{0, 1, 2});
		if (CollectionUtils.isNotEmpty(list)) {
			for (String[] group : list) {
				property = property.replace(group[0], group[1] + "_" + group[2].toLowerCase());
			}
		}
		return property;
	}
	
	public static String parseColumn(String columnName, String entityClassName) {
		try {
			Class<?> entityClass = Class.forName(entityClassName);
			return parseColumn(columnName, entityClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String parseColumn(String columnName, Class<?> entityClass) {
		Field field = ReflectUtils.findField(entityClass, columnName);
		if (field != null) {
			Column column = field.getAnnotation(Column.class);
			if (column != null && StringUtils.isNotBlank(column.name())) {
				return column.name();
			}
		}
		return parseColumn(columnName);
	}
	
	/**
	 * 获取#mapper.style的配置，然后根据配置进行转换
	 *
	 * @param columnName
	 * @return
	 */
	public static String parseColumn(String columnName) {
		Style style = SpringUtils.getProperty("mapper.style", Style.class);
		if (style == null) {
			return columnName;
		}
		switch (style) {
			case camelhump:
				//驼峰转下划线
				columnName = camelhump(columnName);
				break;
			case camelhumpAndLowercase:
				columnName = camelhump(columnName).toLowerCase();
				break;
			case camelhumpAndUppercase:
				columnName = camelhump(columnName).toUpperCase();
				break;
			case lowercase:
				columnName = columnName.toLowerCase();
				break;
			case uppercase:
				columnName = columnName.toUpperCase();
				break;
			case normal:
			default:
				break;
		}
		return columnName;
	}
}
