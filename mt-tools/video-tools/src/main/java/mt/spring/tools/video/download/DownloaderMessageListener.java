package mt.spring.tools.video.download;


import org.jetbrains.annotations.NotNull;

/**
 * 任务更新
 */
public interface DownloaderMessageListener {
	/**
	 * 更新进度
	 *
	 * @param message
	 */
	void update(@NotNull String message);
}