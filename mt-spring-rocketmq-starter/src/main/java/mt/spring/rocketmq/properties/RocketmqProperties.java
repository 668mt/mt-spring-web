package mt.spring.rocketmq.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * @Author Martin
 * @Date 2024/3/19
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "rocketmq.config")
public class RocketmqProperties {
	private String topicPrefix;
	private String endPoints = "192.168.0.104:8081";
	private Boolean enableSsl = false;
	private String accessKey;
	private String accessSecret;
}
