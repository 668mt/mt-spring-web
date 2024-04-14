package mt.spring.rocketmq.service;

import lombok.SneakyThrows;
import mt.spring.rocketmq.properties.RocketmqProperties;
import mt.spring.rocketmq.utils.RocketmqBuilder;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.shaded.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author Martin
 * @Date 2024/4/14
 */
@Service
public class RocketmqService {
	@Autowired
	private Producer producer;
	@Autowired
	private RocketmqBuilder rocketmqBuilder;
	@Autowired
	private RocketmqProperties rocketmqProperties;
	
	@SneakyThrows
	public void send(@NotNull String topic, byte[] body) {
		send(topic, body, null);
	}
	
	@SneakyThrows
	public void send(@NotNull String topic, byte[] body, @Nullable String tag, String... keys) {
		if (StringUtils.isNotBlank(rocketmqProperties.getTopicPrefix())) {
			topic = rocketmqProperties.getTopicPrefix() + topic;
		}
		MessageBuilder messageBuilder = rocketmqBuilder.messageBuilder(topic, body);
		messageBuilder.setTag(tag);
		messageBuilder.setKeys(keys);
		messageBuilder.build();
		producer.send(messageBuilder.build());
	}
}
