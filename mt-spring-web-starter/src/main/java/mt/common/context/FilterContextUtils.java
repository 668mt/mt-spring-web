package mt.common.context;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
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
	/**
	 * 添加过滤器
	 *
	 * @param entityClass     实体类
	 * @param originalFilters 原始过滤器
	 * @return 新增之后的过滤器
	 */
	public static List<Filter> addFilters(Class<?> entityClass, List<Filter> originalFilters) {
		List<Filter> filters = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(originalFilters)) {
			filters.addAll(originalFilters);
		}
		//需要将PageHelper备份
		Page<Object> localPage = PageHelper.getLocalPage();
		//清空PageHelper
		PageHelper.clearPage();
		try {
			//获取当前过滤器上下文
			List<FilterContext> filterContexts = FilterContextHolder.get();
			if (CollectionUtils.isEmpty(filterContexts)) {
				return filters;
			}
			List<Field> fields = ReflectUtils.findAllFields(entityClass, UseFilterContextField.class);
			if (CollectionUtils.isEmpty(fields)) {
				return filters;
			}
			for (FilterContext filterContext : filterContexts) {
				String contextName = filterContext.name();
				List<FieldInfo> fieldInfos = new ArrayList<>();
				for (Field field : fields) {
					UseFilterContextField useFilterContextField = field.getAnnotation(UseFilterContextField.class);
					String context = useFilterContextField.contextName();
					if (StringUtils.isBlank(context) || context.equals(contextName)) {
						//使用context
						String fieldName = field.getName();
						FieldInfo fieldInfo = new FieldInfo();
						fieldInfo.setFieldName(fieldName);
						fieldInfos.add(fieldInfo);
					}
				}
				if (CollectionUtils.isNotEmpty(fieldInfos)) {
					filterContext.addContextFilter(entityClass, filters, fieldInfos);
				}
			}
		} finally {
			//恢复PageHelper
			if (localPage != null) {
				PageHelper.setLocalPage(localPage);
			}
		}
		return filters;
	}
}
