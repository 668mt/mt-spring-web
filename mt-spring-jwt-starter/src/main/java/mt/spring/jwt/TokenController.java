package mt.spring.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import mt.spring.jwt.dto.ResResult;
import mt.spring.jwt.dto.TokenDTO;
import mt.spring.jwt.dto.TokenRefreshDTO;
import mt.spring.jwt.dto.TokenResult;
import mt.spring.jwt.exception.TokenIsExpiredException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Martin
 * @Date 2023/12/1
 */
@RequestMapping("/token")
@ResponseBody
@RestController
@Slf4j
public class TokenController {
	private final TokenService tokenService;
	
	public TokenController(TokenService tokenService) {
		this.tokenService = tokenService;
	}
	
	@PostMapping("/apply")
	public TokenResult applyV1(@RequestBody @Validated TokenDTO tokenDTO) {
		return tokenService.applyToken(tokenDTO);
	}
	
	@PostMapping("/refresh")
	public TokenResult refreshV1(@RequestBody @Validated TokenRefreshDTO tokenRefreshDTO) throws TokenIsExpiredException {
		return tokenService.refreshToken(tokenRefreshDTO);
	}
	
	@PostMapping("/v2/apply")
	public ResResult<TokenResult> applyV2(@RequestBody @Validated TokenDTO tokenDTO) {
		try {
			return ResResult.success(tokenService.applyToken(tokenDTO));
		} catch (Exception e) {
			log.error("apply token failed:{}", e.getMessage(), e);
			return ResResult.error(e.getMessage());
		}
	}
	
	@PostMapping("/v2/refresh")
	public ResResult<TokenResult> refreshV2(@RequestBody @Validated TokenRefreshDTO tokenRefreshDTO, HttpServletResponse response) {
		try {
			return ResResult.success(tokenService.refreshToken(tokenRefreshDTO));
		} catch (Exception e) {
			String code = "0000";
			if (e instanceof TokenIsExpiredException || e instanceof ExpiredJwtException) {
				code = "0001";
			}
			log.error("refresh token failed:{}", e.getMessage(), e);
			ResResult<TokenResult> error = ResResult.error(e.getMessage());
			error.setCode(code);
			return error;
		}
	}
}
