package mt.common.currentUser;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @Author Martin
 * @Date 2019/8/24
 */
@Configuration
@ConditionalOnBean(UserContext.class)
public class CurrentUserWebMvcConfiguration {
	
	@Bean
	public WebMvcConfigurer currentUserWebMvcConfigurer(
		CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver,
		CurrentUserIdMethodArgumentResolver currentUserIdMethodArgumentResolver,
		CurrentUserNameMethodArgumentResolver currentUserNameMethodArgumentResolver,
		CurrentUserHandlerInterceptor currentUserHandlerInterceptor
	) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(@NotNull InterceptorRegistry registry) {
				registry.addInterceptor(currentUserHandlerInterceptor);
			}
			
			@Override
			public void addArgumentResolvers(@NotNull List<HandlerMethodArgumentResolver> resolvers) {
				resolvers.add(currentUserIdMethodArgumentResolver);
				resolvers.add(currentUserNameMethodArgumentResolver);
				resolvers.add(currentUserMethodArgumentResolver);
			}
		};
	}
}
