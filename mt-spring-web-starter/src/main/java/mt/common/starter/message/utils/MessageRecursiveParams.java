package mt.common.starter.message.utils;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2024/5/26
 */
@Data
public class MessageRecursiveParams {
	private final Object target;
	
	public MessageRecursiveParams(Object target) {
		this.target = target;
	}
	
	private Map<BatchMessageKey, List<BatchHandleTarget>> batchHandleTarget;
	private Set<String> group;
	
	public MessageRecursiveParams copy(Object target) {
		MessageRecursiveParams messageRecursiveParams = new MessageRecursiveParams(target);
		messageRecursiveParams.batchHandleTarget = this.batchHandleTarget;
		messageRecursiveParams.group = this.group;
		return messageRecursiveParams;
	}
}
