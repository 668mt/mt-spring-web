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
	private final FiltersBuilder filtersBuilder;
	private final List<Filter> filters = new ArrayList<>();
	
	public OrGroupBuilder(FiltersBuilder filtersBuilder) {
		this.filtersBuilder = filtersBuilder;
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
	public FiltersBuilder endOrGroup() {
		if (CollectionUtils.isNotEmpty(filters)) {
			filtersBuilder.filters.add(new OrFilter(filters.toArray(new Filter[0])));
		}
		return filtersBuilder;
	}
}
