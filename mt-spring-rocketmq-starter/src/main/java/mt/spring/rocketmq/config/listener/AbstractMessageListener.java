package mt.spring.rocketmq.config.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * @Author Martin
 * @Date 2024/3/19
 */
@Slf4j
public abstract class AbstractMessageListener implements MessageListener {
	@Override
	public ConsumeResult consume(MessageView messageView) {
		try {
			ByteBuffer buffer = messageView.getBody();
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			consume(new MessageContext(messageView, bytes));
			return ConsumeResult.SUCCESS;
		} catch (Throwable e) {
			log.error("任务执行出错：{}", e.getMessage(), e);
			return ConsumeResult.FAILURE;
		}
	}
	
	/**
	 * 消费消息
	 *
	 * @param messageContext 消息上下文
	 * @throws Throwable 异常
	 */
	public abstract void consume(@NotNull MessageContext messageContext) throws Throwable;
}
