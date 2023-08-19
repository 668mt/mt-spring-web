package mt.spring.tools.base.file;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.base.http.ServiceClient;
import mt.spring.tools.base.io.IOUtils;
import mt.spring.tools.base.io.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static mt.spring.tools.base.io.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2022/10/29
 */
@Slf4j
@Data
public class FastFileDownloader implements FileDownloader {
	private FastFileDownloaderHttpSupport httpExecutor;
	/**
	 * 最小的分片大小，单位byte，默认5MB
	 */
	private long minPartSize = 5 * MB;
	/**
	 * 最大的分片大小，单位byte，默认20MB
	 */
	private long maxPartSize = 20 * MB;
	/**
	 * 期望的分片数量
	 */
	private int expectChunks = 50;
	private ThreadPoolExecutor threadPoolExecutor;
	
	public FastFileDownloader(int threads) {
		this.threadPoolExecutor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
		this.httpExecutor = new HttpClientFastFileDownloaderHttpSupport(new ServiceClient());
	}
	
	public FastFileDownloader(int threads, FastFileDownloaderHttpSupport httpExecutor) {
		this.threadPoolExecutor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
		this.httpExecutor = httpExecutor;
	}
	
	public FastFileDownloader(FastFileDownloaderHttpSupport httpExecutor) {
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
		this.httpExecutor = httpExecutor;
		this.threadPoolExecutor = threadPoolExecutor;
	}
	
	public FastFileDownloader(FastFileDownloaderHttpSupport httpExecutor, ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
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
		RecordFile recordFile = null;
		if (length > minPartSize) {
			log.info("使用多线程下载");
			log.debug("临时文件路径：{}", FileUtils.getTempDirectoryPath());
			IOUtils.SplitResult splitResult = IOUtils.split(length, minPartSize, maxPartSize, expectChunks);
			recordFile = getRecordFile(desFile);
			log.debug("文件[{}]分片数：{}，分片大小：{}", name, splitResult.getChunks(), SizeUtils.getReadableSize(splitResult.getPartSize()));
			RecordFile finalRecordFile = recordFile;
			List<? extends Future<?>> futures = splitResult.getSplitParts().stream()
				.map(part -> getThreadPoolExecutor().submit(new DownloadTask(finalRecordFile, url, name, part, tempFile)))
				.collect(Collectors.toList());
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			log.info("使用单线程下载");
			try (InputStream content = httpExecutor.getInputStream(url, null);
				 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
				org.apache.commons.io.IOUtils.copyLarge(content, outputStream);
			}
		}
		if (desFile.exists()) {
			desFile.delete();
		}
		FileUtils.moveFile(tempFile, desFile);
		taskTimeWatch.end();
		long costMills = taskTimeWatch.getCostMills();
		log.info("{}下载完成，用时：{}，平均下载速度：{}", name, TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
		if (recordFile != null) {
			recordFile.clear();
		}
	}
	
	private File getTempFile(File desFile) {
		return new File(desFile.getPath() + ".tmp");
	}
	
	private PropertiesRecordFile getRecordFile(File desFile) {
		return new PropertiesRecordFile(new File(desFile.getPath() + ".rcd"));
	}
	
	/**
	 * 清理临时文件
	 *
	 * @param desFile
	 */
	@Override
	public void deleteTempFiles(@NotNull File desFile) {
		PropertiesRecordFile propertiesRecordFile = getRecordFile(desFile);
		propertiesRecordFile.clear();
		File tempFile = getTempFile(desFile);
		FileUtils.deleteQuietly(tempFile);
	}
	
	private static String getSpeed(long length, long costMills) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(costMills);
		if (seconds <= 0) {
			seconds = 1;
		}
		long speed = BigDecimal.valueOf(length).divide(BigDecimal.valueOf(seconds), 0, RoundingMode.HALF_UP).longValue();
		return SizeUtils.getReadableSize(speed) + "/s";
	}
	
	public class DownloadTask implements Runnable {
		private final String url;
		private final String name;
		private final IOUtils.SplitPart part;
		private final File tempFile;
		private final RecordFile recordFile;
		
		public DownloadTask(RecordFile recordFile, String url, String name, IOUtils.SplitPart part, File tempFile) {
			this.recordFile = recordFile;
			this.url = url;
			this.name = name;
			this.part = part;
			this.tempFile = tempFile;
		}
		
		@Override
		public void run() {
			if (recordFile.hasDownload(part.getIndex())) {
				log.info("part{}已经下载，跳过", part.getIndex());
				return;
			}
			RandomAccessFile randomAccessFile = null;
			InputStream inputStream = null;
			try {
				TaskTimeWatch taskTimeWatch = new TaskTimeWatch();
				taskTimeWatch.start();
				log.trace("[{}]下载分片{}...", name, part.getIndex());
				Map<String, String> headers = new HashMap<>();
				headers.put("Range", "bytes=" + part.getStart() + "-" + part.getEnd());
				inputStream = httpExecutor.getInputStream(url, headers);
				randomAccessFile = new RandomAccessFile(tempFile, "rw");
				randomAccessFile.seek(part.getStart());
				byte[] buffer = new byte[4096];
				int read;
				while ((read = inputStream.read(buffer)) != -1) {
					randomAccessFile.write(buffer, 0, read);
				}
				long length = part.getLength();
				taskTimeWatch.end();
				long costMills = taskTimeWatch.getCostMills();
				log.info("[{}]分片{}下载完成，用时：{},平均下载速度：{}", name, part.getIndex(), TimeUtils.getReadableTime(costMills), getSpeed(length, costMills));
				recordFile.finish(part.getIndex());
			} catch (IOException e) {
				throw new RuntimeException("下载" + name + "分片" + part.getIndex() + "失败", e);
			} finally {
				IOUtils.closeQuietly(randomAccessFile);
				IOUtils.closeQuietly(inputStream);
			}
		}
		
	}
}
