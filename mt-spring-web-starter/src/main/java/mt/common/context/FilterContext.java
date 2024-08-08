package mt.common.context;

import javax.servlet.http.HttpServletRequest;
import mt.common.tkmapper.Filter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public interface FilterContext {
	/**
	 * 名称
	 *
	 * @return 名称
	 */
	String name();
	
	/**
	 * 准备上下文
	 *
	 * @param request 请求
	 */
	void prepareContext(@NotNull HttpServletRequest request);
	
	/**
	 * 添加上下文过滤器
	 *
	 * @param entityClass 实体类
	 * @param filters     过滤器
	 * @param fieldInfos  字段信息
	 */
	void addContextFilter(@NotNull Class<?> entityClass, @NotNull List<Filter> filters, @NotNull List<FieldInfo> fieldInfos);
	
	/**
	 * 清除上下文
	 */
	void clearContext();
}
