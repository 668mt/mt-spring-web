package mt.spring.rocketmq;

import mt.spring.rocketmq.config.listener.AbstractMessageListener;
import mt.spring.rocketmq.config.listener.MessageContext;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * @Author Martin
 * @Date 2025/2/16
 */
public class TestConsumer {
	public static void main(String[] args) throws Exception {
		ClientConfiguration configuration = ClientConfiguration.newBuilder()
			.setEndpoints("192.168.0.104:8081").enableSsl(false).build();
		String topic = "video-logs";
		String consumerGroup = "flink";
		FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);
		Map<String, FilterExpression> filterExpressionMap = new HashMap<>();
		filterExpressionMap.put(topic, filterExpression);
		
		ClientServiceProvider provider = ClientServiceProvider.loadService();
		SimpleConsumer simpleConsumer = provider.newSimpleConsumerBuilder()
			.setConsumerGroup(consumerGroup)
			.setClientConfiguration(configuration)
			.setSubscriptionExpressions(filterExpressionMap)
			.setAwaitDuration(Duration.ofSeconds(30))
			.build();
		
//		while (true) {
			List<MessageView> messageViews = simpleConsumer.receive(1, Duration.ofSeconds(10));
			for (MessageView messageView : messageViews) {
				long bornTimestamp = messageView.getBornTimestamp();
				ByteBuffer buffer = messageView.getBody();
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				String content = new String(bytes, StandardCharsets.UTF_8);
				String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(bornTimestamp));
				System.out.println("接收到消息：" + date + "," + content);
				simpleConsumer.ack(messageView);
			}
//		}
		simpleConsumer.close();
//		PushConsumer consumer = provider.newPushConsumerBuilder()
//			.setMaxCacheMessageCount(1)
//			.setClientConfiguration(configuration)
//			.setConsumptionThreadCount(5)
//			.setConsumerGroup(consumerGroup)
//			.setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
//			.setMessageListener(new AbstractMessageListener() {
//				@Override
//				public void consume(@NotNull MessageContext messageContext) throws Throwable {
//					byte[] body = messageContext.getBody();
//					System.out.println(new String(body, StandardCharsets.UTF_8));
//				}
//			})
//			.build();
	}
}
