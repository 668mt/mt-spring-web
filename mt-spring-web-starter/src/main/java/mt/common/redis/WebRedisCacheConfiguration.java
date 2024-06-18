package mt.common.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@ConditionalOnMissingBean(CachingConfigurerSupport.class)
public class WebRedisCacheConfiguration extends CachingConfigurerSupport {
	
	@Value("${spring.application.name:default}")
	private String applicationName;
	
	@Override
	public KeyGenerator keyGenerator() {
		return (target, method, params) -> {
			String path = target.getClass().getName() + "." + method.getName();
			for (Object param : params) {
				path += ":" + param;
			}
			return path;
		};
	}
	
	@Bean
	@ConditionalOnBean({RedisCacheSupport.class})
	@ConditionalOnMissingBean(RedisCacheManager.class)
	public RedisCacheManager cacheManager(@Value("${spring.profiles.active:}") String profiles, RedisConnectionFactory factory, RedisCacheProperties redisCacheProperties, RedisCacheSupport redisCacheSupport) {
		
		RedisSerializer<String> redisSerializer = new StringRedisSerializer();
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		
		//解决查询缓存转换异常的问题
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);
		
		//配置序列化(解决乱码的问题)
		String prefix = redisCacheProperties.getPrefix();
		if (StringUtils.isBlank(prefix)) {
			prefix = applicationName;
			if (StringUtils.isNotBlank(profiles)) {
				prefix = prefix + "-" + profiles;
			}
		}
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
			.defaultCacheConfig()
			.entryTtl(Duration.ofHours(1))
			.prefixCacheNameWith(prefix + ":")
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
//				.disableCachingNullValues()
			;
		
		// 对每个缓存空间应用不同的配置
		Map<String, RedisCacheConfiguration> configMap = redisCacheSupport.getConfigurations(defaultConfig);
		
		return RedisCacheManager.builder(factory).cacheDefaults(defaultConfig).initialCacheNames(configMap.keySet())// 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
			.withInitialCacheConfigurations(configMap).build();
	}
	
}