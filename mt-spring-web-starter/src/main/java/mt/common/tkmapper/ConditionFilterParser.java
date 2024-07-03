package mt.common.tkmapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/9/25
 */
public interface ConditionFilterParser<FieldType> {
	List<Filter> parseFilters(@NotNull Object condition, @Nullable FieldType fieldValue, @Nullable String[] params);
}
