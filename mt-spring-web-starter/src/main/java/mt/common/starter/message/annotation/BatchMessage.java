package mt.common.starter.message.annotation;

import mt.common.starter.message.messagehandler.BatchMessageHandler;
import mt.common.starter.message.messagehandler.MessageHandler;

import java.lang.annotation.*;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
@Documented
public @interface BatchMessage {
	String column();
	
	Class<? extends BatchMessageHandler<?, ?>> handlerClass();
	
	String[] params() default "";
}
