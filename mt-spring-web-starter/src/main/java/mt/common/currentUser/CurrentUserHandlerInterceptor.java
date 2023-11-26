package mt.common.currentUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CurrentUserHandlerInterceptor implements HandlerInterceptor {
	@Autowired(required = false)
	private UserContext userContext;
	/**
	 * "当前用户"属性名称
	 */
	private static final String currentUserAttributeName = "currentUser";
	private static final String currentUserIdAttributeName = "currentUserId";
	
	@Override
	public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) throws Exception {
		if (userContext != null) {
			request.setAttribute(currentUserAttributeName, userContext.getCurrentUser());
			request.setAttribute(currentUserIdAttributeName, userContext.getCurrentUserId());
		}
	}
	
}
