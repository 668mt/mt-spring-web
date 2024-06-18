package mt.common.rank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Martin
 * @Date 2024/6/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankMember {
	private String member;
	private Double score;
}
