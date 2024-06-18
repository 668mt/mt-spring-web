package test.delayexecute;

import mt.common.delayexecute.RedisDelayExecutor;
import mt.common.redis.RedisServiceImpl;
import org.junit.Test;
import test.redis.AbstractRedisTest;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public class TestDelayExecute extends AbstractRedisTest {
	
	@Test
	public void test() throws InterruptedException {
		RedisDelayExecutor redisDelayExecutor = new RedisDelayExecutor(redisService, "delay-exe-test", s -> {
			System.out.println("消费：" + s);
		});
		redisDelayExecutor.register("test1", System.currentTimeMillis() + 1000);
		redisDelayExecutor.register("test2", System.currentTimeMillis() + 2000);
		Thread.sleep(3000);
		redisDelayExecutor.pull();
		redisTemplate.delete(redisDelayExecutor.getKey());
	}
}
