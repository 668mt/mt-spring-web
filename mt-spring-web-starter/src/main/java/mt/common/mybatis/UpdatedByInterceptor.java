package mt.common.mybatis;

import lombok.Data;
import mt.common.annotation.UpdatedByUserId;
import mt.common.annotation.UpdatedByUserName;
import mt.common.currentUser.UserContext;
import mt.utils.ReflectUtils;
import mt.utils.common.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

/**
 * 修改人拦截
 *
 * @author Martin
 * @date 2017-9-29 上午10:43:30
 * Mybatis拦截器只能拦截四种类型的接口：Executor、StatementHandler、ParameterHandler和ResultSetHandler。
 */
@Intercepts({
		@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class})
})
public class UpdatedByInterceptor implements Interceptor {
	private final UserContext<?, ?> userContext;
	
	public UpdatedByInterceptor(UserContext<?, ?> userContext) {
		this.userContext = userContext;
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object[] args = invocation.getArgs();
		Object parameters = args[1];
		if (parameters == null) {
			return invocation.proceed();
		}
		//根据当前用户名称设置修改人
		String currentUserName = userContext.getCurrentUserName();
		if (StringUtils.isNotBlank(currentUserName)) {
			List<Field> fields = ReflectUtils.findAllFields(parameters.getClass(), UpdatedByUserName.class);
			if (CollectionUtils.isNotEmpty(fields)) {
				for (Field field : fields) {
					field.setAccessible(true);
					if (String.class.isAssignableFrom(field.getType())) {
						field.set(parameters, currentUserName);
					}
				}
			}
		}
		//根据当前用户ID设置修改人
		Object currentUserId = userContext.getCurrentUserId();
		if (currentUserId != null) {
			List<Field> fields = ReflectUtils.findAllFields(parameters.getClass(), UpdatedByUserId.class);
			if (CollectionUtils.isNotEmpty(fields)) {
				for (Field field : fields) {
					field.setAccessible(true);
					if (field.getType().isAssignableFrom(currentUserId.getClass())) {
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
