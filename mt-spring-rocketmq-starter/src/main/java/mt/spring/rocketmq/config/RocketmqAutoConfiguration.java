package mt.spring.rocketmq.config;

import lombok.extern.slf4j.Slf4j;
import mt.spring.rocketmq.properties.RocketmqProperties;
import mt.spring.rocketmq.utils.RocketmqBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author Martin
 * @Date 2024/3/19
 */
@EnableConfigurationProperties(RocketmqProperties.class)
@ComponentScan(basePackages = "mt.spring.rocketmq")
@Slf4j
@ConditionalOnBean(MessageListener.class)
@ConditionalOnProperty(name = "rocketmq.enabled", matchIfMissing = true)
public class RocketmqAutoConfiguration {
	@Autowired
	private RocketmqBuilder rocketmqBuilder;
	@Autowired
	private ConfigurableListableBeanFactory beanFactory;
	private final AtomicBoolean isInited = new AtomicBoolean(false);
	
	@EventListener
	public void init(ContextRefreshedEvent contextRefreshedEvent) throws ClientException {
		Map<String, Object> beansWithAnnotation = beanFactory.getBeansWithAnnotation(RocketmqListener.class);
		if (beansWithAnnotation.isEmpty()) {
			return;
		}
		if (isInited.getAndSet(true)) {
			return;
		}
		for (Map.Entry<String, Object> stringObjectEntry : beansWithAnnotation.entrySet()) {
			String beanName = stringObjectEntry.getKey();
			Object bean = stringObjectEntry.getValue();
			if (bean instanceof MessageListener) {
				MessageListener messageListener = (MessageListener) bean;
				log.info("初始化RocketmqListener:{}", beanName);
				RocketmqListener rocketmqListener = bean.getClass().getAnnotation(RocketmqListener.class);
				PushConsumer consumer = rocketmqBuilder.createConsumer(rocketmqListener, messageListener);
				if (consumer == null) {
					continue;
				}
				beanFactory.registerSingleton(beanName + ".consumer", consumer);
			}
		}
	}
	
	@Bean
	public Producer producer() throws ClientException {
		return rocketmqBuilder.createProducer();
	}
}
