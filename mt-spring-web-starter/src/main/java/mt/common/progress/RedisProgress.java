package mt.common.progress;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class RedisProgress implements Progress {
	private final RedisTemplate<String, Object> redisTemplate;
	private final String keyPrefix;
	
	public RedisProgress(@NotNull RedisTemplate<String, Object> redisTemplate, @NotNull String keyPrefix) {
		this.redisTemplate = redisTemplate;
		this.keyPrefix = keyPrefix;
	}
	
	public RedisProgress(@NotNull RedisTemplate<String, Object> redisTemplate) {
		this(redisTemplate, "");
	}
	
	private String getKey(@NotNull String key) {
		return "progress:" + keyPrefix + ":" + key;
	}
	
	@Override
	public void init(@NotNull String key) {
		update(key, 0);
	}
	
	@Override
	public void update(@NotNull String key, double percent) {
		redisTemplate.opsForValue().set(getKey(key), percent, 1, TimeUnit.DAYS);
	}
	
	@Override
	public void add(@NotNull String key, double percent) {
		redisTemplate.opsForValue().increment(getKey(key), percent);
	}
	
	@Override
	public void remove(@NotNull String key) {
		redisTemplate.delete(getKey(key));
	}
	
	@Override
	public double getPercent(@NotNull String key) {
		Object value = redisTemplate.opsForValue().get(getKey(key));
		if (value == null) {
			return 0;
		}
		return Double.parseDouble(value.toString());
	}
}
