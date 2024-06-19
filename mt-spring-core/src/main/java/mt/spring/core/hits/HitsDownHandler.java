package mt.spring.core.hits;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public interface HitsDownHandler<SCOPE, KEY> {
	/**
	 * 处理落库
	 *
	 * @param hitsMap key为资源id，value为命中次数
	 */
	void doHitsDown(@NotNull SCOPE scope, @NotNull Map<KEY, Long> hitsMap);
}
