package mt.common.entity.dto;

import lombok.Data;
import mt.common.biz.EntityId;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
@Data
public class BaseDTO implements EntityId<Long> {
	private Long id;
}
