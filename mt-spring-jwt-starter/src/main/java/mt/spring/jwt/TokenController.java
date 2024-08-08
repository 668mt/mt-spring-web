package mt.spring.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import mt.spring.jwt.dto.ResResult;
import mt.spring.jwt.dto.TokenDTO;
import mt.spring.jwt.dto.TokenRefreshDTO;
import mt.spring.jwt.dto.TokenResult;
import mt.spring.jwt.exception.TokenIsExpiredException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

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
	public ResResult<TokenResult> apply(@RequestBody @Validated TokenDTO tokenDTO) {
		try {
			return ResResult.success(tokenService.applyToken(tokenDTO));
		} catch (Exception e) {
			log.error("apply token failed:{}", e.getMessage(), e);
			return ResResult.error(e.getMessage());
		}
	}
	
	@PostMapping("/refresh")
	public ResResult<TokenResult> refresh(@RequestBody @Validated TokenRefreshDTO tokenRefreshDTO, HttpServletResponse response) {
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
