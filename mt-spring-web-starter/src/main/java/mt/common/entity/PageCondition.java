package mt.common.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2024/4/20
 */
@Data
public class PageCondition {
	private Integer pageNum;
	private Integer pageSize;
	
	public String getOrderBy() {
		return "id desc";
	}
	
	/**
	 * 是否允许查询所有
	 *
	 * @return 是否允许查询所有
	 */
	public boolean isAllowSelectAll() {
		return false;
	}
}
