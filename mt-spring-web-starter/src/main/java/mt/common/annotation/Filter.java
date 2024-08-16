package mt.common.annotation;


import mt.common.converter.Converter;
import mt.common.converter.DefaultConverter;
import mt.common.tkmapper.ConditionFilterParser;
import mt.common.tkmapper.DefaultConditionFilterParser;
import mt.common.tkmapper.Operator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 过滤器
 *
 * @ClassName: Filter
 * @Description:
 * @Author Martin
 * @date 2017-11-14 上午10:00:56
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Filter {
	/**
	 * 列名
	 *
	 * @return
	 */
	String column() default "";
	
	/**
	 * 过滤符号
	 *
	 * @return
	 */
	Operator operator() default Operator.eq;
	
	/**
	 * 参数内容前缀
	 *
	 * @return
	 */
	String prefix() default "";
	
	/**
	 * 参数内容后缀
	 *
	 * @return
	 */
	String suffix() default "";
	
	/**
	 * 转换器
	 *
	 * @return
	 */
	Class<? extends Converter<?>> converter() default DefaultConverter.class;
	
	/**
	 * 只有当operator为condition时有效
	 *
	 * @return sql
	 */
	String sql() default "";
	
	Class<? extends ConditionFilterParser<?>> parserClass() default DefaultConditionFilterParser.class;
	
	/**
	 * 解析器入参
	 *
	 * @return
	 */
	String[] parserParams() default {};
}
