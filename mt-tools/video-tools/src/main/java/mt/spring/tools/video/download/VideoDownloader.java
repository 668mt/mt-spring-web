package mt.spring.tools.video.download;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @Author Martin
 * @Date 2022/10/29
 */
public interface VideoDownloader {
	/**
	 * 下载mp4文件
	 *
	 * @param url                       视频地址
	 * @param desFile                   目标文件
	 * @param convertMp4                是否需要转换成mp4文件
	 * @param downloaderMessageListener 下载进度更新
	 */
	void downloadMp4(@NotNull String url, @NotNull File desFile, boolean convertMp4, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException;
	
	M3u8Info downloadM3u8Files(@NotNull String m3u8Url, @NotNull File path, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException;
	
	/**
	 * 下载m3u8地址
	 *
	 * @param m3u8Url                   m3u8地址
	 * @param desFile                   目标文件
	 * @param downloaderMessageListener 下载进度更新
	 * @throws IOException 异常
	 */
	void downloadM3u8(@NotNull String m3u8Url, @NotNull File desFile, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException;
	
	/**
	 * 下载m3u8地址
	 */
	default void shutdown() {
	}
}
