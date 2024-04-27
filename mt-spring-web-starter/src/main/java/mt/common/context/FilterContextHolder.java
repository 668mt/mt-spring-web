package mt.common.context;

import lombok.extern.slf4j.Slf4j;
import mt.utils.common.Assert;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
@Slf4j
public class FilterContextHolder {
	private static final ThreadLocal<FilterContext> filterContextThreadLocal = new ThreadLocal<>();
	
	public static FilterContext get() {
		return filterContextThreadLocal.get();
	}
	
	public static void set(FilterContext filterContext) {
		if (filterContextThreadLocal.get() != null) {
			FilterContext existsContext = filterContextThreadLocal.get();
			log.error("FilterContext已存在[" + existsContext.name() + "]，将会被覆盖！");
		}
		filterContextThreadLocal.set(filterContext);
	}
	
	public static void remove() {
		filterContextThreadLocal.remove();
	}
}
