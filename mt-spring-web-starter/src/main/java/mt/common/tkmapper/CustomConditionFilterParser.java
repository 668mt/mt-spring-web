package mt.common.tkmapper;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/9/25
 */
public interface CustomConditionFilterParser<ConditionType, FieldType> {
	List<Filter> parseFilters(ConditionType condition, FieldType fieldValue);
}
