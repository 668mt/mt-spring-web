package mt.spring.redis.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mt.spring.core.delayexecute.AbstractDelayExecutor;
import mt.spring.core.thread.NamePrefixThreadFactory;
import mt.spring.redis.config.RedisService;
import mt.utils.common.CollectionUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
@Slf4j
public class RedisDelayExecutor extends AbstractDelayExecutor {
	@Getter
	private final String key;
	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
	private final RedisService redisService;
	
	public RedisDelayExecutor(RedisService redisService, String redisKey, Consumer<String> consumer) {
		super(consumer);
		this.key = redisKey;
		log.info("RedisDelayExecutor init,redisKey: {}", redisKey);
		this.redisService = redisService;
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new NamePrefixThreadFactory("delay-exe-" + redisKey + "-"));
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(this::tryPull, 0, 10, TimeUnit.SECONDS);
	}
	
	public boolean tryPull() {
		String lockKey = "delay-execute-lock-" + key;
		RedissonClient redissonClient = redisService.getLockService().getRedissonClient();
		RLock lock = redissonClient.getLock(lockKey);
		try {
			if (lock.tryLock()) {
				doPull();
				return true;
			}
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
		return false;
	}
	
	public void pull() {
		String lockKey = "delay-execute-lock-" + key;
		RedissonClient redissonClient = redisService.getLockService().getRedissonClient();
		RLock lock = redissonClient.getLock(lockKey);
		try {
			lock.lock();
			doPull();
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
	
	private void doPull() {
		long currentTimeMillis = System.currentTimeMillis();
		Set<Object> objects;
		int pageSize = 1000;
		do {
			objects = redisService.getRedisTemplate().opsForZSet().rangeByScore(key, 0, currentTimeMillis, 0, pageSize);
			if (CollectionUtils.isNotEmpty(objects)) {
				for (Object object : objects) {
					//消费
					consumer.accept(object.toString());
					//删除
					redisService.getRedisTemplate().opsForZSet().remove(key, object);
				}
			}
		} while (CollectionUtils.isNotEmpty(objects) && objects.size() == pageSize);
	}
	
	public void register(String member, long expiredTime) {
		redisService.getRedisTemplate().opsForZSet().add(key, member, expiredTime);
	}
	
	public void shutdown() {
		scheduledThreadPoolExecutor.shutdown();
	}
	
	@Override
	public void clear() {
		redisService.getRedisTemplate().delete(key);
	}
}
