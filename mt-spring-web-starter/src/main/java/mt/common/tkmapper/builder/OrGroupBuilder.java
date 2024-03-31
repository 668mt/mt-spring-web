package mt.common.tkmapper.builder;

import mt.common.tkmapper.Filter;
import mt.common.tkmapper.OrFilter;
import mt.utils.common.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2024/3/31
 */
public class OrGroupBuilder {
	private final FilterBuilder filterBuilder;
	private final List<Filter> filters = new ArrayList<>();
	
	public OrGroupBuilder(FilterBuilder filterBuilder) {
		this.filterBuilder = filterBuilder;
	}
	
	public OrGroupBuilder addOr(String property, Filter.Operator operator, Object value) {
		filters.add(new Filter(property, operator, value));
		return this;
	}
	
	/**
	 * 结束or组
	 *
	 * @return
	 */
	public FilterBuilder endOrGroup() {
		if (CollectionUtils.isNotEmpty(filters)) {
			filterBuilder.filters.add(new OrFilter(filters.toArray(new Filter[0])));
		}
		return filterBuilder;
	}
}
