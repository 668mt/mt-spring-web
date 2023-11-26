package mt.common.currentUser;

import mt.common.annotation.CurrentUserId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Security - 当前用户MethodArgumentResolver
 */
@Component
public class CurrentUserIdMethodArgumentResolver implements HandlerMethodArgumentResolver {
	
	@Autowired(required = false)
	private UserContext userContext;
	
	/**
	 * 支持参数
	 *
	 * @param methodParameter MethodParameter
	 * @return 是否支持参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter methodParameter) {
		return methodParameter.hasParameterAnnotation(CurrentUserId.class);
	}
	
	/**
	 * 解析变量
	 *
	 * @param methodParameter       MethodParameter
	 * @param modelAndViewContainer ModelAndViewContainer
	 * @param nativeWebRequest      NativeWebRequest
	 * @param webDataBinderFactory  WebDataBinderFactory
	 * @return 变量
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object resolveArgument(@NotNull MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
		return userContext.getCurrentUserId();
	}
	
}