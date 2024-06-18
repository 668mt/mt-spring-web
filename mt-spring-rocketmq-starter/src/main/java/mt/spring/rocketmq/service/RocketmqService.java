package mt.spring.rocketmq.service;

import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import mt.spring.rocketmq.properties.RocketmqProperties;
import mt.spring.rocketmq.utils.RocketmqBuilder;
import mt.spring.rocketmq.utils.TopicNameUtils;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.shaded.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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
	
	public void sendJsonString(@NotNull String topic, Object task) {
		send(topic, JSONObject.toJSONString(task).getBytes(StandardCharsets.UTF_8));
	}
	
	@SneakyThrows
	public void send(@NotNull String topic, byte[] body) {
		send(topic, body, null);
	}
	
	@SneakyThrows
	public void send(@NotNull String topic, byte[] body, @Nullable String tag, String... keys) {
		if (StringUtils.isNotBlank(rocketmqProperties.getTopicPrefix())) {
			topic = rocketmqProperties.getTopicPrefix() + topic;
		}
		topic = TopicNameUtils.getSafetyTopicName(topic);
		MessageBuilder messageBuilder = rocketmqBuilder.messageBuilder(topic, body);
		if (StringUtils.isNotBlank(tag)) {
			messageBuilder.setTag(tag);
		}
		if (keys != null && keys.length > 0) {
			messageBuilder.setKeys(keys);
		}
		messageBuilder.build();
		producer.send(messageBuilder.build());
	}
}
