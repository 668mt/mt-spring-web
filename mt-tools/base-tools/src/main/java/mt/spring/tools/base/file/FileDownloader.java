package mt.spring.tools.base.file;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @Author Martin
 * @Date 2023/8/19
 */
public interface FileDownloader {
	
	/**
	 * 下载文件
	 *
	 * @param url     文件地址
	 * @param desFile 目标文件
	 * @throws IOException IO异常
	 */
	void downloadFile(@NotNull String url, @NotNull File desFile) throws IOException;
	
	/**
	 * 删除临时文件
	 *
	 * @param desFile 目标文件
	 */
	void deleteTempFiles(@NotNull File desFile);
	
	/**
	 * 关闭
	 */
	default void shutdown(){};
}
