package mt.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2024/12/29
 */
@Data
@NoArgsConstructor
public class PageDTO implements Pageable {
	private Integer pageNum;
	private Integer pageSize;
	private String orderBy;
	private boolean allowSelectAll;
	
	public PageDTO(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy) {
		this.pageNum = pageNum;
		this.pageSize = pageSize;
		this.orderBy = orderBy;
	}
	
	public PageDTO(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, boolean allowSelectAll) {
		this.pageNum = pageNum;
		this.pageSize = pageSize;
		this.orderBy = orderBy;
		this.allowSelectAll = allowSelectAll;
	}
}
