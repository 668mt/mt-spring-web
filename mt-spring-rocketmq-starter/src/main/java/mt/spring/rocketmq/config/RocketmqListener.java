package mt.spring.rocketmq.config;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Martin
 * @Date 2024/3/19
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RocketmqListener {
	/**
	 * 是否启用
	 *
	 * @return 是否启用
	 */
	String enabled() default "true";
	
	/**
	 * topic
	 *
	 * @return topic
	 */
	String topic();
	
	/**
	 * 消费者组
	 *
	 * @return 消费者组
	 */
	String consumerGroup();
	
	String maxCacheMessageCount() default "1024";
	
	String consumptionThreadCount() default "20";
	
	/**
	 * 监听tag表达式
	 *
	 * @return tag表达式
	 */
	String tagExpression() default "*";
	
	boolean useGlobalPrefix() default true;
}
