package mt.common.redis;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author Martin
 * @Date 2024/6/19
 */
public interface RedisService {
	RedisTemplate<String, Object> getRedisTemplate();
	
	LockService getLockService();
}
