package mt.spring.redis.service;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2024/6/19
 */
@Getter
public class RedisServiceImpl implements RedisService {
	
	private final RedisTemplate<String, Object> redisTemplate;
	private final LockService lockService;
	private final String redisPrefix;
	
	public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate, LockService lockService) {
		this(redisTemplate, lockService, "");
	}
	
	public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate, LockService lockService, String redisPrefix) {
		this.redisTemplate = redisTemplate;
		this.lockService = lockService;
		this.redisPrefix = redisPrefix;
	}
	
	private String getKey(String key) {
		if (StringUtils.isBlank(redisPrefix)) {
			return key;
		}
		return redisPrefix + ":" + key;
	}
	
	@Override
	public void set(@NotNull String key, @NotNull Object value, long timeout, @NotNull TimeUnit timeUnit) {
		redisTemplate.opsForValue().set(getKey(key), value, timeout, timeUnit);
	}
	
	@Override
	public void setJson(@NotNull String key, @NotNull Object value, long timeout, @NotNull TimeUnit timeUnit) {
		redisTemplate.opsForValue().set(getKey(key), JSONObject.toJSONString(value), timeout, timeUnit);
	}
	
	@Override
	public void set(@NotNull String key, @NotNull Object value) {
		redisTemplate.opsForValue().set(getKey(key), value);
	}
	
	@Override
	public Object get(@NotNull String key) {
		return redisTemplate.opsForValue().get(getKey(key));
	}
	
	@Override
	public <T> T getJson(@NotNull String key, @NotNull Class<T> clazz) {
		Object value = get(key);
		return value == null ? null : JSONObject.parseObject(value.toString(), clazz);
	}
	
	@Override
	public String getString(@NotNull String key) {
		Object value = get(key);
		return value == null ? null : value.toString();
	}
	
	@Override
	public void delete(@NotNull String key) {
		redisTemplate.delete(getKey(key));
	}
	
	@Override
	public boolean hasKey(@NotNull String key) {
		Boolean hasKey = redisTemplate.hasKey(getKey(key));
		return hasKey != null && hasKey;
	}
	
	@Override
	public void expire(@NotNull String key, long timeout, @NotNull TimeUnit timeUnit) {
		redisTemplate.expire(getKey(key), timeout, timeUnit);
	}
	
	@Override
	public Double increment(String key, double number) {
		return redisTemplate.opsForValue().increment(getKey(key), number);
	}
}
