package mt.common.progress;

import org.jetbrains.annotations.NotNull;

public interface ProgressListener {
	/**
	 * 监听进度
	 *
	 * @param key
	 * @param percent
	 */
	void listen(@NotNull String key, double percent);
}