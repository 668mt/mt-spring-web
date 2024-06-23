package mt.spring.redis.service;

import cn.hutool.cron.CronUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mt.spring.core.delayexecute.AbstractDelayExecutor;
import mt.utils.common.Assert;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Set;
import java.util.function.Consumer;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
@Slf4j
public class RedisDelayExecutor extends AbstractDelayExecutor {
	@Getter
	private final String key;
	private final RedisService redisService;
	private final String cronKey;
	
	public RedisDelayExecutor(RedisService redisService) {
		String key = redisService.getRedisPrefix() + ":delay-execute";
		this.key = key;
		log.info("RedisDelayExecutor init,redisKey: {}", key);
		this.redisService = redisService;
		this.cronKey = "delayExecute:" + key;
		CronUtil.schedule(this.cronKey, "0/10 * * * * ?", this::tryPull);
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
					try {
						//消费
						Task task = JSONObject.parseObject(object.toString(), Task.class);
						String taskId = task.getTaskId();
						String taskContent = task.getTaskContent();
						Consumer<String> taskConsumer = getTaskConsumer(taskId);
						Assert.notNull(taskConsumer, "delayExecute:未找到对应的任务消费者，请先注册。taskId=" + taskId);
						taskConsumer.accept(taskContent);
					} catch (Exception e) {
						log.error("任务执行失败:{}", e.getMessage(), e);
					} finally {
						//删除
						redisService.getRedisTemplate().opsForZSet().remove(key, object);
					}
				}
			}
		} while (CollectionUtils.isNotEmpty(objects) && objects.size() == pageSize);
	}
	
	@Override
	public void addTask(@NotNull String taskId, @NotNull String taskContent, long expiredTime) {
		Task task = new Task(taskId, taskContent);
		redisService.getRedisTemplate().opsForZSet().add(key, JSONObject.toJSONString(task), expiredTime);
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Task {
		private String taskId;
		private String taskContent;
	}
	
	@Override
	public void shutdown() {
		CronUtil.remove(cronKey);
	}
	
	@Override
	public void clear() {
		redisService.getRedisTemplate().delete(key);
	}
}
