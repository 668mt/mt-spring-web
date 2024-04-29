package mt.common.context;

import mt.common.context.annotation.UseFilterContextField;
import mt.common.tkmapper.Filter;
import mt.utils.ReflectUtils;
import mt.utils.common.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public class FilterContextUtils {
	public static List<Filter> addFilters(Class<?> entityClass, List<Filter> originalFilters) {
		List<Filter> filters = new ArrayList<>(originalFilters);
		//获取当前过滤器上下文
		FilterContext filterContext = FilterContextHolder.get();
		if (filterContext == null) {
			return filters;
		}
		String contextName = filterContext.name();
		List<Field> fields = ReflectUtils.findAllFields(entityClass, UseFilterContextField.class);
		if (CollectionUtils.isEmpty(fields)) {
			return filters;
		}
		List<FieldInfo> fieldInfos = new ArrayList<>();
		for (Field field : fields) {
			UseFilterContextField useFilterContextField = field.getAnnotation(UseFilterContextField.class);
			String context = useFilterContextField.contextName();
			if (StringUtils.isBlank(context) || context.equals(contextName)) {
				//使用context
				String fieldName = field.getName();
				String useField = useFilterContextField.useField();
				FieldInfo fieldInfo = new FieldInfo();
				fieldInfo.setFieldName(fieldName);
				fieldInfo.setUseField(useField);
				fieldInfos.add(fieldInfo);
			}
		}
		if (CollectionUtils.isNotEmpty(fieldInfos)) {
			filterContext.addContextFilter(entityClass, filters, fieldInfos);
		}
		return filters;
	}
}
