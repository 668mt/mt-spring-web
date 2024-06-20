package mt.spring.redis.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2023/4/1
 */
@ConfigurationProperties(prefix = "redis")
@Data
@Component
public class RedisConfigProperties {
	/**
	 * 默认是${spring.application.name}-${spring.profiles.active}
	 */
	private String prefix;
}
