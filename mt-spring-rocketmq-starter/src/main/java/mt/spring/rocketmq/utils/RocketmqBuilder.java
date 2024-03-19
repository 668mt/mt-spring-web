package mt.spring.rocketmq.utils;

import lombok.extern.slf4j.Slf4j;
import mt.spring.rocketmq.config.RocketmqListener;
import mt.spring.rocketmq.properties.RocketmqProperties;
import org.apache.rocketmq.client.apis.*;
import org.apache.rocketmq.client.apis.consumer.*;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.shaded.commons.lang3.AnnotationUtils;
import org.apache.rocketmq.shaded.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @Author Martin
 * @Date 2024/3/19
 */
@Slf4j
@Component("mtRocketmqBuilder")
public class RocketmqBuilder {
	private static final ClientServiceProvider provider = ClientServiceProvider.loadService();
	private final RocketmqProperties rocketmqProperties;
	private final Environment environment;
	
	public RocketmqBuilder(Environment environment, RocketmqProperties rocketmqProperties) {
		this.environment = environment;
		this.rocketmqProperties = rocketmqProperties;
	}
	
	public ClientConfiguration createClientConfiguration() {
		String accessKey = rocketmqProperties.getAccessKey();
		String accessSecret = rocketmqProperties.getAccessSecret();
		boolean enableSsl = rocketmqProperties.getEnableSsl() != null && rocketmqProperties.getEnableSsl();
		SessionCredentialsProvider sessionCredentialsProvider = null;
		if (StringUtils.isNotBlank(accessSecret) && StringUtils.isNotBlank(accessSecret)) {
			sessionCredentialsProvider = new StaticSessionCredentialsProvider(accessKey, accessSecret);
		}
		ClientConfigurationBuilder clientConfigurationBuilder = ClientConfiguration.newBuilder()
			.setEndpoints(rocketmqProperties.getEndPoints())
			.enableSsl(enableSsl);
		if (sessionCredentialsProvider != null) {
			clientConfigurationBuilder.setCredentialProvider(sessionCredentialsProvider);
		}
		return clientConfigurationBuilder.build();
	}
	
	public Producer createProducer() throws ClientException {
		return provider.newProducerBuilder().setClientConfiguration(createClientConfiguration()).build();
	}
	
	public Message createMessage(@NotNull String topic, @NotNull byte[] body, @Nullable String keys, @Nullable String tag) {
		MessageBuilder messageBuilder = messageBuilder(topic, body);
		if (StringUtils.isNotBlank(keys)) {
			messageBuilder.setKeys(keys);
		}
		if (StringUtils.isNotBlank(tag)) {
			messageBuilder.setTag(tag);
		}
		return messageBuilder.build();
	}
	
	public MessageBuilder messageBuilder(@NotNull String topic, @NotNull byte[] body) {
		return provider.newMessageBuilder().setTopic(topic).setBody(body);
	}
	
	@Nullable
	public PushConsumer createConsumer(@NotNull RocketmqListener rocketmqListener, @NotNull MessageListener messageListener) throws ClientException {
		boolean useGlobalPrefix = rocketmqListener.useGlobalPrefix();
		String topicPrefix = useGlobalPrefix && StringUtils.isNotBlank(rocketmqProperties.getTopicPrefix()) ? rocketmqProperties.getTopicPrefix() : "";
		String topic = environment.resolvePlaceholders(topicPrefix + rocketmqListener.topic()).replace(":", "");
		String consumerGroup = environment.resolvePlaceholders(topicPrefix + rocketmqListener.consumerGroup()).replace(":","");
		log.info("createConsumer,topic:{}", topic);
		boolean enabled = toBoolean(rocketmqListener.enabled());
		if (!enabled) {
			log.info("listener is disabled,topic:{}", topic);
			return null;
		}
		FilterExpression filterExpression = new FilterExpression(environment.resolvePlaceholders(rocketmqListener.tagExpression()), FilterExpressionType.TAG);
		return provider.newPushConsumerBuilder()
			.setMaxCacheMessageCount(toInt(rocketmqListener.maxCacheMessageCount()))
			.setClientConfiguration(createClientConfiguration())
			.setConsumptionThreadCount(toInt(rocketmqListener.consumptionThreadCount()))
			.setConsumerGroup(consumerGroup)
			.setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
			.setMessageListener(messageListener)
			.build();
	}
	
	private int toInt(String value) {
		return Integer.parseInt(environment.resolvePlaceholders(value));
	}
	
	private boolean toBoolean(String value) {
		return Boolean.parseBoolean(environment.resolvePlaceholders(value));
	}
}
