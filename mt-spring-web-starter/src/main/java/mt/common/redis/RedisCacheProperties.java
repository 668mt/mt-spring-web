package mt.common.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author Martin
 * @Date 2023/4/1
 */
@ConfigurationProperties(prefix = "redis.cache")
@Data
public class RedisCacheProperties {
	/**
	 * 默认是${spring.application.name}-${spring.profiles.active}
	 */
	private String prefix;
}
