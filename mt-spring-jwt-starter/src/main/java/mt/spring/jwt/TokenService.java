package mt.spring.jwt;

import mt.spring.jwt.dto.TokenDTO;
import mt.spring.jwt.dto.TokenRefreshDTO;
import mt.spring.jwt.dto.TokenResult;
import mt.spring.jwt.service.JwtUserService;
import mt.utils.common.Assert;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Author Martin
 * @Date 2023/12/2
 */
public class TokenService {
	private final JwtTokenUtil jwtTokenUtil;
	private final JwtUserService jwtUserService;
	private final PasswordEncoder passwordEncoder;
	
	public TokenService(JwtTokenUtil jwtTokenUtil, JwtUserService jwtUserService, PasswordEncoder passwordEncoder) {
		this.jwtTokenUtil = jwtTokenUtil;
		this.jwtUserService = jwtUserService;
		this.passwordEncoder = passwordEncoder;
	}
	
	public TokenResult applyToken(@NotNull TokenDTO tokenDTO) {
		String username = tokenDTO.getUsername();
		String password = tokenDTO.getPassword();
		// 密码需要客户端加密后传递
		// 根据用户名从数据库中获取用户信息
		UserDetails userDetails;
		try {
			userDetails = jwtUserService.loadUserByUsernameWithoutCache(username);
		} catch (UsernameNotFoundException usernameNotFoundException) {
			throw new IllegalStateException("账号或密码错误");
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage());
		}
		// 进行密码匹配
		if (!passwordEncoder.matches(password, userDetails.getPassword())) {
			throw new IllegalStateException("账号或密码错误");
		}
		// 检查用户是否被禁用
		if (!userDetails.isEnabled()) {
			throw new IllegalStateException("用户已被禁用");
		}
		// 封装用户信息（由于使用 JWT 进行验证，这里不需要凭证）
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		// 将用户信息存储到 Security 上下文中，以便于 Security 进行权限验证
		SecurityContextHolder.getContext().setAuthentication(authentication);
		// 生成 token
		String userData = jwtUserService.getUserData(userDetails);
		return jwtTokenUtil.generateToken(userData);
	}
	
	public TokenResult refreshToken(@NotNull TokenRefreshDTO tokenRefreshDTO) {
		String refreshToken = tokenRefreshDTO.getRefreshToken();
		Assert.notBlank(refreshToken, "refreshToken不能为空");
		String userData = jwtTokenUtil.getUserDataFromToken(refreshToken);
		UserDetails userDetails = jwtUserService.loadUserByUserDataWithoutCache(userData);
		Assert.state(userDetails.isEnabled(), "用户已被禁用");
		return jwtTokenUtil.refreshToken(refreshToken);
	}
}
