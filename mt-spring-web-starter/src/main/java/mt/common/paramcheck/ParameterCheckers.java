package mt.common.paramcheck;

import mt.utils.common.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;

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
	
	public static class NotNullChecker1 implements ParameterChecker<NotNull, Object> {
		
		@Override
		public boolean isValid(Object value) {
			return value != null;
		}
		
		@Override
		public String errorMsg(NotNull annotation, Method method, Parameter parameter, Object value, Object target) {
			String message = annotation.message();
			if (StringUtils.isNotBlank(message) && !message.startsWith("{")) {
				return message;
			}
			return getDefaultErrorMsg(target, method, parameter, NotNull.class, "must not be null");
		}
		
	}
	
	public static class NotNullChecker2 implements ParameterChecker<org.jetbrains.annotations.NotNull, Object> {
		
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
	
	public static class NotEmptyChecker implements ParameterChecker<NotEmpty, Object> {
		
		@Override
		public boolean isValid(Object value) {
			if (value == null) {
				return false;
			}
			if (value instanceof Collection) {
				return CollectionUtils.isNotEmpty((Collection<?>) value);
			} else if (value instanceof Object[]) {
				return ArrayUtils.isNotEmpty((Object[]) value);
			} else if (value instanceof int[]) {
				return ArrayUtils.isNotEmpty((int[]) value);
			} else if (value instanceof boolean[]) {
				return ArrayUtils.isNotEmpty((boolean[]) value);
			} else if (value instanceof long[]) {
				return ArrayUtils.isNotEmpty((long[]) value);
			} else if (value instanceof float[]) {
				return ArrayUtils.isNotEmpty((float[]) value);
			} else if (value instanceof double[]) {
				return ArrayUtils.isNotEmpty((double[]) value);
			} else if (value instanceof short[]) {
				return ArrayUtils.isNotEmpty((short[]) value);
			} else if (value instanceof char[]) {
				return ArrayUtils.isNotEmpty((char[]) value);
			} else if (value instanceof byte[]) {
				return ArrayUtils.isNotEmpty((byte[]) value);
			}
			return true;
		}
		
		@Override
		public String errorMsg(NotEmpty annotation, Method method, Parameter parameter, Object value, Object target) {
			String message = annotation.message();
			if (StringUtils.isNotBlank(message) && !message.startsWith("{")) {
				return message;
			}
			return getDefaultErrorMsg(target, method, parameter, NotEmpty.class, "must not be empty");
		}
	}
	
	public static class NotBlankChecker implements ParameterChecker<NotBlank, String> {
		
		@Override
		public boolean isValid(String value) {
			return StringUtils.isNotBlank(value);
		}
		
		@Override
		public String errorMsg(NotBlank annotation, Method method, Parameter parameter, String value, Object target) {
			String message = annotation.message();
			if (StringUtils.isNotBlank(message) && !message.startsWith("{")) {
				return message;
			}
			return getDefaultErrorMsg(target, method, parameter, NotBlank.class, "must not be blank");
		}
	}
	
}
