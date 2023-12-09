package mt.spring.jwt;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mt.spring.jwt.dto.ResResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

import static mt.spring.jwt.JwtAuthenticationTokenFilter.ATTR_IS_TOKEN;

/**
 * 自定义认证失败处理：没有登录或 token 过期时
 */
public class RestAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
	
	public RestAuthenticationEntryPoint() {
		super("/#/signin");
	}
	
	/**
	 * 当认证失败时，此方法会被调用
	 *
	 * @param request       请求对象
	 * @param response      响应对象
	 * @param authException 认证失败时抛出的异常
	 * @throws IOException      IO 异常
	 * @throws ServletException Servlet 异常
	 */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
		Object isToken = request.getAttribute(ATTR_IS_TOKEN);
		if (isToken == null || !Boolean.parseBoolean(isToken.toString())) {
			super.commence(request, response, authException);
		} else {
			response.setStatus(401);
			// 设置响应头，允许任何域进行跨域请求
			response.setHeader("Access-Control-Allow-Origin", "*");
			// 设置响应头，指示响应不应被缓存
			response.setHeader("Cache-Control", "no-cache");
			// 设置响应的字符编码为 UTF-8
			response.setCharacterEncoding("UTF-8");
			// 设置响应内容类型为 JSON
			response.setContentType("application/json");
			// 将认证失败的消息写入响应体
			ResResult<Object> resResult = ResResult.error(authException.getMessage());
			response.getWriter().println(JSONUtil.parse(resResult));
			// 刷新响应流，确保数据被发送
			response.getWriter().flush();
		}
	}
}
