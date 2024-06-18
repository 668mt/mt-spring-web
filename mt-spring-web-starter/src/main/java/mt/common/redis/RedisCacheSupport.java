package mt.common.redis;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/4/1
 */
public interface RedisCacheSupport {
	/**
	 * 获取缓存配置
	 *
	 * @return key为缓存名称，value为缓存配置
	 */
	Map<String, RedisCacheConfiguration> getConfigurations(RedisCacheConfiguration defaultConfiguration);
}
