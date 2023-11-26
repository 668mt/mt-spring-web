package mt.common.paramcheck;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @Author Martin
 * @Date 2021/8/29
 */
public class ParameterCheckers {
	public static String getDefaultErrorMsg(Object target, Method method, Parameter parameter, Class<? extends Annotation> annotationClass, String message) {
		return String.format("Argument for @%s parameter '%s' of %s.%s %s",
			annotationClass.getSimpleName(),
			parameter.getName(),
			target.getClass().getName().replace(".", "/"),
			method.getName(),
			message);
	}
	
	public static class NotNullChecker implements ParameterChecker<org.jetbrains.annotations.NotNull, Object> {
		
		@Override
		public boolean isValid(Object value) {
			return value != null;
		}
		
		@Override
		public String errorMsg(org.jetbrains.annotations.NotNull annotation, Method method, Parameter parameter, Object value, Object target) {
			String message = annotation.value();
			if (StringUtils.isNotBlank(message)) {
				return message;
			}
			return getDefaultErrorMsg(target, method, parameter, org.jetbrains.annotations.NotNull.class, "must not be null");
		}
	}
}
