package mt.spring.jwt.exception;

/**
 * @Author Martin
 * @Date 2024/4/23
 */
public class TokenIsExpiredException extends Exception {
	public TokenIsExpiredException() {
		super("令牌已过期");
	}
}
