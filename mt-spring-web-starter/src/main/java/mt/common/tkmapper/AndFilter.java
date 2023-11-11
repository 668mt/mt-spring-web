//package mt.common.tkmapper;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Map;
//
///**
// * @Author Martin
// * @Date 2023/11/11
// */
//@Data
//@EqualsAndHashCode(callSuper = false)
//public class AndFilter extends Filter {
//	private Filter[] filters;
//
//	public AndFilter(Filter... filters) {
//		super();
//		this.filters = filters;
//	}
//
//	@Override
//	public String toMyBatisSql(@NotNull String paramName, @Nullable String alias) {
//		return super.toMyBatisSql(paramName, alias);
//	}
//
//	@Override
//	public void addToParameterMap(@NotNull Map<String, Object> parameterMap, @NotNull String parameterName) {
//		super.addToParameterMap(parameterMap, parameterName);
//	}
//}
