package mt.spring.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import mt.spring.core.fragment.TaskFragment;
import mt.spring.redis.service.LockService;
import mt.spring.redis.service.RedisTaskFragment;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
	public RedisCacheProperties redisCacheProperties() {
		return new RedisCacheProperties();
	}
	
	@Bean
	@ConditionalOnMissingBean(RedisTemplate.class)
	@SuppressWarnings("all")
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		// key采用String的序列化方式
		template.setKeySerializer(stringRedisSerializer);
		// hash的key也采用String的序列化方式
		template.setHashKeySerializer(stringRedisSerializer);
		// value序列化方式采用jackson
		template.setValueSerializer(jackson2JsonRedisSerializer);
		// hash的value序列化方式采用jackson
		template.setHashValueSerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}
	
	@Bean
	@ConditionalOnMissingBean(TaskFragment.class)
	public RedisTaskFragment redisTaskFragment(@Value("${spring.application.name:default}") String applicationName, ServerProperties serverProperties, RedisTemplate<String, Object> redisTemplate) {
		Integer port = serverProperties.getPort();
		return new RedisTaskFragment(applicationName, redisTemplate, RedisTaskFragment.getHostIp(null) + ":" + port);
	}
	
	@Bean
	@ConditionalOnBean(RedissonClient.class)
	public LockService lockService(RedissonClient redissonClient) {
		return new LockService(redissonClient);
	}
	
	@Bean
	@ConditionalOnBean(LockService.class)
	public RedisService redisService(RedisTemplate<String, Object> redisTemplate, LockService lockService) {
		return new RedisServiceImpl(redisTemplate, lockService);
	}
}
