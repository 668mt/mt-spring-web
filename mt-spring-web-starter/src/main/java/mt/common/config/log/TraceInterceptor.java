package mt.common.config.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * @Author Martin
 * @Date 2023/9/15
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {
	
	@Override
	public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
		String traceId = request.getHeader("traceId");
		if (StringUtils.isBlank(traceId)) {
			traceId = TraceContext.create();
		}
		TraceContext.setTraceId(traceId);
		return true;
	}
	
	@Override
	public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
		TraceContext.removeTraceId();
	}
}
