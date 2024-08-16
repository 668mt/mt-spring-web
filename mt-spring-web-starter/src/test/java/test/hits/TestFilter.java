package test.hits;

import mt.common.tkmapper.Filter;
import mt.common.tkmapper.Operator;
import mt.common.tkmapper.OrFilter;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/8/24
 */
public class TestFilter {
	public static void main(String[] args) {
		List<Filter> orFilters = new ArrayList<>();
		List<Filter> filters = new ArrayList<>();
		String name = "张三";
		Filter nameFilter = new Filter("MATCH (name) AGAINST ('" + StringEscapeUtils.escapeSql(name) + "' IN BOOLEAN MODE)", Operator.condition);
		Filter tagFilter = new Filter("MATCH (tags) AGAINST ('" + StringEscapeUtils.escapeSql(name) + "' IN BOOLEAN MODE)", Operator.condition);
//		Filter userFilter = new Filter("uploadedUserId", Filter.Operator.in, Arrays.asList(3));
		Filter userFilter = new Filter("uploadedUserId", Operator.in, "aaa");
//		Filter userFilter = new Filter("uploadedUserId", Filter.Operator.eq, "aaa");
		orFilters.add(userFilter);
		orFilters.add(nameFilter);
		orFilters.add(tagFilter);
		filters.add(new OrFilter(orFilters.toArray(new Filter[0])));
		filters.add(new Filter("isReview", Operator.eq, true));
		filters.add(new Filter("isReviewPass", Operator.eq, true));
		String conditionSql = Filter.filtersToMybatisSql("params", filters, "v");
		Map<String, Object> params = Filter.filtersToParameterMap(filters);
		System.out.println(conditionSql);
		System.out.println(params);
	}
}
