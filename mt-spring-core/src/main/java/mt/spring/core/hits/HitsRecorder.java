package mt.spring.core.hits;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public interface HitsRecorder<SCOPE,KEY> {
	
	/**
	 * 记录点击量
	 *
	 * @param scope 范围
	 * @param key   资源
	 * @param hits  点击量
	 */
	void recordHits(@NotNull SCOPE scope, @NotNull KEY key, long hits);
	
	/**
	 * 点击量下发
	 */
	void hitsDown();
}
