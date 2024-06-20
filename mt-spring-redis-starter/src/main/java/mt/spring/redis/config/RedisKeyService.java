package mt.spring.redis.config;

import mt.spring.redis.config.properties.RedisConfigProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author Martin
 * @Date 2024/6/21
 */
@Service
public class RedisKeyService {
	@Value("${spring.application.name:default}")
	private String applicationName;
	@Value("${spring.profiles.active:}")
	private String profiles;
	@Autowired
	private RedisConfigProperties redisConfigProperties;
	
	public String getPrefix() {
		String prefix = redisConfigProperties.getPrefix();
		if (StringUtils.isBlank(prefix)) {
			prefix = applicationName;
			if (StringUtils.isNotBlank(profiles)) {
				prefix = prefix + "-" + profiles;
			}
		}
		return prefix;
	}
}
