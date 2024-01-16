package mt.common.progress;

/**
 * 进度
 *
 * @Author Martin
 * @Date 2023/4/5
 */
public interface Progress {
	
	/**
	 * 初始化
	 */
	default void init() {
		update(0);
	}
	
	/**
	 * 更新进度
	 *
	 * @param percent 进度百分比
	 */
	void update(double percent);
	
	/**
	 * 新增进度
	 *
	 * @param percent 进度百分比
	 */
	void add(double percent);
	
	/**
	 * 完成
	 */
	default void finish() {
		update(1);
	}
	
	/**
	 * 移除进度
	 */
	void remove();
	
	/**
	 * 获取进度
	 *
	 * @return 进度百分比
	 */
	double getPercent();
}
