package mt.common.mybatis;

import mt.common.annotation.CreatedByUserId;
import mt.common.annotation.CreatedByUserName;
import mt.common.currentUser.UserContext;
import mt.utils.ReflectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

/**
 * 创建人拦截器
 *
 * @author Martin
 * @date 2017-9-29 上午10:38:30
 * Mybatis拦截器只能拦截四种类型的接口：Executor、StatementHandler、ParameterHandler和ResultSetHandler。
 */
@Intercepts({
		@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class CreatedByInterceptor implements Interceptor {
	private final UserContext<?, ?> userContext;
	
	public CreatedByInterceptor(UserContext<?, ?> userContext) {
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
		//根据当前用户名称设置创建人
		String currentUserName = userContext.getCurrentUserName();
		if (StringUtils.isNotBlank(currentUserName)) {
			List<Field> createdByFields = ReflectUtils.findAllFields(parameters.getClass(), CreatedByUserName.class);
			if (CollectionUtils.isNotEmpty(createdByFields)) {
				for (Field field : createdByFields) {
					field.setAccessible(true);
					Object createdByUserName = field.get(parameters);
					if (createdByUserName == null && String.class.isAssignableFrom(field.getType())) {
						field.set(parameters, currentUserName);
					}
				}
			}
		}
		//根据当前用户ID设置创建人
		Object currentUserId = userContext.getCurrentUserId();
		if (currentUserId != null) {
			List<Field> createdByFields = ReflectUtils.findAllFields(parameters.getClass(), CreatedByUserId.class);
			if (CollectionUtils.isNotEmpty(createdByFields)) {
				for (Field field : createdByFields) {
					field.setAccessible(true);
					Object createdById = field.get(parameters);
					if (createdById == null && field.getType().isAssignableFrom(currentUserId.getClass())) {
						field.set(parameters, currentUserId);
					}
				}
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
