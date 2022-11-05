package mt.common.tkmapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/9/25
 */
public interface CustomConditionFilterParser<ConditionType, FieldType> {
	List<Filter> parseFilters(@NotNull ConditionType condition, @Nullable FieldType fieldValue);
}
