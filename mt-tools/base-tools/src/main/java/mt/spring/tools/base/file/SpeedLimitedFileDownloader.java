package mt.spring.tools.base.file;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.base.http.ServiceClient;
import mt.spring.tools.base.io.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static mt.spring.tools.base.file.SpeedUtils.getSpeed;

/**
 * @Author Martin
 * @Date 2023/8/19
 */
@Slf4j
@Data
public class SpeedLimitedFileDownloader implements FileDownloader {
	private FastFileDownloaderHttpSupport httpExecutor;
	private long limitKbSeconds = -1;
	
	public SpeedLimitedFileDownloader() {
		this.httpExecutor = new HttpClientFastFileDownloaderHttpSupport(new ServiceClient());
	}
	
	public SpeedLimitedFileDownloader(FastFileDownloaderHttpSupport httpExecutor) {
		this.httpExecutor = httpExecutor;
	}
	
	@Override
	public void downloadFile(@NotNull String url, @NotNull File desFile) throws IOException {
		String name = desFile.getName();
		long length = httpExecutor.getFileLength(url);
		log.info("下载文件：dstFile:{},url:{}", desFile.getAbsolutePath(), url);
		File parentFile = desFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
		taskTimeWatch.start();
		File tempFile = getTempFile(desFile);
		try (InputStream inputStream = httpExecutor.getInputStream(url, null);
			 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
			if (limitKbSeconds > 0) {
				try (InputStream limitInputStream = new LimitInputStream(inputStream, limitKbSeconds)) {
					org.apache.commons.io.IOUtils.copyLarge(limitInputStream, outputStream);
				}
			} else {
				org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream);
			}
		}
		if (desFile.exists()) {
			desFile.delete();
		}
		FileUtils.moveFile(tempFile, desFile);
		taskTimeWatch.end();
		long costMills = taskTimeWatch.getCostMills();
		log.info("{}下载完成，用时：{}，平均下载速度：{}", name, TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
	}
	
	private File getTempFile(File desFile) {
		return new File(desFile.getPath() + ".tmp");
	}
	
	@Override
	public void deleteTempFiles(@NotNull File desFile) {
		File tempFile = getTempFile(desFile);
		FileUtils.deleteQuietly(tempFile);
	}
}
