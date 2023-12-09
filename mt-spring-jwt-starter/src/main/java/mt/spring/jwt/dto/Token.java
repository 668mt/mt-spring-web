package mt.spring.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Martin
 * @Date 2023/12/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
	private String token;
	private Long expiration;
}
