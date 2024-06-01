package mt.common.context.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface UseFilterContextField {
	/**
	 * 上下文名称
	 *
	 * @return 上下文名称
	 */
	String contextName();
}
