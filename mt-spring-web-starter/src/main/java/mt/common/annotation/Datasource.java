package mt.common.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Martin
 * @Date 2024/3/30
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@Repository
public @interface Datasource {
	/**
	 * 数据源名称
	 *
	 * @return
	 */
	String sqlSessionFactoryRef() default "";
	
	/**
	 * sqlSessionTemplate名称
	 *
	 * @return
	 */
	String sqlSessionTemplateRef() default "";
}
