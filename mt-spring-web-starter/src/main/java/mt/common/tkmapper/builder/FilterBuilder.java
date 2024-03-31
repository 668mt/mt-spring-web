package mt.common.tkmapper.builder;

import mt.common.tkmapper.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2024/3/31
 */
public class FilterBuilder {
	final List<Filter> filters = new ArrayList<>();
	
	public FilterBuilder and(@NotNull String property, @NotNull Filter.Operator operator) {
		filters.add(new Filter(property, operator));
		return this;
	}
	
	public FilterBuilder and(@NotNull String property, @NotNull Filter.Operator operator, @NotNull Object value) {
		filters.add(new Filter(property, operator, value));
		return this;
	}
	
	public FilterBuilder and(@NotNull String property, @NotNull Filter.Operator operator, @NotNull Object value1, @NotNull Object value2) {
		filters.add(new Filter(property, operator, value1, value2));
		return this;
	}
	
	public OrGroupBuilder andOrGroup() {
		return new OrGroupBuilder(this);
	}
	
	public List<Filter> build() {
		return filters;
	}
}
