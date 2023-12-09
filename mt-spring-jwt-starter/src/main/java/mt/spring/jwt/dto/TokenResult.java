package mt.spring.jwt.dto;

import lombok.Data;

@Data
public class TokenResult {
	private Token accessToken;
	private Token refreshToken;
}