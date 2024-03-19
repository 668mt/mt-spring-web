package mt.spring.rocketmq.config.listener;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.rocketmq.client.apis.message.MessageView;

/**
 * @Author Martin
 * @Date 2024/3/19
 */
@Data
@AllArgsConstructor
public class MessageContext {
	private MessageView messageView;
	private byte[] body;
}
