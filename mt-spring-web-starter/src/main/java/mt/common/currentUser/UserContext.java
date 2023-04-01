package mt.common.currentUser;

/**
 * 用户上下文
 *
 * @Author Martin
 * @Date 2019/8/24
 */
public interface UserContext<ENTITY, ID> {
	/**
	 * 获取当前用户
	 *
	 * @return 当前用户
	 */
	ENTITY getCurrentUser();
	
	/**
	 * 获取当前用户id
	 *
	 * @return 当前用户id
	 */
	ID getCurrentUserId();
	
	/**
	 * 获取当前用户名称
	 *
	 * @return 当前用户名称
	 */
	String getCurrentUserName();
}
