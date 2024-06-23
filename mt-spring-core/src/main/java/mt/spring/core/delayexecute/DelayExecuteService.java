package mt.spring.core.delayexecute;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @Author Martin
 * @Date 2024/6/23
 */
public interface DelayExecuteService {
	/**
	 * 注册延迟执行
	 *
	 * @param taskId   任务标识
	 * @param consumer 任务执行回调
	 */
	void register(@NotNull String taskId, @NotNull Consumer<String> consumer);
	
	/**
	 * 添加任务
	 *
	 * @param taskId      任务标识
	 * @param taskContent 任务内容
	 * @param expiredTime 过期时间
	 */
	void addTask(@NotNull String taskId, @NotNull String taskContent, long expiredTime);
	
	/**
	 * 清除所有任务
	 */
	void clear();
	
	/**
	 * 停止
	 */
	void shutdown();
}
