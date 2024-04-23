package mt.spring.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2023/12/2
 */
@Data
@ConfigurationProperties(prefix = "jwt")
@RefreshScope
public class JwtProperties {
	private String secret = "1998be41-521d-4987-97e9-b204af11739f";
	private String tokenHeader = "Authorization";
	private String tokenHead = "Bearer";
	private Integer expirationMinutes = 120;
	/**
	 * 默认30天
	 */
	private Integer refreshExpirationMinutes = 30 * 24 * 60;
}
