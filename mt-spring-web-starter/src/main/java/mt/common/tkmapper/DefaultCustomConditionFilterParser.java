package mt.common.tkmapper;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/9/25
 */
public class DefaultCustomConditionFilterParser implements CustomConditionFilterParser<Object, Object> {
	@Override
	public List<Filter> parseFilters(Object condition, Object fieldValue) {
		return null;
	}
}
