package mt.spring.jwt;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import mt.spring.jwt.dto.Token;
import mt.spring.jwt.dto.TokenResult;
import mt.spring.jwt.exception.TokenIsExpiredException;
import mt.utils.common.Assert;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTokenUtil {
	// Claim 中的用户名
	private static final String CLAM_KEY_USERDATA = "sub";
	private static final String CLAM_KEY_TYPE = "type";
	public static final String CLAM_KEY_TYPE_ACCESS = "access";
	public static final String CLAM_KEY_TYPE_REFRESH = "refresh";
	
	// Claim 中的创建时间
	private static final String CLAM_KEY_CREATED = "created";
	private final JwtProperties jwtProperties;
	
	public JwtTokenUtil(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}
	
	/**
	 * 根据负载生成 JWT 的 token
	 *
	 * @param claims     负载
	 * @param expiration 过期时间
	 * @return JWT 的 token
	 */
	private String generateToken(Map<String, Object> claims, Date expiration) {
		return Jwts.builder()
			.setClaims(claims)  // 设置负载
			.setExpiration(expiration)    // 设置过期时间
			.signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret())  // 设置签名使用的签名算法和签名使用的秘钥
			.compact();
	}
	
	/**
	 * 生成过期时间
	 *
	 * @param expireMinutes 过期时间（单位：分钟）
	 * @return 过期时间
	 */
	private Date generateExpirationDate(int expireMinutes) {
		return new Date(System.currentTimeMillis() + expireMinutes * 60_000L);
	}
	
	/**
	 * 从 token 中获取 JWT 中的负载
	 *
	 * @param token JWT 的 token
	 * @return JWT 中的负载
	 */
	private Claims getClaimsFromToken(String token) throws ExpiredJwtException {
		return Jwts.parser() // 解析 JWT 的 token
			.setSigningKey(jwtProperties.getSecret()) // 指定签名使用的密钥（会自动推断签名的算法）
			.parseClaimsJws(token) // 解析 JWT 的 token
			.getBody();
	}
	
	/**
	 * 验证 token 是否过期
	 *
	 * @param token JWT 的 token
	 * @return token 是否过期 true：过期 false：未过期
	 */
	private boolean isTokenExpired(String token) {
		Date expiredDate = getExpiredDateFromToken(token);
		return expiredDate.before(new Date());
	}
	
	/**
	 * 从 token 中获取过期时间
	 *
	 * @param token JWT 的 token
	 * @return 过期时间
	 */
	private Date getExpiredDateFromToken(String token) {
		return getClaimsFromToken(token).getExpiration();
	}
	
	/**
	 * 判断 token 是否可以被刷新
	 *
	 * @param token JWT 的 token
	 * @param time  指定时间段（单位：秒）
	 * @return token 是否可以被刷新 true：可以 false：不可以
	 */
	private boolean tokenRefreshJustBefore(String token, int time) {
		// 解析 JWT 的 token 拿到负载
		Claims claims = getClaimsFromToken(token);
		// 获取 token 的创建时间
		Date tokenCreateDate = claims.get(CLAM_KEY_CREATED, Date.class);
		// 获取当前时间
		Date refreshDate = new Date();
		// 条件1: 当前时间在 token 创建时间之后
		// 条件2: 当前时间在（token 创建时间 + 指定时间段）之前（即指定时间段内可以刷新 token）
		return refreshDate.after(tokenCreateDate) && refreshDate.before(DateUtil.offsetSecond(tokenCreateDate, time));
	}
	
	//================ public methods ==================
	
	/**
	 * 从 token 中获取登录用户名
	 *
	 * @param token JWT 的 token
	 * @return 登录用户名
	 */
	public String getUserDataFromToken(String token) {
		// 从 token 中获取 JWT 中的负载
		Claims claims = getClaimsFromToken(token);
		return claims.getSubject();
	}
	
	public boolean validateToken(String token, String userData) {
		// 从 token 中获取用户名
		String tokenUserData = getUserDataFromToken(token);
		// 条件一：用户名不为 null
		// 条件二：用户名和 UserDetails 中的用户名一致
		// 条件三：token 未过期
		return tokenUserData.equals(userData) && !isTokenExpired(token);
	}
	
	public TokenResult generateToken(@NotNull String userData) {
		// 创建负载
		Map<String, Object> baseClaims = new HashMap<>();
		// 设置负载中的用户名
		baseClaims.put(CLAM_KEY_USERDATA, userData);
		// 设置负载中的创建时间
		baseClaims.put(CLAM_KEY_CREATED, new Date());
		Map<String, Object> accessMap = new HashMap<>(baseClaims);
		accessMap.put(CLAM_KEY_TYPE, CLAM_KEY_TYPE_ACCESS);
		Map<String, Object> refreshMap = new HashMap<>(baseClaims);
		refreshMap.put(CLAM_KEY_TYPE, CLAM_KEY_TYPE_REFRESH);
		
		// 根据负载生成 token
		Date expire = generateExpirationDate(jwtProperties.getExpirationMinutes());
		Date refreshTokenExpire = generateExpirationDate(jwtProperties.getRefreshExpirationMinutes());
		String accessTokenString = generateToken(accessMap, expire);
		String refreshTokenString = generateToken(refreshMap, refreshTokenExpire);
		TokenResult tokenResult = new TokenResult();
		tokenResult.setAccessToken(new Token(accessTokenString, expire.getTime()));
		tokenResult.setRefreshToken(new Token(refreshTokenString, refreshTokenExpire.getTime()));
		return tokenResult;
	}
	
	/**
	 * 判断 token 是否可以被刷新
	 *
	 * @param oldRefreshToken JWT 的 token
	 * @return token 刷新后的token，如果不可以刷新则返回 null
	 */
	@NotNull
	public TokenResult refreshToken(@NotNull String oldRefreshToken) throws TokenIsExpiredException {
		Assert.notBlank(oldRefreshToken, "token不能为空");
		String token = oldRefreshToken.substring(jwtProperties.getTokenHead().length());
		Assert.notBlank(token, "token格式错误");
		Claims claims = getClaimsFromToken(oldRefreshToken);
		Assert.notNull(claims, "token格式错误");
		Assert.state(CLAM_KEY_TYPE_REFRESH.equals(claims.get(CLAM_KEY_TYPE)), "token格式错误");
		Assert.notBlank(claims.getSubject(), "token格式错误");
		Assert.state(!isTokenExpired(oldRefreshToken), "token已过期");
		// 设置负载中的创建时间
		claims.put(CLAM_KEY_CREATED, new Date());
		Object userData = claims.get(CLAM_KEY_USERDATA);
		Assert.notNull(userData, "token格式错误");
		//重新生成新的token
		return generateToken(userData.toString());
	}
}