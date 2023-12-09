package mt.spring.jwt.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @Author Martin
 * @Date 2023/12/4
 */
public interface JwtUserService {
	/**
	 * 根据用户数据加载用户信息
	 *
	 * @param userData 用户数据
	 * @return 用户信息，需要带上权限信息，否则会提示没有权限
	 */
	UserDetails loadUserByUserDataUseCache(@NotNull String userData);
	
	/**
	 * 根据用户名加载用户信息
	 *
	 * @param userData 用户数据
	 * @return 用户信息
	 */
	UserDetails loadUserByUserDataWithoutCache(@NotNull String userData);
	
	/**
	 * 根据用户名加载用户信息
	 *
	 * @param username 用户名
	 * @return 用户信息
	 */
	UserDetails loadUserByUsernameWithoutCache(@NotNull String username);
	
	/**
	 * 获取用户数据
	 *
	 * @param userDetails 用户信息
	 * @return 用户数据
	 */
	String getUserData(@NotNull UserDetails userDetails);
	
}
