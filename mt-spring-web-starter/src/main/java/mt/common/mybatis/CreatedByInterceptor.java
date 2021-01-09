package mt.common.mybatis;

import mt.common.annotation.CreatedBy;
import mt.common.currentUser.UserContext;
import mt.utils.common.ObjectUtils;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

/**
 * 创建日期拦截器
 *
 * @author Martin
 * @date 2017-9-29 上午10:38:30
 * Mybatis拦截器只能拦截四种类型的接口：Executor、StatementHandler、ParameterHandler和ResultSetHandler。
 */
@Intercepts({
		@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class CreatedByInterceptor implements Interceptor {
	private final UserContext userContext;
	
	public CreatedByInterceptor(UserContext userContext) {
		this.userContext = userContext;
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
		//获取参数
		Object[] args = invocation.getArgs();
		Object parameters = args[1];
		if (parameters == null) {
			return invocation.proceed();
		}
		if (!InterceptorHelper.isSave(ms, parameters)) {
			return invocation.proceed();
		}
		//查找创建日期注解
		List<Field> createdByFields = ReflectUtils.findAllFields(parameters.getClass(), CreatedBy.class);
		if (CollectionUtils.isEmpty(createdByFields)) {
			return invocation.proceed();
		}
		//数据库对象
		for (Field field : createdByFields) {
			//数据库创建时间
			field.setAccessible(true);
			Object createdBy = field.get(parameters);
			String currentUserName = userContext.getCurrentUserName();
			if (createdBy == null && String.class.isAssignableFrom(field.getType()) && currentUserName != null) {
				field.set(parameters, currentUserName);
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
