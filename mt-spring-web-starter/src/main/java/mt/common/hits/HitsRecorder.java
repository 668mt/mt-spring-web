package mt.common.hits;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public interface HitsRecorder<SCOPE, KEY> {
	
	/**
	 * 记录点击量
	 *
	 * @param scope 范围
	 * @param key   资源
	 * @param hits  点击量
	 */
	void recordHits(@Nullable SCOPE scope, @NotNull KEY key, long hits);
	
	/**
	 * 记录点击量，scope为default
	 *
	 * @param key  资源
	 * @param hits 点击量
	 */
	default void recordHits(@NotNull KEY key, long hits) {
		recordHits(null, key, hits);
	}
	
	/**
	 * 点击量下发
	 */
	void hitsDown();
}
