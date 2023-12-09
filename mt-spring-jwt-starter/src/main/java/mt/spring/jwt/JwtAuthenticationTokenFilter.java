package mt.spring.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mt.spring.jwt.service.JwtUserService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 登录授权过滤器
 */
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);
	
	// 用户详细信息服务（用于从数据库中加载用户信息，需要自定义实现）
	// JWT 工具类
	private final JwtTokenUtil jwtTokenUtil;
	private final JwtProperties jwtProperties;
	private final JwtUserService jwtUserService;
	
	public JwtAuthenticationTokenFilter(JwtTokenUtil jwtTokenUtil, JwtProperties jwtProperties, JwtUserService jwtUserService) {
		this.jwtTokenUtil = jwtTokenUtil;
		this.jwtProperties = jwtProperties;
		this.jwtUserService = jwtUserService;
	}
	
	public static final String ATTR_IS_TOKEN = "isToken";
	
	/**
	 * 从请求中获取 JWT 令牌，并根据令牌获取用户信息，最后将用户信息封装到 Authentication 中，方便后续校验（只会执行一次）
	 *
	 * @param request     请求
	 * @param response    响应
	 * @param filterChain 过滤器链
	 * @throws ServletException Servlet 异常
	 * @throws IOException      IO 异常
	 */
	@Override
	protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
		// 从请求中获取 JWT 令牌的请求头（即：Authorization）
		String authHeader = request.getHeader(jwtProperties.getTokenHeader());
		String tokenHead = jwtProperties.getTokenHead();
		String accessToken = request.getParameter("accessToken");
		
		if (StringUtils.isBlank(authHeader) && StringUtils.isNotBlank(accessToken)) {
			authHeader = tokenHead + " " + accessToken;
		}
		
		// 如果请求头不为空，并且以 JWT 令牌前缀（即：Bearer）开头
		if (authHeader != null && authHeader.startsWith(tokenHead)) {
			request.setAttribute(ATTR_IS_TOKEN, true);
			// 获取 JWT 令牌的内容（即：去掉 JWT 令牌前缀后的内容）
			String authToken = authHeader.substring(tokenHead.length());
			try {
				// 从 JWT 令牌中获取用户名
				UserDetails userDetails = jwtUserService.loadUserByUserDataUseCache(jwtTokenUtil.getUserDataFromToken(authToken));
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				// 将请求中的详细信息（即：IP、SessionId 等）封装到 UsernamePasswordAuthenticationToken 对象中方便后续校验
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception e) {
				LOGGER.error("token校验失败：{}，{}", authToken, e.getMessage());
			}
		} else {
			request.setAttribute(ATTR_IS_TOKEN, false);
		}
		// 放行，执行下一个过滤器
		filterChain.doFilter(request, response);
	}
}
