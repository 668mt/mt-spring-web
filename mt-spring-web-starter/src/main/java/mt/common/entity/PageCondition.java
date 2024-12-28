package mt.common.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2024/4/20
 */
@Data
public class PageCondition implements Pageable {
	private Integer pageNum;
	private Integer pageSize;
	/**
	 * 排序类型
	 */
	private String orderType;
	
	public Map<String, String> getOrderTypeMapping() {
		return new HashMap<>();
	}
	
	public String getOrderBy() {
		Map<String, String> orderTypeMapping = getOrderTypeMapping();
		if (StringUtils.isNotBlank(orderType) && orderTypeMapping != null) {
			String orderBy = orderTypeMapping.get(orderType);
			if (StringUtils.isNotBlank(orderBy)) {
				return orderBy;
			}
		}
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
