package mt.common.tkmapper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Martin
 * @date 2020/4/30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrFilter extends Filter {
	private Filter[] filters;
	
	public OrFilter(Filter... filters) {
		super();
		this.filters = filters;
	}
	
	@Override
	public String toMyBatisSql(@NotNull String parameterName, @Nullable String alias) {
		List<String> sqls = new ArrayList<>();
		int index = 1;
		for (Filter filter : filters) {
			String sql = filter.toMyBatisSql(parameterName + "_" + index, alias);
			sqls.add(sql);
			index++;
		}
		return "(" + StringUtils.join(sqls, " or ") + ")";
	}
	
	@Override
	public void addToParameterMap(@NotNull Map<String, Object> parameterMap, @NotNull String parameterName) {
		int index = 0;
		for (Filter filter : filters) {
			filter.addToParameterMap(parameterMap, parameterName + "_" + index);
			index++;
		}
	}
}
