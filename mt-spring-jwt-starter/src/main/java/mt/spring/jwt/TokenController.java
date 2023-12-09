package mt.spring.jwt;

import mt.spring.jwt.dto.TokenDTO;
import mt.spring.jwt.dto.TokenRefreshDTO;
import mt.spring.jwt.dto.TokenResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Martin
 * @Date 2023/12/1
 */
@RequestMapping("/token")
@ResponseBody
@RestController
public class TokenController {
	private final TokenService tokenService;
	
	public TokenController(TokenService tokenService) {
		this.tokenService = tokenService;
	}
	
	@PostMapping("/apply")
	public TokenResult apply(@RequestBody @Validated TokenDTO tokenDTO) {
		return tokenService.applyToken(tokenDTO);
	}
	
	@PostMapping("/refresh")
	public TokenResult refresh(@RequestBody @Validated TokenRefreshDTO tokenRefreshDTO) {
		return tokenService.refreshToken(tokenRefreshDTO);
	}
}
