package mt.spring.rocketmq.config.listener;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * @Author Martin
 * @Date 2024/5/29
 */
public abstract class AbstractJsonTaskListener<T> extends AbstractMessageListener {
	@Override
	public void consume(@NotNull MessageContext messageContext) throws Throwable {
		byte[] body = messageContext.getBody();
		String json = new String(body, StandardCharsets.UTF_8);
		T task = JSONObject.parseObject(json, getType());
		consumeTask(task, messageContext.getMessageView());
	}
	
	/**
	 * 消费任务
	 *
	 * @param task        任务
	 * @param messageView 消息
	 */
	public abstract void consumeTask(@NotNull T task, @NotNull MessageView messageView) throws Throwable;
	
	protected abstract Class<T> getType();
}
