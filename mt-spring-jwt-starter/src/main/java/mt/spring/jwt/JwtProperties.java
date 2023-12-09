package mt.spring.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author Martin
 * @Date 2023/12/2
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
	private String secret = "1998be41-521d-4987-97e9-b204af11739f";
	private String tokenHeader = "Authorization";
	private String tokenHead = "Bearer";
	private Integer expirationHours = 2;
	private Integer refreshExpirationDays = 30;
}
