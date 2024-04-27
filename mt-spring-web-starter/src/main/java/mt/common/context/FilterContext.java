package mt.common.context;

import jakarta.servlet.http.HttpServletRequest;
import mt.common.tkmapper.Filter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public interface FilterContext {
	String name();
	
	void prepareContext(@NotNull HttpServletRequest request);
	
	void addContextFilter(@NotNull Class<?> entityClass, @NotNull List<Filter> filters, @NotNull List<FieldInfo> fieldInfos);
	
	void clearContext();
}
