package mt.common.mybatis;

import mt.common.mybatis.entity.MtExample;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.util.MetaObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Intercepts({
		@Signature(method = "query", type = Executor.class, args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
		@Signature(method = "query", type = Executor.class, args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
})
@Component
public class ResultMapInterceptor extends BaseInterceptor {
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object parameters = getParameters(invocation);
		MappedStatement ms = getMappedStatement(invocation);
		if (parameters == null) {
			return invocation.proceed();
		}
		if (parameters instanceof MtExample) {
			MtExample example = (MtExample) parameters;
			String resultMap = example.getResultMap();
			if (StringUtils.isNotBlank(resultMap)) {
				List<ResultMap> resultMaps = new ArrayList<>();
				resultMaps.add(ms.getConfiguration().getResultMap(resultMap));
				MetaObject metaObject = MetaObjectUtil.forObject(ms);
				metaObject.setValue("resultMaps", Collections.unmodifiableList(resultMaps));
			}
		}
		return invocation.proceed();
	}
	
	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}
	
	@Override
	public void setProperties(Properties properties) {
	}
	
}
