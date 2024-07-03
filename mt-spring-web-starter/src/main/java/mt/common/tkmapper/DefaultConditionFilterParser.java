package mt.common.tkmapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/9/25
 */
public class DefaultConditionFilterParser implements ConditionFilterParser<Object> {
	@Override
	public List<Filter> parseFilters(@NotNull Object condition, @Nullable Object fieldValue, @Nullable String[] params) {
		return null;
	}
}
