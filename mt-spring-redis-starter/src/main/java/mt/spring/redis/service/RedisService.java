package mt.spring.redis.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2024/6/19
 */
public interface RedisService {
	RedisTemplate<String, Object> getRedisTemplate();
	
	String getRedisPrefix();
	
	LockService getLockService();
	
	void set(@NotNull String key, @NotNull Object value, long timeout, @NotNull TimeUnit timeUnit);
	
	void setJson(@NotNull String key, @NotNull Object value, long timeout, @NotNull TimeUnit timeUnit);
	
	void set(@NotNull String key, @NotNull Object value);
	
	Object get(@NotNull String key);
	
	<T> T getJson(@NotNull String key, @NotNull Class<T> clazz);
	
	String getString(@NotNull String key);
	
	void delete(@NotNull String key);
	
	boolean hasKey(@NotNull String key);
	
	void expire(@NotNull String key, long timeout, @NotNull TimeUnit timeUnit);
	
	Double increment(String key, double number);
}
