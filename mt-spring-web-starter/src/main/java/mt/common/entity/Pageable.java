package mt.common.entity;

/**
 * @Author Martin
 * @Date 2024/12/29
 */
public interface Pageable {
	Integer getPageNum();
	
	Integer getPageSize();
	
	String getOrderBy();
	
	boolean isAllowSelectAll();
}
