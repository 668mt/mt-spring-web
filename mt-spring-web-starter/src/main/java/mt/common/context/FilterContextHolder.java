package mt.common.context;

import lombok.extern.slf4j.Slf4j;
import mt.utils.common.Assert;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
@Slf4j
public class FilterContextHolder {
	private static final ThreadLocal<List<FilterContext>> filterContextThreadLocal = new ThreadLocal<>();
	
	public static List<FilterContext> get() {
		return filterContextThreadLocal.get();
	}
	
	public static void set(List<FilterContext> filterContexts) {
		if (CollectionUtils.isNotEmpty(filterContextThreadLocal.get())) {
			List<FilterContext> existsContexts = filterContextThreadLocal.get();
			String contextNames = existsContexts.stream().map(FilterContext::name).collect(Collectors.joining(","));
			log.error("FilterContext已存在[" + contextNames + "]，将会被覆盖！");
		}
		filterContextThreadLocal.set(filterContexts);
	}
	
	public static void remove() {
		filterContextThreadLocal.remove();
	}
}
