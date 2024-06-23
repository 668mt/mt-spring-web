package test.delayexecute;

import mt.spring.redis.service.RedisDelayExecutor;
import org.junit.Test;
import test.redis.AbstractRedisTest;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public class TestDelayExecute extends AbstractRedisTest {
	
	@Test
	public void test() throws InterruptedException {
		RedisDelayExecutor redisDelayExecutor = new RedisDelayExecutor(redisService);
		String taskId = "test";
		redisDelayExecutor.register(taskId, System.out::println);
		redisDelayExecutor.addTask(taskId, "test1", System.currentTimeMillis() + 1000);
		redisDelayExecutor.addTask(taskId, "test2", System.currentTimeMillis() + 2000);
		Thread.sleep(3000);
		redisDelayExecutor.pull();
		redisTemplate.delete(redisDelayExecutor.getKey());
	}
}
