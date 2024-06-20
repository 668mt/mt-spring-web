package mt.spring.core.progress;

import org.jetbrains.annotations.NotNull;

/**
 * 进度
 *
 * @Author Martin
 * @Date 2023/4/5
 */
public interface ProgressService {
	
	/**
	 * 初始化
	 *
	 * @param key 进度key
	 */
	default void init(@NotNull String key) {
		update(key, 0);
	}
	
	/**
	 * 更新进度
	 *
	 * @param key     进度key
	 * @param percent 进度百分比
	 */
	void update(@NotNull String key, double percent);
	
	/**
	 * 新增进度
	 *
	 * @param key     进度key
	 * @param percent 进度百分比
	 */
	void add(@NotNull String key, double percent);
	
	/**
	 * 完成
	 *
	 * @param key
	 */
	default void finish(@NotNull String key) {
		update(key, 1);
	}
	
	/**
	 * 移除进度
	 *
	 * @param key 进度key
	 */
	void remove(@NotNull String key);
	
	/**
	 * 获取进度
	 *
	 * @param key 进度key
	 * @return 进度百分比
	 */
	double getPercent(@NotNull String key);
}
