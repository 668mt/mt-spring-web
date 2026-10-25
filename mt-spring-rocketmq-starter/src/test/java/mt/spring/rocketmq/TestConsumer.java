package mt.spring.rocketmq;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2025/2/16
 */
public class TestConsumer {
	public static void main(String[] args) throws Exception {
		// 5.x gRPC 客户端期望 endpoints 带 scheme（grpc:// 或 grpcs://）。
		ClientConfiguration configuration = ClientConfiguration.newBuilder()
			.setEndpoints("192.168.3.69:8083").enableSsl(false).build();
//		String topic = "test-topic2";
		String topic = "mukeyuan-video-dev-video-transfer";
		String consumerGroup = "flink";
		FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);
		Map<String, FilterExpression> filterExpressionMap = new HashMap<>();
		filterExpressionMap.put(topic, filterExpression);

		ClientServiceProvider provider = ClientServiceProvider.loadService();

		// === Step 1: 用 Producer 先 send 一条，让 broker 自动建出 topic ===
		// 在 5.x gRPC client 模型下：subscribe 不会触发自动建，只有 send 才会。
		// broker 端要 autoCreateTopicEnable=true（broker.conf 已开）。
		System.out.println("[bootstrap] start sending bootstrap message to topic=" + topic);
		Producer bootstrapProducer = provider.newProducerBuilder()
			.setClientConfiguration(configuration)
			.setTopics(topic)
			.build();
		Message bootstrapMsg = provider.newMessageBuilder()
			.setTopic(topic)
			.setBody("topic-bootstrap".getBytes(StandardCharsets.UTF_8))
			.build();

		SendReceipt bootstrapReceipt;
		try {
			bootstrapReceipt = bootstrapProducer.send(bootstrapMsg);
			System.out.println("[bootstrap] success, msgId=" + bootstrapReceipt.getMessageId());
		} catch (Throwable t) {
			// 关键修复 2：如果 bootstrap 失败，consumer 起来照样拿不到 route，会抛 NotFoundException。
			// 这里把异常和诊断信息都打出来，方便排查（不要"吞掉后又跑"）。
			System.err.println("[bootstrap] FAILED. broker 不会建出 topic " + topic + "，consumer 起来会报 NotFoundException。");
			System.err.println("[bootstrap] common causes:");
			System.err.println("  1. endpoints 缺少 scheme（应为 grpc://host:port）");
			System.err.println("  2. broker 端 autoCreateTopicEnable=false（broker.conf 设 true）");
			System.err.println("  3. proxy gRPC 端口不通（默认 8083）");
			t.printStackTrace();
			throw t;
		} finally {
			bootstrapProducer.close();
		}

		// 关键修复 3：给 namesrv 一点时间同步 topic route 到 proxy 的元数据缓存
		// 否则 simpleConsumer.build() 会立刻 QueryRoute 失败
		Thread.sleep(500);

		// === Step 2: 建 SimpleConsumer 订阅 ===
		SimpleConsumer simpleConsumer = provider.newSimpleConsumerBuilder()
			.setConsumerGroup(consumerGroup)
			.setClientConfiguration(configuration)
			.setSubscriptionExpressions(filterExpressionMap)
			.setAwaitDuration(Duration.ofSeconds(30))
			.build();

		System.out.println("[consumer] started, listening on topic=" + topic + " group=" + consumerGroup);
		while (true) {
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
		}
//		simpleConsumer.close();
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
