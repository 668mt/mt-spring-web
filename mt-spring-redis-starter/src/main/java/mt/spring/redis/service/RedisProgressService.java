package mt.spring.redis.service;

import mt.spring.core.progress.ProgressService;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class RedisProgressService implements ProgressService {
	private final RedisService redisService;
	
	public RedisProgressService(@NotNull RedisService redisService) {
		this.redisService = redisService;
	}
	
	private String getKey(@NotNull String key) {
		return "progress:" + key;
	}
	
	@Override
	public void init(@NotNull String key) {
		update(getKey(key), 0);
	}
	
	@Override
	public void update(@NotNull String key, double percent) {
		redisService.set(getKey(key), percent, 1, TimeUnit.DAYS);
	}
	
	@Override
	public void add(@NotNull String key, double percent) {
		redisService.increment(getKey(key), percent);
	}
	
	@Override
	public void remove(@NotNull String key) {
		redisService.delete(getKey(key));
	}
	
	@Override
	public double getPercent(@NotNull String key) {
		Object value = redisService.get(getKey(key));
		if (value == null) {
			return 0;
		}
		return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
