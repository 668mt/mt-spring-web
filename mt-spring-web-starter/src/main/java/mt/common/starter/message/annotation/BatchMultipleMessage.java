package mt.common.starter.message.annotation;

import mt.common.starter.message.messagehandler.BatchMultipleMessageHandler;

import java.lang.annotation.*;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
@Documented
public @interface BatchMultipleMessage {
	String[] columns();
	
	Class<? extends BatchMultipleMessageHandler<?>> handlerClass();
	
	String[] params() default "";
}
