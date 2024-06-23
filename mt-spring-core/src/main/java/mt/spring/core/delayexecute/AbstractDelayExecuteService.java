package mt.spring.core.delayexecute;

import mt.utils.common.Assert;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public abstract class AbstractDelayExecuteService implements DelayExecuteService {
	protected final Map<String, Consumer<String>> taskConsumerMap = new HashMap<>();
	
	@Override
	public void register(@NotNull String taskId, @NotNull Consumer<String> runnable) {
		Consumer<String> existsRunnable = taskConsumerMap.get(taskId);
		Assert.isNull(existsRunnable, "taskId已存在:" + taskId);
		taskConsumerMap.put(taskId, runnable);
	}
	
	protected Consumer<String> getTaskConsumer(String taskId) {
		return taskConsumerMap.get(taskId);
	}
}
