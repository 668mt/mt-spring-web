package mt.common.tkmapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/9/25
 */
public class DefaultCustomConditionFilterParser implements CustomConditionFilterParser<Object, Object> {
	@Override
	public List<Filter> parseFilters(@NotNull Object condition, @Nullable Object fieldValue) {
		return null;
	}
}
