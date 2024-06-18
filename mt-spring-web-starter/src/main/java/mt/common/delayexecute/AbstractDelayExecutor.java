package mt.common.delayexecute;

import java.util.function.Consumer;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public abstract class AbstractDelayExecutor {
	protected final Consumer<String> consumer;
	
	public AbstractDelayExecutor(Consumer<String> consumer) {
		this.consumer = consumer;
	}
	
	/**
	 * 注册延迟执行
	 *
	 * @param member      任务标识
	 * @param expiredTime 过期时间
	 */
	public abstract void register(String member, long expiredTime);
	
	public abstract void clear();
}
