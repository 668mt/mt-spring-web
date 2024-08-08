package mt.spring.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import mt.spring.core.fragment.TaskFragment;
import mt.spring.core.progress.ProgressService;
import mt.spring.redis.service.*;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @Author Martin
 * @Date 2023/4/1
 */
@Import({WebRedisCacheConfiguration.class})
@ComponentScan("mt.spring.redis")
@EnableAutoConfiguration
public class WebRedisConfiguration {
	
	@Bean
	@ConditionalOnMissingBean(RedisTemplate.class)
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		// key采用String的序列化方式
		template.setKeySerializer(stringRedisSerializer);
		// hash的key也采用String的序列化方式
		template.setHashKeySerializer(stringRedisSerializer);
		// value序列化方式采用jackson
		template.setValueSerializer(createJacksonSerializer());
		// hash的value序列化方式采用jackson
		template.setHashValueSerializer(createJacksonSerializer());
		template.afterPropertiesSet();
		return template;
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
	@ConditionalOnMissingBean(TaskFragment.class)
	public RedisTaskFragment redisTaskFragment(RedisKeyService redisKeyService, ServerProperties serverProperties, RedisTemplate<String, Object> redisTemplate) {
		Integer port = serverProperties.getPort();
		return new RedisTaskFragment(redisKeyService.getPrefix(), redisTemplate, RedisTaskFragment.getHostIp(null) + ":" + port);
	}
	
	@Bean
	@ConditionalOnMissingBean(LockService.class)
	public LockService lockService(RedissonClient redissonClient) {
		return new LockService(redissonClient);
	}
	
	@Bean
	@ConditionalOnMissingBean(RedisService.class)
	public RedisService redisService(RedisKeyService redisKeyService, RedisTemplate<String, Object> redisTemplate, LockService lockService) {
		return new RedisServiceImpl(redisTemplate, lockService, redisKeyService.getPrefix());
	}
	
	@Bean
	@ConditionalOnMissingBean(ProgressService.class)
	public ProgressService progress(RedisService redisService) {
		return new RedisProgressService(redisService);
	}
}
