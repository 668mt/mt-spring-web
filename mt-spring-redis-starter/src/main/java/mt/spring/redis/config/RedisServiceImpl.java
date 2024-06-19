package mt.spring.redis.config;

import mt.spring.redis.service.LockService;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author Martin
 * @Date 2024/6/19
 */
public record RedisServiceImpl(RedisTemplate<String, Object> redisTemplate,
							   LockService lockService) implements RedisService {
	@Override
	public RedisTemplate<String, Object> getRedisTemplate() {
		return redisTemplate;
	}
	
	@Override
	public LockService getLockService() {
		return lockService;
	}
}
