package test.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import mt.common.redis.LockService;
import mt.common.redis.RedisService;
import mt.common.redis.RedisServiceImpl;
import org.junit.Before;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.File;
import java.io.IOException;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public class AbstractRedisTest {
	protected RedisTemplate<String, Object> redisTemplate;
	protected RedissonClient redissonClient;
	protected LockService lockService;
	protected RedisService redisService;
	
	@Before
	public void beforeTest() throws IOException {
		LoggingSystem.get(AbstractRedisTest.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		File file = new File("D:\\work\\redissonClient-local.yaml");
		Config config = Config.fromYAML(file);
		redissonClient = Redisson.create(config);
		lockService = new LockService(redissonClient);
		RedissonConnectionFactory redissonConnectionFactory = new RedissonConnectionFactory(redissonClient);
		redisTemplate = redisTemplate(redissonConnectionFactory);
		redisTemplate.setConnectionFactory(redissonConnectionFactory);
		redisService = new RedisServiceImpl(redisTemplate, lockService);
	}
	
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
	
}
