package mt.common.mybatis.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2024/7/25
 */
@Data
public class GroupCount {
	/**
	 * 分组字段
	 */
	private String field;
	/**
	 * 次数
	 */
	private Long value;
}
