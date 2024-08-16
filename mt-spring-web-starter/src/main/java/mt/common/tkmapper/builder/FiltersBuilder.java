package mt.common.tkmapper.builder;

import mt.common.tkmapper.Filter;
import mt.common.tkmapper.Operator;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * @Author Martin
 * @Date 2024/3/31
 */
public class FiltersBuilder {
	final List<Filter> filters = new ArrayList<>();
	
	public FiltersBuilder add(@NotNull String property, @NotNull Operator operator) {
		filters.add(new Filter(property, operator));
		return this;
	}
	
	public FiltersBuilder add(@NotNull String property, @NotNull Operator operator, @NotNull Object value) {
		filters.add(new Filter(property, operator, value));
		return this;
	}
	
	public FiltersBuilder add(@NotNull String property, @NotNull Operator operator, @NotNull Object value1, @NotNull Object value2) {
		filters.add(new Filter(property, operator, value1, value2));
		return this;
	}
	
	public FiltersBuilder addIfNotNull(Supplier<Filter> supplier) {
		Filter filter = supplier.get();
		if (filter != null) {
			this.filters.add(filter);
		}
		return this;
	}
	
	public FiltersBuilder addAllIfNotNull(Supplier<List<Filter>> supplier) {
		List<Filter> filters = supplier.get();
		if (CollectionUtils.isNotEmpty(filters)) {
			this.filters.addAll(filters);
		}
		return this;
	}
	
	public FiltersBuilder addIf(boolean condition, Filter... filters) {
		if (condition && filters != null) {
			this.filters.addAll(Arrays.asList(filters));
		}
		return this;
	}
	
	public FiltersBuilder addIf(boolean condition, String property, Operator operator, Object value) {
		if (condition) {
			this.filters.add(new Filter(property, operator, value));
		}
		return this;
	}
	
	public OrGroupBuilder addOrGroup() {
		return new OrGroupBuilder(this);
	}
	
	public List<Filter> build() {
		return filters;
	}
}
