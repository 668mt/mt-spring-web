package mt.spring.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
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

@ConditionalOnMissingBean(CachingConfigurer.class)
public class WebRedisCacheConfiguration implements CachingConfigurer {
	
	@Override
	public CacheManager cacheManager() {
		return null;
	}
	
	@Override
	public CacheResolver cacheResolver() {
		return null;
	}
	
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
	
	@Override
	public CacheErrorHandler errorHandler() {
		return null;
	}
	
	private Jackson2JsonRedisSerializer<Object> createJacksonSerializer() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
		jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
		return jackson2JsonRedisSerializer;
	}
	
	@Bean
	@ConditionalOnBean({RedisCacheSupport.class})
	@ConditionalOnMissingBean(RedisCacheManager.class)
	public RedisCacheManager cacheManager(RedisKeyService redisKeyService, RedisConnectionFactory factory, RedisCacheSupport redisCacheSupport) {
		RedisSerializer<String> redisSerializer = new StringRedisSerializer();
		//配置序列化(解决乱码的问题)
		String prefix = redisKeyService.getPrefix();
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
			.defaultCacheConfig()
			.entryTtl(Duration.ofHours(1))
			.prefixKeysWith(prefix + ":")
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJacksonSerializer()))
//				.disableCachingNullValues()
			;
		
		// 对每个缓存空间应用不同的配置
		Map<String, RedisCacheConfiguration> configMap = redisCacheSupport.getConfigurations(defaultConfig);
		
		return RedisCacheManager.builder(factory)
			.cacheDefaults(defaultConfig)
			.initialCacheNames(configMap.keySet())// 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
			.withInitialCacheConfigurations(configMap).build();
	}
	
}