package mt.spring.tools.base.file;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2022/10/29
 */
public interface FastFileDownloaderHttpSupport {
	/**
	 * 获取文件大小
	 *
	 * @param url 文件地址
	 * @return
	 */
	long getFileLength(@NotNull String url) throws IOException;
	
	/**
	 * 获取文件流
	 *
	 * @param url 文件地址
	 * @return
	 */
	InputStream getInputStream(@NotNull String url, @Nullable Map<String, String> headers) throws IOException;
}
