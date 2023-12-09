package mt.spring.jwt;

import mt.spring.jwt.service.JwtUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Author Martin
 * @Date 2023/12/4
 */
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {
	@Bean
	public JwtTokenUtil jwtTokenUtil(JwtProperties jwtProperties) {
		return new JwtTokenUtil(jwtProperties);
	}
	
	@Bean
	public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(JwtTokenUtil jwtTokenUtil, JwtProperties jwtProperties, JwtUserService jwtUserService) {
		return new JwtAuthenticationTokenFilter(jwtTokenUtil, jwtProperties, jwtUserService);
	}
	
	@Bean
	public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
		return new RestAuthenticationEntryPoint();
	}
	
	@Bean
	public RestAccessDeniedHandler restfulAccessDeniedHandler() {
		return new RestAccessDeniedHandler();
	}
	
	@Bean
	@ConditionalOnMissingBean(PasswordEncoder.class)
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public TokenService tokenService(JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder, JwtUserService jwtUserService) {
		return new TokenService(jwtTokenUtil, jwtUserService, passwordEncoder);
	}
	
	@Bean
	@ConditionalOnMissingBean(TokenController.class)
	public TokenController tokenController(TokenService tokenService) {
		return new TokenController(tokenService);
	}
}
