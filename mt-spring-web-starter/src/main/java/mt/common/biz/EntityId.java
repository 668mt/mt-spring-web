package mt.common.biz;

/**
 * @Author Martin
 * @Date 2024/8/16
 */
public interface EntityId<T> {
	/**
	 * 获取id
	 *
	 * @return id
	 */
	T getId();
	
	/**
	 * 设置id
	 *
	 * @param id id
	 */
	void setId(T id);
}
